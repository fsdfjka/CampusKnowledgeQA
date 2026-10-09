import http from './http'

/** 文档列表（含分类名、上传人名） */
export function listDocuments() {
  return http.get('/documents')
}

/** 分类列表 */
export function listCategories() {
  return http.get('/categories')
}

/** 新增分类 */
export function createCategory(data) {
  return http.post('/categories', data)
}

/** 修改分类 */
export function updateCategory(id, data) {
  return http.put(`/categories/${id}`, data)
}

/** 删除分类 */
export function deleteCategory(id) {
  return http.delete(`/categories/${id}`)
}

/** 统一文件上传入库（可带 sourceName、categoryId） */
export function uploadDocument(formData) {
  return http.post('/uploadDocument', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 清空向量知识库 */
export function clearVector() {
  return http.delete('/clearVector')
}

/** 删除文档（同时清理其向量） */
export function deleteDocument(id) {
  return http.delete(`/documents/${id}`)
}

/** 修改文档（名称/分类） */
export function updateDocument(id, data) {
  return http.put(`/documents/${id}`, data)
}

/** 重新解析文档 */
export function reparseDocument(id) {
  return http.post(`/documents/${id}/reparse`)
}

/** 查看文档切片（分页） */
export function listChunks(id, params) {
  return http.get(`/documents/${id}/chunks`, { params })
}
