<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listLogs } from '@/api/admin'
import { documents, qaHistory } from '@/mock/student'

const activeTab = ref('log')

const logs = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({ module: '', username: '', keyword: '' })

const backups = ref([
  { id: 1, name: '知识库文档备份', file: 'kb_documents_20260311.json', size: '12 KB', createdAt: '2026-03-11 02:00' },
  { id: 2, name: '问答记录备份', file: 'qa_records_20260311.json', size: '48 KB', createdAt: '2026-03-11 02:00' },
])

async function loadLogs() {
  loading.value = true
  try {
    const res = await listLogs({
      module: filters.module || undefined,
      username: filters.username || undefined,
      keyword: filters.keyword || undefined,
      page: page.value,
      size: size.value,
    })
    const data = res?.data || {}
    logs.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadLogs()
}

function reset() {
  filters.module = ''
  filters.username = ''
  filters.keyword = ''
  search()
}

function download(filename, data) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function exportDocuments() {
  download('kb_documents.json', documents)
  ElMessage.success('知识库文档已导出')
}

function exportQa() {
  download('qa_records.json', qaHistory)
  ElMessage.success('问答记录已导出')
}

onMounted(loadLogs)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="操作日志" name="log">
        <div class="page-toolbar">
          <span class="title">系统操作日志</span>
          <div class="filters">
            <el-input v-model="filters.username" placeholder="操作人" clearable style="width: 140px"
              @keyup.enter="search" @clear="search" />
            <el-input v-model="filters.keyword" placeholder="搜索详情" clearable style="width: 180px"
              :prefix-icon="'Search'" @keyup.enter="search" @clear="search" />
            <el-select v-model="filters.module" placeholder="全部模块" clearable style="width: 140px" @change="search">
              <el-option v-for="m in ['登录', '文档管理', '分类管理', '用户管理', '角色权限', '敏感词管理', '反馈管理']"
                :key="m" :label="m" :value="m" />
            </el-select>
            <el-button type="primary" :loading="loading" @click="search">搜索</el-button>
            <el-button @click="reset">重置</el-button>
          </div>
        </div>

        <el-table v-loading="loading" :data="logs" stripe>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="username" label="操作人" width="130" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="action" label="动作" width="110" />
          <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.detail || '-' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="130">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'danger' : 'success'" effect="plain">
                {{ row.status === 0 ? '失败' : '成功' }}
              </el-tag>
              <el-tooltip v-if="row.status === 0 && row.errorMsg" :content="row.errorMsg">
                <el-icon class="err-icon"><WarningFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="createdAt" label="时间" width="170" />
          <template #empty>暂无操作日志</template>
        </el-table>

        <el-pagination v-if="total > 0" class="pager" background layout="prev, pager, next, total"
          :total="total" :page-size="size" :current-page="page"
          @current-change="(p) => { page = p; loadLogs() }" />
      </el-tab-pane>

      <el-tab-pane label="数据备份" name="backup">
        <div class="page-toolbar">
          <span class="title">数据导出备份</span>
          <div class="filters">
            <el-button type="primary" :icon="'Document'" @click="exportDocuments">导出知识库文档</el-button>
            <el-button type="primary" :icon="'Tickets'" @click="exportQa">导出问答记录</el-button>
          </div>
        </div>

        <el-table :data="backups" stripe>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="name" label="备份内容" min-width="180" />
          <el-table-column prop="file" label="文件名" min-width="220" />
          <el-table-column prop="size" label="大小" width="100" />
          <el-table-column prop="createdAt" label="生成时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
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

.err-icon {
  margin-left: 4px;
  color: #f56c6c;
  vertical-align: middle;
}
</style>
