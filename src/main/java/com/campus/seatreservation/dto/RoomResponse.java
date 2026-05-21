package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 自习室响应 DTO — 返回给前端的自习室详细信息
 *
 * 包含自习室的基本信息、容量信息和时段列表。
 */
@Data
@AllArgsConstructor
public class RoomResponse {
    /** 自习室ID */
    private Long id;
    /** 自习室名称 */
    private String name;
    /** 总容量 */
    private Integer totalCapacity;
    /** 当前可用容量 */
    private Integer availableCapacity;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 时段列表 */
    private List<TimeSlotItem> timeSlots;

    /**
     * 时段项内部类
     */
    @Data
    @AllArgsConstructor
    public static class TimeSlotItem {
        /** 时段ID */
        private Long id;
        /** 开始时间 */
        private LocalTime startTime;
        /** 结束时间 */
        private LocalTime endTime;
    }
}