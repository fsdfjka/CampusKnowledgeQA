<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { myRecords, recordDetail, setFavorite } from '@/api/qa'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const onlyFavorite = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const detailData = ref(null)

async function loadList() {
  loading.value = true
  try {
    const res = await myRecords({
      keyword: keyword.value || undefined,
      favorite: onlyFavorite.value ? true : undefined,
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

function feedbackLabel(feedback) {
  if (feedback === 'LIKE') return '点赞'
  if (feedback === 'DISLIKE') return '点踩'
  return ''
}

function askAgain(row) {
  router.push({ path: '/student/chat', query: { q: row.question } })
}

async function toggleFavorite(item) {
  const next = !item.favorite
  await setFavorite(item.id, next)
  item.favorite = next
  ElMessage.success(next ? '已收藏' : '已取消收藏')
  if (onlyFavorite.value && !next) {
    loadList()
  }
}

async function openDetail(row) {
  const res = await recordDetail(row.id)
  detailData.value = res?.data || null
  detailVisible.value = true
}

onMounted(loadList)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title"><el-icon><Clock /></el-icon> 我的问答记录</span>
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索问题" clearable style="width: 220px"
          :prefix-icon="'Search'" @keyup.enter="search" @clear="search" />
        <el-radio-group v-model="onlyFavorite" @change="search">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">仅收藏</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="search">搜索</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!list.length" description="暂无问答记录" />

      <div v-for="item in list" :key="item.id" class="record">
        <div class="record-head">
          <span class="question"><el-icon><QuestionFilled /></el-icon> {{ item.question }}</span>
          <div class="record-actions">
            <el-tag v-if="feedbackLabel(item.feedback)" size="small"
              :type="item.feedback === 'LIKE' ? 'success' : 'danger'" effect="plain">
              {{ feedbackLabel(item.feedback) }}
            </el-tag>
            <el-button link :type="item.favorite ? 'warning' : 'default'" @click="toggleFavorite(item)">
              <el-icon><StarFilled v-if="item.favorite" /><Star v-else /></el-icon>
              {{ item.favorite ? '已收藏' : '收藏' }}
            </el-button>
            <el-button link type="primary" @click="openDetail(item)">详情</el-button>
            <el-button link type="primary" @click="askAgain(item)">再问一次</el-button>
          </div>
        </div>
        <div class="answer">{{ item.answer }}</div>
        <div class="record-foot text-muted">
          <span v-for="s in item.sources" :key="s" class="source">
            <el-icon><Document /></el-icon> {{ s }}
          </span>
          <span>{{ item.createdAt }}</span>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > 0" class="pager" background layout="prev, pager, next, total"
      :total="total" :page-size="size" :current-page="page"
      @current-change="(p) => { page = p; loadList() }" />

    <el-dialog v-model="detailVisible" title="问答详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="反馈">{{ feedbackLabel(detailData?.feedback) || '未评价' }}</el-descriptions-item>
        <el-descriptions-item label="收藏">{{ detailData?.favorite ? '已收藏' : '未收藏' }}</el-descriptions-item>
        <el-descriptions-item label="时间" :span="2">{{ detailData?.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-block">
        <div class="detail-label">问题</div>
        <p>{{ detailData?.question }}</p>
      </div>
      <div class="detail-block">
        <div class="detail-label">回答</div>
        <p>{{ detailData?.answer }}</p>
      </div>
      <div class="detail-block">
        <div class="detail-label">引用来源</div>
        <el-tag v-for="s in detailData?.sources || []" :key="s" size="small" effect="plain" class="src-tag">
          {{ s }}
        </el-tag>
      </div>
    </el-dialog>
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

.filters {
  display: flex;
  gap: 12px;
}

.record {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 14px;
  transition: box-shadow 0.2s;
}

.record:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #303133;
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.answer {
  margin: 10px 0;
  padding: 10px 14px;
  background: #f7f9fc;
  border-radius: 6px;
  line-height: 1.7;
  color: #606266;
  white-space: pre-wrap;
}

.record-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.source {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 12px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-block {
  margin-top: 16px;
}

.detail-label {
  font-weight: 600;
  margin-bottom: 6px;
}

.detail-block p {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.src-tag {
  margin: 0 6px 6px 0;
}
</style>
