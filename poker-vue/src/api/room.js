import request from '../utils/request'

export const getRoomList = () => {
  return request.get('/room/all')
}

export const createRoom = (data) => {
  return request.post('/room/create', data)
}

export const joinRoom = (roomCode, buyInChips) => {
  return request.post(`/room/join/${roomCode}?buyInChips=${buyInChips}`)
}

export const leaveRoom = (roomCode) => {
  return request.post(`/room/leave/${roomCode}`)
}

export const rebuy = (roomCode, amount) => {
  return request.post(`/room/rebuy?roomCode=${roomCode}&amount=${amount}`)
}

export const getRoomInfo = (roomCode) => {
  return request.get(`/room/${roomCode}`)
}