package com.xuqi.aicodehelper.controller;

import com.xuqi.aicodehelper.common.Result;
import com.xuqi.aicodehelper.common.UserContext;
import com.xuqi.aicodehelper.dto.ConversationVO;
import com.xuqi.aicodehelper.dto.CreateConversationRequest;
import com.xuqi.aicodehelper.dto.MessageVO;
import com.xuqi.aicodehelper.entity.Conversation;
import com.xuqi.aicodehelper.service.ConversationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话管理接口
 * <p>
 * 所有接口都从 JWT 解析出的 userId 取数据，前端无需也不能传 userId，杜绝越权。
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    /**
     * 新建会话：自动生成 int 会话ID 并锁定模式
     *
     * @param request 模式（rag / mcp / mix）
     * @return 新会话信息
     */
    @PostMapping
    public Result<ConversationVO> create(@RequestBody CreateConversationRequest request) {
        Long userId = UserContext.getUserId();
        Conversation conversation = conversationService.create(userId, request.getMode());

        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setMode(conversation.getMode());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        return Result.ok(vo);
    }

    /**
     * 我的会话列表（按最后活跃时间倒序）
     *
     * @return 会话列表
     */
    @GetMapping
    public Result<List<ConversationVO>> list() {
        return Result.ok(conversationService.listMine(UserContext.getUserId()));
    }

    /**
     * 查询某个会话的历史消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @GetMapping("/{conversationId}/messages")
    public Result<List<MessageVO>> messages(@PathVariable Long conversationId) {
        return Result.ok(conversationService.listMessages(UserContext.getUserId(), conversationId));
    }

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     * @return 空
     */
    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable Long conversationId) {
        conversationService.delete(UserContext.getUserId(), conversationId);
        return Result.ok();
    }
}
