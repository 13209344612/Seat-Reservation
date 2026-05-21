package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 自习室实体 — 对应数据库 study_room 表
 *
 * 存储自习室基本信息和容量信息，支持乐观锁防止超卖。
 */
@Data
@TableName("study_room")
public class StudyRoom {

    /** 自习室ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 自习室名称 */
    private String name;              // 自习室名称

    /** 总容量（座位数） */
    private Integer totalCapacity;    // 总容量

    /** 当前可用容量（剩余座位数） */
    private Integer availableCapacity; // 当前可用容量

    /** 乐观锁版本号，用于并发控制 */
   @Version
    private Integer version;          // 乐观锁版本号（第4天用）

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}