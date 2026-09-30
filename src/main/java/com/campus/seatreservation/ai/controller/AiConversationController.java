package com.campus.seatreservation.ai.controller;

import com.campus.seatreservation.ai.dto.ConversationResponse;
import com.campus.seatreservation.ai.dto.MessageResponse;
import com.campus.seatreservation.ai.service.AiConversationService;
import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 历史会话控制器 —— 提供会话列表、消息回溯、删除会话三个端点。
 *
 * 位于 /api/ai/conversations 下，经 JWT 认证；当前用户通过 @AuthenticationPrincipal 注入，
 * 服务层据此校验会话归属，用户只能访问自己的历史会话。
 */
@RestController
@RequestMapping("/api/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService conversationService;

    /** 列出当前用户的历史会话（按最后活跃时间倒序）。 */
    @GetMapping
    public Result<List<ConversationResponse>> list(@AuthenticationPrincipal User user) {
        return Result.success(conversationService.listByUser(user.getId()));
    }

    /** 回溯某会话的全部消息（按时间正序）。 */
    @GetMapping("/{conversationId}/messages")
    public Result<List<MessageResponse>> messages(@AuthenticationPrincipal User user,
                                                  @PathVariable String conversationId) {
        return Result.success(conversationService.getMessages(conversationId, user.getId()));
    }

    /** 删除某会话及其消息。 */
    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@AuthenticationPrincipal User user,
                               @PathVariable String conversationId) {
        conversationService.deleteConversation(conversationId, user.getId());
        return Result.success("删除成功");
    }
}
