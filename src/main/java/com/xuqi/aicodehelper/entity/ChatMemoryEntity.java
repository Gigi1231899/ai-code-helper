package com.xuqi.aicodehelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话记忆持久化实体（对应表 t_chat_memory）
 * <p>
 * memoryId 直接复用会话ID：会话ID 全局唯一，天然保证多用户记忆隔离。
 * content 是 LangChain4j 官方序列化器产出的 JSON 消息列表。
 */
@Data
@TableName("t_chat_memory")
public class ChatMemoryEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记忆ID = 会话ID */
    private Long memoryId;

    /** JSON 序列化的 ChatMessage 列表 */
    private String content;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
