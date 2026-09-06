package com.xuqi.aicodehelper.dto;

import lombok.Data;

/**
 * 新建会话请求体
 */
@Data
public class CreateConversationRequest {

    /**
     * 对话模式：rag=知识库模式 / mcp=联网搜索模式 / mix=混合模式
     * 会话一旦创建，模式不可修改。
     */
    private String mode;
}
