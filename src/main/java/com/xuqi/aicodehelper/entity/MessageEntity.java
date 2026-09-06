package com.xuqi.aicodehelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息实体（对应表 t_message）
 * <p>
 * 这里只保存「给用户看」的一问一答，用于前端渲染历史记录。
 * 大模型真正使用的上下文（含工具调用消息）存在 t_chat_memory 中。
 * 类名加 Entity 后缀是为了和 LangChain4j 的 ChatMessage 区分开。
 */
@Data
@TableName("t_message")
public class MessageEntity {

    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID */
    private Long conversationId;

    /** 角色：user / ai */
    private String role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createTime;
}
