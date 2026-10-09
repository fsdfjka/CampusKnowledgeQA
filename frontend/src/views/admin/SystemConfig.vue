<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemConfig } from '@/mock/admin'

const form = reactive({ ...systemConfig })
const saving = ref(false)

const models = ['qwen-turbo', 'qwen-plus', 'qwen-max', 'qwen-long']

function save() {
  saving.value = true
  setTimeout(() => {
    saving.value = false
    ElMessage.success('系统配置已保存（无需重启）')
  }, 400)
}

function testConnection() {
  ElMessage.success('连接测试成功')
}
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">系统参数配置</span>
      <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
    </div>

    <el-form :model="form" label-width="150px" style="max-width: 640px">
      <el-form-item label="大模型接口地址">
        <el-input v-model="form.apiUrl" placeholder="https://dashscope.aliyuncs.com" />
      </el-form-item>
      <el-form-item label="API Key">
        <el-input v-model="form.apiKey" type="password" show-password placeholder="请输入 API Key" />
      </el-form-item>
      <el-form-item label="对话模型">
        <el-select v-model="form.model" style="width: 100%">
          <el-option v-for="m in models" :key="m" :label="m" :value="m" />
        </el-select>
      </el-form-item>
      <el-form-item label="温度 (temperature)">
        <el-slider v-model="form.temperature" :min="0" :max="1" :step="0.1" show-input />
      </el-form-item>
      <el-form-item label="最大 token">
        <el-input-number v-model="form.maxTokens" :min="256" :max="8192" :step="256" />
      </el-form-item>
      <el-form-item label="检索片段数量 (Top-K)">
        <el-input-number v-model="form.topK" :min="1" :max="10" />
      </el-form-item>
      <el-form-item>
        <el-button @click="testConnection">测试连接</el-button>
      </el-form-item>
    </el-form>

    <el-alert type="warning" :closable="false" show-icon
      title="修改后即时生效，无需重启后端服务。" />
  </el-card>
</template>

<style scoped>
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
