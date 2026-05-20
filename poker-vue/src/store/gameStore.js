import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from './userStore'

export const GAME_PHASE = {
  WAITING:   'WAITING',
  PRE_FLOP:  'PRE_FLOP',
  FLOP:      'FLOP',
  TURN:      'TURN',
  RIVER:     'RIVER',
  SHOWDOWN:  'SHOWDOWN',
  FINISHED:  'FINISHED'
}

export const useGameStore = defineStore('game', () => {
  const userStore = useUserStore()

  const roomCode = ref(null)
  const roomInfo = ref(null)
  const players = ref([])
  const myCards = ref([])
  const communityCards = ref([])
  const pot = ref(0)
  const currentTurnIndex = ref(null)
  const phase = ref(GAME_PHASE.WAITING)
  const currentBet = ref(0)
  const blinds = ref({ small: 10, big: 20 })
  const minRaise = ref(20)
  const availableActions = ref([])
  const callAmount = ref(0)
  const mySeatIndex = ref(null)
  const isMyTurn = ref(false)
  const gameStarted = ref(false)
  const dealerIndex = ref(0)
  const winnerList = ref([])
  const showdownPlayers = ref([])
  const error = ref(null)
  const gameLogs = ref([])

  const myPlayer = computed(() => players.value.find(p => p.isMe) || null)
  const myChips = computed(() => myPlayer.value?.chips || 0)
  const myCurrentBet = computed(() => myPlayer.value?.currentBet || 0)
  const activePlayers = computed(() => players.value.filter(p => !p.isFold))
  const toCall = computed(() => Math.max(0, currentBet.value - myCurrentBet.value))
  const canCheck = computed(() => toCall.value === 0)

  const phaseText = computed(() => {
    const map = {
      [GAME_PHASE.WAITING]:  '等待开始',
      [GAME_PHASE.PRE_FLOP]: '翻牌前',
      [GAME_PHASE.FLOP]:     '翻牌',
      [GAME_PHASE.TURN]:     '转牌',
      [GAME_PHASE.RIVER]:    '河牌',
      [GAME_PHASE.SHOWDOWN]: '摊牌',
      [GAME_PHASE.FINISHED]: '已结束'
    }
    return map[phase.value] || phase.value
  })

  function addLog(type, msg) {
    gameLogs.value.push({ type, message: msg, ts: Date.now() })
    if (gameLogs.value.length > 50) gameLogs.value.shift()
  }

  function mapPlayer(p, myUserId) {
    return {
      userId:     p.userId,
      username:   p.username,
      nickname:   p.nickname || p.username,
      chips:      p.chips || 0,
      currentBet: p.currentBet || 0,
      isFold:     !!p.isFold,
      isAllIn:    !!p.isAllIn,
      isOnline:   p.isOnline !== false,
      seat: p.position ?? 0,
      isMe:       p.userId === myUserId
    }
  }

  function setRoomCode(code) { roomCode.value = code }

  function setRoomInfo(room) {
    roomInfo.value = room
    if (room?.smallBlind != null) blinds.value.small = room.smallBlind
    if (room?.bigBlind != null) blinds.value.big = room.bigBlind
  }

  function setPlayers(playerList, myUserId) {
    const uid = myUserId ?? userStore.userInfo?.id
    const mapped = (playerList || []).map(p => mapPlayer(p, uid))
    players.value = mapped
    const me = mapped.find(p => p.isMe)
    if (me) mySeatIndex.value = me.seat
  }

  function updatePlayersFromGameState(playersState) {
    if (!playersState) return
    const uid = userStore.userInfo?.id
    players.value = playersState.map(p => mapPlayer(p, uid))
    const me = players.value.find(p => p.isMe)
    if (me) mySeatIndex.value = me.seat
  }

  function updateMyCard(cards) { myCards.value = cards || [] }
  function setCommunityCards(cards) { communityCards.value = cards || [] }
  function setPot(amount) { pot.value = amount || 0 }
  function setCurrentTurn(index) { currentTurnIndex.value = index }
  function setPhase(newPhase) { phase.value = newPhase }
  function setCurrentBet(bet) { currentBet.value = bet || 0 }
  function setDealer(index) { dealerIndex.value = index ?? 0 }
  function setWinnerList(list) { winnerList.value = list || [] }

  function startGame(data) {
    const uid = userStore.userInfo?.id
    gameStarted.value = true
    setPhase(GAME_PHASE.PRE_FLOP)
    if (data?.dealerIndex != null) setDealer(data.dealerIndex)
    if (data?.players) setPlayers(data.players, uid)
    if (data?.currentTurnIndex != null) setCurrentTurn(data.currentTurnIndex)
    addLog('game', '游戏开始！')
  }

  function handleWsMessage(type, data) {
    console.log('[GameStore]', type, data)
    const myUserId = userStore.userInfo?.id

    switch (type) {
      case 'CONNECT':
        addLog('system', `已连接，用户: ${data?.username}`)
        break

      case 'ROOM_UPDATE': {
        if (data?.code) setRoomCode(data.code)
        setRoomInfo(data)
        if (data?.players) setPlayers(data.players, myUserId)
        if (data?.isPlaying) gameStarted.value = true
        break
      }

      case 'PLAYER_JOINED': {
        if (!players.value.find(x => x.userId === data?.userId)) {
          const p = mapPlayer({
            userId: data.userId,
            username: data.username,
            nickname: data.username,
            chips: 0,
            currentBet: 0,
            isFold: false,
            isAllIn: false,
            isOnline: true,
            seat: players.value.length
          }, myUserId)
          players.value.push(p)
        }
        addLog('room', `${data.username} 加入了房间`)
        break
      }

      case 'PLAYER_LEFT': {
        const reason = data?.reason === 'disconnect' ? '掉线' : '离开'
        const p = players.value.find(x => x.userId === data?.userId)
        if (p) {
          addLog('room', `${p.nickname} ${reason}了`)
          players.value = players.value.filter(x => x.userId !== data?.userId)
        }
        break
      }

      case 'PLAYER_READY': {
        const p = players.value.find(x => x.userId === data?.userId)
        if (p) addLog('room', `${p.nickname} 已准备`)
        break
      }

      case 'GAME_STATE': {
        if (data.phase != null) setPhase(data.phase)
        if (data.pot != null) setPot(data.pot)
        if (data.currentBet != null) setCurrentBet(data.currentBet)
        if (data.dealerIndex != null) setDealer(data.dealerIndex)
        if (data.currentTurnIndex != null) setCurrentTurn(data.currentTurnIndex)
        if (data.communityCards != null) setCommunityCards(data.communityCards)
        if (data.players != null) updatePlayersFromGameState(data.players)
        if (data.minRaise != null) minRaise.value = data.minRaise
        break
      }

      case 'GAME_START': {
        startGame(data)
        break
      }

      case 'GAME_END':
        gameStarted.value = false
        addLog('game', '游戏结束')
        break

      case 'DEAL_CARDS':
        if (data?.cards) {
          updateMyCard(data.cards)
          addLog('deal', `发到手牌: ${data.cards.join(' ')}`)
        }
        break

      case 'PHASE_CHANGE': {
        setPhase(data.phase)
        if (data?.communityCards) setCommunityCards(data.communityCards)
        const names = { 'FLOP': '翻牌', 'TURN': '转牌', 'RIVER': '河牌' }
        if (names[data.phase]) {
          addLog('deal', `进入${names[data.phase]}阶段，公共牌: ${(data.communityCards || []).join(' ')}`)
        }
        break
      }

      case 'YOUR_TURN': {
        if (data?.userId === myUserId) {
          isMyTurn.value = true
          addLog('turn', '轮到你了！')
        } else {
          isMyTurn.value = false
        }
        if (Array.isArray(data?.availableActions)) {
          availableActions.value = data.availableActions
        }
        if (data?.callAmount != null) callAmount.value = data.callAmount
        if (data?.minRaise != null) minRaise.value = data.minRaise
        if (data?.currentTurnIndex != null) currentTurnIndex.value = data.currentTurnIndex
        break
      }

      case 'ACTION_RESULT': {
        const p = players.value.find(x => x.userId === data?.userId)
        const name = p?.nickname || data.username || '玩家'
        const actNames = { FOLD: '弃牌', CHECK: '过牌', CALL: '跟注', RAISE: '加注', ALL_IN: '全下' }
        addLog('action', `${name} ${actNames[data.action] || data.action}${data.amount ? ' ' + data.amount : ''}`)
        break
      }

      case 'ROUND_RESULT': {
        const p = players.value.find(x => x.userId === data?.winnerId)
        addLog('game', `${p?.nickname || data.winnerName} 获胜，赢得 ${data.winAmount} 筹码`)
        setWinnerList([{ userId: data.winnerId, nickname: p?.nickname || data.winnerName, amount: data.winAmount }])
        gameStarted.value = false
        break
      }

      case 'SHOWDOWN': {
        const p = players.value.find(x => x.userId === data?.winnerId)
        addLog('game', `${p?.nickname || data.winnerName} 获胜！${data.handRank} 赢得 ${data.winAmount} 筹码`)
        setWinnerList([{
          userId: data.winnerId,
          nickname: p?.nickname || data.winnerName,
          amount: data.winAmount,
          handName: data.handRank
        }])
        gameStarted.value = false
        break
      }

      case 'SHOWDOWN_RESULT': {
        const rawPlayers = data.players || []
        showdownPlayers.value = rawPlayers.map(p => ({
          userId: p.userId,
          nickname: p.nickname || p.username,
          amount: p.winAmount || 0,
          handName: p.handRank || (p.isFold ? '已弃牌' : '未知'),
          handCards: p.handCards || [],
          isFold: !!p.isFold,
          isWinner: !!p.isWinner
        }))

        const winners = showdownPlayers.value.filter(p => p.isWinner)
        setWinnerList(winners)

        if (data.communityCards) setCommunityCards(data.communityCards)
        gameStarted.value = false

        if (data.isSplit) {
          const names = winners.map(w => w.nickname).join('、')
          addLog('game', `平局！${names} 各赢得 ${winners[0]?.amount} 筹码`)
        } else {
          const w = winners[0]
          if (w) addLog('game', `${w.nickname} 获胜！${w.handName} 赢得 ${w.amount} 筹码`)
        }
        break
      }

      case 'CHAT':
        break

      case 'ERROR':
        error.value = data?.message
        addLog('error', `错误: ${data?.message}`)
        break

      default:
        console.warn('[GameStore] 未知消息类型:', type)
    }
  }

  function resetRound() {
    myCards.value = []
    communityCards.value = []
    pot.value = 0
    currentTurnIndex.value = null
    isMyTurn.value = false
    currentBet.value = 0
    winnerList.value = []
    showdownPlayers.value = []
    players.value = players.value.map(p => ({
      ...p, currentBet: 0, isFold: false, isAllIn: false, handCards: undefined
    }))
    availableActions.value = []
    callAmount.value = 0
  }

  function resetGame() {
    roomCode.value = null
    roomInfo.value = null
    players.value = []
    myCards.value = []
    communityCards.value = []
    pot.value = 0
    currentTurnIndex.value = null
    phase.value = GAME_PHASE.WAITING
    currentBet.value = 0
    blinds.value = { small: 10, big: 20 }
    minRaise.value = 20
    mySeatIndex.value = null
    isMyTurn.value = false
    gameStarted.value = false
    dealerIndex.value = 0
    winnerList.value = []
    showdownPlayers.value = []
    error.value = null
    gameLogs.value = []
    availableActions.value = []
    callAmount.value = 0
  }

  return {
    roomCode, roomInfo, players, myCards, communityCards, pot,
    currentTurnIndex, phase, currentBet, blinds, minRaise,
    mySeatIndex, isMyTurn, gameStarted, dealerIndex,
    winnerList, showdownPlayers, error, gameLogs,
    availableActions, callAmount,
    myPlayer, myChips, myCurrentBet, activePlayers,
    toCall, canCheck, phaseText,
    setRoomCode, setRoomInfo, setPlayers, updatePlayersFromGameState,
    updateMyCard, setCommunityCards, setPot, setCurrentTurn, setPhase,
    setCurrentBet, setDealer, setWinnerList, handleWsMessage,
    startGame, resetRound, resetGame, addLog
  }
})