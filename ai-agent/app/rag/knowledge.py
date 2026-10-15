"""知识库加载与检索（RAG）。

应用启动时把 knowledge/ 下的 Markdown 文档按空行切段、向量化后写入内存向量库
（等价原 Java 的 SimpleVectorStore + QuestionAnswerAdvisor）。运行时通过 search_knowledge
工具做 topK 相似度检索，增强"预约规则/签到取消"等问答。

健壮性：加载与向量化（含网络调用）全程 try/except，失败仅告警、不阻断启动，
此时 AI 问答仍可用，只是缺少知识库增强。
"""
import glob
import os
import re

import httpx
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_core.tools import tool
from langchain_core.vectorstores import InMemoryVectorStore

from app.common.logger import logger
from app.core.config import settings

# DashScope 向量接口单次批量上限较小，分批写入更稳妥
_EMBED_BATCH = 10

_vector_store: InMemoryVectorStore | None = None


class DashScopeEmbeddings(Embeddings):
    """DashScope 向量接口自定义实现。

    langchain_openai.OpenAIEmbeddings 在较新版本中可能使用 OpenAI 新版
    input.contents 格式发送请求，DashScope 兼容接口不支持该格式，
    因此直接使用 httpx 调用以确保 input 为标准 str / list[str]。
    """

    def __init__(self, model: str, api_key: str, base_url: str):
        self.model = model
        self.api_key = api_key
        # 确保 base_url 以 /embeddings 结尾用于拼接
        self.base_url = base_url.rstrip("/")

    def _call_api(self, texts: list[str]) -> list[list[float]]:
        url = f"{self.base_url}/embeddings"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        # DashScope 兼容接口要求 input 为 str 或 list[str]
        payload = {"input": texts, "model": self.model}
        resp = httpx.post(url, json=payload, headers=headers, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        # 按 index 排序后提取 embedding 向量
        items = sorted(data["data"], key=lambda x: x["index"])
        return [item["embedding"] for item in items]

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return self._call_api(texts)

    def embed_query(self, text: str) -> list[float]:
        return self._call_api([text])[0]


def _embeddings() -> Embeddings:
    """构建 DashScope 向量模型客户端。"""
    return DashScopeEmbeddings(
        model=settings.EMBEDDING_MODEL,
        api_key=settings.DASHSCOPE_API_KEY,
        base_url=settings.DASHSCOPE_BASE_URL,
    )


def _split_paragraphs(content: str) -> list[str]:
    """按空行切分为段落，过滤过短片段，提升检索粒度（与 Java 版一致）。"""
    parts = re.split(r"\n\s*\n", content)
    return [p.strip() for p in parts if len(p.strip()) >= 10]


def load_knowledge_base() -> None:
    """启动时加载知识库到内存向量库。"""
    global _vector_store
    try:
        if not settings.DASHSCOPE_API_KEY:
            logger.warning("[RAG] 未配置 DASHSCOPE_API_KEY，跳过知识库加载（问答仍可用，无增强）")
            return

        pattern = os.path.join(settings.KNOWLEDGE_DIR, "*.md")
        files = sorted(glob.glob(pattern))
        if not files:
            logger.warning("[RAG] 未在 %s 下找到可用文档", settings.KNOWLEDGE_DIR)
            return

        documents: list[Document] = []
        for path in files:
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            source = os.path.basename(path)
            for text in _split_paragraphs(content):
                documents.append(Document(page_content=text, metadata={"source": source}))

        if not documents:
            logger.warning("[RAG] 文档切分后无有效片段")
            return

        store = InMemoryVectorStore(_embeddings())
        for i in range(0, len(documents), _EMBED_BATCH):
            batch = documents[i : i + _EMBED_BATCH]
            store.add_texts(
                [d.page_content for d in batch],
                metadatas=[d.metadata for d in batch],
            )
        _vector_store = store
        logger.info("[RAG] 知识库加载完成，共写入 %d 个文档片段", len(documents))
    except Exception as e:  # noqa: BLE001 - 加载失败不阻断启动
        logger.warning("[RAG] 知识库加载失败（AI 问答仍可用，但无知识库增强）：%s", e)


@tool
def search_knowledge(query: str) -> str:
    """检索自习室预约规则、签到与取消等知识库资料。
    当用户询问"预约规则/多久不签到会被取消/能否代预约/名额已满怎么办"等政策性问题时调用，
    依据检索到的资料作答，不要编造规则。"""
    if _vector_store is None:
        return "知识库暂不可用，请依据通用预约常识谨慎回答，或引导用户查看系统页面。"
    try:
        docs = _vector_store.similarity_search(query, k=settings.RAG_TOP_K)
    except Exception as e:  # noqa: BLE001
        logger.warning("[RAG] 检索失败：%s", e)
        return "知识库检索失败，请依据通用预约常识谨慎回答。"
    if not docs:
        return "未检索到相关知识库资料。"
    return "\n\n".join(
        f"[{d.metadata.get('source', 'knowledge')}] {d.page_content}" for d in docs
    )
