package com.campus.seatreservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class RoomRequest {

    @NotBlank(message = "自习室名称不能为空")
    private String name;

    @NotNull(message = "总容量不能为空")
    @Min(value = 1, message = "总容量至少为1")
    private Integer totalCapacity;

    @NotEmpty(message = "至少需要一个时段")
    private List<TimeSlotItem> timeSlots;

    @Data
    public static class TimeSlotItem {
        @NotNull(message = "开始时间不能为空")
        private String startTime;   // 格式 "08:00"

        @NotNull(message = "结束时间不能为空")
        private String endTime;     // 格式 "12:00"
    }
}