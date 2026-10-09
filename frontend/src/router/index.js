import { createRouter, createWebHistory } from 'vue-router'

const BOTH = ['ADMIN', 'SUPER_ADMIN']
const SUPER = ['SUPER_ADMIN']

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '统一登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/login',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer', module: 'dashboard', roles: BOTH },
      },
      {
        path: 'documents',
        name: 'documents',
        redirect: '/documents/list',
        meta: { title: '知识库管理', icon: 'Collection', module: 'documents', roles: BOTH },
        children: [
          {
            path: 'list',
            name: 'document-list',
            component: () => import('@/views/documents/DocumentManage.vue'),
            meta: { title: '文档管理', icon: 'Document', module: 'documents', roles: BOTH },
          },
          {
            path: 'category',
            name: 'document-category',
            component: () => import('@/views/documents/CategoryManage.vue'),
            meta: { title: '分类管理', icon: 'Files', module: 'documents', roles: BOTH },
          },
        ],
      },
      {
        path: 'qa',
        name: 'qa',
        redirect: '/qa/chat',
        meta: { title: '智能问答', icon: 'ChatDotRound', module: 'qa', roles: BOTH },
        children: [
          {
            path: 'chat',
            name: 'qa-chat',
            component: () => import('@/views/qa/QaChat.vue'),
            meta: { title: '问答对话', icon: 'ChatLineRound', module: 'qa', roles: BOTH },
          },
          {
            path: 'records',
            name: 'qa-records',
            component: () => import('@/views/qa/QaRecords.vue'),
            meta: { title: '问答记录', icon: 'Tickets', module: 'qa', roles: BOTH },
          },
        ],
      },
      {
        path: 'stats',
        name: 'stats',
        component: () => import('@/views/stats/Statistics.vue'),
        meta: { title: '数据统计', icon: 'DataLine', module: 'stats', roles: BOTH },
      },
      {
        path: 'feedback',
        name: 'feedback',
        component: () => import('@/views/admin/FeedbackManage.vue'),
        meta: { title: '反馈管理', icon: 'Star', module: 'feedback', roles: BOTH },
      },
      {
        path: 'sensitive',
        name: 'sensitive',
        component: () => import('@/views/admin/SensitiveManage.vue'),
        meta: { title: '敏感词管理', icon: 'WarnTriangleFilled', module: 'sensitive', roles: BOTH },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', module: 'users', roles: SUPER },
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/admin/RoleManage.vue'),
        meta: { title: '角色权限', icon: 'Key', module: 'roles', roles: SUPER },
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('@/views/admin/LogBackup.vue'),
        meta: { title: '日志与备份', icon: 'Notebook', module: 'logs', roles: SUPER },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/admin/SystemConfig.vue'),
        meta: { title: '系统配置', icon: 'Setting', module: 'settings', roles: SUPER },
      },
    ],
  },
  { path: '/student/login', redirect: '/login' },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/chat',
    children: [
      {
        path: 'chat',
        name: 'student-chat',
        component: () => import('@/views/student/StudentChat.vue'),
        meta: { title: '智能问答' },
      },
      {
        path: 'knowledge',
        name: 'student-knowledge',
        component: () => import('@/views/student/StudentKnowledge.vue'),
        meta: { title: '知识库' },
      },
      {
        path: 'history',
        name: 'student-history',
        component: () => import('@/views/student/StudentHistory.vue'),
        meta: { title: '历史记录' },
      },
      {
        path: 'hot',
        name: 'student-hot',
        component: () => import('@/views/student/StudentHot.vue'),
        meta: { title: '热门问题' },
      },
      {
        path: 'profile',
        name: 'student-profile',
        component: () => import('@/views/student/StudentProfile.vue'),
        meta: { title: '个人中心' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

const VALID_ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN']

function currentAdminRole() {
  try {
    return JSON.parse(localStorage.getItem('ck_user') || 'null')?.role || ''
  } catch {
    return ''
  }
}

function isTokenExpired(token) {
  if (!token) return true
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    )
    const payload = JSON.parse(json)
    return !payload.exp || payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

function hasValidAdminSession() {
  const token = localStorage.getItem('ck_token')
  return !!token && !isTokenExpired(token) && VALID_ADMIN_ROLES.includes(currentAdminRole())
}

function hasValidStudentSession() {
  const token = localStorage.getItem('stu_token')
  return !!token && !isTokenExpired(token)
}

function currentAdminPermissions() {
  try {
    return JSON.parse(localStorage.getItem('ck_perms') || '[]')
  } catch {
    return []
  }
}

router.beforeEach((to) => {
  // 清理无效/过期的会话
  if (localStorage.getItem('ck_token') && !hasValidAdminSession()) {
    localStorage.removeItem('ck_token')
    localStorage.removeItem('ck_user')
    localStorage.removeItem('ck_perms')
  }
  if (localStorage.getItem('stu_token') && !hasValidStudentSession()) {
    localStorage.removeItem('stu_token')
    localStorage.removeItem('stu_user')
  }

  const studentOk = hasValidStudentSession()
  const adminOk = hasValidAdminSession()

  if (to.path === '/login') {
    // 始终展示登录页（即使浏览器里还有登录态）
    return true
  }

  if (to.path.startsWith('/student')) {
    if (studentOk) return true
    if (adminOk) return { path: '/dashboard' }
    return { path: '/login' }
  }

  // 后台区域
  if (!adminOk) {
    if (studentOk) return { path: '/student/chat' }
    return { path: '/login' }
  }
  // 按后端下发的权限模块校验
  const moduleKey = to.meta?.module
  if (moduleKey && !currentAdminPermissions().includes(moduleKey) && to.path !== '/dashboard') {
    return { path: '/dashboard' }
  }
  return true
})

export default router
