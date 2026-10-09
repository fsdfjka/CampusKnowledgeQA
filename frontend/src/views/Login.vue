<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useStudentStore } from '@/stores/student'
import { login as loginApi, register as registerApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const studentStore = useStudentStore()

const formRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const mode = ref('login')

const roles = [
  { label: '学生', value: 'STUDENT', username: 'student', password: '123456', desc: '进入问答前台' },
  { label: '管理员', value: 'ADMIN', username: 'admin', password: 'admin123', desc: '进入后台管理' },
  { label: '超级管理员', value: 'SUPER_ADMIN', username: 'superadmin', password: 'super123', desc: '进入后台管理（最高权限）' },
]

const form = reactive({ role: 'STUDENT', username: 'student', password: '123456' })
const registerForm = reactive({ username: '', nickname: '', password: '', confirm: '' })

const currentRole = computed(() => roles.find((r) => r.value === form.role))
const isStudent = computed(() => form.role === 'STUDENT')

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [{ required: true, message: '请输入学号/用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === registerForm.password ? cb() : cb(new Error('两次密码不一致')),
      trigger: 'blur',
    },
  ],
}

function onRoleChange() {
  mode.value = 'login'
  form.username = currentRole.value.username
  form.password = currentRole.value.password
}

function switchMode(target) {
  if (target === 'register' && !isStudent.value) {
    ElMessage.warning('仅学生可注册')
    return
  }
  mode.value = target
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await loginApi({ username: form.username, password: form.password })
    const data = res.data
    if (data.role !== form.role) {
      ElMessage.error(`该账号是「${roles.find((r) => r.value === data.role)?.label || data.role}」，请切换对应角色登录`)
      return
    }
    if (data.role === 'STUDENT') {
      studentStore.login(data)
      router.push('/student/chat')
    } else {
      userStore.login(data)
      router.push('/dashboard')
    }
    ElMessage.success('登录成功，欢迎回来')
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname,
    })
    ElMessage.success('注册成功，请登录')
    form.role = 'STUDENT'
    form.username = registerForm.username
    form.password = registerForm.password
    mode.value = 'login'
  } catch (e) {
    // 错误已由 axios 拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="brand">
        <el-icon :size="34" color="#1890ff"><Reading /></el-icon>
        <h2>校园知识库问答系统</h2>
        <p class="text-muted">{{ mode === 'login' ? '统一登录入口' : '学生注册' }}</p>
      </div>

      <el-radio-group v-model="form.role" class="role-group" @change="onRoleChange">
        <el-radio-button v-for="item in roles" :key="item.value" :value="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
      <p class="role-desc text-muted">{{ currentRole?.desc }}</p>

      <el-form v-if="mode === 'login'" ref="formRef" :model="form" :rules="rules" size="large"
        @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名 / 学号" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码"
            :prefix-icon="'Lock'" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
        <div v-if="isStudent" class="switch-line">
          还没有账号？<el-link type="primary" @click="switchMode('register')">注册新账号</el-link>
        </div>
      </el-form>

      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="学号 / 用户名" :prefix-icon="'User'" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="姓名" :prefix-icon="'Avatar'" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="密码"
            :prefix-icon="'Lock'" />
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="registerForm.confirm" type="password" show-password placeholder="确认密码"
            :prefix-icon="'Lock'" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleRegister">
          注 册
        </el-button>
        <div class="switch-line">
          已有账号？<el-link type="primary" @click="switchMode('login')">返回登录</el-link>
        </div>
      </el-form>

      <div class="demo-tip text-muted">
        演示账号：student/123456（学生）· admin/admin123（管理员）· superadmin/super123（超级管理员）
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #001529 100%);
}

.login-card {
  width: 420px;
  border-radius: 10px;
  padding: 12px 8px;
}

.brand {
  text-align: center;
  margin-bottom: 20px;
}

.brand h2 {
  margin: 8px 0 0;
  font-size: 20px;
}

.brand p {
  margin: 4px 0 0;
}

.role-group {
  display: flex;
  width: 100%;
  margin-bottom: 8px;
}

.role-group :deep(.el-radio-button) {
  flex: 1;
}

.role-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.role-desc {
  margin: 0 0 16px;
  font-size: 12px;
  text-align: center;
}

.login-btn {
  width: 100%;
}

.switch-line {
  margin-top: 12px;
  font-size: 13px;
  text-align: center;
}

.demo-tip {
  margin-top: 14px;
  font-size: 12px;
  text-align: center;
  line-height: 1.6;
}
</style>
