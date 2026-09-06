import axios from 'axios'

/**
 * axios 实例
 * baseURL 用 /api，配合 vite 代理转发到后端 http://localhost:8090/api
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截：统一带上 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 未登录处理：清空本地 token 并跳转登录页
 * 这里不用 router 是为了避免循环依赖（router -> views -> api -> router）
 */
function redirectToLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  if (!window.location.pathname.startsWith('/login')) {
    window.location.assign('/login')
  }
}

// 响应拦截：拆掉统一响应体，只把 data 交给调用方
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回结构：{ code, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      if (body.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      redirectToLogin()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    return Promise.reject(
      new Error(error.response?.data?.message || '网络异常，请检查后端服务是否已启动')
    )
  }
)

export default request
