import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || null)
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const chips = ref(parseInt(localStorage.getItem('chips') || '10000'))

  const isLoggedIn = computed(() => !!token.value)

  function setUser(user) {
    token.value = user.token || user.accessToken || ''
    userId.value = user.userId || user.id
    username.value = user.username || ''
    nickname.value = user.nickname || user.username || ''
    chips.value = user.chips || 10000

    localStorage.setItem('token', token.value)
    localStorage.setItem('userId', userId.value)
    localStorage.setItem('username', username.value)
    localStorage.setItem('nickname', nickname.value)
    localStorage.setItem('chips', chips.value)
  }

  function updateUserInfo(userInfo) {
    if (userInfo.nickname) nickname.value = userInfo.nickname
    if (userInfo.chips) chips.value = userInfo.chips
    if (userInfo.avatar) localStorage.setItem('avatar', userInfo.avatar)

    if (userInfo.nickname) localStorage.setItem('nickname', userInfo.nickname)
    if (userInfo.chips) localStorage.setItem('chips', userInfo.chips)
  }

  function updateChips(newChips) {
    chips.value = newChips
    localStorage.setItem('chips', newChips)
  }

  function clearUser() {
    token.value = ''
    userId.value = null
    username.value = ''
    nickname.value = ''
    chips.value = 10000

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    localStorage.removeItem('chips')
  }

  return {
    token,
    userId,
    username,
    nickname,
    chips,
    isLoggedIn,
    setUser,
    updateUserInfo,
    updateChips,
    clearUser
  }
})