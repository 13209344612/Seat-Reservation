package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalTime;

@Data
@TableName("time_slot")
public class TimeSlot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;           // 外键，关联 study_room.id

    private LocalTime startTime;   // 开始时间，如 08:00

    private LocalTime endTime;     // 结束时间，如 12:00
}
