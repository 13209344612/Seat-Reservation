package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话实体 — 对应数据库 ai_conversation 表
 *
 * 持久化 AI 助手的多轮会话元信息，供前端历史会话列表与回溯使用。
 * conversationId 与 Spring AI ChatMemory 及前端保持一致。
 */
@Data
@TableName("ai_conversation")
public class AiConversation {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID（与 ChatMemory / 前端一致），全局唯一 */
    private String conversationId;

    /** 所属用户ID，外键关联 user.id */
    private Long userId;

    /** 会话标题，取首条用户提问 */
    private String title;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后活跃时间，插入/更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
