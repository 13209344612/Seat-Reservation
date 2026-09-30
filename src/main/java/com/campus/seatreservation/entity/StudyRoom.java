package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 自习室实体 — 对应数据库 study_room 表
 *
 * 存储自习室基本信息。座位余量不再存储为计数器，
 * 而是按「房间+日期+时段」维度从 reservation 表实时派生统计。
 */
@Data
@TableName("study_room")
public class StudyRoom {

    /** 自习室ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 自习室名称 */
    private String name;              // 自习室名称

    /** 总容量（座位数），单个时段单日的可预约上限 */
    private Integer totalCapacity;    // 总容量

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
