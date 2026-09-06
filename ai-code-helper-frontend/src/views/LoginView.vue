<template>
  <div class="login-page">
    <!-- 左侧：机器人插画 -->
    <aside class="login-aside">
      <RobotMascot :size="220" />
      <h2 class="aside-title">Hi，未来的大神 👋</h2>
      <p class="aside-desc">
        陪你刷题、改 bug、背八股、改简历<br />
        让编程学习和求职这件事，不再一个人硬扛
      </p>
    </aside>

    <!-- 右侧：表单 -->
    <main class="login-main">
      <div class="login-card card">
        <!-- 切换标签 -->
        <div class="tabs">
          <button
            class="tab"
            :class="{ active: mode === 'login' }"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            class="tab"
            :class="{ active: mode === 'register' }"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <h3 class="form-title">{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h3>

        <form @submit.prevent="submit">
          <label class="field">
            <span class="field-label">用户名</span>
            <input
              v-model="form.username"
              class="input"
              type="text"
              placeholder="4~20 位字母/数字/下划线，字母开头"
              autocomplete="username"
            />
          </label>

          <label class="field">
            <span class="field-label">密码</span>
            <input
              v-model="form.password"
              class="input"
              type="password"
              placeholder="8~64 位，需含大小写字母和数字"
              autocomplete="current-password"
            />
          </label>

          <p v-if="mode === 'register'" class="tip">
            💡 密码需 8~64 位，且同时包含大写字母、小写字母和数字
          </p>

          <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

          <button class="btn btn-primary submit" type="submit" :disabled="loading">
            {{ loading ? '处理中...' : mode === 'login' ? '登录' : '注册并进入' }}
          </button>
        </form>

        <p class="switch-hint">
          {{ mode === 'login' ? '还没有账号？' : '已经有账号了？' }}
          <a href="javascript:;" @click="switchMode(mode === 'login' ? 'register' : 'login')">
            {{ mode === 'login' ? '去注册' : '去登录' }}
          </a>
        </p>

        <router-link class="back-home" to="/">← 回到首页</router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import RobotMascot from '@/components/RobotMascot.vue'
import { login, register } from '@/api/auth'

const router = useRouter()

/** login = 登录；register = 注册 */
const mode = ref('login')
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: '',
})

/** 切换登录/注册，同时清空提示 */
function switchMode(next) {
  mode.value = next
  errorMsg.value = ''
}

/** 提交表单 */
async function submit() {
  if (!form.username.trim() || !form.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''
  try {
    // 注册与登录成功后后端都会返回 token，直接存起来进入聊天页
    const data = mode.value === 'login'
      ? await login(form.username.trim(), form.password)
      : await register(form.username.trim(), form.password)

    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    router.push('/chat')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #f2fbf8 0%, #e8f3fe 100%);
}

/* ---------- 左侧插画区 ---------- */
.login-aside {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 40px;
}

.aside-title {
  font-size: 26px;
  margin: 0;
  color: #2f4858;
}

.aside-desc {
  text-align: center;
  line-height: 1.9;
  color: var(--text-light);
  margin: 0;
}

/* ---------- 右侧表单区 ---------- */
.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 32px;
}

.tabs {
  display: flex;
  background: #f1f8f5;
  border-radius: 999px;
  padding: 5px;
  margin-bottom: 24px;
}

.tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-light);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.tab.active {
  background: #fff;
  color: var(--mint-deep);
  box-shadow: var(--shadow-soft);
}

.form-title {
  font-size: 20px;
  margin: 0 0 20px;
  color: #2f4858;
}

.field {
  display: block;
  margin-bottom: 16px;
}

.field-label {
  display: block;
  font-size: 13px;
  color: var(--text-light);
  margin-bottom: 8px;
}

.input {
  width: 100%;
  border: 2px solid #e6f2ee;
  border-radius: var(--radius-md);
  padding: 13px 16px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s ease;
  font-family: inherit;
  color: var(--text);
}

.input:focus {
  border-color: var(--mint);
}

.tip {
  font-size: 12px;
  color: var(--text-light);
  background: #fffbe8;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  margin: 0 0 14px;
}

.error {
  font-size: 13px;
  color: #e2685f;
  background: #fff1f0;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  margin: 0 0 14px;
}

.submit {
  width: 100%;
  padding: 13px 0;
  font-size: 16px;
}

.switch-hint {
  text-align: center;
  font-size: 14px;
  color: var(--text-light);
  margin: 18px 0 0;
}

.switch-hint a {
  color: var(--mint-deep);
  text-decoration: none;
  font-weight: 600;
}

.back-home {
  display: block;
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-light);
  text-decoration: none;
}

@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }

  .login-aside {
    padding: 32px 20px 0;
  }
}
</style>
