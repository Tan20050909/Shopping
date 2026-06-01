import request from '../utils/request'

export const getChatList = (params) => request.get('/chat/list', { params })
export const sendChatMessage = (data) => request.post('/chat', data)
