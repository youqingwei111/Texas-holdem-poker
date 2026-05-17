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

    <!-- 创建房间对话框 -->
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

    <!-- 加入房间对话框 -->
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
    const list = await getRoomList()
    roomStore.setRoomList(list || [])
  } catch (error) {
    console.error('获取房间列表失败:', error)
    ElMessage.error('获取房间列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreateRoom = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true
      try {
        const room = await createRoom(createForm)
        roomStore.setCurrentRoom(room)
        ElMessage.success('房间创建成功')
        router.push('/game')
      } catch (error) {
        console.error('创建房间失败:', error)
        ElMessage.error(error.message || '创建房间失败')
      } finally {
        creating.value = false
      }
    }
  })
}

const openJoinDialog = (room) => {
  selectedRoom.value = room
  joinForm.buyInChips = room.minBuyIn
  showJoinDialog.value = true
}

const handleJoinRoom = async () => {
  if (!selectedRoom.value) return
  joining.value = true
  try {
    await joinRoom(selectedRoom.value.code, joinForm.buyInChips)
    roomStore.setCurrentRoom(selectedRoom.value)
    ElMessage.success('加入房间成功')
    router.push('/game')
  } catch (error) {
    console.error('加入房间失败:', error)
    ElMessage.error(error.message || '加入房间失败')
  } finally {
    joining.value = false
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