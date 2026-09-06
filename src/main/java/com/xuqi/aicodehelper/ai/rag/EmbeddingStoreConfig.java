package com.xuqi.aicodehelper.ai.rag;

/**
 * 【已废弃】全局嵌入式存储配置
 * <p>
 * 废弃原因：原来这里提供的是全局唯一的 InMemoryEmbeddingStore，
 * 所有用户共享同一份向量数据，既无法隔离，服务重启后也会全部丢失。
 * <p>
 * 现在的实现：
 * - 每用户一个独立向量库，见 {@link UserEmbeddingStoreManager}
 * - 落盘路径 data/vector/{userId}/embedding-store.json，重启自动恢复
 * - 检索器按用户动态构建，见 {@link RagConfig}
 * <p>
 * 本类已不再声明任何 Bean，可以直接删除。
 */
@Deprecated
public final class EmbeddingStoreConfig {

    private EmbeddingStoreConfig() {
    }
}
