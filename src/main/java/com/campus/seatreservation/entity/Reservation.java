package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约记录实体 — 对应数据库 reservation 表
 *
 * 存储用户的预约信息，包括预约状态和签到时间。
 */
@Data
@TableName("reservation")
public class Reservation {

    /** 预约ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约用户ID，外键关联 user.id */
    private Long userId;

    /** 自习室ID，外键关联 study_room.id */
    private Long roomId;

    /** 时段ID，外键关联 time_slot.id */
    private Long timeSlotId;

    /** 预约日期 */
    private LocalDate reservationDate;

    /** 预约状态：booked（已预约）、signed（已签到）、cancelled（已取消） */
    private String status;

    /** 签到时间 */
    private LocalDateTime signTime;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
