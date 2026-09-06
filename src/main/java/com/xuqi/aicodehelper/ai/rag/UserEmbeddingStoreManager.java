package com.xuqi.aicodehelper.ai.rag;

import com.xuqi.aicodehelper.config.AppProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 用户向量库管理器：每个用户一个独立的、持久化到本地磁盘的向量库
 * <p>
 * 设计说明（对应需求中的"用户知识库要隔离"问题）：
 * 1. 原来是全局唯一的 InMemoryEmbeddingStore，所有用户共享 -> 换成"一人一库"
 * 2. 文件落盘路径：{base-path}/vector/{userId}/embedding-store.json，
 *    服务重启后自动加载，不会像纯内存方案那样丢失
 * 3. 内容检索器在每次对话时基于当前用户的 store 构建，天然隔离
 * <p>
 * 注意：InMemoryEmbeddingStore 是"内存结构 + 磁盘快照"的方案，
 * 适合单机小数据量（本项目限制每人 50 个文件）。
 * 若后续数据量变大，可平滑替换为 Milvus / PGVector 等真正的向量数据库。
 */
@Slf4j
@Component
public class UserEmbeddingStoreManager {

    /** 向量库缓存：userId -> 该用户的向量库 */
    private final Map<Long, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    /** 向量模型（阿里百炼 text-embedding-v1） */
    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    /** 存储路径等配置 */
    @Resource
    private AppProperties appProperties;

    /**
     * 获取（懒加载）某个用户的向量库
     *
     * @param userId 用户ID
     * @return 该用户专属的向量库
     */
    public EmbeddingStore<TextSegment> getStore(Long userId) {
        return storeCache.computeIfAbsent(userId, this::loadOrCreate);
    }

    /**
     * 把文档切片、向量化后写入用户向量库，并落盘
     *
     * @param userId    用户ID
     * @param documents 待摄入的文档（调用方需已设置好 file_name 元数据）
     * @return 摄入产生的切片数量
     */
    public synchronized int ingest(Long userId, List<Document> documents) {
        EmbeddingStore<TextSegment> store = getStore(userId);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                // 按段落切分：每段最多 1000 字符，相邻段重叠 200 字符，避免语义被截断
                .documentSplitter(new DocumentByParagraphSplitter(1000, 200))
                // 把文件名拼到切片正文前，提升检索命中率，也方便回答时标注来源
                .textSegmentTransformer(segment -> TextSegment.from(
                        segment.metadata().getString("file_name") + "\n" + segment.text(),
                        segment.metadata()))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(documents);
        persist(userId);
        log.info("知识库摄入完成：userId={}, 文档数={}", userId, documents.size());
        return documents.size();
    }

    /**
     * 删除某个文件对应的所有向量切片
     *
     * @param userId   用户ID
     * @param fileName 原始文件名（入库时写入了 file_name 元数据）
     */
    public synchronized void removeByFileName(Long userId, String fileName) {
        EmbeddingStore<TextSegment> store = getStore(userId);
        // 按元数据精确过滤，只删这个文件产生的切片，不影响其他文件
        store.removeAll(metadataKey("file_name").isEqualTo(fileName));
        persist(userId);
        log.info("已删除文件对应的向量：userId={}, fileName={}", userId, fileName);
    }

    /**
     * 把用户向量库持久化到磁盘
     *
     * @param userId 用户ID
     */
    public synchronized void persist(Long userId) {
        EmbeddingStore<TextSegment> store = storeCache.get(userId);
        if (!(store instanceof InMemoryEmbeddingStore<TextSegment> inMemoryStore)) {
            return;
        }
        try {
            Path path = storePath(userId);
            Files.createDirectories(path.getParent());
            inMemoryStore.serializeToFile(path);
        } catch (IOException e) {
            // 落盘失败只记录日志，不阻断主流程（内存中的库仍可用于本次会话）
            log.error("向量库落盘失败：userId={}", userId, e);
        }
    }

    /**
     * 从磁盘加载用户向量库，文件不存在则创建空库
     *
     * @param userId 用户ID
     * @return 向量库实例
     */
    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> loadOrCreate(Long userId) {
        Path path = storePath(userId);
        if (Files.exists(path)) {
            try {
                EmbeddingStore<TextSegment> loaded =
                        (EmbeddingStore<TextSegment>) InMemoryEmbeddingStore.fromFile(path);
                log.info("加载用户向量库：userId={}, path={}", userId, path);
                return loaded;
            } catch (Exception e) {
                log.error("向量库加载失败，将创建空库：userId={}, path={}", userId, path, e);
            }
        }
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 用户向量库文件路径
     *
     * @param userId 用户ID
     * @return data/vector/{userId}/embedding-store.json
     */
    private Path storePath(Long userId) {
        return Paths.get(appProperties.getStorage().getBasePath(), "vector",
                String.valueOf(userId), "embedding-store.json");
    }
}
