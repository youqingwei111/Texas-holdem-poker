<template>
  <div class="login-container">
    <h1>德州扑克</h1>
    <el-form :model="form" class="login-form">
      <el-form-item>
        <el-input v-model="form.username" placeholder="用户名" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" type="password" placeholder="密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleLogin">登录</el-button>
        <el-button @click="handleRegister">注册</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/userStore'
import { login, register } from '../api/auth'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  try {
    const data = await login(form)
    userStore.setUser(data)
    router.push('/lobby')
  } catch (error) {
    console.error('登录失败:', error)
  }
}

const handleRegister = async () => {
  try {
    await register(form)
    handleLogin()
  } catch (error) {
    console.error('注册失败:', error)
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

.login-form {
  width: 300px;
}
</style>