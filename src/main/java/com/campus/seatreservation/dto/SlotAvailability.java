package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 时段余量 DTO —— 描述某自习室在指定日期下、某个时段的座位余量。
 *
 * 座位余量按「房间 + 日期 + 时段」维度派生统计，remaining = max(0, total - 活跃预约数)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotAvailability {
    /** 时段ID */
    private Long slotId;
    /** 开始时间 */
    private LocalTime startTime;
    /** 结束时间 */
    private LocalTime endTime;
    /** 该时段总容量 */
    private Integer total;
    /** 该时段在指定日期的剩余可预约数 */
    private Integer remaining;
}
