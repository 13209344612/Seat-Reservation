"""应用入口：FastAPI + Uvicorn。

- 挂载 /api/ai 路由（保持前端契约）；
- 启动时加载 RAG 知识库（失败不阻断启动）；
- 允许前端跨域访问（开发环境）。
"""
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.ai import router as ai_router
from app.common.logger import logger
from app.core.config import settings
from app.rag.knowledge import load_knowledge_base


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动：加载知识库到内存向量库（内部已 try/except，失败仅告警）
    logger.info("AI Agent 启动中，加载知识库……")
    load_knowledge_base()
    logger.info("AI Agent 就绪，监听端口 %s", settings.PORT)
    yield
    logger.info("AI Agent 关闭")


app = FastAPI(
    title="Seat Reservation AI Agent",
    description="校园自习室预约助手 —— Python Agent 主线（LangChain/LangGraph + DashScope）",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(ai_router)


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.PORT, reload=False)
