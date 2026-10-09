<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer', module: 'dashboard' },
  {
    title: '知识库管理',
    icon: 'Collection',
    module: 'documents',
    children: [
      { path: '/documents/list', title: '文档管理', icon: 'Document' },
      { path: '/documents/category', title: '分类管理', icon: 'Files' },
    ],
  },
  {
    title: '智能问答',
    icon: 'ChatDotRound',
    module: 'qa',
    children: [
      { path: '/qa/chat', title: '问答对话', icon: 'ChatLineRound' },
      { path: '/qa/records', title: '问答记录', icon: 'Tickets' },
    ],
  },
  { path: '/stats', title: '数据统计', icon: 'DataLine', module: 'stats' },
  { path: '/feedback', title: '反馈管理', icon: 'Star', module: 'feedback' },
  { path: '/sensitive', title: '敏感词管理', icon: 'WarnTriangleFilled', module: 'sensitive' },
  { path: '/users', title: '用户管理', icon: 'UserFilled', module: 'users' },
  { path: '/roles', title: '角色权限', icon: 'Key', module: 'roles' },
  { path: '/logs', title: '日志与备份', icon: 'Notebook', module: 'logs' },
  { path: '/settings', title: '系统配置', icon: 'Setting', module: 'settings' },
]

// 菜单按后端返回的权限模块过滤
const visibleMenus = computed(() => menus.filter((menu) => userStore.canAccess(menu.module)))
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '工作台')

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  if (!userStore.permissions.length) {
    userStore.loadPermissions()
  }
})
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon><Reading /></el-icon>
        <span>校园知识库问答</span>
      </div>
      <el-menu :default-active="activeMenu" router class="sidebar-menu" background-color="#001529"
        text-color="rgba(255,255,255,.72)" active-text-color="#fff">
        <template v-for="menu in visibleMenus" :key="menu.title">
          <el-sub-menu v-if="menu.children" :index="menu.title">
            <template #title>
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.path" :index="child.path">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon" /></el-icon>
            <span>{{ menu.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ pageTitle }}</div>
        <div class="header-right">
          <el-tag :type="userStore.isSuperAdmin ? 'danger' : 'success'" effect="plain">
            {{ userStore.roleLabel || '访客' }}
          </el-tag>
          <el-dropdown @command="handleLogout">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  height: 100%;
}

.sidebar {
  background: #001529;
  overflow-x: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 20px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #1890ff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  outline: none;
}

.main {
  padding: 16px;
}
</style>
