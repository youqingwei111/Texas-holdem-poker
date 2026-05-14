import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || null)
  const username = ref(localStorage.getItem('username') || '')

  function setUser(user) {
    token.value = user.token
    userId.value = user.userId
    username.value = user.username
    localStorage.setItem('token', user.token)
    localStorage.setItem('userId', user.userId)
    localStorage.setItem('username', user.username)
  }

  function clearUser() {
    token.value = ''
    userId.value = null
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
  }

  function isLoggedIn() {
    return !!token.value
  }

  return {
    token,
    userId,
    username,
    setUser,
    clearUser,
    isLoggedIn
  }
})