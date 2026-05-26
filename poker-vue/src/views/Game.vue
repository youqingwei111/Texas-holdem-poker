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
      <div class="header-actions">
        <el-button
          v-if="gameStore.phase === 'WAITING' || gameStore.phase === 'SHOWDOWN'"
          type="warning"
          size="small"
          @click="showRebuyDialog = true"
        >
          补充筹码
        </el-button>
        <el-button
          type="danger"
          size="small"
          :disabled="!canLeaveRoom"
          @click="handleLeaveRoom"
        >
          退出房间
        </el-button>
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

    <!-- 结算弹窗 -->
    <el-dialog
      v-model="showShowdownDialog"
      :title="showdownDialogTitle"
      width="500px"
      :close-on-click-modal="false"
      center
    >
      <div class="showdown-content" v-if="gameStore.showdownData">
        <!-- 赢家信息 -->
        <div class="winner-section" v-if="gameStore.showdownData.winners.length > 0">
          <div class="winner-amount">
            <span class="label">底池</span>
            <span class="value highlight">{{ gameStore.showdownData.pot }}</span>
          </div>
          <div v-if="gameStore.showdownData.isSplit" class="split-notice">
            平局！{{ splitWinnerNames }} 各获得 {{ gameStore.showdownData.winAmount }} 筹码
          </div>
        </div>

        <!-- 公共牌展示 -->
        <div class="community-cards" v-if="gameStore.showdownData.communityCards?.length > 0">
          <div class="section-label">公共牌</div>
          <div class="cards-row">
            <span
              v-for="(card, idx) in gameStore.showdownData.communityCards"
              :key="idx"
              class="community-card"
            >
              {{ card }}
            </span>
          </div>
        </div>

        <!-- 玩家手牌 -->
        <div class="players-hands">
          <div class="section-label">摊牌</div>
          <div
            v-for="player in gameStore.showdownData.players"
            :key="player.userId"
            class="player-hand"
            :class="{ 'is-winner': player.isWinner, 'is-folded': player.isFold }"
          >
            <div class="player-info">
              <span class="player-name">{{ player.nickname }}</span>
              <span class="hand-result" :class="{ 'winner': player.isWinner }">
                {{ player.handName }}
              </span>
            </div>
            <div class="player-cards">
              <span
                v-for="(card, idx) in player.handCards"
                :key="idx"
                class="card"
                :class="{ 'winning-card': player.isWinner }"
              >
                {{ card }}
              </span>
              <span v-if="player.isFold" class="folded-label">已弃牌</span>
            </div>
            <div class="win-amount" v-if="player.isWinner">
              +{{ player.amount }}
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="showdown-footer">
          <el-button type="primary" @click="closeShowdownDialog">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 开始游戏按钮（房主可见） -->
    <div v-if="showStartButton" class="start-game-area">
      <el-button type="success" size="large" @click="handleStartGame">
        开始游戏
      </el-button>
    </div>

    <!-- 补充筹码弹窗 -->
    <el-dialog v-model="showRebuyDialog" title="补充筹码" width="400px" center>
      <div style="text-align: center;">
        <p style="margin-bottom: 15px; color: #aaa;">当前桌上筹码: <strong>{{ currentTableChips }}</strong></p>
        <p style="margin-bottom: 20px; color: #67c23a;">钱包余额: <strong>{{ userStore.chips }}</strong></p>
        <el-form-item label="补充数量">
          <el-input-number
            v-model="rebuyAmount"
            :min="1"
            :max="userStore.chips"
            size="large"
          />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="showRebuyDialog = false">取消</el-button>
        <el-button type="warning" @click="handleRebuy" :loading="rebuying">确认补充</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../store/gameStore'
import { useUserStore } from '../store/userStore'
import { connectWs, disconnectWs, onWsMessage, startPollWs, stopPollWs, playerActionWs, startGameWs, playerReadyWs } from '../websocket/ws'
import { leaveRoom as leaveRoomApi, rebuy as rebuyApi } from '../api/room'
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

// 补充筹码弹窗
const showRebuyDialog = ref(false)
const rebuyAmount = ref(100)
const rebuying = ref(false)
const currentTableChips = ref(0)

// 监听补充筹码弹窗打开，同步当前桌上筹码
import { watch } from 'vue'
watch(showRebuyDialog, (val) => {
  if (val) {
    const me = gameStore.players.find(p => p.userId === userStore.userId)
    currentTableChips.value = me ? (me.chips || 0) : 0
    rebuyAmount.value = 100
  }
})

// 结算弹窗控制
const showShowdownDialog = computed({
  get: () => !!gameStore.showdownData,
  set: (val) => { if (!val) gameStore.showdownData = null }
})

const showdownDialogTitle = computed(() => {
  if (!gameStore.showdownData?.winners?.length) return '结算'
  const w = gameStore.showdownData.winners[0]
  if (gameStore.showdownData.isSplit) {
    return '平局！'
  }
  return `恭喜 ${w.nickname} 获胜！`
})

const splitWinnerNames = computed(() => {
  if (!gameStore.showdownData?.winners) return ''
  return gameStore.showdownData.winners.map(w => w.nickname).join('、')
})

function closeShowdownDialog() {
  gameStore.showdownData = null
  gameStore.resetRound()
}

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

// 允许退出的阶段：WAITING（等待开始）或 SHOWDOWN（结算结束）且游戏未在玩
const canLeaveRoom = computed(() => {
  const phase = gameStore.phase
  return phase === 'WAITING' || phase === 'SHOWDOWN'
})

function handleCheck() {
  gameStore.isMyTurn = false
  playerActionWs('CHECK', 0)
}

function handleCall() {
  gameStore.isMyTurn = false
  playerActionWs('CALL', gameStore.toCall)
}

function handleRaise(amount) {
  gameStore.isMyTurn = false
  playerActionWs('RAISE', amount)
}

function handleFold() {
  gameStore.isMyTurn = false
  playerActionWs('FOLD', 0)
}

function handleAllIn() {
  gameStore.isMyTurn = false
  playerActionWs('ALL_IN', gameStore.myChips)
}

function handleStartGame() {
  startGameWs()
}

async function handleLeaveRoom() {
  try {
    await leaveRoomApi(roomCode.value)
    ElMessage.success('已退出房间')
    // 同步最新余额（退款到账）
    userStore.fetchUserInfo()
  } catch (e) {
    console.warn('[Game] 退出房间接口失败:', e.message)
  }
  disconnectWs()
  gameStore.resetGame()
  router.push('/')
}

async function handleRebuy() {
  if (rebuyAmount.value <= 0) {
    ElMessage.warning('请输入有效的补充数量')
    return
  }
  if (rebuyAmount.value > userStore.chips) {
    ElMessage.warning('钱包余额不足')
    return
  }
  rebuying.value = true
  try {
    await rebuyApi(roomCode.value, rebuyAmount.value)
    ElMessage.success('补充成功')
    showRebuyDialog.value = false
    // 刷新用户余额
    userStore.fetchUserInfo()
    // 刷新游戏状态中的我的筹码
    const me = gameStore.players.find(p => p.userId === userStore.userId)
    if (me) {
      me.chips = (me.chips || 0) + rebuyAmount.value
    }
  } catch (e) {
    ElMessage.error(e.message || '补充筹码失败')
  } finally {
    rebuying.value = false
  }
}

function handleWsMessage(type, data) {
  gameStore.handleWsMessage(type, data)
}

onMounted(() => {
  roomCode.value = route.params.roomCode

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
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  width: 500px;
  max-height: 200px;
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
  max-height: 150px;
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

/* 结算弹窗样式 */
.showdown-content {
  text-align: center;
}

.winner-section {
  margin-bottom: 20px;
}

.winner-amount {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-bottom: 10px;
}

.winner-amount .label {
  font-size: 16px;
  color: #aaa;
}

.winner-amount .value {
  font-size: 28px;
  font-weight: bold;
  color: #ffd700;
}

.winner-amount .value.highlight {
  color: #ffd700;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);
}

.split-notice {
  color: #4fc3f7;
  font-size: 16px;
  padding: 10px;
  background: rgba(79, 195, 247, 0.1);
  border-radius: 8px;
}

.section-label {
  font-size: 12px;
  color: #888;
  margin-bottom: 8px;
  text-align: left;
}

.community-cards {
  margin-bottom: 20px;
}

.cards-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.community-card {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 65px;
  background: #fff;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.players-hands {
  text-align: left;
}

.player-hand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 15px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.player-hand.is-winner {
  background: rgba(255, 215, 0, 0.15);
  border-color: rgba(255, 215, 0, 0.4);
}

.player-hand.is-folded {
  opacity: 0.5;
}

.player-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.player-name {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.hand-result {
  font-size: 12px;
  color: #aaa;
}

.hand-result.winner {
  color: #ffd700;
}

.player-cards {
  display: flex;
  gap: 6px;
  align-items: center;
}

.card {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 55px;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  color: #333;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.card.winning-card {
  border: 2px solid #ffd700;
  box-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
}

.folded-label {
  font-size: 12px;
  color: #666;
  font-style: italic;
}

.win-amount {
  font-size: 18px;
  font-weight: bold;
  color: #4caf50;
}

.showdown-footer {
  text-align: center;
}
</style>