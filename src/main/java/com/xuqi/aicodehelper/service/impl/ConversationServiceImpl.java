package com.xuqi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuqi.aicodehelper.ai.memory.PersistentChatMemoryStore;
import com.xuqi.aicodehelper.common.BusinessException;
import com.xuqi.aicodehelper.dto.ConversationVO;
import com.xuqi.aicodehelper.dto.MessageVO;
import com.xuqi.aicodehelper.entity.Conversation;
import com.xuqi.aicodehelper.entity.MessageEntity;
import com.xuqi.aicodehelper.mapper.ConversationMapper;
import com.xuqi.aicodehelper.mapper.MessageMapper;
import com.xuqi.aicodehelper.service.ConversationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务实现
 */
@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    /** 会话默认标题，首条消息后会被替换为用户的问题 */
    private static final String DEFAULT_TITLE = "新对话";

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private MessageMapper messageMapper;

    /** 删除会话时需要同步清理 AI 记忆 */
    @Resource
    private PersistentChatMemoryStore chatMemoryStore;

    @Override
    public Conversation create(Long userId, String mode) {
        // ① 校验模式合法性（只支持 rag / mcp / mix）
        String normalizedMode = normalizeMode(mode);

        // ② 落库，主键由数据库自增生成，即需求中的"int 类型对话ID"
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(DEFAULT_TITLE);
        conversation.setMode(normalizedMode);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);

        log.info("新建会话：userId={}, conversationId={}, mode={}", userId, conversation.getId(), normalizedMode);
        return conversation;
    }

    @Override
    public List<ConversationVO> listMine(Long userId) {
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdateTime)
        );
        return conversations.stream().map(this::toVO).toList();
    }

    @Override
    public List<MessageVO> listMessages(Long userId, Long conversationId) {
        // 先校验归属，防止通过改ID越权查看他人会话
        getAndCheckOwner(userId, conversationId);

        List<MessageEntity> messages = messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .orderByAsc(MessageEntity::getId)
        );
        return messages.stream().map(this::toMessageVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long conversationId) {
        getAndCheckOwner(userId, conversationId);

        // ① 删除展示用的历史消息
        messageMapper.delete(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getConversationId, conversationId));

        // ② 删除大模型侧的记忆（t_chat_memory）
        chatMemoryStore.deleteMessages(conversationId);

        // ③ 删除会话本身
        conversationMapper.deleteById(conversationId);

        log.info("删除会话：userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    public Conversation getAndCheckOwner(Long userId, Long conversationId) {
        if (conversationId == null) {
            throw new BusinessException("会话ID不能为空");
        }
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            // 不区分"不存在"和"无权限"，避免被探测他人会话ID
            throw new BusinessException(404, "会话不存在或无权访问");
        }
        return conversation;
    }

    @Override
    public void saveMessage(Long conversationId, String role, String content) {
        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    @Override
    public void refreshAfterMessage(Long conversationId, String message) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return;
        }

        Conversation update = new Conversation();
        update.setId(conversationId);
        update.setUpdateTime(LocalDateTime.now());

        // 标题只在还是默认值时改写一次：取用户第一个问题的前 30 个字符
        if (!StringUtils.hasText(conversation.getTitle()) || DEFAULT_TITLE.equals(conversation.getTitle())) {
            update.setTitle(truncate(message, 30));
        }
        conversationMapper.updateById(update);
    }

    /**
     * 截断字符串，超出部分用省略号
     *
     * @param text   原文
     * @param maxLen 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String text, int maxLen) {
        if (text == null) {
            return DEFAULT_TITLE;
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen) + "...";
    }

    /**
     * 规范化并校验对话模式
     *
     * @param mode 原始模式字符串
     * @return 小写后的合法模式
     */
    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            throw new BusinessException("请选择对话模式");
        }
        String normalized = mode.trim().toLowerCase();
        if (!SUPPORTED_MODES.contains(normalized)) {
            throw new BusinessException("不支持的对话模式：" + mode + "（可选值：rag / mcp / mix）");
        }
        return normalized;
    }

    /** 实体转 VO */
    private ConversationVO toVO(Conversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setMode(conversation.getMode());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        return vo;
    }

    /** 消息实体转 VO */
    private MessageVO toMessageVO(MessageEntity message) {
        MessageVO vo = new MessageVO();
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
