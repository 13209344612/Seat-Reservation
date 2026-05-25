package com.campus.seatreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsMessage implements Serializable {
    private Long reservationId;
    private Long userId;
    private String phoneNumber;    // 实际查用户表获取
    private String nickName;       // 用户昵称
    private String roomName;       // 自习室名
    private String timeRange;      // "08:00-12:00"
    private LocalDate date;        // 预约日期
}