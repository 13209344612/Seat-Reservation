package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private String name;
    private Integer totalCapacity;
    private Integer availableCapacity;
    private LocalDateTime createTime;
    private List<TimeSlotItem> timeSlots;

    @Data
    @AllArgsConstructor
    public static class TimeSlotItem {
        private Long id;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}