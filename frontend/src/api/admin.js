import http from './http'

/** 查看角色权限（超管） */
export function getPermissions() {
  return http.get('/admin/permissions')
}

/** 修改角色权限（超管） */
export function updatePermissions(data) {
  return http.put('/admin/permissions', data)
}

/** 反馈列表（筛选 + 分页） */
export function listFeedback(params) {
  return http.get('/admin/feedback', { params })
}

/** 标记反馈为已处理 */
export function handleFeedback(id) {
  return http.put(`/admin/feedback/${id}/handle`)
}
