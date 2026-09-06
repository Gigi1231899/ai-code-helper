import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import ChatView from '@/views/ChatView.vue'

/**
 * 路由表
 * meta.requiresAuth = true 的页面必须登录后才能进入
 */
const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/chat', name: 'chat', component: ChatView, meta: { requiresAuth: true } },
  // 兜底：未匹配的地址回首页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：未登录访问聊天页 -> 跳转登录页
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    return { name: 'login' }
  }
  return true
})

export default router
