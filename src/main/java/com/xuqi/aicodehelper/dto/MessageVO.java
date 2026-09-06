package com.xuqi.aicodehelper.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史消息视图对象
 */
@Data
public class MessageVO {

    /** 角色：user / ai */
    private String role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createTime;
}
