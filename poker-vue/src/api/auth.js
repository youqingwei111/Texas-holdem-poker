import request from '../utils/request'

export const login = (data) => {
  return request.post('/auth/login', data)
}

export const register = (data) => {
  return request.post('/auth/register', data)
}

export const logout = () => {
  return request.post('/auth/logout')
}

export const getUserInfo = () => {
  return request.get('/user/info')
}

export const updateUserInfo = (data) => {
  return request.put('/user/update', data)
}