package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalTime;

/**
 * 时段实体 — 对应数据库 time_slot 表
 *
 * 存储自习室的开放时段信息，每个自习室可以有多个时段。
 */
@Data
@TableName("time_slot")
public class TimeSlot {

    /** 时段ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属自习室ID，外键关联 study_room.id */
    private Long roomId;           // 外键，关联 study_room.id

    /** 开始时间，如 08:00 */
    private LocalTime startTime;   // 开始时间，如 08:00

    /** 结束时间，如 12:00 */
    private LocalTime endTime;     // 结束时间，如 12:00
}
