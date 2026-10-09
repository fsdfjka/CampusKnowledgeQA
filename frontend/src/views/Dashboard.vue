<script setup>
import { computed, onMounted, ref } from 'vue'
import { statsOverview } from '@/api/admin'
import { adminRecords } from '@/api/qa'

const overview = ref({ documents: 0, categories: 0, questions: 0, goodRate: 0 })
const recent = ref([])

const cards = computed(() => [
  { label: '知识库文档', value: overview.value.documents, icon: 'Document', color: '#1890ff' },
  { label: '知识库分类', value: overview.value.categories, icon: 'Files', color: '#52c41a' },
  { label: '累计提问量', value: overview.value.questions, icon: 'ChatDotRound', color: '#faad14' },
  { label: '问答好评率', value: `${overview.value.goodRate}%`, icon: 'Star', color: '#f5222d' },
])

function feedbackLabel(feedback) {
  return { LIKE: '点赞', DISLIKE: '点踩', NONE: '未反馈' }[feedback] || '未反馈'
}

function feedbackType(feedback) {
  if (feedback === 'LIKE') return 'success'
  if (feedback === 'DISLIKE') return 'danger'
  return 'info'
}

async function load() {
  const [o, records] = await Promise.all([
    statsOverview(),
    adminRecords({ page: 1, size: 5 }),
  ])
  overview.value = o?.data || overview.value
  recent.value = records?.data?.list || []
}

onMounted(load)
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-inner">
            <el-icon :size="42" :color="card.color"><component :is="card.icon" /></el-icon>
            <div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="text-muted">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card recent" shadow="never">
      <div class="page-toolbar">
        <span class="title">最近问答记录</span>
        <router-link to="/qa/records">
          <el-button type="primary" link>查看全部</el-button>
        </router-link>
      </div>
      <el-table :data="recent" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="username" label="提问用户" width="140">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="反馈" width="90">
          <template #default="{ row }">
            <el-tag :type="feedbackType(row.feedback)" effect="plain">{{ feedbackLabel(row.feedback) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160" />
        <template #empty>暂无问答记录</template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stat-card {
  margin-bottom: 16px;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.recent {
  margin-top: 8px;
}
</style>
