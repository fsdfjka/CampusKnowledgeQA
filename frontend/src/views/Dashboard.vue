<script setup>
import { computed } from 'vue'
import { overview } from '@/mock/stats'
import { qaRecords } from '@/mock/qa'

const cards = computed(() => [
  { label: '知识库文档', value: overview.documents, icon: 'Document', color: '#1890ff' },
  { label: '知识库分类', value: overview.categories, icon: 'Files', color: '#52c41a' },
  { label: '累计提问量', value: overview.questions, icon: 'ChatDotRound', color: '#faad14' },
  { label: '问答好评率', value: `${overview.goodRate}%`, icon: 'Star', color: '#f5222d' },
])

const recent = computed(() => qaRecords.slice(0, 5))

function feedbackType(feedback) {
  if (feedback === '点赞') return 'success'
  if (feedback === '点踩') return 'danger'
  return 'info'
}
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
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="user" label="提问用户" width="140" />
        <el-table-column label="反馈" width="90">
          <template #default="{ row }">
            <el-tag :type="feedbackType(row.feedback)" effect="plain">{{ row.feedback }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
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
