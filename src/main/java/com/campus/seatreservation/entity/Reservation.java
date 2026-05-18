package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roomId;

    private Long timeSlotId;

    private LocalDate reservationDate;

    private String status;   // booked / signed / cancelled

    private LocalDateTime signTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
