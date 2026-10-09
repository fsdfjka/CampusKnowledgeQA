<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { operationLogs } from '@/mock/admin'
import { documents, qaHistory } from '@/mock/student'

const activeTab = ref('log')
const keyword = ref('')
const moduleFilter = ref('')

const logs = ref(operationLogs.map((item) => ({ ...item })))
const backups = ref([
  { id: 1, name: '知识库文档备份', file: 'kb_documents_20260311.json', size: '12 KB', createdAt: '2026-03-11 02:00' },
  { id: 2, name: '问答记录备份', file: 'qa_records_20260311.json', size: '48 KB', createdAt: '2026-03-11 02:00' },
])

const filteredLogs = computed(() =>
  logs.value.filter((item) => {
    const matchKeyword =
      !keyword.value || item.detail.includes(keyword.value) || item.username.includes(keyword.value)
    const matchModule = !moduleFilter.value || item.module === moduleFilter.value
    return matchKeyword && matchModule
  }),
)

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
</script>

<template>
  <el-card class="page-card" shadow="never">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="操作日志" name="log">
        <div class="page-toolbar">
          <span class="title">系统操作日志</span>
          <div class="filters">
            <el-input v-model="keyword" placeholder="搜索用户/详情" clearable style="width: 200px"
              :prefix-icon="'Search'" />
            <el-select v-model="moduleFilter" placeholder="全部模块" clearable style="width: 140px">
              <el-option v-for="m in ['登录', '文档管理', '用户管理', '系统配置']" :key="m" :label="m" :value="m" />
            </el-select>
          </div>
        </div>

        <el-table :data="filteredLogs" stripe>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="username" label="操作人" width="130" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="action" label="动作" width="90" />
          <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
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
</style>
