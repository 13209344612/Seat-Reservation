package com.campus.seatreservation.ai.tool;

import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.dto.SlotAvailability;
import com.campus.seatreservation.service.ReservationService;
import com.campus.seatreservation.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约工具集 —— 通过 @Tool 把现有业务 Service 暴露给大模型进行 Tool Calling。
 *
 * 安全设计：所有涉及用户身份的操作，userId 一律从 ToolContext 读取（由后端在调用时注入当前登录用户），
 * 绝不作为大模型可填写的参数，从根本上杜绝越权（AI 无法伪造他人身份操作数据）。
 *
 * 写操作（创建/取消预约）内部捕获异常并返回友好文案，避免工具抛错中断整轮对话，
 * 让模型能把失败原因如实转达给用户。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTools {

    private final StudyRoomService studyRoomService;
    private final ReservationService reservationService;

    /** 从 ToolContext 中提取当前登录用户ID */
    private Long currentUserId(ToolContext toolContext) {
        Object uid = toolContext.getContext().get("userId");
        if (uid == null) {
            throw new IllegalStateException("缺少用户身份上下文");
        }
        return Long.valueOf(uid.toString());
    }

    @Tool(description = "查询所有自习室列表，包含每个自习室的名称、总容量，以及各时段（含时段ID、开始/结束时间）。用于回答'有哪些自习室/某自习室的时段'等问题。注意：座位余量按日期+时段区分，需调用 queryAvailability 查询具体余量。")
    public List<RoomResponse> listRooms() {
        return studyRoomService.listRooms();
    }

    @Tool(description = "查询某自习室在指定日期下各时段的座位余量（总容量与剩余可预约数）。需要自习室ID、日期(yyyy-MM-dd)。用于回答'某天某自习室还有多少空位/哪个时段还有位置'等问题。")
    public List<SlotAvailability> queryAvailability(
            @ToolParam(description = "自习室ID") Long roomId,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        return studyRoomService.getAvailability(roomId, LocalDate.parse(date));
    }

    @Tool(description = "查询当前登录用户的全部预约记录（含预约ID、自习室、日期、时段、状态）。用于回答'我的预约/我约了哪些'等问题。")
    public List<ReserveResponse> listMyReservations(ToolContext toolContext) {
        return reservationService.listMyReservations(currentUserId(toolContext));
    }

    @Tool(description = "为当前登录用户创建一个座位预约。需要自习室ID、时段ID、预约日期(yyyy-MM-dd，只能是今天或未来)。成功返回预约详情，失败返回原因。")
    public String createReservation(
            @ToolParam(description = "自习室ID") Long roomId,
            @ToolParam(description = "时段ID") Long timeSlotId,
            @ToolParam(description = "预约日期，格式 yyyy-MM-dd，只能是今天或未来") String reservationDate,
            ToolContext toolContext) {
        try {
            ReserveRequest req = new ReserveRequest();
            req.setRoomId(roomId);
            req.setTimeSlotId(timeSlotId);
            req.setReservationDate(LocalDate.parse(reservationDate));
            ReserveResponse resp = reservationService.reserve(currentUserId(toolContext), req);
            return String.format("预约成功：预约ID=%d，%s，%s，%s-%s，状态=%s",
                    resp.getId(), resp.getRoomName(), resp.getReservationDate(),
                    resp.getStartTime(), resp.getEndTime(), resp.getStatus());
        } catch (Exception e) {
            log.warn("[AI工具] 创建预约失败：{}", e.getMessage());
            return "预约失败：" + e.getMessage();
        }
    }

    @Tool(description = "取消当前登录用户的某个预约。需要预约ID。仅 booked(已预约) 状态可取消。成功或失败都会返回说明。")
    public String cancelReservation(
            @ToolParam(description = "要取消的预约ID") Long reservationId,
            ToolContext toolContext) {
        try {
            reservationService.cancel(reservationId, currentUserId(toolContext));
            return "取消成功：预约ID=" + reservationId + " 已取消。";
        } catch (Exception e) {
            log.warn("[AI工具] 取消预约失败：{}", e.getMessage());
            return "取消失败：" + e.getMessage();
        }
    }
}
