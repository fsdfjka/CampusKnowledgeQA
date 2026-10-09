<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sensitiveWords as mockWords } from '@/mock/admin'

const list = ref(mockWords.map((item) => ({ ...item })))
const keyword = ref('')
const newWord = ref('')

const filtered = computed(() =>
  list.value.filter((item) => !keyword.value || item.word.includes(keyword.value)),
)

function addWord() {
  const word = newWord.value.trim()
  if (!word) {
    ElMessage.warning('请输入敏感词')
    return
  }
  if (list.value.some((item) => item.word === word)) {
    ElMessage.warning('该敏感词已存在')
    return
  }
  list.value.unshift({ id: Date.now(), word, createdAt: new Date().toLocaleString() })
  newWord.value = ''
  ElMessage.success('新增成功')
}

function removeWord(row) {
  ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '提示', { type: 'warning' })
    .then(() => {
      list.value = list.value.filter((item) => item.id !== row.id)
      ElMessage.success('删除成功')
    })
    .catch(() => {})
}
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">敏感词库</span>
      <div class="filters">
        <el-input v-model="newWord" placeholder="输入敏感词" style="width: 180px"
          @keyup.enter="addWord" />
        <el-button type="primary" :icon="'Plus'" @click="addWord">新增</el-button>
        <el-input v-model="keyword" placeholder="搜索" clearable style="width: 160px"
          :prefix-icon="'Search'" />
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="tip"
      title="用户提问命中敏感词时会被直接拦截，不会提交给大模型。" />

    <el-table :data="filtered" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="word" label="敏感词" min-width="200" />
      <el-table-column prop="createdAt" label="添加时间" width="200" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="removeWord(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
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

.tip {
  margin-bottom: 16px;
}
</style>
