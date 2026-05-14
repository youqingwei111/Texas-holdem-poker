<template>
  <div class="game-container">
    <h1>游戏房间</h1>
    <div class="game-content">
      <div class="community-cards">
        <div v-for="(card, index) in communityCards" :key="index" class="card">
          {{ card }}
        </div>
      </div>
      <div class="players">
        <div v-for="player in players" :key="player.userId" class="player-seat">
          <span>{{ player.username }}</span>
          <span>{{ player.chips }}</span>
        </div>
      </div>
      <div class="pot">底池: {{ pot }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '../store/gameStore'
import { useUserStore } from '../store/userStore'

const route = useRoute()
const gameStore = useGameStore()
const userStore = useUserStore()

const communityCards = ref([])
const players = ref([])
const pot = ref(0)

onMounted(() => {
  const roomCode = route.query.roomCode
  console.log('进入房间:', roomCode)
})

onUnmounted(() => {
  gameStore.clearGame()
})
</script>

<style scoped>
.game-container {
  padding: 20px;
}

.game-content {
  margin-top: 20px;
}

.community-cards {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 20px;
}

.card {
  width: 60px;
  height: 90px;
  border: 1px solid #333;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
}

.players {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.player-seat {
  padding: 10px;
  border: 1px solid #333;
  border-radius: 5px;
}

.pot {
  text-align: center;
  font-size: 20px;
  margin-top: 20px;
}
</style>