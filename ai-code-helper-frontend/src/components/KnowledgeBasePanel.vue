<template>
  <!-- 遮罩层：点击空白处关闭 -->
  <div class="mask" @click.self="$emit('close')">
    <div class="panel card">
      <header class="panel-header">
        <h3 class="panel-title">📚 我的私人知识库</h3>
        <button class="icon-btn" @click="$emit('close')">✕</button>
      </header>

      <p class="panel-tip">
        支持 txt / md / doc / docx / pdf，单文件不超过 10MB，最多 50 个文件。<br />
        上传后会自动切片并写入<b>只属于你</b>的向量库，其他用户检索不到。
      </p>

      <!-- 上传区 -->
      <label class="upload-area" :class="{ disabled: uploading }">
        <input
          ref="fileInput"
          type="file"
          accept=".txt,.md,.doc,.docx,.pdf"
          @change="onFileChange"
          hidden
        />
        <span class="upload-icon">⬆️</span>
        <span class="upload-text">{{ uploading ? '正在解析并向量化...' : '点击选择文档上传' }}</span>
      </label>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <!-- 文件列表 -->
      <div class="file-list">
        <div class="file-list-title">
          已上传 {{ files.length }} / 50
        </div>

        <div v-if="files.length === 0" class="empty">
          还没有文件，上传你的技术文档或面试题库吧～
        </div>

        <div v-for="file in files" :key="file.id" class="file-item">
          <div class="file-info">
            <span class="file-name" :title="file.fileName">📄 {{ file.fileName }}</span>
            <span class="file-size">{{ formatSize(file.fileSize) }}</span>
          </div>
          <button class="icon-btn danger" @click="remove(file.id)">🗑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { deleteKbFile, listKbFiles, uploadKbFile } from '@/api/knowledge'

defineEmits(['close'])

/** 文件列表 */
const files = ref([])
/** 是否正在上传（解析 + 向量化较慢，需要loading态） */
const uploading = ref(false)
/** 错误提示 */
const errorMsg = ref('')

/** 组件挂载时加载已有文件 */
onMounted(async () => {
  await loadFiles()
})

async function loadFiles() {
  try {
    files.value = (await listKbFiles()) || []
  } catch (e) {
    errorMsg.value = e.message
  }
}

/** 选择文件后直接上传 */
async function onFileChange(event) {
  const file = event.target.files?.[0]
  // 清空 input，否则连续选同一个文件不会触发 change
  event.target.value = ''
  if (!file) {
    return
  }

  uploading.value = true
  errorMsg.value = ''
  try {
    await uploadKbFile(file)
    await loadFiles()
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    uploading.value = false
  }
}

/** 删除文件 */
async function remove(fileId) {
  if (!window.confirm('删除后该文档的向量数据也会一并移除，确定删除吗？')) {
    return
  }
  errorMsg.value = ''
  try {
    await deleteKbFile(fileId)
    await loadFiles()
  } catch (e) {
    errorMsg.value = e.message
  }
}

/** 字节转可读大小 */
function formatSize(bytes) {
  if (!bytes) {
    return '0 KB'
  }
  const kb = bytes / 1024
  return kb >= 1024 ? `${(kb / 1024).toFixed(2)} MB` : `${kb.toFixed(1)} KB`
}
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(47, 72, 88, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.panel {
  width: 460px;
  max-width: 92vw;
  max-height: 86vh;
  overflow-y: auto;
  padding: 24px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 18px;
  margin: 0;
  color: #2f4858;
}

.icon-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 15px;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  transition: background 0.2s ease;
}

.icon-btn:hover {
  background: #f1f8f5;
}

.icon-btn.danger:hover {
  background: #fff1f0;
}

.panel-tip {
  font-size: 12px;
  line-height: 1.8;
  color: var(--text-light);
  background: #f7fcfa;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  margin: 0 0 16px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 96px;
  border: 2px dashed var(--mint);
  border-radius: var(--radius-md);
  background: var(--mint-soft);
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 14px;
}

.upload-area:hover {
  background: #d9f5e9;
}

.upload-area.disabled {
  opacity: 0.6;
  cursor: wait;
}

.upload-icon {
  font-size: 22px;
}

.upload-text {
  font-size: 14px;
  color: var(--mint-deep);
  font-weight: 600;
}

.error {
  font-size: 13px;
  color: #e2685f;
  background: #fff1f0;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  margin: 0 0 12px;
}

.file-list-title {
  font-size: 13px;
  color: var(--text-light);
  margin-bottom: 10px;
}

.empty {
  text-align: center;
  color: var(--text-light);
  font-size: 13px;
  padding: 24px 0;
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: #f7fcfa;
  margin-bottom: 8px;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: var(--text-light);
}
</style>
