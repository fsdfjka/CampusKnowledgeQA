<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { me, updateProfile, changePassword } from '@/api/auth'
import { myRecords, setFavorite } from '@/api/qa'
import { useStudentStore } from '@/stores/student'

const studentStore = useStudentStore()

const loading = ref(false)
const saving = ref(false)
const changing = ref(false)
const activeMenu = ref('profile')
const favorites = ref([])
const favLoading = ref(false)

const profile = reactive({
  username: '',
  nickname: '',
  studentNo: '',
  college: '',
  major: '',
  email: '',
  phone: '',
  role: '',
})

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function load() {
  loading.value = true
  try {
    const res = await me()
    const data = res?.data || {}
    Object.assign(profile, {
      username: data.username || '',
      nickname: data.nickname || '',
      studentNo: data.studentNo || '',
      college: data.college || '',
      major: data.major || '',
      email: data.email || '',
      phone: data.phone || '',
      role: data.role || '',
    })
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile({
      nickname: profile.nickname,
      studentNo: profile.studentNo,
      college: profile.college,
      major: profile.major,
      email: profile.email,
      phone: profile.phone,
    })
    studentStore.userInfo = { ...(studentStore.userInfo || {}), nickname: profile.nickname }
    localStorage.setItem('stu_user', JSON.stringify(studentStore.userInfo))
    ElMessage.success('个人信息已保存')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirm) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  changing.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirm = ''
    ElMessage.success('密码修改成功')
  } finally {
    changing.value = false
  }
}

async function loadFavorites() {
  favLoading.value = true
  try {
    const res = await myRecords({ favorite: true, page: 1, size: 100 })
    favorites.value = res?.data?.list || []
  } finally {
    favLoading.value = false
  }
}

function handleMenuSelect(key) {
  activeMenu.value = key
  if (key === 'favorites') {
    loadFavorites()
  }
}

async function removeFavorite(item) {
  await setFavorite(item.id, false)
  favorites.value = favorites.value.filter((f) => f.id !== item.id)
  ElMessage.success('已取消收藏')
}

onMounted(load)
</script>

<template>
  <el-row :gutter="16" v-loading="loading">
    <el-col :xs="24" :md="6">
      <el-card class="page-card menu-card" shadow="never">
        <el-menu :default-active="activeMenu" @select="handleMenuSelect">
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
          <el-menu-item index="favorites">
            <el-icon><StarFilled /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
        </el-menu>
      </el-card>
    </el-col>

    <el-col :xs="24" :md="18">
      <el-card v-if="activeMenu === 'profile'" class="page-card" shadow="never">
        <div class="page-toolbar"><span class="title">个人资料</span></div>
        <el-form :model="profile" label-width="90px" style="max-width: 520px">
          <el-form-item label="用户名">
            <el-input v-model="profile.username" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="profile.nickname" />
          </el-form-item>
          <el-form-item label="学号">
            <el-input v-model="profile.studentNo" />
          </el-form-item>
          <el-form-item label="学院">
            <el-input v-model="profile.college" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="profile.major" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profile.email" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profile.phone" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-else-if="activeMenu === 'favorites'" class="page-card" shadow="never">
        <div class="page-toolbar"><span class="title">我的收藏</span></div>
        <div v-loading="favLoading">
          <el-empty v-if="!favorites.length" description="暂无收藏" />
          <div v-for="item in favorites" :key="item.id" class="fav-item">
            <div class="fav-head">
              <span class="fav-q"><el-icon><StarFilled /></el-icon> {{ item.question }}</span>
              <el-button link type="danger" @click="removeFavorite(item)">取消收藏</el-button>
            </div>
            <div class="fav-answer">{{ item.answer }}</div>
            <div class="text-muted fav-time">{{ item.createdAt }}</div>
          </div>
        </div>
      </el-card>

      <el-card v-else class="page-card" shadow="never">
        <div class="page-toolbar"><span class="title">修改密码</span></div>
        <el-form :model="passwordForm" label-width="90px" style="max-width: 520px">
          <el-form-item label="原密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirm" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="changing" @click="handleChangePassword">确认修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}

.menu-card {
  padding: 0;
}

.menu-card :deep(.el-card__body) {
  padding: 0;
}

.menu-card :deep(.el-menu) {
  border-right: none;
}

.fav-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
}

.fav-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.fav-q {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #303133;
}

.fav-answer {
  margin: 10px 0;
  padding: 10px 14px;
  background: #f7f9fc;
  border-radius: 6px;
  line-height: 1.7;
  color: #606266;
  white-space: pre-wrap;
}

.fav-time {
  font-size: 12px;
  text-align: right;
}
</style>
