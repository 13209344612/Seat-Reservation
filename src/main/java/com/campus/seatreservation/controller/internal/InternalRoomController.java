package com.campus.seatreservation.controller.internal;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.dto.SlotAvailability;
import com.campus.seatreservation.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 内部自习室接口 —— 仅供 Python Agent 服务通过服务间鉴权调用（非对外 /api）。
 *
 * 位于 /internal/** 下，由 {@link com.campus.seatreservation.config.InternalApiSecurityConfig}
 * 校验 X-Internal-Token，不走用户 JWT 鉴权。直接委托现有业务 Service，
 * 与原 StudyRoomController 的查询能力等价，但不做角色限制（Agent 以当前用户身份只读查询）。
 */
@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
public class InternalRoomController {

    private final StudyRoomService studyRoomService;

    /** 自习室列表（含各时段）。 */
    @GetMapping
    public Result<List<RoomResponse>> list() {
        return Result.success(studyRoomService.listRooms());
    }

    /** 某自习室在指定日期下各时段的座位余量（date 缺省为今天）。 */
    @GetMapping("/{roomId}/availability")
    public Result<List<SlotAvailability>> availability(
            @PathVariable Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(studyRoomService.getAvailability(roomId, date == null ? LocalDate.now() : date));
    }
}
