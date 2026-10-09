import http from './http'

/** 新建会话，返回 { id, title } */
export function createConversation(title) {
  return http.post('/conversations', { title })
}

/** 会话消息列表 */
export function listMessages(conversationId) {
  return http.get(`/conversations/${conversationId}/messages`)
}
