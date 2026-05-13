import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000 // 增加超时容忍度
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 1. 从本地获取最新 Token
    const token = localStorage.getItem('token')
    
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
      
      // 调试：在控制台打印当前请求的 Token 状态
      console.log(`[Request] ${config.url} - Token injected`)
    } else {
      console.warn(`[Request] ${config.url} - No token found in localStorage`)
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 业务层面的非 200 处理
    if (res.code && res.code !== 200) {
      // 只有在明确是登录失效业务码时才踢出
      if (res.code === 401 || res.code === 403) {
        handleLogout('登录已过期，请重新登录')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return response
  },
  error => {
    // HTTP 状态码 401 处理 (网关抛出的)
    if (error.response && error.response.status === 401) {
      handleLogout('鉴权失败，请重新登录')
    } else {
      const msg = error.response?.data?.message || error.message
      ElMessage.error(`网络异常: ${msg}`)
    }
    return Promise.reject(error)
  }
)

function handleLogout(msg: string) {
  ElMessage.warning(msg)
  localStorage.removeItem('token')
  // 延时跳转，防止弹窗闪现
  setTimeout(() => {
    router.push('/login')
  }, 1000)
}

export default service
