import { useUserStore } from '../store/userStore'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    console.log('[Request]', config.method?.toUpperCase(), config.url, config.data || '')
    return config
  },
  error => {
    console.error('[Request Error]', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    console.log('[Response]', response.config.url, '=>', JSON.stringify(res).substring(0, 200))

    if (res.code !== 0 && res.code !== 200) {
      console.warn('[Response Error] code:', res.code, 'message:', res.message)
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    console.log('[Response Success] data:', JSON.stringify(res.data)?.substring(0, 200))
    return res.data
  },
  error => {
    console.error('[Network Error]', error.message, error.response?.status)

    if (error.response) {
      const { status, data } = error.response
      console.error('[HTTP Error]', status, JSON.stringify(data)?.substring(0, 200))

      if (status === 401) {
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