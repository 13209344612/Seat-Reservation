package com.campus.seatreservation.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息响应 DTO —— GET /api/ai/conversations/{id}/messages 返回的单条消息
 */
@Data
@AllArgsConstructor
public class MessageResponse {

    /** 角色：user（用户）或 assistant（助手） */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}
