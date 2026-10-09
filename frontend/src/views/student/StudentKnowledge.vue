<script setup>
import { ref, computed, onMounted } from 'vue'
import { listCategories, listDocuments } from '@/api/document'

const categories = ref([])
const documents = ref([])
const loading = ref(false)
const activeCategory = ref('')
const keyword = ref('')
const detailVisible = ref(false)
const current = ref(null)

async function load() {
  loading.value = true
  try {
    const [categoryRes, documentRes] = await Promise.all([listCategories(), listDocuments()])
    categories.value = categoryRes?.data || []
    documents.value = (documentRes?.data || []).map((doc) => ({
      id: doc.id,
      name: doc.name,
      category: doc.categoryName,
      type: doc.fileType,
      size: doc.fileSize,
      chunks: doc.chunkCount,
      createdAt: doc.createdAt,
    }))
  } finally {
    loading.value = false
  }
}

const countByCategory = computed(() => {
  const map = {}
  documents.value.forEach((doc) => {
    const key = doc.category || '未分类'
    map[key] = (map[key] || 0) + 1
  })
  return map
})

const filtered = computed(() =>
  documents.value.filter((doc) => {
    const matchCategory = !activeCategory.value || doc.category === activeCategory.value
    const matchKeyword = !keyword.value || doc.name.includes(keyword.value)
    return matchCategory && matchKeyword
  }),
)

function formatSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

function openDetail(doc) {
  current.value = doc
  detailVisible.value = true
}

onMounted(load)
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title"><el-icon><Collection /></el-icon> 知识库文档</span>
      <el-input v-model="keyword" placeholder="搜索文档名称" clearable style="width: 220px"
        :prefix-icon="'Search'" />
    </div>

    <div class="categories">
      <el-tag :type="activeCategory === '' ? 'primary' : 'info'" effect="plain"
        @click="activeCategory = ''">全部（{{ documents.length }}）</el-tag>
      <el-tag v-for="cat in categories" :key="cat.id"
        :type="activeCategory === cat.name ? 'primary' : 'info'" effect="plain"
        @click="activeCategory = cat.name">
        {{ cat.name }}（{{ countByCategory[cat.name] || 0 }}）
      </el-tag>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!filtered.length" description="该分类下暂无公开文档" />

      <el-row v-else :gutter="16">
        <el-col v-for="doc in filtered" :key="doc.id" :xs="24" :sm="12" :md="8">
          <el-card class="doc-card" shadow="hover" @click="openDetail(doc)">
            <div class="doc-head">
              <el-icon :size="22" color="#1890ff"><Document /></el-icon>
              <span class="doc-name">{{ doc.name }}</span>
            </div>
            <p class="doc-summary text-muted">共 {{ doc.chunks || 0 }} 个知识片段</p>
            <div class="doc-meta text-muted">
              <el-tag size="small" effect="plain">{{ doc.category || '未分类' }}</el-tag>
              <span>{{ doc.type }} · {{ formatSize(doc.size) }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-drawer v-model="detailVisible" :title="current?.name" size="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="分类">{{ current?.category || '未分类' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current?.type }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatSize(current?.size) }}</el-descriptions-item>
        <el-descriptions-item label="知识片段">{{ current?.chunks }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ current?.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
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

.categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.categories .el-tag {
  cursor: pointer;
}

.doc-card {
  margin-bottom: 16px;
  cursor: pointer;
}

.doc-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-summary {
  margin: 10px 0;
  font-size: 13px;
}

.doc-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}
</style>
