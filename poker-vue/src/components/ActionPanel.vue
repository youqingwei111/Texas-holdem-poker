<template>
  <div class="action-panel" :class="{ disabled: !isMyTurn }">
    <div v-if="!isMyTurn && gameStarted" class="turn-tip">
      <span v-if="currentPlayerName">等待 {{ currentPlayerName }} 操作...</span>
      <span v-else>等待其他玩家操作...</span>
    </div>

    <div class="bet-info" v-if="showBetInfo && isMyTurn">
      <div class="info-item">
        <span class="label">当前下注</span>
        <span class="value">{{ currentBet }}</span>
      </div>
      <div class="info-item" v-if="toCall > 0">
        <span class="label">需跟注</span>
        <span class="value highlight">{{ toCall }}</span>
      </div>
      <div class="info-item">
        <span class="label">我的筹码</span>
        <span class="value">{{ myChips }}</span>
      </div>
    </div>

    <div class="action-buttons">
      <el-button
        v-if="actions.includes('CHECK')"
        :disabled="!isMyTurn"
        :loading="isActioning && !isMyTurn"
        type="default"
        size="large"
        @click="handleCheck"
      >
        <el-icon><Check /></el-icon>
        过牌
      </el-button>

      <el-button
        v-if="actions.includes('CALL')"
        type="primary"
        size="large"
        :disabled="!isMyTurn || toCall > myChips"
        :loading="isActioning && !isMyTurn"
        @click="handleCall"
      >
        <el-icon><Top /></el-icon>
        跟注 {{ toCall }}
      </el-button>

      <el-popover
        placement="top"
        :width="350"
        trigger="click"
        :disabled="!isMyTurn || !actions.includes('RAISE') || isActioning"
        v-model:visible="showRaisePanel"
      >
        <template #reference>
          <el-button
            type="warning"
            size="large"
            :disabled="!isMyTurn || !actions.includes('RAISE')"
            :loading="isActioning && !isMyTurn"
          >
            <el-icon><Top /></el-icon>
            加注
          </el-button>
        </template>

        <div class="raise-panel">
          <div class="raise-header">
            <span>加注金额</span>
            <span class="range-info">
              加注 <b>{{ raiseAmount }}</b>，总计 <b>{{ raiseAmount + toCall }}</b>
            </span>
          </div>

          <el-slider
            v-if="effectiveMax >= minRaise"
            v-model="raiseAmount"
            :min="minRaise"
            :max="effectiveMax"
            :step="stepSize"
            :format-tooltip="formatTooltip"
          />

          <div class="quick-buttons">
            <el-button size="small" @click="setRaise(minRaise)">最小</el-button>
            <el-button size="small" @click="setRaise(halfPot)">1/2 底池</el-button>
            <el-button size="small" @click="setRaise(pot)">底池</el-button>
            <el-button size="small" type="danger" @click="setRaise(myChips)">All In</el-button>
          </div>

          <div class="raise-actions">
            <el-button @click="showRaisePanel = false">取消</el-button>
            <el-button type="primary" @click="confirmRaise">确认加注</el-button>
          </div>
        </div>
      </el-popover>

      <el-button
        type="danger"
        size="large"
        :disabled="!isMyTurn"
        :loading="isActioning && !isMyTurn"
        plain
        @click="handleFold"
      >
        <el-icon><Close /></el-icon>
        弃牌
      </el-button>

      <el-button
        v-if="actions.includes('ALL_IN')"
        type="danger"
        size="large"
        :disabled="!isMyTurn"
        :loading="isActioning && !isMyTurn"
        @click="handleAllIn"
      >
        <el-icon><Star /></el-icon>
        全押 {{ myChips }}
      </el-button>
    </div>

    <div class="shortcut-tips" v-if="isMyTurn">
      <span>快捷键: </span>
      <kbd>C</kbd> 过牌/跟注
      <kbd>R</kbd> 加注
      <kbd>F</kbd> 弃牌
      <kbd>A</kbd> 全押
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Check, Top, Close, Star } from '@element-plus/icons-vue'

const props = defineProps({
  isMyTurn: {
    type: Boolean,
    default: false
  },
  currentBet: {
    type: Number,
    default: 0
  },
  toCall: {
    type: Number,
    default: 0
  },
  myChips: {
    type: Number,
    default: 0
  },
  minRaise: {
    type: Number,
    default: 0
  },
  pot: {
    type: Number,
    default: 0
  },
  bigBlind: {
    type: Number,
    default: 20
  },
  showBetInfo: {
    type: Boolean,
    default: true
  },
  currentPlayerName: {
    type: String,
    default: ''
  },
  gameStarted: {
    type: Boolean,
    default: false
  },
  availableActions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['check', 'call', 'raise', 'fold', 'allIn', 'resetActing'])

const showRaisePanel = ref(false)
const raiseAmount = ref(props.minRaise)
const isActioning = ref(false)

const actions = computed(() => props.availableActions || [])

const stepSize = computed(() => props.bigBlind)

const halfPot = computed(() => Math.floor(props.pot / 2))
const effectiveMax = computed(() => props.myChips)

watch(() => props.minRaise, (val) => {
  raiseAmount.value = val
})

watch(() => props.isMyTurn, (isTurn) => {
  if (isTurn) {
    isActioning.value = false
    emit('resetActing')
  }
})

function setRaise(amount) {
  raiseAmount.value = Math.min(Math.max(amount, props.minRaise), props.myChips)
}

function formatTooltip(val) {
  return `${val} (总: ${val + props.toCall})`
}

function confirmRaise() {
  const newBetTotal = raiseAmount.value + props.toCall
  emit('raise', newBetTotal)
  showRaisePanel.value = false
}

function handleCheck() {
  if (!props.isMyTurn || isActioning.value) return
  isActioning.value = true
  emit('check')
}

function handleCall() {
  if (!props.isMyTurn || isActioning.value) return
  isActioning.value = true
  emit('call')
}

function handleFold() {
  if (!props.isMyTurn || isActioning.value) return
  isActioning.value = true
  emit('fold')
}

function handleAllIn() {
  if (!props.isMyTurn || isActioning.value) return
  isActioning.value = true
  emit('allIn')
}

function handleKeydown(e) {
  if (!props.isMyTurn) return
  if (showRaisePanel.value) return

  const actions = props.availableActions || []

  switch (e.key.toLowerCase()) {
    case 'c':
      if (actions.includes('CHECK')) {
        handleCheck()
      } else if (actions.includes('CALL')) {
        handleCall()
      }
      break
    case 'r':
      if (actions.includes('RAISE')) {
        showRaisePanel.value = true
      }
      break
    case 'f':
      if (actions.includes('FOLD')) {
        handleFold()
      }
      break
    case 'a':
      if (actions.includes('ALL_IN')) {
        handleAllIn()
      }
      break
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.action-panel {
  padding: 15px 20px;
  background: rgba(0, 0, 0, 0.5);
  transition: opacity 0.3s;
}

.action-panel.disabled {
  opacity: 0.6;
}

.turn-tip {
  text-align: center;
  color: #aaa;
  font-size: 14px;
  margin-bottom: 10px;
}

.bet-info {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.info-item .label {
  font-size: 12px;
  color: #aaa;
}

.info-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.info-item .value.highlight {
  color: #ffd700;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  min-width: 100px;
}

.raise-panel {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.raise-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.raise-header span:first-child {
  font-weight: bold;
}

.range-info {
  color: #409eff;
}

.range-info b {
  color: #e6a23c;
}

.quick-buttons {
  display: flex;
  gap: 8px;
}

.quick-buttons .el-button {
  flex: 1;
}

.raise-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 5px;
}

.shortcut-tips {
  text-align: center;
  margin-top: 10px;
  color: #999;
  font-size: 12px;
}

.shortcut-tips kbd {
  display: inline-block;
  padding: 2px 6px;
  margin: 0 3px;
  background: #333;
  border: 1px solid #555;
  border-radius: 3px;
  font-size: 11px;
  color: #fff;
}
</style>