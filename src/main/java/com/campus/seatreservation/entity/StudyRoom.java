package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("study_room")
public class StudyRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;              // 自习室名称

    private Integer totalCapacity;    // 总容量

    private Integer availableCapacity; // 当前可用容量

    @Version
    private Integer version;          // 乐观锁版本号（第4天用）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}