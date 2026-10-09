package com.campusknowledgeqa.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiConfig {

    private static final String SCHEMA = "public";
    private static final String TABLE = "vector_store";

    /** 向量维度：与 Embedding 模型输出维度保持一致（改模型时改这里或 application.yml） */
    @Value("${spring.ai.dashscope.embedding.options.dimensions:1024}")
    private int dimensions;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // 系统提示统一在 ChatController 里构建（含 RAG 上下文），此处不再设置 defaultSystem，避免出现两个 system 消息
        return builder.build();
    }

    /**
     * 基于 pgvector 的向量库（PostgreSQL + pgvector 扩展）
     */
    @Bean
    public KnowledgeVectorStore vectorStore(@Qualifier("pgJdbcTemplate") JdbcTemplate jdbcTemplate,
                                            EmbeddingModel embeddingModel) {
        PgVectorStore pgVectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(SCHEMA)
                .vectorTableName(TABLE)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
        // 手动构建的实例不是 Spring Bean，需手动触发建表
        pgVectorStore.afterPropertiesSet();
        return new KnowledgeVectorStore(pgVectorStore, jdbcTemplate, SCHEMA, TABLE);
    }
}
