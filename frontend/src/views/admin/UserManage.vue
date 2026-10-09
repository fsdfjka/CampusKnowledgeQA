<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { users as mockUsers, ROLE_LABELS } from '@/mock/admin'

const list = ref(mockUsers.map((item) => ({ ...item })))
const keyword = ref('')
const roleFilter = ref('')
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ username: '', nickname: '', role: 'STUDENT', studentNo: '', college: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名/学号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
}

const filtered = computed(() =>
  list.value.filter((item) => {
    const matchKeyword =
      !keyword.value || item.username.includes(keyword.value) || item.nickname.includes(keyword.value)
    const matchRole = !roleFilter.value || item.role === roleFilter.value
    return matchKeyword && matchRole
  }),
)

function roleType(role) {
  if (role === 'SUPER_ADMIN') return 'danger'
  if (role === 'ADMIN') return 'warning'
  return 'info'
}

function openCreate() {
  Object.assign(form, { username: '', nickname: '', role: 'STUDENT', studentNo: '', college: '', password: '' })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  if (list.value.some((item) => item.username === form.username)) {
    ElMessage.warning('用户名已存在')
    return
  }
  list.value.unshift({
    id: Date.now(),
    username: form.username,
    nickname: form.nickname,
    role: form.role,
    studentNo: form.studentNo,
    college: form.college,
    status: 1,
    createdAt: new Date().toLocaleString(),
  })
  dialogVisible.value = false
  ElMessage.success('新增成功')
}

function toggleStatus(row) {
  row.status = row.status ? 0 : 1
  ElMessage.success(row.status ? '已启用' : '已禁用')
}

function resetPassword(row) {
  ElMessageBox.confirm(`将「${row.nickname}」的密码重置为 123456？`, '提示', { type: 'warning' })
    .then(() => ElMessage.success('密码已重置为 123456'))
    .catch(() => {})
}
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">用户管理</span>
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索用户名/姓名" clearable style="width: 200px"
          :prefix-icon="'Search'" />
        <el-select v-model="roleFilter" placeholder="全部角色" clearable style="width: 140px">
          <el-option label="学生" value="STUDENT" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="超级管理员" value="SUPER_ADMIN" />
        </el-select>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table :data="filtered" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="姓名" width="120" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="roleType(row.role)" effect="plain">{{ ROLE_LABELS[row.role] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="college" label="学院" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'" effect="plain">
            {{ row.status ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button :type="row.status ? 'danger' : 'success'" link @click="toggleStatus(row)">
            {{ row.status ? '禁用' : '启用' }}
          </el-button>
          <el-button type="primary" link @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增用户" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="学号 / 登录名" />
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="form.college" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 12px;
}
</style>
