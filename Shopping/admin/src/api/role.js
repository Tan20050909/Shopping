import request from '../utils/request'

export const getRoleList = (params) => request.get('/role/list', { params })
export const getAllRoles = () => request.get('/role/all')
export const addRole = (data) => request.post('/role', data)
export const updateRole = (data) => request.put('/role', data)
export const deleteRole = (id) => request.delete(`/role/${id}`)
export const getRolePermissions = (id) => request.get(`/role/${id}/permissions`)
export const setRolePermissions = (id, permissionIds) => request.put(`/role/${id}/permissions`, permissionIds)
