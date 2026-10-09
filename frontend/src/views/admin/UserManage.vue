<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, setUserStatus, resetUserPassword } from '@/api/admin'

const ROLE_LABELS = { STUDENT: '学生', ADMIN: '管理员', SUPER_ADMIN: '超级管理员' }

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = reactive({ keyword: '', role: '', status: '' })

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  nickname: '',
  role: 'STUDENT',
  studentNo: '',
  college: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名/学号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

function roleType(role) {
  if (role === 'SUPER_ADMIN') return 'danger'
  if (role === 'ADMIN') return 'warning'
  return 'info'
}

async function loadList() {
  loading.value = true
  try {
    const res = await listUsers({
      keyword: filters.keyword || undefined,
      role: filters.role || undefined,
      status: filters.status === '' || filters.status == null ? undefined : filters.status,
      page: page.value,
      size: size.value,
    })
    const data = res?.data || {}
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadList()
}

function reset() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  search()
}

function openCreate() {
  Object.assign(form, { username: '', nickname: '', role: 'STUDENT', studentNo: '', college: '', password: '' })
  dialogVisible.value = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await createUser({ ...form })
    dialogVisible.value = false
    ElMessage.success('新增成功')
    await loadList()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status ? 0 : 1
  await setUserStatus(row.id, next)
  ElMessage.success(next ? '已启用' : '已禁用')
  await loadList()
}

function resetPassword(row) {
  ElMessageBox.confirm(`确认将「${row.nickname || row.username}」的密码重置为随机密码？`, '提示', { type: 'warning' })
    .then(async () => {
      const res = await resetUserPassword(row.id)
      const password = res?.data?.password
      ElMessageBox.alert(`新密码：${password}\n请立即告知用户并提醒修改。`, '密码已重置', {
        confirmButtonText: '知道了',
      })
    })
    .catch(() => {})
}

onMounted(loadList)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">用户管理</span>
      <div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜索用户名/姓名" clearable style="width: 200px"
          :prefix-icon="'Search'" @keyup.enter="search" @clear="search" />
        <el-select v-model="filters.role" placeholder="全部角色" clearable style="width: 140px" @change="search">
          <el-option label="学生" value="STUDENT" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="超级管理员" value="SUPER_ADMIN" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px" @change="search">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="姓名" width="120" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="roleType(row.role)" effect="plain">{{ ROLE_LABELS[row.role] || row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="studentNo" label="学号" width="130">
        <template #default="{ row }">{{ row.studentNo || '-' }}</template>
      </el-table-column>
      <el-table-column prop="college" label="学院" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.college || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'" effect="plain">
            {{ row.status ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button :type="row.status ? 'danger' : 'success'" link @click="toggleStatus(row)">
            {{ row.status ? '禁用' : '启用' }}
          </el-button>
          <el-button type="primary" link @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无用户</template>
    </el-table>

    <el-pagination v-if="total > 0" class="pager" background layout="prev, pager, next, total"
      :total="total" :page-size="size" :current-page="page"
      @current-change="(p) => { page = p; loadList() }" />

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
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="form.college" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
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

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
