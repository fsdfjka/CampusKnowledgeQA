<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStudentStore } from '@/stores/student'

const route = useRoute()
const router = useRouter()
const studentStore = useStudentStore()

const navs = [
  { path: '/student/chat', title: '智能问答', icon: 'ChatDotRound' },
  { path: '/student/knowledge', title: '知识库', icon: 'Collection' },
  { path: '/student/history', title: '历史记录', icon: 'Clock' },
  { path: '/student/hot', title: '热门问题', icon: 'TrendCharts' },
]

const activePath = computed(() => route.path)

function go(path) {
  router.push(path)
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/student/profile')
  } else if (command === 'logout') {
    studentStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="student-layout">
    <header class="navbar">
      <div class="navbar-inner">
        <div class="brand" @click="go('/student/chat')">
          <el-icon :size="24"><Reading /></el-icon>
          <span>校园知识库问答</span>
        </div>

        <nav class="nav">
          <a v-for="nav in navs" :key="nav.path" class="nav-item"
            :class="{ active: activePath === nav.path }" @click="go(nav.path)">
            <el-icon><component :is="nav.icon" /></el-icon>
            <span>{{ nav.title }}</span>
          </a>
        </nav>

        <div class="right">
          <el-dropdown @command="handleCommand">
            <span class="user">
              <el-avatar :size="28" icon="UserFilled" />
              {{ studentStore.userInfo?.nickname || studentStore.userInfo?.username || '同学' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.student-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 6px rgba(0, 21, 41, 0.08);
}

.navbar-inner {
  display: flex;
  align-items: center;
  height: 60px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1890ff;
  cursor: pointer;
  white-space: nowrap;
}

.nav {
  display: flex;
  gap: 8px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 6px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-item:hover {
  color: #1890ff;
  background: #f0f7ff;
}

.nav-item.active {
  color: #1890ff;
  background: #e6f4ff;
  font-weight: 600;
}

.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}

.content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
</style>
