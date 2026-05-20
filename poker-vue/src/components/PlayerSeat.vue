<template>
  <div
    class="player-seat"
    :class="seatClasses"
    :style="seatStyle"
  >
    <div v-if="isCurrentTurn" class="turn-glow"></div>

    <div class="player-avatar" :class="{ 'avatar-active': isCurrentTurn }">
      <el-avatar :size="avatarSize" :src="player.avatar">
        {{ player.nickname?.charAt(0) || '?' }}
      </el-avatar>

      <span v-if="isDealer" class="dealer-btn">
        <span>D</span>
      </span>
    </div>

    <div class="player-info">
      <span class="nickname" :title="player.nickname">{{ player.nickname }}</span>
      <div class="chips-display">
        <span class="chip-icon">🪙</span>
        <span class="chips">{{ formatChips(player.chips) }}</span>
      </div>
    </div>

    <div class="player-cards">
      <template v-if="player.isMe && cards.length">
        <PokerCard
          v-for="(card, idx) in cards"
          :key="idx"
          :card="card"
          size="small"
          animate
        />
      </template>
      <template v-else-if="!player.isFold && player.inHand">
        <PokerCard v-for="idx in 2" :key="idx" hidden size="small" />
      </template>
    </div>

    <div v-if="player.currentBet > 0" class="player-bet-chips">
      <div class="bet-chip" v-for="i in Math.min(5, Math.ceil(player.currentBet / 100))" :key="i">
        🪙
      </div>
      <span class="bet-amount">{{ player.currentBet }}</span>
    </div>

    <transition name="fade">
      <div v-if="player.isFold" class="folded-mask">
        <el-icon><Close /></el-icon>
        <span>弃牌</span>
      </div>
    </transition>

    <transition name="bounce">
      <div v-if="player.isAllIn" class="all-in-tag">
        <span>ALL IN</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import PokerCard from './PokerCard.vue'

const props = defineProps({
  player: {
    type: Object,
    required: true
  },
  buttonSeat: {
    type: Number,
    default: -1
  },
  currentTurn: {
    type: Number,
    default: -1
  },
  cards: {
    type: Array,
    default: () => []
  },
  totalPlayers: {
    type: Number,
    default: 6
  },
  avatarSize: {
    type: Number,
    default: 48
  }
})

const isDealer = computed(() => props.player.seat === props.buttonSeat)
const isCurrentTurn = computed(() => props.player.seat === props.currentTurn)

const seatClasses = computed(() => ({
  'is-me': props.player.isMe,
  'is-turn': isCurrentTurn.value,
  'is-folded': props.player.isFold,
  'is-all-in': props.player.isAllIn
}))

const seatStyle = computed(() => {
  const seatIndex = props.player.seat
  const total = props.totalPlayers

  const angle = (seatIndex * 360 / total) - 90
  const radian = angle * Math.PI / 180

  const radiusX = 42
  const radiusY = 38
  const centerX = 50
  const centerY = 50

  const x = centerX + radiusX * Math.cos(radian)
  const y = centerY + radiusY * Math.sin(radian)

  return {
    left: `${x}%`,
    top: `${y}%`,
    transform: 'translate(-50%, -50%)'
  }
})

function formatChips(chips) {
  if (chips >= 10000) {
    return (chips / 1000).toFixed(1) + 'K'
  }
  return chips.toLocaleString()
}
</script>

<style scoped>
.player-seat {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(20, 30, 40, 0.85);
  border: 2px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  min-width: 90px;
  z-index: 1;
}

.player-seat:hover {
  border-color: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%) scale(1.05);
}

.player-seat.is-me {
  background: rgba(0, 80, 40, 0.9);
  border-color: rgba(76, 175, 80, 0.5);
  box-shadow: 0 0 20px rgba(76, 175, 80, 0.3);
}

.player-seat.is-turn {
  z-index: 10;
}

.player-seat.is-folded {
  opacity: 0.4;
}

.turn-glow {
  position: absolute;
  inset: -8px;
  border-radius: 16px;
  background: transparent;
  border: 3px solid #ffd700;
  animation: pulseGlow 1.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes pulseGlow {
  0%, 100% {
    box-shadow: 0 0 10px #ffd700, 0 0 20px #ffd700;
    opacity: 1;
  }
  50% {
    box-shadow: 0 0 20px #ffd700, 0 0 40px #ffd700;
    opacity: 0.7;
  }
}

.player-avatar {
  position: relative;
  transition: transform 0.3s ease;
}

.player-avatar.avatar-active {
  animation: bounce 0.5s ease infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.dealer-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #ffd700 0%, #ffed4a 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  font-weight: bold;
  color: #000;
  box-shadow: 0 2px 8px rgba(255, 215, 0, 0.5);
  border: 2px solid #fff;
}

.player-info {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nickname {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chips-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.chip-icon {
  font-size: 12px;
}

.chips {
  font-size: 13px;
  font-weight: bold;
  color: #ffd700;
}

.player-cards {
  display: flex;
  gap: 3px;
  min-height: 56px;
  justify-content: center;
}

.player-bet-chips {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  pointer-events: none;
}

.bet-chip {
  font-size: 16px;
  animation: chipDrop 0.3s ease-out;
}

@keyframes chipDrop {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.bet-amount {
  background: #ffd700;
  color: #000;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: bold;
  white-space: nowrap;
}

.folded-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  border-radius: 12px;
  color: #ff6b6b;
  font-size: 14px;
  font-weight: bold;
}

.all-in-tag {
  position: absolute;
  top: -15px;
  background: linear-gradient(135deg, #ff416c 0%, #ff4b2b 100%);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: bold;
  color: #fff;
  box-shadow: 0 4px 15px rgba(255, 65, 108, 0.4);
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.bounce-enter-active {
  animation: bounceIn 0.5s ease;
}

@keyframes bounceIn {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
</style>