package com.campus.seatreservation.ai.service;

import com.campus.seatreservation.ai.dto.ConversationResponse;
import com.campus.seatreservation.ai.dto.MessageResponse;

import java.util.List;

/**
 * AI 会话持久化服务
 *
 * 负责会话与消息的落库、按用户列出历史会话、回溯消息、删除会话。
 * 所有读取/删除均校验会话归属，防止越权访问他人会话。
 */
public interface AiConversationService {

    /**
     * 记录一条用户消息：会话不存在则以首条提问为标题创建，存在则刷新活跃时间。
     *
     * @param conversationId 会话ID
     * @param userId         当前用户ID
     * @param content        用户消息内容
     */
    void recordUserMessage(String conversationId, Long userId, String content);

    /**
     * 记录一条助手消息（内容为空则忽略），并刷新会话活跃时间。
     *
     * @param conversationId 会话ID
     * @param userId         当前用户ID
     * @param content        助手回答内容
     */
    void recordAssistantMessage(String conversationId, Long userId, String content);

    /** 列出某用户的历史会话，按最后活跃时间倒序。 */
    List<ConversationResponse> listByUser(Long userId);

    /** 回溯某会话的全部消息（按时间正序），校验归属。 */
    List<MessageResponse> getMessages(String conversationId, Long userId);

    /** 删除某会话及其消息，校验归属；会话不存在时静默返回。 */
    void deleteConversation(String conversationId, Long userId);
}
