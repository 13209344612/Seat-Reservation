package com.campus.seatreservation.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 对话请求 DTO —— 前端 POST /api/ai/chat 或 /api/ai/chat/stream 时传来的 JSON
 */
@Data
public class ChatRequest {

    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /** 会话ID，用于区分多轮对话记忆；为空时后端按用户ID生成默认会话 */
    private String conversationId;
}
