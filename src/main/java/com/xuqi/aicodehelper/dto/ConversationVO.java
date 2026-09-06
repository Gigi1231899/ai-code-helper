package com.xuqi.aicodehelper.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表返回给前端的视图对象（不暴露 userId 等内部字段）
 */
@Data
public class ConversationVO {

    /** 会话ID（int，前端展示给用户） */
    private Long id;

    /** 会话标题 */
    private String title;

    /** 对话模式：rag / mcp / mix */
    private String mode;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后活跃时间，前端按此倒序展示 */
    private LocalDateTime updateTime;
}
