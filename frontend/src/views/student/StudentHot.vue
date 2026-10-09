<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { hotQuestions } from '@/api/qa'

const router = useRouter()
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await hotQuestions({ limit: 10, days: 30 })
    list.value = res?.data || []
  } finally {
    loading.value = false
  }
}

function ask(question) {
  router.push({ path: '/student/chat', query: { q: question } })
}

function rankType(index) {
  if (index === 0) return 'danger'
  if (index === 1) return 'warning'
  if (index === 2) return 'success'
  return 'info'
}

onMounted(load)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title"><el-icon><TrendCharts /></el-icon> 热门校园问题</span>
      <span class="text-muted">近 30 天 · 点击可直接向 AI 提问</span>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!list.length" description="暂无热门问题" />

      <div v-for="(item, index) in list" :key="item.question" class="hot-item" @click="ask(item.question)">
        <el-tag :type="rankType(index)" effect="dark" class="rank">{{ index + 1 }}</el-tag>
        <div class="hot-main">
          <div class="hot-question">{{ item.question }}</div>
          <div class="text-muted">最近提问：{{ item.lastAskedAt || '-' }}</div>
        </div>
        <div class="hot-count">
          <el-icon><View /></el-icon> {{ item.count }}
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 8px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  transition: background 0.2s;
}

.hot-item:hover {
  background: #f7f9fc;
}

.rank {
  width: 26px;
  text-align: center;
}

.hot-main {
  flex: 1;
}

.hot-question {
  font-weight: 500;
  margin-bottom: 2px;
}

.hot-count {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
}
</style>
