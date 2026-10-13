"""预约工具集 —— 把 Java 业务底座的能力以 @tool 暴露给大模型进行 Tool Calling。

安全设计（与原 Java ReservationTools 一致）：
- 所有涉及用户身份的操作，userId 一律从服务端注入的 RunnableConfig.configurable 读取
  （由后端在校验 JWT 后写入），绝不作为大模型可填写的参数，从根本上杜绝越权。
  LangChain 会自动把 RunnableConfig 类型的参数排除在工具入参 schema 之外。
- 写操作（创建/取消预约）内部捕获异常并返回友好文案，避免工具抛错中断整轮对话，
  让模型能把失败原因如实转达给用户。
"""
import json

from langchain_core.runnables import RunnableConfig
from langchain_core.tools import tool

from app.common.logger import logger
from app.tools.java_client import JavaBusinessError, java_client


def _require_user_id(config: RunnableConfig) -> int:
    """从服务端注入的运行时配置中取出当前登录用户ID；缺失即拒绝，杜绝越权。"""
    configurable = (config or {}).get("configurable", {})
    user_id = configurable.get("userId")
    if user_id is None:
        raise ValueError("缺少用户身份上下文")
    return int(user_id)


@tool
def list_rooms(config: RunnableConfig) -> str:
    """查询所有自习室列表，包含每个自习室的名称、总容量，以及各时段（含时段ID、开始/结束时间）。
    用于回答"有哪些自习室/某自习室的时段"等问题。
    注意：座位余量按日期+时段区分，需调用 query_availability 查询具体余量。"""
    _require_user_id(config)
    data = java_client.list_rooms()
    return json.dumps(data, ensure_ascii=False)


@tool
def query_availability(room_id: int, date: str, config: RunnableConfig) -> str:
    """查询某自习室在指定日期下各时段的座位余量（总容量与剩余可预约数）。
    需要自习室ID(room_id)、日期(date, 格式 yyyy-MM-dd)。
    用于回答"某天某自习室还有多少空位/哪个时段还有位置"等问题。"""
    _require_user_id(config)
    data = java_client.query_availability(room_id, date)
    return json.dumps(data, ensure_ascii=False)


@tool
def list_my_reservations(config: RunnableConfig) -> str:
    """查询当前登录用户的全部预约记录（含预约ID、自习室、日期、时段、状态）。
    用于回答"我的预约/我约了哪些"等问题。"""
    user_id = _require_user_id(config)
    data = java_client.list_my_reservations(user_id)
    return json.dumps(data, ensure_ascii=False)


@tool
def create_reservation(
    room_id: int, time_slot_id: int, reservation_date: str, config: RunnableConfig
) -> str:
    """为当前登录用户创建一个座位预约。
    需要自习室ID(room_id)、时段ID(time_slot_id)、预约日期(reservation_date, 格式 yyyy-MM-dd，只能是今天或未来)。
    成功返回预约详情，失败返回原因。"""
    user_id = _require_user_id(config)
    try:
        resp = java_client.create_reservation(
            user_id, room_id, time_slot_id, reservation_date
        )
        return (
            f"预约成功：预约ID={resp.get('id')}，{resp.get('roomName')}，"
            f"{resp.get('reservationDate')}，{resp.get('startTime')}-{resp.get('endTime')}，"
            f"状态={resp.get('status')}"
        )
    except JavaBusinessError as e:
        logger.warning("[AI工具] 创建预约失败：%s", e.message)
        return "预约失败：" + e.message
    except Exception as e:  # noqa: BLE001 - 网络等异常也转友好文案，不中断对话
        logger.warning("[AI工具] 创建预约异常：%s", e)
        return "预约失败：服务暂时不可用，请稍后重试"


@tool
def cancel_reservation(reservation_id: int, config: RunnableConfig) -> str:
    """取消当前登录用户的某个预约。需要预约ID(reservation_id)。
    仅 booked(已预约) 状态可取消。成功或失败都会返回说明。"""
    user_id = _require_user_id(config)
    try:
        java_client.cancel_reservation(user_id, reservation_id)
        return f"取消成功：预约ID={reservation_id} 已取消。"
    except JavaBusinessError as e:
        logger.warning("[AI工具] 取消预约失败：%s", e.message)
        return "取消失败：" + e.message
    except Exception as e:  # noqa: BLE001
        logger.warning("[AI工具] 取消预约异常：%s", e)
        return "取消失败：服务暂时不可用，请稍后重试"


# 供 agent 注册的工具列表
RESERVATION_TOOLS = [
    list_rooms,
    query_availability,
    list_my_reservations,
    create_reservation,
    cancel_reservation,
]
