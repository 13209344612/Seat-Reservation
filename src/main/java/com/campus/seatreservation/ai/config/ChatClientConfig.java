package com.campus.seatreservation.ai.config;

import com.campus.seatreservation.ai.tool.ReservationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;

/**
 * ChatClient 配置
 *
 * 组装一个统一的"校园自习室预约助手"：
 * - 系统提示词：定义角色、能力边界与行为准则；
 * - 对话记忆 Advisor：按 conversationId 保留最近若干轮上下文，实现多轮对话；
 * - RAG Advisor：自动从向量库检索知识库资料增强回答；
 * - 工具集：把现有预约业务 Service 暴露给大模型进行 Tool Calling。
 */
@Configuration
public class ChatClientConfig {

    /** 系统提示词：定义助手角色、能力边界与行为准则 */
    private static final String SYSTEM_PROMPT = """
            你是"校园自习室座位预约助手"，服务于一个大学自习室座位预约系统。请用简洁、友好的中文回答。

            你的能力：
            1. 查询自习室列表及各时段剩余座位（调用 listRooms 工具，禁止编造余量）。
            2. 查询当前用户的预约记录（调用 listMyReservations 工具）。
            3. 帮当前用户创建预约（调用 createReservation 工具）。
            4. 帮当前用户取消预约（调用 cancelReservation 工具）。
            5. 回答自习室使用规则、预约政策等问题（依据检索到的知识库资料作答）。

            行为准则：
            - 创建预约需要三项信息：自习室ID(roomId)、时段ID(timeSlotId)、预约日期(yyyy-MM-dd)。信息不全时先向用户追问，或用 listRooms 查询后引导用户选择，不要臆测ID。
            - 只能预约今天或未来的日期。
            - 涉及数据查询和写操作时必须调用工具获取真实结果，并如实转达工具返回的信息（包括失败原因），不得编造。
            - userId 由系统安全注入，你无需也无法指定他人身份，只能操作当前登录用户的数据。
            """;

    /**
     * 组装含「当前日期」的完整系统提示词（每次请求动态生成）。
     *
     * 大模型自身不知道真实日期，若不注入会臆造（例如把今天说成 2024-05-27）。
     * 这里在每次请求时拼接当天日期，使「今天/现在/明天」等相对日期能被正确换算。
     */
    public static String systemPromptWithDate() {
        return SYSTEM_PROMPT
                + "\n\n【当前日期】今天是 " + LocalDate.now() + "（yyyy-MM-dd）。"
                + "用户说「今天/现在/明天/后天」等相对日期时，一律以该日期为基准换算成具体日期后再调用工具；"
                + "除非用户明确指定其它日期，否则不要反问日期、也不要编造日期。";
    }

    /**
     * 对话记忆：滑动窗口保留最近 20 条消息，按 conversationId 区分不同会话。
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    /**
     * 构建 ChatClient：默认挂载系统提示词、对话记忆 Advisor、RAG 检索 Advisor，并注册预约工具。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 VectorStore vectorStore,
                                 ReservationTools reservationTools) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(4).build())
                                .build()
                )
                .defaultTools(reservationTools)
                .build();
    }
}
