package com.xuqi.aicodehelper.ai.rag;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 嵌入式存储配置类
 * 用于配置和管理嵌入向量存储的实现方式
 */
@Configuration
public class EmbeddingStoreConfig {
    /**
     * 创建并配置嵌入式存储Bean
     * @return 返回一个内存版的嵌入式存储实现
     * 内存版嵌入式存储适合学习和测试场景使用
     * 数据在应用重启后会清空，不适合生产环境使用
     */
    @Bean
    public EmbeddingStore embeddingStore() {
        // 这是内存版，适合学习和测试，数据在应用重启后会清空
        return new InMemoryEmbeddingStore<>();
    }
}
