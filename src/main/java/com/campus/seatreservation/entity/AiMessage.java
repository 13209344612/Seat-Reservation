package com.campus.seatreservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 消息实体 — 对应数据库 ai_message 表
 *
 * 存储会话中的单条消息（用户提问或助手回答），按 conversationId 归属会话。
 */
@Data
@TableName("ai_message")
public class AiMessage {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID，外键关联 ai_conversation.conversation_id */
    private String conversationId;

    /** 角色：user（用户）或 assistant（助手） */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
