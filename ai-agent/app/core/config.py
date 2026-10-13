"""全局配置：从环境变量读取（配合 .env / docker-compose 注入）。

所有敏感项（DashScope Key、JWT 密钥、内部接口 token）一律走环境变量，禁止硬编码。
"""
import os

from dotenv import load_dotenv

# 加载项目根目录下的 .env（若存在）
load_dotenv()


class Settings:
    """应用配置项集中管理。"""

    # ---- 大模型（DashScope OpenAI 兼容接口）----
    DASHSCOPE_API_KEY: str = os.getenv("DASHSCOPE_API_KEY", "")
    DASHSCOPE_BASE_URL: str = os.getenv(
        "DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"
    )
    CHAT_MODEL: str = os.getenv("CHAT_MODEL", "qwen-plus")
    EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "text-embedding-v3")
    TEMPERATURE: float = float(os.getenv("TEMPERATURE", "0.7"))

    # ---- 鉴权：与 Java 后端共用同一 JWT 密钥（Base64 编码，HS256）----
    JWT_SECRET: str = os.getenv("JWT_SECRET", "")

    # ---- 调用 Java 业务底座内部接口 ----
    JAVA_INTERNAL_BASE_URL: str = os.getenv("JAVA_INTERNAL_BASE_URL", "http://localhost:8080")
    INTERNAL_API_TOKEN: str = os.getenv("INTERNAL_API_TOKEN", "")

    # ---- 会话记忆（LangGraph SQLite checkpointer）----
    DB_PATH: str = os.getenv("DB_PATH", "db/reservation_agent.db")

    # ---- 知识库（RAG）----
    KNOWLEDGE_DIR: str = os.getenv("KNOWLEDGE_DIR", "knowledge")
    RAG_TOP_K: int = int(os.getenv("RAG_TOP_K", "4"))

    # ---- 服务端口 ----
    PORT: int = int(os.getenv("PORT", "8000"))


settings = Settings()
