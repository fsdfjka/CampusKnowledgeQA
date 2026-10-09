import http from './http'

/** 登录，返回 { token, username, nickname, role } */
export function login(data) {
  return http.post('/auth/login', data)
}

/** 学生注册 */
export function register(data) {
  return http.post('/auth/register', data)
}

/** 当前登录用户 */
export function me() {
  return http.get('/auth/me')
}

/** 修改密码 */
export function changePassword(data) {
  return http.post('/auth/change-password', data)
}

/** 修改个人资料 */
export function updateProfile(data) {
  return http.put('/auth/profile', data)
}
