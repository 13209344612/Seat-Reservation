package com.campus.seatreservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 自习室请求 DTO — 前端创建/更新自习室时传来的 JSON
 *
 * 用于接收管理员创建或更新自习室时提交的信息，包括时段配置。
 */
@Data
public class RoomRequest {

    /** 自习室名称 */
    @NotBlank(message = "自习室名称不能为空")
    private String name;

    /** 总容量（座位数），至少为1 */
    @NotNull(message = "总容量不能为空")
    @Min(value = 1, message = "总容量至少为1")
    private Integer totalCapacity;

    /** 时段列表，至少需要一个时段 */
    @NotEmpty(message = "至少需要一个时段")
    private List<TimeSlotItem> timeSlots;

    /**
     * 时段项内部类
     */
    @Data
    public static class TimeSlotItem {
        /** 开始时间，格式 "08:00" */
        @NotNull(message = "开始时间不能为空")
        private String startTime;   // 格式 "08:00"

        /** 结束时间，格式 "12:00" */
        @NotNull(message = "结束时间不能为空")
        private String endTime;     // 格式 "12:00"
    }
}