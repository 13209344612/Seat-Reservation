"""JWT 鉴权依赖。

Java 后端用 jjwt 生成 token：subject=userId、claim role=角色，签名算法 HS256，
密钥为 jwt.secret（Base64 串）解码后的原始字节。此处用同一密钥校验，解析出 userId/role，
使 Python Agent 复用现有登录态；校验失败一律返回 401。

安全要点：userId 只来自服务端校验后的 token，绝不接受客户端显式传入，
后续工具调用据此注入身份，杜绝越权。
"""
import base64
from dataclasses import dataclass

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.config import settings

# auto_error=False：自行处理缺失/非法凭证，返回统一的 401 文案
_bearer = HTTPBearer(auto_error=False)


@dataclass
class Principal:
    """当前登录用户身份。"""

    user_id: int
    role: str


def _signing_key() -> bytes:
    """把 Base64 编码的 JWT_SECRET 解码为原始密钥字节（与 Java Keys.hmacShaKeyFor 一致）。"""
    return base64.b64decode(settings.JWT_SECRET)


async def current_principal(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> Principal:
    """FastAPI 依赖：校验 Bearer JWT，返回当前用户身份。"""
    if credentials is None or not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="未提供认证凭证"
        )
    token = credentials.credentials
    try:
        payload = jwt.decode(token, _signing_key(), algorithms=["HS256"])
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="登录已过期或无效，请重新登录"
        )

    sub = payload.get("sub")
    if sub is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="无效的 token")
    try:
        user_id = int(sub)
    except (TypeError, ValueError):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="无效的 token")

    return Principal(user_id=user_id, role=payload.get("role", "USER"))
