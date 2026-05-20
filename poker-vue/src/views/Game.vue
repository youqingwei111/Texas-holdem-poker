<template>
  <div class="game-container">
    <!-- 顶部信息栏 -->
    <div class="game-header">
      <div class="room-info">
        <span class="room-code">房间: {{ roomCode }}</span>
        <span class="phase-badge">{{ gameStore.phaseText }}</span>
      </div>
      <div class="pot-display">
        <span class="pot-label">底池</span>
        <span class="pot-amount">{{ gameStore.pot }}</span>
      </div>
    </div>

    <!-- 毛毡牌桌 -->
    <div class="poker-table">
      <!-- 公共牌区域 -->
      <div class="table-center">
        <CommunityCards
          :cards="displayCommunityCards"
          :phaseText="gameStore.phaseText"
          size="normal"
        />
        <div class="pot-info">
          <span class="current-bet">当前下注: {{ gameStore.currentBet }}</span>
        </div>
      </div>

      <!-- 环形玩家座位 -->
      <PlayerSeat
        v-for="player in displayPlayers"
        :key="player.userId"
        :player="player"
        :buttonSeat="gameStore.dealerIndex"
        :currentTurn="gameStore.currentTurnIndex"
        :cards="player.isMe ? gameStore.myCards : []"
        :totalPlayers="maxPlayers"
      />

      <!-- 庄家按钮 -->
      <div v-if="dealerPosition" class="dealer-button" :style="dealerButtonStyle">
        D
      </div>
    </div>

    <!-- 底部操作面板 -->
    <ActionPanel
      :isMyTurn="gameStore.isMyTurn"
      :currentBet="gameStore.currentBet"
      :toCall="gameStore.toCall"
      :myChips="gameStore.myChips"
      :minRaise="gameStore.minRaise"
      :pot="gameStore.pot"
      :bigBlind="gameStore.blinds.big"
      :showBetInfo="true"
      :currentPlayerName="currentPlayerName"
      :gameStarted="gameStore.gameStarted"
      :availableActions="gameStore.availableActions"
      @check="handleCheck"
      @call="handleCall"
      @raise="handleRaise"
      @fold="handleFold"
      @allIn="handleAllIn"
    />

    <!-- 游戏日志 -->
    <div class="game-log">
      <div class="log-header">游戏日志</div>
      <div class="log-content">
        <div v-for="(log, idx) in gameStore.gameLogs" :key="idx" class="log-item" :class="'log-' + log.type">
          {{ log.message }}
        </div>
      </div>
    </div>

    <!-- 开始游戏按钮（房主可见） -->
    <div v-if="showStartButton" class="start-game-area">
      <el-button type="success" size="large" @click="handleStartGame">
        开始游戏
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../store/gameStore'
import { useUserStore } from '../store/userStore'
import { connectWs, disconnectWs, onWsMessage, startPollWs, stopPollWs, playerActionWs, startGameWs, playerReadyWs } from '../websocket/ws'
import PlayerSeat from '../components/PlayerSeat.vue'
import CommunityCards from '../components/CommunityCards.vue'
import ActionPanel from '../components/ActionPanel.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()
const userStore = useUserStore()

const roomCode = ref('')
const maxPlayers = ref(8)

const displayCommunityCards = computed(() => {
  return gameStore.communityCards.map(c => {
    if (typeof c === 'string') return c
    return c?.displayName || c?.toString() || ''
  })
})

const displayPlayers = computed(() => {
  const players = gameStore.players
  if (players.length === 0) return []

  const total = Math.max(players.length, 2)
  const result = []
  const startSeat = gameStore.mySeatIndex ?? 0

  for (let i = 0; i < players.length; i++) {
    result.push({
      ...players[i],
      seat: i
    })
  }

  return result
})

const dealerPosition = computed(() => gameStore.dealerIndex >= 0)

const dealerButtonStyle = computed(() => {
  const total = maxPlayers.value
  const idx = gameStore.dealerIndex
  const angle = (idx * 360 / total) - 90
  const radian = angle * Math.PI / 180
  const radiusX = 42
  const radiusY = 38
  const x = 50 + radiusX * Math.cos(radian)
  const y = 50 + radiusY * Math.sin(radian)

  return {
    left: `${x}%`,
    top: `${y}%`,
    transform: 'translate(-50%, -50%)'
  }
})

const currentPlayerName = computed(() => {
  if (gameStore.currentTurnIndex == null || gameStore.players.length === 0) return ''
  const player = gameStore.players[gameStore.currentTurnIndex]
  return player?.nickname || ''
})

const showStartButton = computed(() => {
  return gameStore.roomInfo?.ownerId === userStore.userInfo?.id && !gameStore.gameStarted
})

function handleCheck() {
  playerActionWs('CHECK', 0)
}

function handleCall() {
  playerActionWs('CALL', gameStore.toCall)
}

function handleRaise(amount) {
  playerActionWs('RAISE', amount)
}

function handleFold() {
  playerActionWs('FOLD', 0)
}

function handleAllIn() {
  playerActionWs('ALL_IN', gameStore.myChips)
}

function handleStartGame() {
  startGameWs()
}

function handleWsMessage(type, data) {
  gameStore.handleWsMessage(type, data)
}

onMounted(() => {
  roomCode.value = route.query.roomCode || gameStore.roomCode

  if (!roomCode.value) {
    ElMessage.error('房间码不存在')
    router.push('/lobby')
    return
  }

  if (!userStore.token) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }

  gameStore.setRoomCode(roomCode.value)

  connectWs(userStore.token, roomCode.value, userStore.userInfo?.id)

  const unsubscribe = onWsMessage(handleWsMessage)

  startPollWs(() => {
    console.log('[Poll] 轮询检查游戏状态...')
  })

  stopPollWs()

  onUnmounted(() => {
    unsubscribe()
    disconnectWs()
    gameStore.resetGame()
  })
})
</script>

<style scoped>
.game-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 30px;
  background: rgba(0, 0, 0, 0.3);
}

.room-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.room-code {
  font-size: 16px;
  font-weight: bold;
}

.phase-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
}

.pot-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.pot-label {
  font-size: 12px;
  color: #aaa;
}

.pot-amount {
  font-size: 28px;
  font-weight: bold;
  color: #ffd700;
}

.poker-table {
  flex: 1;
  position: relative;
  margin: 20px;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, #0d5c2e 0%, #0a4a24 50%, #083d20 100%);
  border: 15px solid #8b4513;
  box-shadow:
    inset 0 0 50px rgba(0, 0, 0, 0.5),
    0 0 30px rgba(0, 0, 0, 0.5);
}

.table-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.pot-info {
  display: flex;
  gap: 20px;
}

.current-bet {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.dealer-button {
  position: absolute;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #ffd700 0%, #ffed4a 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
  color: #000;
  border: 3px solid #fff;
  box-shadow: 0 4px 15px rgba(255, 215, 0, 0.4);
  z-index: 100;
}

.start-game-area {
  position: fixed;
  bottom: 150px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 50;
}

.game-log {
  position: fixed;
  right: 20px;
  top: 80px;
  width: 280px;
  max-height: 400px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 10px;
  overflow: hidden;
}

.log-header {
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.1);
  font-weight: bold;
  font-size: 14px;
}

.log-content {
  padding: 10px;
  max-height: 350px;
  overflow-y: auto;
}

.log-item {
  padding: 5px 10px;
  font-size: 12px;
  margin-bottom: 3px;
  border-radius: 4px;
}

.log-system {
  background: rgba(100, 100, 255, 0.2);
  color: #aaa;
}

.log-room {
  background: rgba(100, 200, 100, 0.2);
  color: #aaf;
}

.log-game {
  background: rgba(255, 215, 0, 0.2);
  color: #ffd700;
}

.log-deal {
  background: rgba(100, 150, 255, 0.2);
  color: #aaf;
}

.log-turn {
  background: rgba(255, 100, 100, 0.2);
  color: #faa;
}

.log-action {
  background: rgba(200, 200, 200, 0.2);
  color: #ddd;
}

.log-error {
  background: rgba(255, 100, 100, 0.3);
  color: #f88;
}
</style>