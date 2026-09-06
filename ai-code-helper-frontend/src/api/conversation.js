import request from './request'

/**
 * 会话相关接口
 */

/** 新建会话：传入模式(rag/mcp/mix)，返回自动分配的 int 会话ID */
export function createConversation(mode) {
  return request.post('/conversation', { mode })
}

/** 我的会话列表（按最后活跃时间倒序） */
export function listConversations() {
  return request.get('/conversation')
}

/** 某个会话的历史消息 */
export function listMessages(conversationId) {
  return request.get(`/conversation/${conversationId}/messages`)
}

/** 删除会话 */
export function deleteConversation(conversationId) {
  return request.delete(`/conversation/${conversationId}`)
}
