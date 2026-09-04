package com.xuqi.aicodehelper.ai.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP配置类
 * 用于配置和创建McpToolProvider Bean，该Bean负责将MCP Server提供的工具暴露给AI
 */
@Configuration
public class McpConfig {
    // 从配置文件中注入API密钥
    @Value("${bigModel.api-key}")
    private String apiKey;

    /**
     * 创建并配置McpToolProvider Bean
     * @return 配置好的McpToolProvider实例
     */
    @Bean
    public McpToolProvider mcpToolProvider() {
        // ① 创建 MCP 传输层（通信通道）
        // 使用HTTP传输层，配置SSE服务器URL，并启用请求和响应日志
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey)
                .logRequests(true)    // 记录请求日志
                .logResponses(true)   // 记录响应日志
                .build();

        // ② 创建 MCP 客户端（负责与 MCP Server 通信）
        // 使用默认MCP客户端，设置密钥并使用上面创建的传输层
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .key(apiKey)
                .transport(transport)
                .build();

        // ③ 创建工具提供者（把 MCP Server 提供的工具暴露给 AI）
        // 构建工具提供者，使用上面创建的MCP客户端
        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();

        return toolProvider;
    }
}
