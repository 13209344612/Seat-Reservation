package com.campus.seatreservation.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项响应 DTO —— GET /api/ai/conversations 返回的单个会话摘要
 */
@Data
@AllArgsConstructor
public class ConversationResponse {

    /** 会话ID（前端据此加载消息、继续对话） */
    private String conversationId;

    /** 会话标题（取首条提问） */
    private String title;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后活跃时间 */
    private LocalDateTime updateTime;
}
