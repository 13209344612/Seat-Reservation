package com.campus.seatreservation.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库加载服务（RAG）
 *
 * 应用启动完成后，把 classpath:knowledge/ 下的 Markdown 文档按段落切分、向量化后写入 VectorStore，
 * 供 QuestionAnswerAdvisor 检索增强使用。
 *
 * 健壮性：加载过程（含向量化的网络调用）全程 try-catch，失败仅记录告警、不阻断应用启动，
 * 此时 AI 问答仍可用，只是缺少知识库增强。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @EventListener(ApplicationReadyEvent.class)
    public void loadKnowledgeBase() {
        try {
            Resource[] resources = resolver.getResources("classpath*:knowledge/*.md");
            List<Document> documents = new ArrayList<>();
            for (Resource resource : resources) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String source = resource.getFilename() != null ? resource.getFilename() : "knowledge";
                // 按空行切分为段落，过滤过短片段，提升检索粒度
                for (String paragraph : content.split("\\n\\s*\\n")) {
                    String text = paragraph.strip();
                    if (text.length() < 10) {
                        continue;
                    }
                    documents.add(new Document(text, Map.of("source", source)));
                }
            }
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("[RAG] 知识库加载完成，共写入 {} 个文档片段", documents.size());
            } else {
                log.warn("[RAG] 未在 classpath:knowledge/ 下找到可用文档");
            }
        } catch (Exception e) {
            log.warn("[RAG] 知识库加载失败（AI 问答仍可用，但无知识库增强）：{}", e.getMessage());
        }
    }
}
