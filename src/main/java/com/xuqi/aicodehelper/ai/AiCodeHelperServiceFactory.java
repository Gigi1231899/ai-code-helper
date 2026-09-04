package com.xuqi.aicodehelper.ai;

import com.xuqi.aicodehelper.ai.service.McpService;
import com.xuqi.aicodehelper.ai.service.MixService;
import com.xuqi.aicodehelper.ai.service.RagService;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//service工厂
public class AiCodeHelperServiceFactory {
//    @Resource
//    private ChatModel qwenChatModel;  // 注入ChatModel类型的qwenChatModel bean
    @Resource
    private ChatModel myQwenChatModel;  // 注入ChatModel类型的myQwenChatModel bean

    @Resource
    private StreamingChatModel streamingChatModel;  // 注入StreamingChatModel类型的streamingChatModel bean
    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private ContentRetriever contentRetriever;
    /**
     * 创建并配置AiCodeHelperService类型，name=aiCodeHelperService的bean
     * @return 返回一个配置好的AiCodeHelperService实例
     */
    @Bean
    public RagService ragService() {
//        ChatMemory chatMemory= MessageWindowChatMemory.withMaxMessages(10);
//        return AiServices.create(AiCodeHelperService.class, qwenChatModel);
        return (RagService) AiServices.builder(RagService.class).
                chatModel(myQwenChatModel).
                streamingChatModel(streamingChatModel). //流式输出模型
                chatMemoryProvider(memoryId-> MessageWindowChatMemory.withMaxMessages(10)). //每个对话独立存储,lambda表达式,根据memoryId创建一个MessageWindowChatMemory实例
                contentRetriever(contentRetriever).
                build();

    }

    @Bean
    public McpService mcpService() {
        return (McpService) AiServices.builder(McpService.class).
                chatModel(myQwenChatModel).
                streamingChatModel(streamingChatModel). //流式输出模型
                chatMemoryProvider(memoryId-> MessageWindowChatMemory.withMaxMessages(10)). //每个对话独立存储,lambda表达式,根据memoryId创建一个MessageWindowChatMemory实例
                toolProvider(mcpToolProvider).
                build();
    }

    @Bean
    public MixService mixService() {
        return (MixService) AiServices.builder(McpService.class).
                chatModel(myQwenChatModel).
                streamingChatModel(streamingChatModel). //流式输出模型
                chatMemoryProvider(memoryId-> MessageWindowChatMemory.withMaxMessages(10)). //每个对话独立存储,lambda表达式,根据memoryId创建一个MessageWindowChatMemory实例
                toolProvider(mcpToolProvider).
                contentRetriever(contentRetriever).
                build();
    }

}
