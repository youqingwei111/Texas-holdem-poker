import { useUserStore } from '../store/userStore'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        // Token 失效，跳转登录
        ElMessage.error(data.message || '登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.clearUser()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error(data.message || '没有权限')
      } else if (status === 404) {
        ElMessage.error(data.message || '资源不存在')
      } else if (status >= 500) {
        ElMessage.error(data.message || '服务器错误')
      } else {
        ElMessage.error(data.message || '请求失败')
      }
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request