"""校园自习室预约助手 Agent。


多用户隔离：thread_id = f"{userId}:{conversationId}"，userId 来自服务端校验的 JWT，
历史列表只扫描当前用户前缀，用户无法读写他人会话。
"""
import os
import sqlite3
from datetime import date

from langchain.agents import create_agent
from langchain.chat_models import init_chat_model
from langchain_core.messages import AIMessage, HumanMessage
from langgraph.checkpoint.sqlite import SqliteSaver

from app.common.logger import logger
from app.core.config import settings
from app.rag.knowledge import search_knowledge
from app.tools.reservation_tools import RESERVATION_TOOLS

# ---- 系统提示词（迁移自 Java ChatClientConfig.SYSTEM_PROMPT，工具名对齐 Python 实现）----
_BASE_SYSTEM_PROMPT = """你是"校园自习室座位预约助手"，服务于一个大学自习室座位预约系统。请用简洁、友好的中文回答。

你的能力：
1. 查询自习室列表及各时段剩余座位（调用 list_rooms / query_availability 工具，禁止编造余量）。
2. 查询当前用户的预约记录（调用 list_my_reservations 工具）。
3. 帮当前用户创建预约（调用 create_reservation 工具）。
4. 帮当前用户取消预约（调用 cancel_reservation 工具）。
5. 回答自习室使用规则、预约政策等问题（调用 search_knowledge 工具检索知识库后作答）。

行为准则：
- 创建预约需要三项信息：自习室ID(room_id)、时段ID(time_slot_id)、预约日期(yyyy-MM-dd)。信息不全时先向用户追问，或用 list_rooms 查询后引导用户选择，不要臆测ID。
- 只能预约今天或未来的日期。
- 涉及数据查询和写操作时必须调用工具获取真实结果，并如实转达工具返回的信息（包括失败原因），不得编造。
- 用户身份由系统安全注入，你无需也无法指定他人身份，只能操作当前登录用户的数据。"""


def _system_prompt_with_date() -> str:
    """拼接「当前日期」，使"今天/明天"等相对日期能被正确换算（等价 Java systemPromptWithDate）。"""
    today = date.today().isoformat()
    return (
        _BASE_SYSTEM_PROMPT
        + f"\n\n【当前日期】今天是 {today}（yyyy-MM-dd）。"
        + "用户说「今天/现在/明天/后天」等相对日期时，一律以该日期为基准换算成具体日期后再调用工具；"
        + "除非用户明确指定其它日期，否则不要反问日期、也不要编造日期。"
    )


# ---- 模型 ----
model = init_chat_model(
    model=settings.CHAT_MODEL,
    model_provider="openai",
    base_url=settings.DASHSCOPE_BASE_URL,
    api_key=settings.DASHSCOPE_API_KEY,
    temperature=settings.TEMPERATURE,
)

# ---- 记忆：LangGraph SQLite checkpointer（自动建表）----
os.makedirs(os.path.dirname(settings.DB_PATH) or ".", exist_ok=True)
checkpointer = SqliteSaver(sqlite3.connect(settings.DB_PATH, check_same_thread=False))
checkpointer.setup()

_TOOLS = [*RESERVATION_TOOLS, search_knowledge]

# 按「当天日期」缓存 agent：系统提示词含当天日期，跨天自动重建，避免每请求重建开销
_agent_cache: dict = {"date": None, "agent": None}


def _get_agent():
    today = date.today().isoformat()
    if _agent_cache["agent"] is None or _agent_cache["date"] != today:
        _agent_cache["agent"] = create_agent(
            model=model,
            tools=_TOOLS,
            checkpointer=checkpointer,
            system_prompt=_system_prompt_with_date(),
        )
        _agent_cache["date"] = today
    return _agent_cache["agent"]


def build_thread_id(user_id: int, conversation_id: str | None) -> str:
    """thread_id = userId:conversationId；conversationId 缺省时按用户维度生成默认会话。"""
    cid = conversation_id if conversation_id and conversation_id.strip() else f"user-{user_id}"
    return f"{user_id}:{cid}"


def _config(thread_id: str, user_id: int) -> dict:
    return {"configurable": {"thread_id": thread_id, "userId": user_id}}


# ---- 流式对话 ----
def stream_chat(message: str, thread_id: str, user_id: int):
    """调用 agent 流式生成回答，逐块 yield 文本（仅取 AI 文本增量）。

    为同步生成器：agent.stream 本身阻塞，交由 Starlette 在线程池迭代，
    避免阻塞事件循环（对齐 demo 写法）。
    """
    logger.info("[用户 %s] %s (thread=%s)", user_id, message, thread_id)
    agent = _get_agent()
    try:
        for chunk, _metadata in agent.stream(
            {"messages": [HumanMessage(content=message)]},
            _config(thread_id, user_id),
            stream_mode="messages",
        ):
            from langchain_core.messages import AIMessageChunk

            if isinstance(chunk, AIMessageChunk) and chunk.content:
                # content 可能是 str 或多模态 list，这里只取文本
                if isinstance(chunk.content, str):
                    yield chunk.content
                elif isinstance(chunk.content, list):
                    for part in chunk.content:
                        if isinstance(part, dict) and part.get("type") == "text":
                            yield part.get("text", "")
    except Exception as e:  # noqa: BLE001
        logger.error("[AI] 流式对话失败：%s", e)
        raise


def chat_sync(message: str, thread_id: str, user_id: int) -> str:
    """同步对话：一次性返回完整回答。"""
    agent = _get_agent()
    result = agent.invoke(
        {"messages": [HumanMessage(content=message)]},
        _config(thread_id, user_id),
    )
    messages = result.get("messages", [])
    # 取最后一条 AI 消息作为回答
    for msg in reversed(messages):
        if isinstance(msg, AIMessage) and msg.content:
            return msg.content if isinstance(msg.content, str) else str(msg.content)
    return ""


# ---- 历史会话（读同一 checkpoint 库，仅当前用户）----
def _title_from_messages(channel_values: dict) -> str:
    for msg in channel_values.get("messages", []):
        if not isinstance(msg, HumanMessage) or not msg.content:
            continue
        if isinstance(msg.content, str):
            return msg.content
        return "".join(
            part.get("text", "") for part in msg.content if isinstance(part, dict)
        )
    return ""


def list_threads(user_id: int, limit: int = 50) -> list[dict]:
    """列出当前用户的历史会话（conversationId/title/updateTime），按更新时间倒序。"""
    prefix = f"{user_id}:"
    try:
        with checkpointer.cursor(transaction=False) as cur:
            cur.execute("SELECT DISTINCT thread_id FROM checkpoints")
            thread_ids = [row[0] for row in cur.fetchall() if str(row[0]).startswith(prefix)]
    except Exception as e:  # noqa: BLE001
        logger.warning("[AI] 查询会话列表失败：%s", e)
        return []

    conversations = []
    for thread_id in thread_ids:
        tup = checkpointer.get_tuple({"configurable": {"thread_id": thread_id}})
        if not tup:
            continue
        channel_values = tup.checkpoint.get("channel_values") or {}
        title = _title_from_messages(channel_values)
        if not title:
            continue
        conversations.append(
            {
                "conversationId": thread_id[len(prefix) :],
                "title": title.strip().replace("\n", " ")[:24],
                "createTime": tup.checkpoint.get("ts", ""),
                "updateTime": tup.checkpoint.get("ts", ""),
            }
        )
    conversations.sort(key=lambda c: c["updateTime"], reverse=True)
    return conversations[:limit]


def get_messages(user_id: int, thread_id: str) -> list[dict]:
    """回溯某会话的全部消息（按时间正序）。"""
    checkpoint = checkpointer.get({"configurable": {"thread_id": thread_id}})
    if not checkpoint:
        return []
    channel_values = checkpoint.get("channel_values")
    if not channel_values:
        return []
    result = []
    for msg in channel_values.get("messages", []):
        if not msg.content:
            continue
        content = msg.content if isinstance(msg.content, str) else str(msg.content)
        if isinstance(msg, HumanMessage):
            result.append({"role": "user", "content": content, "createTime": None})
        elif isinstance(msg, AIMessage):
            result.append({"role": "assistant", "content": content, "createTime": None})
    return result


def clear_messages(user_id: int, thread_id: str) -> None:
    """删除某会话及其消息（幂等）。"""
    logger.info("[AI] 清空历史消息，user=%s thread=%s", user_id, thread_id)
    try:
        checkpointer.delete_thread(thread_id)
    except Exception as e:  # noqa: BLE001
        logger.warning("[AI] 删除会话失败：%s", e)
