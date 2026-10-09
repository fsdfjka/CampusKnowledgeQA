import http from './http'

/** 我的问答历史（分页，支持 keyword / favorite 筛选） */
export function myRecords(params) {
  return http.get('/qa/records/my', { params })
}

/** 单条问答详情 */
export function recordDetail(id) {
  return http.get(`/qa/records/${id}`)
}

/** 提交问答反馈：{ recordId, type: 'LIKE' | 'DISLIKE', reason? } */
export function submitFeedback(payload) {
  return http.post('/qa/feedback', payload)
}

/** 收藏 / 取消收藏（PUT 设置语义） */
export function setFavorite(id, favorite) {
  return http.put(`/qa/records/${id}/favorite`, { favorite })
}

/** 管理员检索问答记录 */
export function adminRecords(params) {
  return http.get('/admin/qa/records', { params })
}

/** 有问答记录的用户（后台筛选下拉） */
export function recordUsers() {
  return http.get('/admin/qa/record-users')
}

/** 热门问题 */
export function hotQuestions(params) {
  return http.get('/qa/hot', { params })
}
