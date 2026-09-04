package com.xuqi.aicodehelper.ai.model;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 配置类，用于配置QwenChatModel的相关参数
 * 通过@ConfigurationProperties注解，将配置文件中以"langchain4j.community.dashscope.chat-model"为前缀的属性绑定到该类
 */
@Configuration
@ConfigurationProperties("langchain4j.community.dashscope.chat-model")
@Data
public class QwenChatModelConfig {
    // API密钥，用于访问Qwen服务
    private String apiKey;
    // 模型名称，指定要使用的具体Qwen模型
    private String modelName;

    // 注入ChatModelListener监听器，用于监听模型交互事件
    @Resource
    private ChatModelListener chatModelListener;
    /**
     * 创建并配置QwenChatModel Bean
     * @return 配置好的QwenChatModel实例
     */
    @Bean
    public QwenChatModel myQwenChatModel() {
        return QwenChatModel.builder()
                .modelName(modelName)    // 设置模型名称
                .apiKey(apiKey)          // 设置API密钥
                .listeners(List.of(chatModelListener))  // 设置监听器
                .build();
    }
}
