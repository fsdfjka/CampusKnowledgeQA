package com.campusknowledgeqa.campusknowledgeqa.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                        你是校园知识库问答助手。
                        规则：
                        1. 只能回答和校园相关的问题；
                        2. 不知道的内容，不要编造，直接回复：暂时没有查到相关校园资料；
                        3. 回答简洁易懂。
                        """).build();
    }
    // 内存向量库，注入Embedding模型
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 使用builder构建，不再new
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File vectorJsonFile = new File("vector_store.json");

        // 如果向量文件存在，启动加载旧向量
        if (vectorJsonFile.exists()) {
            store.load(vectorJsonFile);
            System.out.println("✅ 成功加载历史向量文件 vector_store.json");
        }

        // 项目关闭钩子，退出自动保存向量到json
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                store.save(vectorJsonFile);
                System.out.println("💾 向量数据保存到 vector_store.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        return store;
    }
}
