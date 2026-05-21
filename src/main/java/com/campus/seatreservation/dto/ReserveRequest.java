package com.campus.seatreservation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 预约请求 DTO — 前端 POST /api/reservations 时传来的 JSON
 */
@Data
public class ReserveRequest {
    @NotNull(message = "自习室ID不能为空")
    private Long roomId;

    @NotNull(message = "时段ID不能为空")
    private Long timeSlotId;

    @NotNull(message = "预约日期不能为空")
    @FutureOrPresent(message = "只能预约今天或未来的日期")
    private LocalDate reservationDate;
}
