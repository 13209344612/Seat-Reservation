package com.campus.seatreservation.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.seatreservation.ai.dto.ConversationResponse;
import com.campus.seatreservation.ai.dto.MessageResponse;
import com.campus.seatreservation.ai.service.AiConversationService;
import com.campus.seatreservation.entity.AiConversation;
import com.campus.seatreservation.entity.AiMessage;
import com.campus.seatreservation.mapper.AiConversationMapper;
import com.campus.seatreservation.mapper.AiMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 会话持久化服务实现
 *
 * 会话以 conversationId 为业务主键（与 ChatMemory / 前端一致），消息按 conversationId 归属。
 * 写入路径（recordUserMessage / recordAssistantMessage）由对话接口调用；读取与删除校验归属。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    /** 标题最大长度，超出截断加省略号 */
    private static final int TITLE_MAX = 24;

    @Override
    @Transactional
    public void recordUserMessage(String conversationId, Long userId, String content) {
        AiConversation conv = findByConversationId(conversationId);
        if (conv == null) {
            conv = new AiConversation();
            conv.setConversationId(conversationId);
            conv.setUserId(userId);
            conv.setTitle(deriveTitle(content));
            conversationMapper.insert(conv);
        } else {
            assertOwner(conv, userId);
            touch(conv);
        }
        appendMessage(conversationId, "user", content);
    }

    @Override
    @Transactional
    public void recordAssistantMessage(String conversationId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        AiConversation conv = findByConversationId(conversationId);
        if (conv == null) {
            // 正常情况下用户消息已创建会话；此处兜底，避免助手消息落库时外键失败
            conv = new AiConversation();
            conv.setConversationId(conversationId);
            conv.setUserId(userId);
            conv.setTitle(deriveTitle(content));
            conversationMapper.insert(conv);
        } else {
            assertOwner(conv, userId);
            touch(conv);
        }
        appendMessage(conversationId, "assistant", content);
    }

    @Override
    public List<ConversationResponse> listByUser(Long userId) {
        List<AiConversation> list = conversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime));
        return list.stream()
                .map(c -> new ConversationResponse(
                        c.getConversationId(), c.getTitle(), c.getCreateTime(), c.getUpdateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageResponse> getMessages(String conversationId, Long userId) {
        AiConversation conv = findByConversationId(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        assertOwner(conv, userId);
        List<AiMessage> msgs = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getId));
        return msgs.stream()
                .map(m -> new MessageResponse(m.getRole(), m.getContent(), m.getCreateTime()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId, Long userId) {
        AiConversation conv = findByConversationId(conversationId);
        if (conv == null) {
            return;  // 幂等：已删除或不存在
        }
        assertOwner(conv, userId);
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conv.getId());
    }

    // ------- 内部辅助 -------

    private AiConversation findByConversationId(String conversationId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getConversationId, conversationId));
    }

    private void appendMessage(String conversationId, String role, String content) {
        AiMessage msg = new AiMessage();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        messageMapper.insert(msg);
    }

    /** 刷新会话最后活跃时间（用于历史列表按最近排序） */
    private void touch(AiConversation conv) {
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    /** 归属校验：会话必须属于当前用户，否则拒绝，防止越权读写他人会话 */
    private void assertOwner(AiConversation conv, Long userId) {
        if (!conv.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该会话");
        }
    }

    /** 由首条消息派生标题：去多余空白、截断 */
    private String deriveTitle(String content) {
        if (content == null) {
            return "新对话";
        }
        String t = content.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return "新对话";
        }
        return t.length() > TITLE_MAX ? t.substring(0, TITLE_MAX) + "…" : t;
    }
}
