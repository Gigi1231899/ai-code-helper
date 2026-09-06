<template>
  <div class="chat-page">
    <!-- ============ 左侧侧边栏 ============ -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <span class="logo-emoji">🤖</span>
        <span class="logo-text">AI 编程小助手</span>
      </div>

      <button class="btn btn-primary new-chat" @click="newConversation">
        ➕ 新建对话
      </button>

      <button class="btn btn-ghost kb-btn" @click="showKb = true">
        📚 私人知识库
      </button>

      <!-- 会话历史列表 -->
      <div class="conv-list">
        <div class="conv-list-title">历史会话</div>

        <div v-if="conversations.length === 0" class="conv-empty">
          还没有会话，点上面的「新建对话」开始吧
        </div>

        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === activeId }"
          @click="selectConversation(conv)"
        >
          <div class="conv-main">
            <span class="conv-id">#{{ conv.id }}</span>
            <span class="conv-title">{{ conv.title }}</span>
          </div>
          <span class="conv-mode" :class="conv.mode">{{ modeLabel(conv.mode) }}</span>
          <button class="conv-del" @click.stop="removeConversation(conv.id)">🗑</button>
        </div>
      </div>

      <!-- 底部用户信息 -->
      <div class="sidebar-footer">
        <span class="username">👤 {{ username }}</span>
        <button class="logout" @click="logout">退出</button>
      </div>
    </aside>

    <!-- ============ 右侧主区域 ============ -->
    <main class="chat-main">
      <!-- 顶部：当前会话信息 -->
      <header class="chat-header">
        <div v-if="activeId" class="header-info">
          <span class="header-id">会话 #{{ activeId }}</span>
          <span class="header-mode">{{ modeLabel(activeMode) }}</span>
        </div>
        <div v-else class="header-info">
          <span class="header-id">新对话</span>
        </div>
        <span class="header-hint">每开启新对话自动分配唯一 int 类型对话 ID，会话隔离</span>
      </header>

      <!-- 消息区 -->
      <div ref="messageBox" class="messages">
        <!-- 空白状态 -->
        <div v-if="messages.length === 0" class="empty-state">
          <RobotMascot :size="150" />
          <p class="empty-text">输入你的编程问题吧</p>
        </div>

        <!-- 消息气泡 -->
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-row"
          :class="msg.role"
        >
          <div class="avatar">{{ msg.role === 'user' ? '🧑‍💻' : '🤖' }}</div>
          <div class="bubble">
            <!-- AI 回答按 Markdown 渲染，用户消息保持纯文本 -->
            <div
              v-if="msg.role === 'ai'"
              class="markdown-body"
              v-html="renderMarkdown(msg.content)"
            ></div>
            <template v-else>{{ msg.content }}</template>
            <!-- 流式输出时的打字光标 -->
            <span v-if="loading && index === messages.length - 1" class="cursor"></span>
          </div>
        </div>
      </div>

      <!-- 底部输入控制栏 -->
      <footer class="composer">
        <!-- 模式选择：会话创建后锁定，必须新建会话才能切换 -->
        <div class="mode-selector">
          <label
            v-for="option in modeOptions"
            :key="option.value"
            class="mode-option"
            :class="{ checked: activeMode === option.value, locked: !!activeId }"
          >
            <input
              v-model="activeMode"
              type="radio"
              name="mode"
              :value="option.value"
              :disabled="!!activeId"
            />
            <span>{{ option.icon }} {{ option.label }}</span>
          </label>

          <span v-if="activeId" class="mode-lock-tip">
            🔒 当前会话已锁定模式，切换模式请新建对话
          </span>
        </div>

        <div class="input-row">
          <textarea
            v-model="inputText"
            class="input"
            :placeholder="activeId ? '继续提问...' : '输入你的编程问题，发送后自动创建会话'"
            rows="1"
            @keydown.enter.exact.prevent="send"
          ></textarea>
          <button class="btn btn-primary send" :disabled="loading || !inputText.trim()" @click="send">
            {{ loading ? '生成中' : '发送' }}
          </button>
        </div>

        <p class="composer-tip">
          ✨ 每开启新对话自动分配唯一 int 类型对话 ID，会话隔离 · Enter 发送 / Shift+Enter 换行
        </p>
      </footer>
    </main>

    <!-- 知识库面板 -->
    <KnowledgeBasePanel v-if="showKb" @close="showKb = false" />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import RobotMascot from '@/components/RobotMascot.vue'
import KnowledgeBasePanel from '@/components/KnowledgeBasePanel.vue'
import { createConversation, deleteConversation, listConversations, listMessages } from '@/api/conversation'
import { renderMarkdown } from '@/utils/markdown'
import { streamChat } from '@/utils/sse'

const router = useRouter()

/** 三种模式选项 */
const modeOptions = [
  { value: 'rag', label: 'RAG 知识库模式', icon: '📚' },
  { value: 'mcp', label: 'MCP 联网搜索模式', icon: '🌐' },
  { value: 'mix', label: 'RAG+MCP 混合模式', icon: '⚡' },
]

/** 会话列表 */
const conversations = ref([])
/** 当前会话ID，null 表示还没创建（此时可以选择模式） */
const activeId = ref(null)
/** 当前模式 */
const activeMode = ref('rag')
/** 消息列表：{ role: 'user' | 'ai', content } */
const messages = ref([])
/** 用户输入 */
const inputText = ref('')
/** 是否正在等待/接收 AI 输出 */
const loading = ref(false)
/** 是否展示知识库面板 */
const showKb = ref(false)
/** 消息区 DOM，用于自动滚动 */
const messageBox = ref(null)

const username = computed(() => localStorage.getItem('username') || '同学')

/** 模式值转中文标签 */
function modeLabel(mode) {
  const found = modeOptions.find((item) => item.value === mode)
  return found ? found.label : mode
}

onMounted(async () => {
  await loadConversations()
})

/** 加载会话列表 */
async function loadConversations() {
  try {
    conversations.value = (await listConversations()) || []
  } catch (e) {
    // request 拦截器已处理 401，这里只兜底提示
    console.error('加载会话失败', e)
  }
}

/** 切换到某个历史会话 */
async function selectConversation(conv) {
  if (loading.value) {
    return
  }
  activeId.value = conv.id
  // 历史会话的模式是锁定的，直接用会话自己的模式
  activeMode.value = conv.mode
  try {
    const list = (await listMessages(conv.id)) || []
    messages.value = list.map((item) => ({ role: item.role, content: item.content }))
    scrollToBottom()
  } catch (e) {
    console.error('加载历史消息失败', e)
  }
}

/** 新建对话：清空消息并解除模式锁定 */
function newConversation() {
  if (loading.value) {
    return
  }
  activeId.value = null
  messages.value = []
}

/** 删除会话 */
async function removeConversation(id) {
  if (!window.confirm('确定删除这个会话吗？相关历史记录也会一起删除')) {
    return
  }
  try {
    await deleteConversation(id)
    if (activeId.value === id) {
      activeId.value = null
      messages.value = []
    }
    await loadConversations()
  } catch (e) {
    window.alert(e.message)
  }
}

/** 发送消息 */
async function send() {
  const text = inputText.value.trim()
  if (!text || loading.value) {
    return
  }
  inputText.value = ''

  // ① 还没有会话：先用当前选择的模式创建，拿到后端自动分配的 int 会话ID
  if (!activeId.value) {
    try {
      const conv = await createConversation(activeMode.value)
      activeId.value = conv.id
      await loadConversations()
    } catch (e) {
      window.alert(e.message)
      return
    }
  }

  // ② 先把用户消息上屏
  messages.value.push({ role: 'user', content: text })

  // ③ 占位一条 AI 消息，后续流式往里追加
  // ⚠️ 关键点：必须通过 messages.value[index] 访问并修改，不要持有原始对象引用。
  // messages 是 ref([])，push 进去的是原始对象；直接改原始对象绕过了 Vue 的 Proxy，
  // 不会触发视图更新，表现就是「AI 要等全部输出完才一次性显示」。
  // 通过数组索引拿到的是响应式代理对象，修改它才能逐字刷新。
  messages.value.push({ role: 'ai', content: '' })
  const aiIndex = messages.value.length - 1
  loading.value = true
  scrollToBottom()

  // ④ SSE 流式接收
  await streamChat({
    url: '/api/ai/chat',
    params: { conversationId: activeId.value, message: text },
    onMessage: (chunk) => {
      messages.value[aiIndex].content += chunk
      scrollToBottom()
    },
    onError: (e) => {
      messages.value[aiIndex].content += `\n\n⚠️ ${e.message}`
    },
    onDone: () => {
      loading.value = false
      // 标题和排序会在后端刷新，这里重新拉一次列表
      loadConversations()
    },
  })
}

/** 滚动到消息底部 */
function scrollToBottom() {
  nextTick(() => {
    const el = messageBox.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

/** 退出登录 */
function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: var(--bg);
}

/* ============ 侧边栏 ============ */
.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #eef6f3;
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 17px;
  color: #2f4858;
  margin-bottom: 20px;
}

.logo-emoji {
  font-size: 22px;
}

.new-chat,
.kb-btn {
  width: 100%;
  margin-bottom: 10px;
  padding: 11px 0;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 8px;
}

.conv-list-title {
  font-size: 12px;
  color: var(--text-light);
  margin-bottom: 8px;
  padding-left: 4px;
}

.conv-empty {
  font-size: 12px;
  color: var(--text-light);
  text-align: center;
  padding: 24px 8px;
  line-height: 1.8;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  margin-bottom: 6px;
  transition: background 0.2s ease;
}

.conv-item:hover {
  background: #f4fbf8;
}

.conv-item.active {
  background: var(--mint-soft);
}

.conv-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.conv-id {
  font-size: 11px;
  color: var(--mint-deep);
  font-weight: 700;
}

.conv-title {
  font-size: 13px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-mode {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--sky-soft);
  color: #4a7fa5;
  white-space: nowrap;
}

.conv-mode.mix {
  background: #fff6dd;
  color: #a5812f;
}

.conv-del {
  border: none;
  background: transparent;
  cursor: pointer;
  opacity: 0;
  font-size: 13px;
  transition: opacity 0.2s ease;
}

.conv-item:hover .conv-del {
  opacity: 1;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #eef6f3;
  font-size: 13px;
  color: var(--text-light);
}

.logout {
  border: none;
  background: transparent;
  color: var(--text-light);
  cursor: pointer;
  font-size: 13px;
  font-family: inherit;
}

.logout:hover {
  color: var(--danger);
}

/* ============ 主区域 ============ */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 28px;
  background: #fff;
  border-bottom: 1px solid #eef6f3;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-id {
  font-weight: 700;
  color: #2f4858;
}

.header-mode {
  font-size: 12px;
  padding: 3px 12px;
  border-radius: 999px;
  background: var(--mint-soft);
  color: var(--mint-deep);
}

.header-hint {
  font-size: 12px;
  color: var(--text-light);
}

/* ---------- 消息区 ---------- */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.empty-text {
  color: var(--text-light);
  font-size: 15px;
  margin: 0;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

/* 用户消息靠右，AI 消息靠左 */
.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-soft);
  font-size: 17px;
}

.bubble {
  max-width: 68%;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  line-height: 1.75;
  font-size: 14px;
  word-break: break-word;
  box-shadow: var(--shadow-soft);
}

/* 用户：淡蓝色气泡 */
.message-row.user .bubble {
  background: var(--sky-soft);
  border-top-right-radius: 6px;
  white-space: pre-wrap;
}

/* AI：薄荷绿气泡 */
.message-row.ai .bubble {
  background: var(--mint-soft);
  border-top-left-radius: 6px;
}

/* 打字光标 */
.cursor {
  display: inline-block;
  width: 6px;
  height: 15px;
  margin-left: 3px;
  background: var(--mint-deep);
  vertical-align: text-bottom;
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  to {
    visibility: hidden;
  }
}

/* ---------- 输入控制栏 ---------- */
.composer {
  padding: 14px 28px 18px;
  background: #fff;
  border-top: 1px solid #eef6f3;
}

.mode-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.mode-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 999px;
  background: #f7fcfa;
  border: 2px solid transparent;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.mode-option.checked {
  border-color: var(--mint);
  background: var(--mint-soft);
  color: var(--mint-deep);
  font-weight: 600;
}

/* 会话已创建 -> 模式锁定 */
.mode-option.locked {
  cursor: not-allowed;
  opacity: 0.55;
}

.mode-option input {
  display: none;
}

.mode-lock-tip {
  font-size: 12px;
  color: var(--text-light);
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input {
  flex: 1;
  border: 2px solid #e6f2ee;
  border-radius: var(--radius-md);
  padding: 12px 16px;
  font-size: 15px;
  font-family: inherit;
  color: var(--text);
  outline: none;
  resize: none;
  max-height: 160px;
  transition: border-color 0.2s ease;
}

.input:focus {
  border-color: var(--mint);
}

.send {
  padding: 12px 28px;
  flex-shrink: 0;
}

.composer-tip {
  font-size: 12px;
  color: var(--text-light);
  margin: 10px 0 0;
}

@media (max-width: 860px) {
  .sidebar {
    width: 200px;
  }

  .bubble {
    max-width: 80%;
  }
}
</style>
