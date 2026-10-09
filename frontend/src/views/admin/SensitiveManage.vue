<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSensitiveWords, addSensitiveWord, deleteSensitiveWord } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const newWord = ref('')
const adding = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await listSensitiveWords({ keyword: keyword.value || undefined })
    list.value = res?.data || []
  } finally {
    loading.value = false
  }
}

async function addWord() {
  const word = newWord.value.trim()
  if (!word) {
    ElMessage.warning('请输入敏感词')
    return
  }
  adding.value = true
  try {
    await addSensitiveWord(word)
    newWord.value = ''
    ElMessage.success('新增成功')
    await loadList()
  } finally {
    adding.value = false
  }
}

function removeWord(row) {
  ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteSensitiveWord(row.id)
      ElMessage.success('删除成功')
      await loadList()
    })
    .catch(() => {})
}

onMounted(loadList)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">敏感词库</span>
      <div class="filters">
        <el-input v-model="newWord" placeholder="输入敏感词" style="width: 180px"
          @keyup.enter="addWord" />
        <el-button type="primary" :icon="'Plus'" :loading="adding" @click="addWord">新增</el-button>
        <el-input v-model="keyword" placeholder="搜索" clearable style="width: 160px"
          :prefix-icon="'Search'" @keyup.enter="loadList" @clear="loadList" />
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="tip"
      title="用户提问命中敏感词时会被直接拦截，不会提交给大模型。" />

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="word" label="敏感词" min-width="200" />
      <el-table-column prop="createdAt" label="添加时间" width="200" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="removeWord(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无敏感词</template>
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
