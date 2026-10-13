"""Java 业务底座内部接口客户端。

统一封装对 Java `/internal/**` 的调用：所有请求携带服务间鉴权头 `X-Internal-Token`，
并按 userId 显式传参。Java 侧返回统一的 Result 结构 {code, message, data}：
- code==200 取 data；
- code!=200（业务失败，如"请勿重复预约"/"名额已满"）抛 JavaBusinessError，
  由上层工具捕获转成友好文案回传给大模型，避免中断整轮对话。
"""
from typing import Any, Optional

import httpx

from app.core.config import settings


class JavaBusinessError(Exception):
    """Java 业务层返回的失败（Result.code != 200）。"""

    def __init__(self, message: str, code: int = 400):
        super().__init__(message)
        self.message = message
        self.code = code


class JavaInternalClient:
    """对 Java 内部接口的薄封装。"""

    def __init__(self, base_url: str, token: str, timeout: float = 15.0):
        self._base_url = base_url.rstrip("/")
        self._headers = {"X-Internal-Token": token}
        self._timeout = timeout

    # ---- 内部工具 ----
    def _client(self) -> httpx.Client:
        return httpx.Client(
            base_url=self._base_url, headers=self._headers, timeout=self._timeout
        )

    @staticmethod
    def _unwrap(resp: httpx.Response) -> Any:
        """解析 Result 结构，成功返回 data，失败抛 JavaBusinessError。"""
        if resp.status_code != 200:
            raise JavaBusinessError(f"内部接口 HTTP {resp.status_code}", resp.status_code)
        body = resp.json()
        code = body.get("code")
        if code != 200:
            raise JavaBusinessError(body.get("message") or "业务处理失败", code or 400)
        return body.get("data")

    # ---- 业务方法（对应原 ReservationTools 的 5 个工具）----
    def list_rooms(self) -> list[dict]:
        with self._client() as c:
            return self._unwrap(c.get("/internal/rooms")) or []

    def query_availability(self, room_id: int, date: str) -> list[dict]:
        with self._client() as c:
            return self._unwrap(
                c.get(f"/internal/rooms/{room_id}/availability", params={"date": date})
            ) or []

    def list_my_reservations(self, user_id: int) -> list[dict]:
        with self._client() as c:
            return self._unwrap(c.get(f"/internal/users/{user_id}/reservations")) or []

    def create_reservation(
        self, user_id: int, room_id: int, time_slot_id: int, reservation_date: str
    ) -> dict:
        payload = {
            "roomId": room_id,
            "timeSlotId": time_slot_id,
            "reservationDate": reservation_date,
        }
        with self._client() as c:
            return self._unwrap(
                c.post("/internal/reservations", params={"userId": user_id}, json=payload)
            )

    def cancel_reservation(self, user_id: int, reservation_id: int) -> Optional[dict]:
        with self._client() as c:
            return self._unwrap(
                c.post(
                    f"/internal/reservations/{reservation_id}/cancel",
                    params={"userId": user_id},
                )
            )


# 全局单例（配置来自环境变量）
java_client = JavaInternalClient(
    base_url=settings.JAVA_INTERNAL_BASE_URL,
    token=settings.INTERNAL_API_TOKEN,
)
