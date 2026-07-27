package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约响应 DTO — 返回给前端的预约详细信息
 *
 * 包含预约记录的所有相关信息，用于展示用户的预约详情。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResponse {
    /** 预约ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 自习室ID */
    private Long roomId;
    /** 自习室名称 */
    private String roomName;
    /** 时段ID */
    private Long timeSlotId;
    /** 开始时间 */
    private LocalTime startTime;
    /** 结束时间 */
    private LocalTime endTime;
    /** 预约日期 */
    private LocalDate reservationDate;
    /** 预约状态 */
    private String status;
    /** 签到时间 */
    private LocalDateTime signTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}