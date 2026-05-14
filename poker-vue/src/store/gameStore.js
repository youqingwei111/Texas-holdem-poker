import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGameStore = defineStore('game', () => {
  const gameState = ref(null)
  const communityCards = ref([])
  const pot = ref(0)
  const currentTurnUserId = ref(null)
  const myCards = ref([])

  function setGameState(state) {
    gameState.value = state
  }

  function setCommunityCards(cards) {
    communityCards.value = cards
  }

  function setPot(amount) {
    pot.value = amount
  }

  function setCurrentTurnUserId(userId) {
    currentTurnUserId.value = userId
  }

  function setMyCards(cards) {
    myCards.value = cards
  }

  function clearGame() {
    gameState.value = null
    communityCards.value = []
    pot.value = 0
    currentTurnUserId.value = null
    myCards.value = []
  }

  return {
    gameState,
    communityCards,
    pot,
    currentTurnUserId,
    myCards,
    setGameState,
    setCommunityCards,
    setPot,
    setCurrentTurnUserId,
    setMyCards,
    clearGame
  }
})