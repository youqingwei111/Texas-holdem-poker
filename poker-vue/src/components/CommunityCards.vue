<template>
  <div class="community-cards" :class="{ 'dealing': isDealing }">
    <div class="cards-label" v-if="showLabel">
      <span class="label-text">{{ label }}</span>
      <span class="phase-indicator" v-if="phaseText">{{ phaseText }}</span>
    </div>
    <div class="cards-row">
      <transition-group name="card-deal">
        <PokerCard
          v-for="(card, idx) in cards"
          :key="card"
          :card="card"
          :size="size"
          animate
          class="community-card"
          :style="{ animationDelay: `${idx * 0.1}s` }"
        />
      </transition-group>
      <div
        v-for="i in emptySlots"
        :key="'empty-' + i"
        class="card-slot empty"
      >
        <div class="slot-inner"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PokerCard from './PokerCard.vue'

const props = defineProps({
  cards: {
    type: Array,
    default: () => []
  },
  showLabel: {
    type: Boolean,
    default: true
  },
  label: {
    type: String,
    default: '公共牌'
  },
  size: {
    type: String,
    default: 'normal'
  },
  phaseText: {
    type: String,
    default: ''
  },
  isDealing: {
    type: Boolean,
    default: false
  }
})

const emptySlots = computed(() => Math.max(0, 5 - props.cards.length))
</script>

<style scoped>
.community-cards {
  text-align: center;
}

.cards-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin-bottom: 15px;
}

.label-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  letter-spacing: 2px;
}

.phase-indicator {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.cards-row {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.community-card {
  transition: all 0.3s ease;
}

.community-card:hover {
  transform: translateY(-8px) scale(1.05);
}

.card-slot {
  width: 60px;
  height: 84px;
  border-radius: 8px;
  background: transparent;
  opacity: 0.3;
}

.slot-inner {
  width: 100%;
  height: 100%;
  border: 2px dashed rgba(255, 255, 255, 0.3);
  border-radius: 8px;
}

.card-deal-enter-active {
  animation: dealCard 0.4s ease-out;
}

.card-deal-leave-active {
  animation: dealCard 0.3s ease-in reverse;
}

@keyframes dealCard {
  0% {
    opacity: 0;
    transform: translateY(-50px) rotateY(180deg) scale(0.5);
  }
  100% {
    opacity: 1;
    transform: translateY(0) rotateY(0) scale(1);
  }
}

.community-cards.dealing .community-card {
  animation: flipIn 0.5s ease-out;
}

@keyframes flipIn {
  0% {
    transform: perspective(1000px) rotateY(90deg);
    opacity: 0;
  }
  100% {
    transform: perspective(1000px) rotateY(0);
    opacity: 1;
  }
}
</style>