<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDocuments, listCategories, uploadDocument, clearVector, deleteDocument, updateDocument, reparseDocument } from '@/api/document'

const list = ref([])
const categories = ref([])
const keyword = ref('')
const loading = ref(false)
const uploading = ref(false)

const uploadVisible = ref(false)
const uploadRef = ref()
const uploadForm = ref({ name: '', categoryId: null, file: null })
const uploadFormRef = ref()
const uploadRules = {
  name: [{ required: true, message: '请输入文档名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  file: [{ required: true, message: '请选择文件', trigger: 'change' }],
}

const previewVisible = ref(false)
const previewDoc = ref(null)

const editVisible = ref(false)
const editing = ref(false)
const editForm = ref({ id: null, name: '', categoryId: null })

const filtered = computed(() =>
  list.value.filter((item) => !keyword.value || item.name.includes(keyword.value)),
)

function statusType(status) {
  if (status === 'DONE') return 'success'
  if (status === 'PARSING') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function statusLabel(status) {
  return { DONE: '已入库', PARSING: '解析中', FAILED: '失败', PENDING: '待解析' }[status] || status
}

function formatSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

async function loadList() {
  loading.value = true
  try {
    const res = await listDocuments()
    list.value = res?.data || []
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await listCategories()
    categories.value = res?.data || []
  } catch (e) {
    categories.value = []
  }
}

function openUpload() {
  uploadForm.value = { name: '', categoryId: null, file: null }
  uploadVisible.value = true
  uploadRef.value?.clearFiles()
  uploadFormRef.value?.clearValidate()
}

function onFileChange(uploadFile) {
  uploadForm.value.file = uploadFile.raw
  uploadFormRef.value?.validateField('file')
}

function onFileRemove() {
  uploadForm.value.file = null
  uploadFormRef.value?.validateField('file')
}

async function handleUpload() {
  try {
    await uploadFormRef.value.validate()
  } catch (e) {
    return
  }
  const formData = new FormData()
  formData.append('file', uploadForm.value.file)
  formData.append('sourceName', uploadForm.value.name.trim())
  formData.append('categoryId', uploadForm.value.categoryId)

  uploading.value = true
  try {
    await uploadDocument(formData)
    ElMessage.success('上传并入库成功')
    uploadVisible.value = false
    await loadList()
  } finally {
    uploading.value = false
  }
}

function openPreview(row) {
  previewDoc.value = row
  previewVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm(`确认删除文档「${row.name}」？删除后同步移除向量库中的切片。`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDocument(row.id)
      ElMessage.success('删除成功')
      await loadList()
    })
    .catch(() => {})
}

function openEdit(row) {
  editForm.value = { id: row.id, name: row.name, categoryId: row.categoryId }
  editVisible.value = true
}

async function handleEdit() {
  if (!editForm.value.name?.trim()) {
    ElMessage.warning('请输入文档名称')
    return
  }
  editing.value = true
  try {
    await updateDocument(editForm.value.id, {
      name: editForm.value.name.trim(),
      categoryId: editForm.value.categoryId,
    })
    ElMessage.success('修改成功')
    editVisible.value = false
    await loadList()
  } finally {
    editing.value = false
  }
}

function handleReparse(row) {
  ElMessageBox.confirm(`确认重新解析「${row.name}」？将重建其向量数据。`, '提示', { type: 'warning' })
    .then(async () => {
      await reparseDocument(row.id)
      ElMessage.success('已重新解析')
      await loadList()
    })
    .catch(() => {})
}

function handleClear() {
  ElMessageBox.confirm('确认清空整个知识库（向量库）吗？', '提示', { type: 'warning' })
    .then(async () => {
      await clearVector()
      ElMessage.success('知识库已清空')
      await loadList()
    })
    .catch(() => {})
}

onMounted(() => {
  loadCategories()
  loadList()
})
</script>

<template>
  <el-card class="page-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">文档管理</span>
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索文档名称" clearable style="width: 200px"
          :prefix-icon="'Search'" />
        <el-button type="danger" plain :icon="'Delete'" @click="handleClear">清空知识库</el-button>
        <el-button type="primary" :icon="'UploadFilled'" @click="openUpload">上传文档</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="filtered" stripe>
      <el-table-column type="index" label="#" width="55" />
      <el-table-column prop="name" label="文档名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="所属分类" width="120">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="fileType" label="类型" width="80" align="center" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="切片数" width="90" align="center" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传人" width="110">
        <template #default="{ row }">{{ row.uploaderName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="160" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openPreview(row)">预览</el-button>
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="warning" link @click="handleReparse(row)">重新解析</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无文档</template>
    </el-table>

    <el-dialog v-model="uploadVisible" title="上传文档" width="480px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="文档名称" prop="name">
          <el-input v-model="uploadForm.name" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="uploadForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件" prop="file">
          <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" :on-change="onFileChange"
            :on-remove="onFileRemove" style="width: 100%">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="text-muted">支持 PDF、Word（.docx/.doc）、TXT、Markdown</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">确定上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewDoc?.name" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="所属分类">{{ previewDoc?.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ previewDoc?.fileType }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatSize(previewDoc?.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="切片数">{{ previewDoc?.chunkCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(previewDoc?.status) }}</el-descriptions-item>
        <el-descriptions-item label="上传人">{{ previewDoc?.uploaderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上传时间" :span="2">{{ previewDoc?.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑文档" width="460px">
      <el-form label-width="90px">
        <el-form-item label="文档名称">
          <el-input v-model="editForm.name" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" clearable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
}
</style>
