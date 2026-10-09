import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { me } from '@/api/auth'

const ROLE_LABELS = {
  STUDENT: '学生',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('ck_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('ck_user') || 'null'))
  const permissions = ref(JSON.parse(localStorage.getItem('ck_perms') || '[]'))

  const role = computed(() => userInfo.value?.role || '')
  const roleLabel = computed(() => ROLE_LABELS[role.value] || '')
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')
  const modules = computed(() => permissions.value)

  function login(data) {
    token.value = data.token
    userInfo.value = {
      username: data.username,
      nickname: data.nickname,
      role: data.role,
    }
    localStorage.setItem('ck_token', data.token)
    localStorage.setItem('ck_user', JSON.stringify(userInfo.value))
    loadPermissions()
  }

  async function loadPermissions() {
    try {
      const res = await me()
      permissions.value = res?.data?.permissions || []
      localStorage.setItem('ck_perms', JSON.stringify(permissions.value))
    } catch (e) {
      // 未登录或失败：忽略
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('ck_token')
    localStorage.removeItem('ck_user')
    localStorage.removeItem('ck_perms')
  }

  function canAccess(moduleKey) {
    return permissions.value.includes(moduleKey)
  }

  return {
    token,
    userInfo,
    role,
    roleLabel,
    isSuperAdmin,
    permissions,
    modules,
    login,
    logout,
    loadPermissions,
    canAccess,
  }
})
