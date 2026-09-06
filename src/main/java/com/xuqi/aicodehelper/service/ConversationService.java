package com.xuqi.aicodehelper.service;

import com.xuqi.aicodehelper.dto.ConversationVO;
import com.xuqi.aicodehelper.dto.MessageVO;
import com.xuqi.aicodehelper.entity.Conversation;

import java.util.List;
import java.util.Set;

/**
 * 会话服务：新建会话、查询列表、查询历史消息、删除会话
 */
public interface ConversationService {

    /** 支持的三种对话模式 */
    Set<String> SUPPORTED_MODES = Set.of("rag", "mcp", "mix");

    /**
     * 新建会话（自动分配 int 会话ID，并锁定对话模式）
     *
     * @param userId 用户ID
     * @param mode   对话模式
     * @return 新建的会话
     */
    Conversation create(Long userId, String mode);

    /**
     * 查询当前用户的会话列表（按最后活跃时间倒序）
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ConversationVO> listMine(Long userId);

    /**
     * 查询某个会话的历史消息（会校验归属，防止越权查看他人会话）
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<MessageVO> listMessages(Long userId, Long conversationId);

    /**
     * 删除会话（连同消息、AI 记忆一起清理）
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     */
    void delete(Long userId, Long conversationId);

    /**
     * 获取会话并校验归属
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 会话实体
     */
    Conversation getAndCheckOwner(Long userId, Long conversationId);

    /**
     * 保存一条对话消息（用于前端展示历史）
     * <p>
     * 注意：这是"给人看"的记录，大模型真正使用的上下文由 PersistentChatMemoryStore 维护。
     *
     * @param conversationId 会话ID
     * @param role           角色：user / ai
     * @param content        消息内容
     */
    void saveMessage(Long conversationId, String role, String content);

    /**
     * 收到新消息后刷新会话：首条用户消息自动作为标题，并更新最后活跃时间
     *
     * @param conversationId 会话ID
     * @param message        用户本轮输入
     */
    void refreshAfterMessage(Long conversationId, String message);
}
