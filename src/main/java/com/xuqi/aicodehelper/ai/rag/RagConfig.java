package com.xuqi.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;

import java.util.List;

@Configuration
public class RagConfig {
    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public ContentRetriever contentRetriever() {
//        =======RAG====
        // 手动创建一个 Apache POI 文档解析器
        DocumentParser parser = new ApachePoiDocumentParser();

        // 加载文档时，显式传入解析器
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                "src/main/resources/docs",
                parser  // ← 关键：强制使用 POI 解析器
        );
//        加载文档
//        List<Document> documents=FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
//        文档切割，按照段落分割，字符数量约束，1000，重叠200
        DocumentByParagraphSplitter documentByParagraphSplitter=new DocumentByParagraphSplitter(1000,200);
//        （文档入库员）向量数据库摄入器，通过向量模型转成向量，存入向量库，textsegment把文档名加上提高segment质量
        EmbeddingStoreIngestor ingestor=EmbeddingStoreIngestor.builder().
                documentSplitter(documentByParagraphSplitter).
                textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name")+"\n"+textSegment.text(),textSegment.metadata())).
                embeddingModel(qwenEmbeddingModel).
                embeddingStore(embeddingStore).
                build();
        ingestor.ingest(documents);

//        内容检索器
        EmbeddingStoreContentRetriever retriever=EmbeddingStoreContentRetriever.builder()
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.75)
                .build();
        return retriever;
    }
}
