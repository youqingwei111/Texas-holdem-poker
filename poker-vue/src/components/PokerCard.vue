<template>
  <div
    class="poker-card"
    :class="cardClasses"
    :style="cardStyle"
  >
    <div class="card-front" v-if="!hidden">
      <div class="card-corner top-left">
        <span class="rank">{{ displayRank }}</span>
        <span class="suit">{{ displaySuit }}</span>
      </div>
      <div class="card-center">
        <span class="suit-large">{{ displaySuit }}</span>
      </div>
      <div class="card-corner bottom-right">
        <span class="rank">{{ displayRank }}</span>
        <span class="suit">{{ displaySuit }}</span>
      </div>
    </div>
    <div class="card-back" v-else>
      <div class="back-pattern"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  card: {
    type: String,
    default: ''
  },
  hidden: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'normal'
  },
  animate: {
    type: Boolean,
    default: false
  }
})

const parsedCard = computed(() => {
  if (!props.card || props.card.length < 2) return { rank: '', suit: '' }
  const suit = props.card.slice(-1).toLowerCase()
  const rank = props.card.slice(0, -1).toUpperCase()
  return { rank, suit }
})

const displayRank = computed(() => parsedCard.value.rank)

const displaySuit = computed(() => {
  const { suit } = parsedCard.value
  const suitMap = { 'h': '♥', 'd': '♦', 'c': '♣', 's': '♠' }
  return suitMap[suit] || suit
})

const isRed = computed(() => {
  const { suit } = parsedCard.value
  return suit === 'h' || suit === 'd'
})

const cardClasses = computed(() => ({
  [`size-${props.size}`]: true,
  'card-hidden': props.hidden,
  'card-red': !props.hidden && isRed.value,
  'card-black': !props.hidden && !isRed.value,
  'card-animate': props.animate
}))

const cardStyle = computed(() => ({
  '--suit-color': isRed.value ? '#dc3545' : '#212529'
}))
</script>

<style scoped>
.poker-card {
  position: relative;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1), 0 4px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  transform-style: preserve-3d;
}

.poker-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2), 0 12px 24px rgba(0, 0, 0, 0.15);
}

.size-small {
  width: 40px;
  height: 56px;
  font-size: 11px;
}

.size-normal {
  width: 60px;
  height: 84px;
  font-size: 14px;
}

.size-large {
  width: 80px;
  height: 112px;
  font-size: 18px;
}

.card-front {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
}

.card-corner {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
}

.card-corner.bottom-right {
  transform: rotate(180deg);
}

.rank {
  font-weight: bold;
  font-family: 'Georgia', serif;
}

.suit {
  font-size: 0.9em;
}

.card-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.suit-large {
  font-size: 2.5em;
  opacity: 0.3;
}

.card-red .rank,
.card-red .suit,
.card-red .suit-large {
  color: #dc3545;
}

.card-black .rank,
.card-black .suit,
.card-black .suit-large {
  color: #212529;
}

.card-back {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  border: 2px solid #fff;
}

.back-pattern {
  width: 80%;
  height: 80%;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 5px,
    rgba(255, 255, 255, 0.1) 5px,
    rgba(255, 255, 255, 0.1) 10px
  );
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.card-animate {
  animation: dealCard 0.3s ease-out;
}

@keyframes dealCard {
  from {
    opacity: 0;
    transform: translateY(-20px) rotateY(90deg);
  }
  to {
    opacity: 1;
    transform: translateY(0) rotateY(0);
  }
}
</style>