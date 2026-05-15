<template>
  <div class="lobby-container">
    <div class="header">
      <h1>游戏大厅</h1>
      <div class="user-info">
        <span class="username">{{ userStore.username }}</span>
        <span class="chips">筹码: {{ userStore.chips }}</span>
        <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" size="large" @click="handleCreateRoom">创建房间</el-button>
      <el-button size="large" @click="loadRoomList">刷新列表</el-button>
    </div>

    <el-table :data="roomList" style="width: 100%; margin-top: 20px" stripe>
      <el-table-column prop="roomCode" label="房间号" width="150" />
      <el-table-column prop="name" label="房间名" />
      <el-table-column prop="playerCount" label="人数" width="100" />
      <el-table-column prop="smallBlind" label="小盲" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'WAITING' ? 'success' : 'warning'">
            {{ scope.row.status === 'WAITING' ? '等待中' : '游戏中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            :disabled="scope.row.status !== 'WAITING'"
            @click="handleJoinRoom(scope.row.roomCode)"
          >
            加入
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="roomList.length === 0" description="暂无房间，点击创建房间开始游戏" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/userStore'
import { useRoomStore } from '../store/roomStore'
import { getRoomList, createRoom, joinRoom } from '../api/room'
import { logout } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const roomStore = useRoomStore()

const roomList = ref([])

const loadRoomList = async () => {
  try {
    const list = await getRoomList()
    roomList.value = list || []
    roomStore.setRoomList(list)
  } catch (error) {
    console.error('获取房间列表失败:', error)
  }
}

const handleCreateRoom = async () => {
  try {
    const room = await createRoom({ name: '房间' + Date.now() })
    roomStore.setCurrentRoom(room)
    router.push('/game')
  } catch (error) {
    console.error('创建房间失败:', error)
  }
}

const handleJoinRoom = async (roomCode) => {
  try {
    await joinRoom(roomCode)
    roomStore.setCurrentRoom({ roomCode })
    router.push('/game')
  } catch (error) {
    console.error('加入房间失败:', error)
  }
}

const handleLogout = async () => {
  try {
    await logout()
  } catch (error) {
    // 忽略退出接口错误
  }
  userStore.clearUser()
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(() => {
  loadRoomList()
})
</script>

<style scoped>
.lobby-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.username {
  font-size: 16px;
  font-weight: bold;
}

.chips {
  color: #67c23a;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>