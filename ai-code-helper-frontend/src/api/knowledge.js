import request from './request'

/**
 * 私人知识库接口
 */

/** 上传文件到我的知识库（后端会自动切片并写入我的向量库） */
export function uploadKbFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge-base/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    // 解析文档 + 向量化比较慢，单独放宽超时
    timeout: 120000,
  })
}

/** 我的知识库文件列表 */
export function listKbFiles() {
  return request.get('/knowledge-base/files')
}

/** 删除我的知识库文件 */
export function deleteKbFile(fileId) {
  return request.delete(`/knowledge-base/files/${fileId}`)
}
