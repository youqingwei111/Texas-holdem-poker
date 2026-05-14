import request from './request'

export const getRoomList = () => {
  return request.get('/room/list')
}

export const createRoom = (data) => {
  return request.post('/room/create', data)
}

export const joinRoom = (roomCode) => {
  return request.post('/room/join', { roomCode })
}

export const leaveRoom = (roomCode) => {
  return request.post('/room/leave', { roomCode })
}

export const getRoomInfo = (roomCode) => {
  return request.get(`/room/${roomCode}`)
}