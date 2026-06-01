import request from '../utils/request'

export const getUserList = (params) => request.get('/user/list', { params })
export const getUserDetail = (id) => request.get(`/user/${id}`)
export const updateUser = (data) => request.put('/user', data)
export const updateUserStatus = (id, status) => request.put(`/user/${id}/status`, null, { params: { status } })
export const deleteUser = (id) => request.delete(`/user/${id}`)
export const updateUserRiskTag = (id, riskTag, reason) => request.put(`/user/${id}/risk-tag`, null, { params: { riskTag, reason } })
export const getUserRiskLogs = (params) => request.get('/user/risk-logs', { params })
