import request from './request'

/**
 * 认证相关接口
 */

/** 注册（成功直接返回 token） */
export function register(username, password) {
  return request.post('/auth/register', { username, password })
}

/** 登录 */
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

/** 获取当前登录用户（用于校验 token 是否有效） */
export function getProfile() {
  return request.get('/auth/me')
}
