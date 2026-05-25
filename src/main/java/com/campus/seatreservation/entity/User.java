package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体 — 对应数据库 user 表
 *
 * 存储系统用户信息，包括学生和管理员两种角色。
 */
@Data
public class User {

    /** 用户ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一标识 */
    private String username;

    /** 密码，BCrypt加密存储 */
    private String password;

    /** 用户角色：student（学生）或 admin（管理员） */
    private String role;

    /** 手机号 */
    private String phone;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
