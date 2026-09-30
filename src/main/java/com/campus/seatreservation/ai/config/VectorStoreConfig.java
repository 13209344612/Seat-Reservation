package com.campus.seatreservation.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置（RAG 基础设施）
 *
 * 使用 Spring AI 内存版 SimpleVectorStore + DashScope 向量模型（text-embedding-v3）。
 * 起步方案：向量存于进程内存，应用重启后由 KnowledgeBaseService 重新加载知识库并重建索引。
 * 生产环境可替换为 Redis / PGVector 等持久化实现，只需更换此 Bean。
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
