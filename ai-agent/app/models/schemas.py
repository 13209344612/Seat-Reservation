"""请求/响应数据模型。"""
from typing import Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    """对话请求体（与前端 api/ai.js 保持一致）。"""

    message: str = Field(..., description="用户提问内容")
    # 前端字段名为 conversationId；缺省时按用户维度生成默认会话
    conversationId: Optional[str] = Field(default=None, description="会话ID（可选）")
