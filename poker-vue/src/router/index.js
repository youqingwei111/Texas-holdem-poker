import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/userStore'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/lobby',
    name: 'Lobby',
    component: () => import('../views/Lobby.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/game/:roomCode',
    name: 'Game',
    component: () => import('../views/Game.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    next({ name: 'Login' })
  } else if (to.name === 'Login' && userStore.isLoggedIn) {
    // 已登录访问登录页，跳转到大厅
    next({ name: 'Lobby' })
  } else {
    next()
  }
})

export default router