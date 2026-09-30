package com.campus.seatreservation.ai.controller;

import com.campus.seatreservation.ai.config.ChatClientConfig;
import com.campus.seatreservation.ai.dto.ChatRequest;
import com.campus.seatreservation.ai.service.AiConversationService;
import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI 助手控制器 —— 提供同步对话与 SSE 流式对话两个端点。
 *
 * 端点位于 /api/ai/** 下，默认经过 JWT 认证（SecurityConfig 的 anyRequest().authenticated()）。
 * 当前登录用户通过 @AuthenticationPrincipal 注入，其 userId 经 ToolContext 安全传递给工具，
 * 确保 AI 只能操作当前用户的数据（无法越权）。
 *
 * 每轮对话的用户消息与助手回答都会经 {@link AiConversationService} 落库，支撑历史会话列表与回溯。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final ChatClient chatClient;
    private final AiConversationService conversationService;

    /**
     * 同步对话：一次性返回完整回答。
     */
    @PostMapping("/chat")
    public Result<String> chat(@AuthenticationPrincipal User user,
                               @Valid @RequestBody ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId(), user.getId());
        try {
            // 先落库用户消息（会话不存在则以本条提问为标题创建）
            conversationService.recordUserMessage(conversationId, user.getId(), request.getMessage());

            String answer = chatClient.prompt()
                    .system(ChatClientConfig.systemPromptWithDate())
                    .user(request.getMessage())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(Map.of("userId", user.getId()))
                    .call()
                    .content();

            // 落库助手回答
            conversationService.recordAssistantMessage(conversationId, user.getId(), answer);
            return Result.success(answer);
        } catch (Exception e) {
            log.error("[AI] 同步对话失败：{}", e.getMessage());
            return Result.error(500, "AI 助手暂时不可用，请稍后重试");
        }
    }

    /**
     * 流式对话（SSE）：以 text/event-stream 增量返回，实现前端打字机效果。
     *
     * 采用 POST + fetch 读取流的方式（而非 GET + EventSource），以便携带 Authorization 头，
     * 复用现有 JWT 认证；原生 EventSource 不支持自定义请求头。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@AuthenticationPrincipal User user,
                                                    @Valid @RequestBody ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId(), user.getId());

        // 先落库用户消息；失败（如会话归属冲突）则以 error 事件返回，保持 SSE 契约
        try {
            conversationService.recordUserMessage(conversationId, user.getId(), request.getMessage());
        } catch (Exception e) {
            log.warn("[AI] 记录用户消息失败：{}", e.getMessage());
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error").data("会话保存失败，请稍后重试").build());
        }

        // 累积助手回答，流正常结束后整体落库
        StringBuilder answer = new StringBuilder();
        return chatClient.prompt()
                .system(ChatClientConfig.systemPromptWithDate())
                .user(request.getMessage())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("userId", user.getId()))
                .stream()
                .content()
                .doOnNext(answer::append)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build())
                .concatWith(Mono.fromRunnable(() -> persistAssistant(conversationId, user.getId(), answer.toString()))
                        .then(Mono.just(ServerSentEvent.<String>builder().event("done").data("[DONE]").build())))
                .onErrorResume(e -> {
                    log.error("[AI] 流式对话失败：{}", e.getMessage());
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("AI 助手暂时不可用，请稍后重试")
                            .build());
                });
    }

    /** 落库助手回答，失败仅记录日志，不影响 SSE 流的正常结束。 */
    private void persistAssistant(String conversationId, Long userId, String content) {
        try {
            conversationService.recordAssistantMessage(conversationId, userId, content);
        } catch (Exception e) {
            log.warn("[AI] 记录助手消息失败：{}", e.getMessage());
        }
    }

    /** 会话ID：前端未提供时按用户ID生成默认会话，保证同一用户的多轮上下文连续。 */
    private String resolveConversationId(String conversationId, Long userId) {
        return (conversationId == null || conversationId.isBlank())
                ? "user-" + userId
                : conversationId;
    }
}
