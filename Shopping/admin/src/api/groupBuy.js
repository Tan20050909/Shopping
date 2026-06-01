import request from '../utils/request'

export const getGroupBuyList = (params) => request.get('/group-buy/list', { params })
export const addGroupBuy = (data) => request.post('/group-buy', data)
export const updateGroupBuy = (data) => request.put('/group-buy', data)
export const updateGroupBuyStatus = (id, status) => request.put(`/group-buy/${id}/status`, null, { params: { status } })
export const deleteGroupBuy = (id) => request.delete(`/group-buy/${id}`)
