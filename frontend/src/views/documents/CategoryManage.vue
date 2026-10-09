<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCategories, createCategory, updateCategory, deleteCategory } from '@/api/document'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref()
const form = reactive({ id: null, name: '', description: '', sort: 0 })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await listCategories()
    list.value = res?.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogTitle.value = '新增分类'
  Object.assign(form, { id: null, name: '', description: '', sort: 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑分类'
  Object.assign(form, { id: row.id, name: row.name, description: row.description, sort: row.sort ?? 0 })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { name: form.name, description: form.description, sort: form.sort }
    if (form.id) {
      await updateCategory(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createCategory(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确认删除分类「${row.name}」？该分类下文档的分类会被置空。`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCategory(row.id)
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
      <span class="title">知识库分类</span>
      <el-button type="primary" :icon="'Plus'" @click="openCreate">新增分类</el-button>
    </div>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="name" label="分类名称" width="200" />
      <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="90" align="center" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>暂无分类</template>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
