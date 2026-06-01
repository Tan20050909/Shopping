import request from '../utils/request'

export const login = (data) => request.post('/admin/login', data)
export const getAdminList = (params) => request.get('/admin/list', { params })
export const addAdmin = (data) => request.post('/admin', data)
export const updateAdmin = (data) => request.put('/admin', data)
export const deleteAdmin = (id) => request.delete(`/admin/${id}`)
export const getAdminRoles = (id) => request.get(`/admin/${id}/roles`)
export const assignRoles = (id, roleIds) => request.post(`/admin/${id}/roles`, roleIds)
