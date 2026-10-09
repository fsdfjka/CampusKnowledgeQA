export const ROLE_LABELS = {
  STUDENT: '学生',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

/** 登录账号（前端模拟鉴权） */
export const accounts = [
  { username: 'student', password: '123456', role: 'STUDENT', nickname: '张三' },
  { username: 'admin', password: 'admin123', role: 'ADMIN', nickname: '管理员' },
  { username: 'superadmin', password: 'super123', role: 'SUPER_ADMIN', nickname: '超级管理员' },
]

export function findAccount(username, password) {
  return accounts.find((a) => a.username === username && a.password === password)
}

/** 角色可访问模块 */
export const roleModules = [
  { key: 'dashboard', name: '工作台' },
  { key: 'documents', name: '知识库管理' },
  { key: 'qa', name: '问答记录' },
  { key: 'feedback', name: '反馈管理' },
  { key: 'stats', name: '数据统计' },
  { key: 'sensitive', name: '敏感词管理' },
  { key: 'users', name: '用户管理' },
  { key: 'roles', name: '角色权限' },
  { key: 'logs', name: '日志与备份' },
  { key: 'settings', name: '系统配置' },
]

export const rolePermissions = {
  ADMIN: ['dashboard', 'documents', 'qa', 'feedback', 'stats', 'sensitive'],
  SUPER_ADMIN: [
    'dashboard', 'documents', 'qa', 'feedback', 'stats', 'sensitive',
    'users', 'roles', 'logs', 'settings',
  ],
}

/** 用户列表（超管管理） */
export const users = [
  { id: 1, username: 'student', nickname: '张三', role: 'STUDENT', studentNo: '20260101', college: '计算机与信息科学学院', status: 1, createdAt: '2026-03-01 10:00' },
  { id: 2, username: 'lisi', nickname: '李四', role: 'STUDENT', studentNo: '20260102', college: '外国语学院', status: 1, createdAt: '2026-03-02 11:20' },
  { id: 3, username: 'wangwu', nickname: '王五', role: 'STUDENT', studentNo: '20250120', college: '经济管理学院', status: 0, createdAt: '2026-02-20 15:40' },
  { id: 4, username: 'admin', nickname: '管理员', role: 'ADMIN', studentNo: '', college: '', status: 1, createdAt: '2026-03-01 09:00' },
  { id: 5, username: 'superadmin', nickname: '超级管理员', role: 'SUPER_ADMIN', studentNo: '', college: '', status: 1, createdAt: '2026-03-01 08:00' },
]

/** 反馈记录（管理员查看） */
export const feedbacks = [
  { id: 1, user: '2025级赵六', question: '转专业需要满足什么条件？', type: 'DISLIKE', reason: '回答信息过时，未包含最新政策', handled: 0, createdAt: '2026-03-11 08:55:20' },
  { id: 2, user: '2024级李四', question: '国家奖学金什么时候申请？', type: 'DISLIKE', reason: '没有给出具体时间节点', handled: 1, createdAt: '2026-03-10 14:20:10' },
  { id: 3, user: '2026级张三', question: '宿舍可以带电脑吗？', type: 'LIKE', reason: '回答准确，很有帮助', handled: 1, createdAt: '2026-03-10 09:30:00' },
  { id: 4, user: '2025级王五', question: '校园卡怎么挂失？', type: 'DISLIKE', reason: '给的办理地点不对', handled: 0, createdAt: '2026-03-09 16:12:45' },
]

/** 敏感词库 */
export const sensitiveWords = [
  { id: 1, word: '违禁词', createdAt: '2026-03-01 10:00' },
  { id: 2, word: '暴恐', createdAt: '2026-03-01 10:00' },
  { id: 3, word: '代考', createdAt: '2026-03-02 09:30' },
  { id: 4, word: '黄牛', createdAt: '2026-03-03 11:15' },
]

/** 操作日志 */
export const operationLogs = [
  { id: 1, username: 'superadmin', module: '系统配置', action: '修改', detail: '更新大模型温度 0.5 -> 0.7', ip: '127.0.0.1', createdAt: '2026-03-11 15:02:11' },
  { id: 2, username: 'admin', module: '文档管理', action: '上传', detail: '上传 2026级新生入学手册.pdf', ip: '127.0.0.1', createdAt: '2026-03-11 10:20:33' },
  { id: 3, username: 'admin', module: '文档管理', action: '删除', detail: '删除 过期通知.txt', ip: '127.0.0.1', createdAt: '2026-03-10 17:45:09' },
  { id: 4, username: 'admin', module: '登录', action: '登录', detail: '管理员登录成功', ip: '127.0.0.1', createdAt: '2026-03-10 09:00:00' },
  { id: 5, username: 'superadmin', module: '用户管理', action: '禁用', detail: '禁用账号 wangwu', ip: '127.0.0.1', createdAt: '2026-03-09 11:30:00' },
]

/** 系统配置 */
export const systemConfig = {
  apiUrl: 'https://dashscope.aliyuncs.com',
  apiKey: 'sk-****',
  model: 'qwen-turbo',
  temperature: 0.7,
  maxTokens: 2048,
  topK: 3,
}
