package com.xuqi.aicodehelper.ai;

import com.xuqi.aicodehelper.ai.memory.PersistentChatMemoryStore;
import com.xuqi.aicodehelper.ai.service.McpService;
import com.xuqi.aicodehelper.ai.service.MixService;
import com.xuqi.aicodehelper.ai.service.RagService;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Service 工厂
 * <p>
 * 职责：把 ChatModel / 记忆 / 工具 / 检索器组装成可直接调用的接口代理。
 * <p>
 * 本次改造要点：
 * 1. 【记忆持久化】接入 PersistentChatMemoryStore，把多轮上下文存进 MySQL，
 *    服务重启后 AI 不再"失忆"（原来是纯内存）。
 * 2. 【知识库隔离】RAG / 混合模式需要绑定"用户私有检索器"，
 *    因此不再注册成全局单例 Bean，改为按用户创建（见 AiServiceManager）。
 *    MCP 模式不依赖知识库，仍然是全局共享单例。
 */
@Configuration
public class AiCodeHelperServiceFactory {

    /** 每个会话保留的最近消息条数（滑动窗口） */
    private static final int MAX_MESSAGES = 20;

    /** 普通对话模型（配置了监听器的自定义 Bean） */
    @Resource
    private ChatModel myQwenChatModel;

    /** 流式对话模型，用于 SSE 逐字输出 */
    @Resource
    private StreamingChatModel streamingChatModel;

    /** MCP 工具提供者（智谱联网搜索） */
    @Resource
    private McpToolProvider mcpToolProvider;

    /** 基于 MySQL 的记忆存储 */
    @Resource
    private PersistentChatMemoryStore chatMemoryStore;

    /**
     * MCP 联网搜索服务：不依赖用户知识库，全局共享一个实例即可
     *
     * @return McpService 代理
     */
    @Bean
    public McpService mcpService() {
        return AiServices.builder(McpService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(this::createChatMemory)
                .toolProvider(mcpToolProvider)
                .build();
    }

    /**
     * 为指定用户创建 RAG 服务（检索器绑定该用户的私有知识库）
     *
     * @param contentRetriever 该用户专属的内容检索器
     * @return RagService 代理
     */
    public RagService createRagService(ContentRetriever contentRetriever) {
        return AiServices.builder(RagService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(streamingChatModel)
//                存方法引用
                .chatMemoryProvider(this::createChatMemory)
                .contentRetriever(contentRetriever)
                .build();
    }

    /**
     * 为指定用户创建混合模式服务（同时具备知识库检索 + 联网搜索能力）
     *
     * @param contentRetriever 该用户专属的内容检索器
     * @return MixService 代理
     */
    public MixService createMixService(ContentRetriever contentRetriever) {
        // 注意：这里必须是 MixService.class，写错会导致 ClassCastException
        return AiServices.builder(MixService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(this::createChatMemory)
                .toolProvider(mcpToolProvider)
                .contentRetriever(contentRetriever)
                .build();
    }

    /**
     * 记忆提供者：按 memoryId（即会话ID）为单位创建独立的滑动窗口记忆
     * <p>
     * 底层存储走 MySQL，所以每个会话的上下文既互相隔离，又能在重启后恢复。
     *
     * @param memoryId 记忆ID（会话ID）
     * @return 会话记忆
     */
    private MessageWindowChatMemory createChatMemory(Object memoryId) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
