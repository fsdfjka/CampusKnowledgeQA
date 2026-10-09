import http from './http'

/** 查看角色权限（超管） */
export function getPermissions() {
  return http.get('/admin/permissions')
}

/** 修改角色权限（超管） */
export function updatePermissions(data) {
  return http.put('/admin/permissions', data)
}

/** 统计总览 */
export function statsOverview() {
  return http.get('/admin/stats/overview')
}

/** 每日提问量 */
export function statsDaily(params) {
  return http.get('/admin/stats/daily', { params })
}

/** 热门问题 TOP N */
export function statsHot(params) {
  return http.get('/admin/stats/hot', { params })
}

/** 反馈分布 */
export function statsFeedback() {
  return http.get('/admin/stats/feedback')
}

/** 用户列表分页 */
export function listUsers(params) {
  return http.get('/admin/users', { params })
}

/** 新增用户 */
export function createUser(data) {
  return http.post('/admin/users', data)
}

/** 启用 / 禁用账号 */
export function setUserStatus(id, status) {
  return http.put(`/admin/users/${id}/status`, { status })
}

/** 重置用户密码（返回随机密码） */
export function resetUserPassword(id) {
  return http.put(`/admin/users/${id}/reset-password`)
}

/** 操作日志分页查询 */
export function listLogs(params) {
  return http.get('/admin/logs', { params })
}

/** 反馈列表（筛选 + 分页） */
export function listFeedback(params) {
  return http.get('/admin/feedback', { params })
}

/** 设置反馈处理状态（handled=false 取消处理） */
export function handleFeedback(id, handled = true) {
  return http.put(`/admin/feedback/${id}/handle`, { handled })
}

/** 敏感词列表 */
export function listSensitiveWords(params) {
  return http.get('/admin/sensitive-words', { params })
}

/** 新增敏感词 */
export function addSensitiveWord(word) {
  return http.post('/admin/sensitive-words', { word })
}

/** 删除敏感词 */
export function deleteSensitiveWord(id) {
  return http.delete(`/admin/sensitive-words/${id}`)
}
