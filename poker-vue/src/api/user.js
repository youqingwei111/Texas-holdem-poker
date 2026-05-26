import request from '../utils/request'

export const getUserInfo = () => {
  return request.get('/user/me')
}