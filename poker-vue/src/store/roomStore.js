import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useRoomStore = defineStore('room', () => {
  const currentRoom = ref(null)
  const roomList = ref([])
  const players = ref([])

  function setCurrentRoom(room) {
    currentRoom.value = room
  }

  function setRoomList(list) {
    roomList.value = list
  }

  function addRoom(room) {
    const index = roomList.value.findIndex(r => r.code === room.code)
    if (index >= 0) {
      roomList.value[index] = room
    } else {
      roomList.value.unshift(room)
    }
  }

  function removeRoom(roomCode) {
    roomList.value = roomList.value.filter(r => r.code !== roomCode)
  }

  function setPlayers(playerList) {
    players.value = playerList
  }

  function addPlayer(player) {
    if (!players.value.find(p => p.userId === player.userId)) {
      players.value.push(player)
    }
  }

  function removePlayer(userId) {
    players.value = players.value.filter(p => p.userId !== userId)
  }

  function updatePlayer(player) {
    const index = players.value.findIndex(p => p.userId === player.userId)
    if (index >= 0) {
      players.value[index] = player
    }
  }

  function clearRoom() {
    currentRoom.value = null
    players.value = []
  }

  return {
    currentRoom,
    roomList,
    players,
    setCurrentRoom,
    setRoomList,
    addRoom,
    removeRoom,
    setPlayers,
    addPlayer,
    removePlayer,
    updatePlayer,
    clearRoom
  }
})