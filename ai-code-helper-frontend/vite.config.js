import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置
 * - @ 指向 src 目录，import 时不用写一长串相对路径
 * - 开发服务器把 /api 代理到后端 http://localhost:8090，避免跨域
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        // SSE 流式相关：避免代理层缓冲，否则前端要等响应结束才收到数据
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // no-transform：禁止中间层做转换或压缩，压缩会导致缓冲
            proxyRes.headers['Cache-Control'] = 'no-cache, no-transform'
            // 关闭 Nginx 一类反向代理的响应缓冲
            proxyRes.headers['X-Accel-Buffering'] = 'no'
            // 流式响应长度不固定，去掉可能存在的 Content-Length
            delete proxyRes.headers['content-length']
          })
        },
      },
    },
  },
})
