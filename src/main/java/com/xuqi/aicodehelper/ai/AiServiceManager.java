package com.xuqi.aicodehelper.ai;

import com.xuqi.aicodehelper.ai.rag.RagConfig;
import com.xuqi.aicodehelper.ai.service.McpService;
import com.xuqi.aicodehelper.ai.service.MixService;
import com.xuqi.aicodehelper.ai.service.RagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户级 AI Service 管理器
 * <p>
 * 为什么需要它：LangChain4j 的 ContentRetriever 是在构建 AI Service 时一次性绑定的，
 * 而每个用户的知识库不同，所以 RAG / 混合模式的 Service 必须"一人一个"。
 * 这里做一层缓存，避免每次对话都重建代理对象。
 * <p>
 * 失效时机：用户上传/删除知识库文件后调用 invalidate(userId)，
 * 下一次对话会带着最新的检索器重建。
 */
@Slf4j
@Component
public class AiServiceManager {

    /** 用户 -> RAG 模式服务 */
    private final Map<Long, RagService> ragServiceCache = new ConcurrentHashMap<>();

    /** 用户 -> 混合模式服务 */
    private final Map<Long, MixService> mixServiceCache = new ConcurrentHashMap<>();

    @Resource
    private AiCodeHelperServiceFactory aiCodeHelperServiceFactory;

    @Resource
    private RagConfig ragConfig;

    /** MCP 模式与知识库无关，全局共享 */
    @Resource
    private McpService mcpService;

    /**
     * 获取用户的 RAG 服务（不存在则创建）
     *
     * @param userId 用户ID
     * @return RagService
     */
    public RagService getRagService(Long userId) {
//        如果指定的 key 不存在，就计算一个值并放入 Map；如果已存在，直接返回现有值。
        return ragServiceCache.computeIfAbsent(userId,
                id -> aiCodeHelperServiceFactory.createRagService(ragConfig.createRetriever(id)));
    }

    /**
     * 获取用户的混合模式服务（不存在则创建）
     *
     * @param userId 用户ID
     * @return MixService
     */
    public MixService getMixService(Long userId) {
        return mixServiceCache.computeIfAbsent(userId,
                id -> aiCodeHelperServiceFactory.createMixService(ragConfig.createRetriever(id)));
    }

    /**
     * 获取 MCP 服务（全局共享）
     *
     * @return McpService
     */
    public McpService getMcpService() {
        return mcpService;
    }

    /**
     * 让某个用户的知识库相关服务失效
     * <p>
     * 上传或删除文件后必须调用，否则 AI 仍在使用旧的检索器（读不到新文档）。
     *
     * @param userId 用户ID
     */
    public void invalidate(Long userId) {
        ragServiceCache.remove(userId);
        mixServiceCache.remove(userId);
        log.info("已刷新用户 AI 服务缓存：userId={}", userId);
    }
}
