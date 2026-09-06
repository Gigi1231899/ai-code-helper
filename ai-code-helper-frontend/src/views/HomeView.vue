<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="nav">
      <div class="logo">
        <span class="logo-emoji">🤖</span>
        <span class="logo-text">AI 编程小助手</span>
      </div>
      <div class="nav-actions">
        <button class="btn btn-ghost" @click="goLogin">登录</button>
        <button class="btn btn-primary" @click="goLogin">注册</button>
      </div>
    </header>

    <!-- 主视觉区 -->
    <section class="hero">
      <div class="hero-text">
        <h1 class="hero-title">
          你的专属<span class="highlight">编程学习 &amp; 面试</span> AI 伙伴
        </h1>
        <p class="hero-sub">刷题练项目｜调试 bug｜面试八股问答</p>
        <button class="btn btn-primary btn-lg" @click="goChat">立即开始使用 →</button>
      </div>
      <div class="hero-illustration">
        <RobotMascot :size="260" />
      </div>
    </section>

    <!-- 三大核心亮点 -->
    <section class="features">
      <div v-for="item in features" :key="item.title" class="feature-card card">
        <div class="feature-icon">{{ item.icon }}</div>
        <h3 class="feature-title">{{ item.title }}</h3>
        <p class="feature-desc">{{ item.desc }}</p>
      </div>
    </section>

    <footer class="footer">用可爱的方式，搞定硬核的编程题 💚</footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import RobotMascot from '@/components/RobotMascot.vue'

const router = useRouter()

/** 三种对话模式亮点 */
const features = [
  {
    icon: '📚',
    title: 'RAG 本地知识库对话',
    desc: '上传你的技术文档与面试题库，AI 只基于你的私人口袋知识回答，标注来源不胡编。',
  },
  {
    icon: '🌐',
    title: 'MCP 联网实时搜索',
    desc: '遇到新技术、新版本？联网检索最新文档与社区讨论，答案永远不过时。',
  },
  {
    icon: '⚡',
    title: 'RAG+MCP 混合增强模式',
    desc: '知识库打底 + 联网补充，既稳又新，复杂问题一次问清楚。',
  },
]

/** 已登录直接进聊天页，否则去登录页 */
function goChat() {
  if (localStorage.getItem('token')) {
    router.push('/chat')
  } else {
    router.push('/login')
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style scoped>
.home {
  min-height: 100%;
  background: linear-gradient(180deg, #f2fbf8 0%, var(--bg) 45%);
}

/* ---------- 顶部导航 ---------- */
.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 48px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 800;
  color: #2f4858;
}

.logo-emoji {
  font-size: 28px;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

/* ---------- 主视觉 ---------- */
.hero {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
  padding: 40px 48px 20px;
  flex-wrap: wrap;
}

.hero-text {
  max-width: 520px;
}

.hero-title {
  font-size: 40px;
  line-height: 1.35;
  margin: 0 0 16px;
  color: #2f4858;
}

.highlight {
  color: var(--mint-deep);
}

.hero-sub {
  font-size: 17px;
  color: var(--text-light);
  margin: 0 0 28px;
  letter-spacing: 1px;
}

.btn-lg {
  font-size: 17px;
  padding: 14px 34px;
}

/* ---------- 亮点卡片 ---------- */
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 24px;
  padding: 40px 48px;
  max-width: 1180px;
  margin: 0 auto;
}

.feature-card {
  padding: 28px 24px;
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.feature-title {
  font-size: 18px;
  margin: 0 0 10px;
  color: #2f4858;
}

.feature-desc {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-light);
  margin: 0;
}

.footer {
  text-align: center;
  padding: 32px 0 40px;
  color: var(--text-light);
  font-size: 14px;
}

@media (max-width: 768px) {
  .nav,
  .hero,
  .features {
    padding-left: 20px;
    padding-right: 20px;
  }

  .hero-title {
    font-size: 30px;
  }
}
</style>
