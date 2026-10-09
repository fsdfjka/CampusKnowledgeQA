<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listFeedback, handleFeedback } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = reactive({
  keyword: '',
  type: '',
  handled: '',
})

function typeLabel(type) {
  return type === 'LIKE' ? '点赞' : '点踩'
}

async function loadList() {
  loading.value = true
  try {
    const res = await listFeedback({
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      handled: filters.handled === '' || filters.handled == null ? undefined : filters.handled,
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
  filters.type = ''
  filters.handled = ''
  search()
}

async function handle(row) {
  const next = !row.handled
  await handleFeedback(row.id, next)
  ElMessage.success(next ? '已标记为已处理' : '已取消处理')
  await loadList()
}

function goDocuments() {
  router.push('/documents/list')
}

onMounted(loadList)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">学生反馈</span>
      <div class="filters">
        <el-input v-model="filters.keyword" placeholder="搜索问题/用户/说明" clearable style="width: 220px"
          :prefix-icon="'Search'" @keyup.enter="search" @clear="search" />
        <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 130px" @change="search">
          <el-option label="点赞" value="LIKE" />
          <el-option label="点踩" value="DISLIKE" />
        </el-select>
        <el-select v-model="filters.handled" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="username" label="用户" width="140">
        <template #default="{ row }">{{ row.username || '-' }}</template>
      </el-table-column>
      <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.question || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'LIKE' ? 'success' : 'danger'" effect="plain">
            {{ typeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="反馈说明" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.reason || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.handled ? 'success' : 'warning'" effect="plain">
            {{ row.handled ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="160" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button :type="row.handled ? 'info' : 'primary'" link @click="handle(row)">
            {{ row.handled ? '取消处理' : '标记已处理' }}
          </el-button>
          <el-button type="warning" link @click="goDocuments">补充资料</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无反馈</template>
    </el-table>

    <el-pagination v-if="total > 0" class="pager" background layout="prev, pager, next, total"
      :total="total" :page-size="size" :current-page="page"
      @current-change="(p) => { page = p; loadList() }" />
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
