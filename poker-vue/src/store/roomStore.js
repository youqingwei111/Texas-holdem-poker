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

  function setPlayers(playerList) {
    players.value = playerList
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
    setPlayers,
    clearRoom
  }
})