package com.xuqi.aicodehelper.ai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 聊天模型监听器配置类
 * 用于配置和创建ChatModelListener Bean
 */
@Configuration
@Slf4j
public class ChatModelListenerConfig {
    /**
     * 创建并返回ChatModelListener Bean
     * 该监听器用于处理聊天模型的请求、响应和错误事件
     *
     * @return ChatModelListener 实例
     */
    @Bean
    public ChatModelListener chatModelListener() {
        return new ChatModelListener() {
            // 注释掉的日志记录器代码
//            private static final Logger log= (Logger) LoggerFactory.getLogger(ChatModelListenerConfig.class);
            /**
             * 处理聊天模型请求事件
             * @param requestContext 请求上下文信息
             */
            public void onRequest(ChatModelRequestContext requestContext) {
                log.info("onRequest(): {}", requestContext);
            }
            /**
             * 处理聊天模型响应事件
             * @param responseContext 响应上下文信息
             */
            public void onResponse(ChatModelResponseContext responseContext) {
                log.info("onResponse(): {}", responseContext);
            }
            /**
             * 处理聊天模型错误事件
             * @param errorContext 错误上下文信息
             */
            public void onError(ChatModelErrorContext errorContext) {
                log.info("onError(): {}", errorContext);
            }
        };

    }
}
