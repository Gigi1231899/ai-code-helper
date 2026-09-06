package com.xuqi.aicodehelper.ai.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuqi.aicodehelper.entity.ChatMemoryEntity;
import com.xuqi.aicodehelper.mapper.ChatMemoryMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话记忆持久化（把 LangChain4j 的 ChatMemory 存到 MySQL）
 * <p>
 * 背景：默认的 ChatMemory 只在内存里，服务重启后 AI 就"失忆"了。
 * 实现 ChatMemoryStore 接口后，MessageWindowChatMemory 每次读写都会走这里，
 * 从而实现多轮上下文的持久化。
 * <p>
 * 隔离方式：memoryId 直接复用会话ID（t_conversation.id），
 * 会话ID 全局唯一，所以不同用户、不同会话的记忆天然隔离。
 */
@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Resource
    private ChatMemoryMapper chatMemoryMapper;

    /**
     * 读取某个会话的历史消息
     *
     * @param memoryId 记忆ID（这里就是会话ID）
     * @return 历史消息列表，无记录时返回空列表（不能返回 null，否则 LangChain4j 会 NPE）
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Long id = toLong(memoryId);
        ChatMemoryEntity entity = selectByMemoryId(id);
        if (entity == null || !StringUtils.hasText(entity.getContent())) {
            return List.of();
        }
        // 用官方序列化器还原消息（支持 UserMessage / AiMessage / ToolExecutionResultMessage）
        return ChatMessageDeserializer.messagesFromJson(entity.getContent());
    }

    /**
     * 覆盖写入某个会话的历史消息
     *
     * @param memoryId 记忆ID（会话ID）
     * @param messages 完整的历史消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Long id = toLong(memoryId);
        String json = ChatMessageSerializer.messagesToJson(messages);

        ChatMemoryEntity entity = selectByMemoryId(id);
        if (entity == null) {
            // 首次写入：新增记录
            ChatMemoryEntity newEntity = new ChatMemoryEntity();
            newEntity.setMemoryId(id);
            newEntity.setContent(json);
            newEntity.setUpdateTime(LocalDateTime.now());
            chatMemoryMapper.insert(newEntity);
        } else {
            // 后续写入：更新内容
            ChatMemoryEntity updateEntity = new ChatMemoryEntity();
            updateEntity.setId(entity.getId());
            updateEntity.setContent(json);
            updateEntity.setUpdateTime(LocalDateTime.now());
            chatMemoryMapper.updateById(updateEntity);
        }
    }

    /**
     * 删除某个会话的记忆（删除会话时调用）
     *
     * @param memoryId 记忆ID（会话ID）
     */
    @Override
    public void deleteMessages(Object memoryId) {
        Long id = toLong(memoryId);
        chatMemoryMapper.delete(new LambdaQueryWrapper<ChatMemoryEntity>()
                .eq(ChatMemoryEntity::getMemoryId, id));
    }

    /**
     * 按 memoryId 查询记录
     *
     * @param memoryId 记忆ID
     * @return 记录；不存在返回 null
     */
    private ChatMemoryEntity selectByMemoryId(Long memoryId) {
        return chatMemoryMapper.selectOne(new LambdaQueryWrapper<ChatMemoryEntity>()
                .eq(ChatMemoryEntity::getMemoryId, memoryId));
    }

    /**
     * 把 memoryId 统一转成 Long
     * <p>
     * LangChain4j 传入的是 Object（@MemoryId 声明的是什么类型就是什么类型），
     * 这里兼容 int / Integer / Long / String 几种情况。
     *
     * @param memoryId 记忆ID
     * @return Long 类型的ID
     */
    private Long toLong(Object memoryId) {
        if (memoryId instanceof Number number) {
            return number.longValue();
        }
        if (memoryId instanceof String str) {
            return Long.valueOf(str);
        }
        throw new IllegalArgumentException("不支持的 memoryId 类型：" + memoryId);
    }
}
