<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminRecords, recordUsers } from '@/api/qa'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const users = ref([])

const filters = reactive({
  keyword: '',
  userId: null,
  feedback: '',
  quick: '',
  dateRange: [],
})

const detailVisible = ref(false)
const detailRow = ref(null)

function feedbackLabel(feedback) {
  return { LIKE: '点赞', DISLIKE: '点踩', NONE: '未反馈' }[feedback] || '未反馈'
}

function feedbackType(feedback) {
  if (feedback === 'LIKE') return 'success'
  if (feedback === 'DISLIKE') return 'danger'
  return 'info'
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function onQuickChange(value) {
  const today = new Date()
  const end = formatDate(today)
  if (value === 'today') {
    filters.dateRange = [end, end]
  } else if (value === '7d') {
    const start = new Date(today)
    start.setDate(today.getDate() - 6)
    filters.dateRange = [formatDate(start), end]
  } else if (value === '30d') {
    const start = new Date(today)
    start.setDate(today.getDate() - 29)
    filters.dateRange = [formatDate(start), end]
  } else {
    filters.dateRange = []
  }
  search()
}

function onDateChange() {
  filters.quick = ''
  search()
}

async function loadUsers() {
  try {
    const res = await recordUsers()
    users.value = res?.data || []
  } catch (e) {
    users.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const [startTime, endTime] = filters.dateRange || []
    const res = await adminRecords({
      keyword: filters.keyword || undefined,
      userId: filters.userId || undefined,
      feedback: filters.feedback || undefined,
      startTime: startTime || undefined,
      endTime: endTime || undefined,
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
  filters.userId = null
  filters.feedback = ''
  filters.quick = ''
  filters.dateRange = []
  search()
}

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadUsers()
  loadList()
})
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜索问题" clearable style="width: 190px"
          :prefix-icon="'Search'" @keyup.enter="search" @clear="search" />
        <el-select v-model="filters.userId" placeholder="全部用户" clearable filterable
          style="width: 170px" @change="search">
          <el-option v-for="u in users" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
        <el-select v-model="filters.feedback" placeholder="全部反馈" clearable style="width: 130px"
          @change="search">
          <el-option label="点赞" value="LIKE" />
          <el-option label="点踩" value="DISLIKE" />
          <el-option label="未反馈" value="NONE" />
        </el-select>
        <el-radio-group v-model="filters.quick" @change="onQuickChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="7d">近7天</el-radio-button>
          <el-radio-button value="30d">近30天</el-radio-button>
        </el-radio-group>
        <el-date-picker v-model="filters.dateRange" type="daterange" value-format="YYYY-MM-DD"
          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
          style="width: 240px" @change="onDateChange" />
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="question" label="问题" min-width="240" show-overflow-tooltip />
      <el-table-column prop="username" label="提问用户" width="140">
        <template #default="{ row }">{{ row.username || '-' }}</template>
      </el-table-column>
      <el-table-column label="反馈" width="100">
        <template #default="{ row }">
          <el-tag :type="feedbackType(row.feedback)" effect="plain">{{ feedbackLabel(row.feedback) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收藏" width="80" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.favorite" color="#e6a23c"><StarFilled /></el-icon>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提问时间" width="160" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无问答记录</template>
    </el-table>

    <el-pagination v-if="total > 0" class="pager" background layout="prev, pager, next, total"
      :total="total" :page-size="size" :current-page="page"
      @current-change="(p) => { page = p; loadList() }" />

    <el-dialog v-model="detailVisible" title="问答详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="提问用户">{{ detailRow?.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="反馈">{{ feedbackLabel(detailRow?.feedback) }}</el-descriptions-item>
        <el-descriptions-item label="收藏">{{ detailRow?.favorite ? '已收藏' : '未收藏' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detailRow?.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-block">
        <div class="detail-label">问题</div>
        <p>{{ detailRow?.question }}</p>
      </div>
      <div class="detail-block">
        <div class="detail-label">回答</div>
        <p>{{ detailRow?.answer }}</p>
      </div>
      <div class="detail-block">
        <div class="detail-label">引用来源</div>
        <el-tag v-for="s in detailRow?.sources || []" :key="s" size="small" effect="plain" class="src-tag">
          {{ s }}
        </el-tag>
        <span v-if="!(detailRow?.sources || []).length" class="text-muted">无</span>
      </div>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-left: auto;
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
  line-height: 1.6;
  white-space: pre-wrap;
}

.src-tag {
  margin: 0 6px 6px 0;
}
</style>
