<template>
  <div class="lobby-container">
    <h1>游戏大厅</h1>
    <div class="room-list">
      <el-button type="primary" @click="handleCreateRoom">创建房间</el-button>
      <el-table :data="roomList" style="width: 100%">
        <el-table-column prop="roomCode" label="房间号" />
        <el-table-column prop="name" label="房间名" />
        <el-table-column prop="playerCount" label="人数" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button @click="handleJoinRoom(scope.row.roomCode)">加入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomStore } from '../store/roomStore'
import { getRoomList, createRoom, joinRoom } from '../api/room'

const router = useRouter()
const roomStore = useRoomStore()

const roomList = ref([])

const loadRoomList = async () => {
  try {
    const list = await getRoomList()
    roomList.value = list
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

onMounted(() => {
  loadRoomList()
})
</script>

<style scoped>
.lobby-container {
  padding: 20px;
}
</style>