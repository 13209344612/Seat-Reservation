package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class ReserveResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long roomId;
    private String roomName;
    private Long timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate reservationDate;
    private String status;
    private LocalDateTime signTime;
    private LocalDateTime createTime;
}