import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('stu_token') || localStorage.getItem('ck_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('stu_token')
      localStorage.removeItem('stu_user')
      localStorage.removeItem('ck_token')
      localStorage.removeItem('ck_user')
      ElMessage.error('登录已过期，请重新登录')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    const msg = error?.response?.data?.msg || error?.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default http
