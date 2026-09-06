package com.xuqi.aicodehelper.ai.rag;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * RAG 检索器工厂
 * <p>
 * 改造点（解决需求末尾"用户知识库要隔离，需要重新调整内容检索器？"这个问题）：
 * - 旧实现：启动时把 src/main/resources/docs 全量灌进一个全局 store，
 *   ContentRetriever 是全局单例，所有用户共享同一份知识库，无法隔离。
 * - 新实现：ContentRetriever 不再作为全局 Bean，而是**按用户动态创建**，
 *   绑定到该用户自己的向量库上，实现"一人一库、检索隔离"。
 * <p>
 * 另外，原来写死的 docs 目录加载被移除，知识库改由用户在前端上传驱动。
 */
@Component
public class RagConfig {

    /** 向量模型 */
    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    /** 每用户独立的向量库管理器 */
    @Resource
    private UserEmbeddingStoreManager userEmbeddingStoreManager;

    /**
     * 为指定用户创建内容检索器
     *
     * @param userId 用户ID
     * @return 只检索该用户知识库的检索器
     */
    public ContentRetriever createRetriever(Long userId) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(qwenEmbeddingModel)
                // 关键：使用当前用户自己的向量库
                .embeddingStore(userEmbeddingStoreManager.getStore(userId))
                // 最多召回 5 个切片
                .maxResults(5)
                // 相似度阈值，低于该值视为不相关，避免把无关内容塞给模型造成幻觉
                .minScore(0.75)
                .build();
    }
}
