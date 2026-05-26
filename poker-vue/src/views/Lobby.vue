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
      <el-button type="primary" size="large" @click="showCreateDialog = true">创建房间</el-button>
      <el-button size="large" @click="loadRoomList">刷新列表</el-button>
    </div>

    <el-table :data="roomStore.roomList" style="width: 100%; margin-top: 20px" stripe v-loading="loading">
      <el-table-column prop="code" label="房间号" width="150" />
      <el-table-column prop="name" label="房间名" />
      <el-table-column prop="playerCount" label="人数" width="100">
        <template #default="scope">
          {{ scope.row.players?.length || 0 }}/{{ scope.row.maxPlayers }}
        </template>
      </el-table-column>
      <el-table-column prop="smallBlind" label="小盲" width="80" />
      <el-table-column prop="bigBlind" label="大盲" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isPlaying ? 'warning' : 'success'">
            {{ scope.row.isPlaying ? '游戏中' : '等待中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            :disabled="scope.row.isPlaying || scope.row.full"
            @click="openJoinDialog(scope.row)"
          >
            加入
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="roomStore.roomList.length === 0 && !loading" description="暂无房间，点击创建房间开始游戏" />

    <el-dialog v-model="showCreateDialog" title="创建房间" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="房间名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="小盲注" prop="smallBlind">
          <el-input-number v-model="createForm.smallBlind" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="大盲注" prop="bigBlind">
          <el-input-number v-model="createForm.bigBlind" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="最小带入" prop="minBuyIn">
          <el-input-number v-model="createForm.minBuyIn" :min="1" :max="1000000" />
        </el-form-item>
        <el-form-item label="最大带入" prop="maxBuyIn">
          <el-input-number v-model="createForm.maxBuyIn" :min="1" :max="1000000" />
        </el-form-item>
        <el-form-item label="最大人数" prop="maxPlayers">
          <el-input-number v-model="createForm.maxPlayers" :min="2" :max="9" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateRoom" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showJoinDialog" title="加入房间" width="400px">
      <p style="margin-bottom: 20px">
        房间: <strong>{{ selectedRoom?.name }}</strong><br>
        房间号: <strong>{{ selectedRoom?.code }}</strong>
      </p>
      <el-form :model="joinForm" label-width="80px">
        <el-form-item label="带入筹码">
          <el-input-number
            v-model="joinForm.buyInChips"
            :min="selectedRoom?.minBuyIn"
            :max="selectedRoom?.maxBuyIn"
          />
          <span style="margin-left: 10px; color: #909399">
            (范围: {{ selectedRoom?.minBuyIn }} - {{ selectedRoom?.maxBuyIn }})
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showJoinDialog = false">取消</el-button>
        <el-button type="primary" @click="handleJoinRoom" :loading="joining">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/userStore'
import { useRoomStore } from '../store/roomStore'
import { getRoomList, createRoom, joinRoom } from '../api/room'
import { logout } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const roomStore = useRoomStore()

const loading = ref(false)
const creating = ref(false)
const joining = ref(false)
const showCreateDialog = ref(false)
const showJoinDialog = ref(false)
const selectedRoom = ref(null)
const createFormRef = ref(null)

const createForm = reactive({
  name: '',
  smallBlind: 5,
  bigBlind: 10,
  minBuyIn: 100,
  maxBuyIn: 2000,
  maxPlayers: 8
})

const joinForm = reactive({
  buyInChips: 500
})

const createRules = {
  name: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
  smallBlind: [{ required: true, message: '请输入小盲注', trigger: 'blur' }],
  bigBlind: [{ required: true, message: '请输入大盲注', trigger: 'blur' }],
  minBuyIn: [{ required: true, message: '请输入最小带入', trigger: 'blur' }],
  maxBuyIn: [{ required: true, message: '请输入最大带入', trigger: 'blur' }],
  maxPlayers: [{ required: true, message: '请输入最大人数', trigger: 'blur' }]
}

const loadRoomList = async () => {
  loading.value = true
  try {
    console.log('[Lobby] loadRoomList 开始请求')
    const list = await getRoomList()
    console.log('[Lobby] loadRoomList 成功, list:', JSON.stringify(list)?.substring(0, 200))
    roomStore.setRoomList(list || [])
    console.log('[Lobby] roomStore.roomList:', roomStore.roomList.length)
  } catch (error) {
    console.error('[Lobby] loadRoomList 失败:', error.message, error)
    ElMessage.error('获取房间列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreateRoom = async () => {
  console.log('[Lobby] handleCreateRoom called, showCreateDialog:', showCreateDialog.value)
  if (!createFormRef.value) {
    console.warn('[Lobby] createFormRef 为空，直接创建房间')
  }

  try {
    const validated = await createFormRef.value.validate().catch(err => {
      console.warn('[Lobby] validate 失败:', err)
      throw err
    })
    console.log('[Lobby] validate 结果:', validated)
  } catch (err) {
    console.warn('[Lobby] 表单校验不通过:', err)
    return
  }

  creating.value = true
  console.log('[Lobby] handleCreateRoom 开始创建, createForm:', JSON.stringify(createForm))

  try {
    console.log('[Lobby] 调用 createRoom API')
    const room = await createRoom(createForm)
    console.log('[Lobby] createRoom 返回 room:', JSON.stringify(room)?.substring(0, 300))

    // 更新本地筹码（扣除买入）
    userStore.updateChips(userStore.chips - createForm.minBuyIn)
    // 同步最新余额
    userStore.fetchUserInfo()

    console.log('[Lobby] 设置 roomStore.currentRoom')
    roomStore.setCurrentRoom(room)
    console.log('[Lobby] roomStore.currentRoom:', JSON.stringify(roomStore.currentRoom)?.substring(0, 200))

    console.log('[Lobby] 显示成功消息')
    ElMessage.success('房间创建成功')

    console.log('[Lobby] 开始路由跳转 /game')
    await router.push({ name: 'Game', params: { roomCode: room.code } }).catch(err => {
      console.error('[Lobby] router.push 失败:', err)
      throw err
    })
    console.log('[Lobby] router.push 完成')

    showCreateDialog.value = false
  } catch (error) {
    console.error('[Lobby] handleCreateRoom 失败:', error.message, error)
    ElMessage.error(error.message || '创建房间失败')
  } finally {
    creating.value = false
    console.log('[Lobby] creating 设置为 false')
  }
}

const openJoinDialog = (room) => {
  console.log('[Lobby] openJoinDialog called, room:', JSON.stringify(room)?.substring(0, 200))
  selectedRoom.value = room
  joinForm.buyInChips = room.minBuyIn
  showJoinDialog.value = true
  console.log('[Lobby] selectedRoom:', JSON.stringify(selectedRoom.value)?.substring(0, 200))
  console.log('[Lobby] showJoinDialog:', showJoinDialog.value)
}

const handleJoinRoom = async () => {
  console.log('[Lobby] handleJoinRoom called')
  console.log('[Lobby] selectedRoom:', JSON.stringify(selectedRoom.value)?.substring(0, 200))
  console.log('[Lobby] joinForm.buyInChips:', joinForm.buyInChips)

  if (!selectedRoom.value) {
    console.warn('[Lobby] selectedRoom 为空，取消加入')
    ElMessage.error('请选择一个房间')
    return
  }

  joining.value = true
  console.log('[Lobby] 调用 joinRoom API, roomCode:', selectedRoom.value.code, 'buyInChips:', joinForm.buyInChips)

  try {
    const room = await joinRoom(selectedRoom.value.code, joinForm.buyInChips)
    console.log('[Lobby] joinRoom 返回 room:', JSON.stringify(room)?.substring(0, 300))

    // 更新本地筹码（扣除买入）
    userStore.updateChips(userStore.chips - joinForm.buyInChips)
    // 同步最新余额
    userStore.fetchUserInfo()

    console.log('[Lobby] 设置 roomStore.currentRoom')
    roomStore.setCurrentRoom(room)
    console.log('[Lobby] roomStore.currentRoom:', JSON.stringify(roomStore.currentRoom)?.substring(0, 200))

    console.log('[Lobby] 显示成功消息')
    ElMessage.success('加入房间成功')

    console.log('[Lobby] 开始路由跳转 /game')
    await router.push({ name: 'Game', params: { roomCode: room.code } }).catch(err => {
      console.error('[Lobby] router.push 失败:', err)
      throw err
    })
    console.log('[Lobby] router.push 完成')

    showJoinDialog.value = false
  } catch (error) {
    console.error('[Lobby] handleJoinRoom 失败:', error.message, error)
    ElMessage.error(error.message || '加入房间失败')
  } finally {
    joining.value = false
    console.log('[Lobby] joining 设置为 false')
  }
}

const handleLogout = async () => {
  try {
    await logout()
    console.log('[Lobby] logout 成功')
  } catch (error) {
    console.warn('[Lobby] logout 接口失败（忽略）:', error.message)
  }
  userStore.clearUser()
  console.log('[Lobby] 跳转登录页')
  router.push('/login')
  ElMessage.success('已退出登录')
}

onMounted(() => {
  console.log('[Lobby] onMounted')
  userStore.fetchUserInfo()
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