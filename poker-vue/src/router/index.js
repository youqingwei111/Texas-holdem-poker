import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/lobby',
    name: 'Lobby',
    component: () => import('../views/Lobby.vue')
  },
  {
    path: '/game',
    name: 'Game',
    component: () => import('../views/Game.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router