package com.xuqi.aicodehelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体（对应表 t_conversation）
 * <p>
 * 一个会话 = 一个 int 对话ID = 一种固定的对话模式。
 * 需求规定：会话创建后模式不可修改，想换模式必须新建会话。
 */
@Data
@TableName("t_conversation")
public class Conversation {

    /** 会话ID（自增 int，前端直接展示给用户） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 会话标题，默认取用户第一条问题 */
    private String title;

    /** 对话模式：rag / mcp / mix */
    private String mode;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后活跃时间（发消息时刷新，用于侧边栏排序） */
    private LocalDateTime updateTime;
}
