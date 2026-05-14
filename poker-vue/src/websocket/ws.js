import { ref } from 'vue'
import { WS_URL, MessageType } from './messageTypes'
import { useUserStore } from '../store/userStore'
import { useGameStore } from '../store/gameStore'

let ws = null
let reconnectTimer = null
let pollTimer = null

export const isConnected = ref(false)

export function connect(roomCode) {
  const userStore = useUserStore()
  const gameStore = useGameStore()

  if (ws && ws.readyState === WebSocket.OPEN) {
    return
  }

  ws = new WebSocket(WS_URL)

  ws.onopen = () => {
    isConnected.value = true
    stopPoll()
    sendMessage({ type: MessageType.CONNECT, userId: userStore.userId, roomCode })
  }

  ws.onmessage = (event) => {
    const message = JSON.parse(event.data)
    handleMessage(message, gameStore)
  }

  ws.onclose = () => {
    isConnected.value = false
    startPoll()
    scheduleReconnect(roomCode)
  }

  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
    isConnected.value = false
  }
}

function handleMessage(message, gameStore) {
  switch (message.type) {
    case MessageType.DEAL_CARDS:
      if (message.isMine) {
        gameStore.setMyCards(message.cards)
      }
      break
    case MessageType.PHASE_CHANGE:
      gameStore.setCommunityCards(message.communityCards)
      break
    case MessageType.GAME_STATE:
      gameStore.setGameState(message.gameState)
      break
    case MessageType.YOUR_TURN:
      gameStore.setCurrentTurnUserId(message.userId)
      break
    case MessageType.SHOWDOWN_RESULT:
      gameStore.setGameState(message.result)
      break
  }
}

export function sendMessage(data) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(data))
  }
}

function scheduleReconnect(roomCode) {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect(roomCode)
  }, 3000)
}

function startPoll() {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    console.log('Polling for game state...')
  }, 2000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

export function disconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  stopPoll()
  if (ws) {
    ws.close()
    ws = null
  }
  isConnected.value = false
}