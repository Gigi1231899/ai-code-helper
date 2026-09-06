import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'
// highlight.js 的代码高亮主题（用于 AI 回答里的代码块）
import 'highlight.js/styles/github.css'

createApp(App).use(router).mount('#app')
