package com.xuqi.aicodehelper.ai.controller;

import com.xuqi.aicodehelper.ai.AiServiceManager;
import com.xuqi.aicodehelper.common.UserContext;
import com.xuqi.aicodehelper.entity.Conversation;
import com.xuqi.aicodehelper.service.ConversationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 对话接口（SSE 流式输出）
 * <p>
 * 调用示例：GET /api/ai/chat?conversationId=1&message=MySQL有哪些锁
 * <p>
 * 与旧版的区别：
 * 1. 不再由前端传 mode —— 模式在"新建会话"时就已锁定，这里从会话记录里取，
 *    保证"一个会话只能用一个模式，切换模式必须新建会话"。
 * 2. 不再需要前端传 userId —— 从 JWT 解析，杜绝越权访问他人会话。
 * 3. 用户消息与 AI 完整回答都会落库，刷新页面可看到历史对话。
 * 4. RAG / 混合模式使用当前用户私有的检索器，实现知识库隔离。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiServiceManager aiServiceManager;

    @Resource
    private ConversationService conversationService;

    /**
     * 流式对话
     *
     * @param conversationId 会话ID（新建会话接口返回的 int ID）
     * @param message        用户本轮输入
     * @return SSE 事件流，每个事件携带一小段 AI 输出
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestParam("conversationId") Long conversationId,
                                              @RequestParam("message") String message) {
        // ① 身份与归属校验：会话不是自己的直接拒绝（必须在当前线程完成，JWT 上下文才有效）
        Long userId = UserContext.getUserId();
        Conversation conversation = conversationService.getAndCheckOwner(userId, conversationId);
        String mode = conversation.getMode();

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        // ② 先把用户消息落库，保证刷新后能看到（AI 回复等流结束后再存）
        conversationService.saveMessage(conversationId, "user", message);
        conversationService.refreshAfterMessage(conversationId, message);

        // ③ 按会话锁定的模式分发到不同 AI 服务
        //    memoryId 直接复用会话ID，配合持久化存储实现多会话记忆隔离
        int memoryId = conversationId.intValue();
        Flux<String> aiFlux = switch (mode) {
            // RAG：只从用户自己的知识库检索
            case "rag" -> aiServiceManager.getRagService(userId).chatRag(memoryId, message);
            // MCP：只使用联网搜索工具
            case "mcp" -> aiServiceManager.getMcpService().chatMcp(memoryId, message);
            // 混合：知识库 + 联网搜索
            default -> aiServiceManager.getMixService(userId).chatMix(memoryId, message);
        };

        // ④ 边流式推送边拼接完整回答，流结束后一次性入库
        //    用 StringBuffer 而不是 StringBuilder：SSE 可能由不同线程推送，需要保证线程安全
        StringBuffer answer = new StringBuffer();

        return aiFlux
                // 每个片段先拼进缓冲区
                .doOnNext(answer::append)
                // 流正常结束时保存 AI 完整回答
                .doOnComplete(() -> conversationService.saveMessage(conversationId, "ai", answer.toString()))
                // 出错时给前端推一条可读的错误提示，而不是直接断开连接
                .onErrorResume(error -> {
                    log.error("AI 对话失败：userId={}, conversationId={}, mode={}",
                            userId, conversationId, mode, error);
                    return Flux.just("抱歉，AI 开小差了，请稍后重试～");
                })
                // 包装成 SSE 事件
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }
}
