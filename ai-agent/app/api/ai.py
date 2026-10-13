"""AI 助手路由 —— 保持与前端 api/ai.js 完全一致的接口契约。

前端不改动，故此处路径、字段名、SSE 帧格式均沿用原 Java AiAssistantController/
AiConversationController 的约定：
- SSE：数据帧 "data: <文本>\n\n"（多行拆成多个 data: 行，前端按 \n 重组），
  结束帧 "event: done\ndata: [DONE]\n\n"，错误帧 "event: error\ndata: <文案>\n\n"；
- 其余接口返回 {code, message, data} 结构（前端拦截器据 code!=200 判错）。

所有端点经 JWT 鉴权，userId 来自校验后的 token；会话以 thread_id=userId:conversationId 隔离。
"""
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.agents.reservation_agent import (
    build_thread_id,
    chat_sync,
    clear_messages,
    get_messages,
    list_threads,
    stream_chat,
)
from app.common.logger import logger
from app.models.schemas import ChatRequest
from app.security.jwt_auth import Principal, current_principal

router = APIRouter(prefix="/api/ai", tags=["AI 助手"])


def _sse_data(text: str) -> str:
    """把文本封装为 SSE data 帧；多行内容拆成多个 data: 行（前端会用 \n 重组）。"""
    if text == "":
        return ""
    lines = text.split("\n")
    return "".join(f"data: {line}\n" for line in lines) + "\n"


def _resolve_conversation_id(conversation_id: str | None, user_id: int) -> str:
    return conversation_id if conversation_id and conversation_id.strip() else f"user-{user_id}"


@router.post("/chat/stream")
async def chat_stream(
    request: ChatRequest,
    principal: Principal = Depends(current_principal),
):
    """流式对话（SSE），实现前端打字机效果。"""
    conversation_id = _resolve_conversation_id(request.conversationId, principal.user_id)
    thread_id = build_thread_id(principal.user_id, conversation_id)

    def event_generator():
        # 同步生成器：Starlette 会在线程池中迭代，避免阻塞事件循环
        try:
            for text in stream_chat(request.message, thread_id, principal.user_id):
                frame = _sse_data(text)
                if frame:
                    yield frame
            yield "event: done\ndata: [DONE]\n\n"
        except Exception as e:  # noqa: BLE001 - 以 error 帧返回，保持 SSE 契约
            logger.error("[AI] 流式对话失败：%s", e)
            yield "event: error\ndata: AI 助手暂时不可用，请稍后重试\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/chat")
def chat(
    request: ChatRequest,
    principal: Principal = Depends(current_principal),
):
    """同步对话：一次性返回完整回答（保留 API 兼容，前端 UI 未使用）。"""
    conversation_id = _resolve_conversation_id(request.conversationId, principal.user_id)
    thread_id = build_thread_id(principal.user_id, conversation_id)
    try:
        answer = chat_sync(request.message, thread_id, principal.user_id)
        return {"code": 200, "message": "操作成功", "data": answer}
    except Exception as e:  # noqa: BLE001
        logger.error("[AI] 同步对话失败：%s", e)
        return {"code": 500, "message": "AI 助手暂时不可用，请稍后重试", "data": None}


@router.get("/conversations")
def conversations(principal: Principal = Depends(current_principal)):
    """列出当前用户的历史会话（按最后活跃时间倒序）。"""
    data = list_threads(principal.user_id)
    return {"code": 200, "message": "操作成功", "data": data}


@router.get("/conversations/{conversation_id}/messages")
def conversation_messages(
    conversation_id: str,
    principal: Principal = Depends(current_principal),
):
    """回溯某会话的全部消息（按时间正序）。"""
    thread_id = build_thread_id(principal.user_id, conversation_id)
    data = get_messages(principal.user_id, thread_id)
    return {"code": 200, "message": "操作成功", "data": data}


@router.delete("/conversations/{conversation_id}")
def delete_conversation(
    conversation_id: str,
    principal: Principal = Depends(current_principal),
):
    """删除某会话及其消息（幂等）。"""
    thread_id = build_thread_id(principal.user_id, conversation_id)
    clear_messages(principal.user_id, thread_id)
    return {"code": 200, "message": "删除成功", "data": None}
