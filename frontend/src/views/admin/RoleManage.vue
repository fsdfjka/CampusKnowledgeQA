<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPermissions, updatePermissions } from '@/api/admin'

const modules = [
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

const roles = [
  { key: 'ADMIN', label: '管理员' },
  { key: 'SUPER_ADMIN', label: '超级管理员' },
]

const perms = ref({ ADMIN: [], SUPER_ADMIN: [] })
const loading = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getPermissions()
    perms.value = res?.data || { ADMIN: [], SUPER_ADMIN: [] }
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await updatePermissions(perms.value)
    ElMessage.success('权限已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">角色权限分配</span>
      <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
    </div>

    <el-alert type="info" :closable="false" show-icon class="tip"
      title="勾选各角色可访问的功能模块；超级管理员默认拥有全部权限。保存后重新登录生效。" />

    <el-table v-loading="loading" :data="modules" border>
      <el-table-column prop="name" label="功能模块" width="200" />
      <el-table-column v-for="role in roles" :key="role.key" :label="role.label" align="center">
        <template #default="{ row }">
          <el-checkbox v-model="perms[role.key]" :value="row.key">
            {{ perms[role.key]?.includes(row.key) ? '可访问' : '无权限' }}
          </el-checkbox>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.tip {
  margin-bottom: 16px;
}
</style>
