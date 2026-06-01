import request from '../utils/request'

export const getConfigList = (params) => request.get('/config/list', { params })
export const getConfigMap = (params) => request.get('/config/map', { params })
export const updateConfig = (id, data) => request.put(`/config/${id}`, data)
