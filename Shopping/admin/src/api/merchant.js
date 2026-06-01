import request from '../utils/request'

export const getMerchantList = (params) => request.get('/merchant/list', { params })
export const auditMerchant = (merchantIdOrData, auditStatus, auditRemark = '') => {
  const data = typeof merchantIdOrData === 'object'
    ? merchantIdOrData
    : { merchantId: merchantIdOrData, auditStatus, auditRemark }
  return request.post('/merchant/audit', data)
}
export const updateMerchantStatus = (id, status) => request.put(`/merchant/${id}/status`, null, { params: { status } })
export const getMerchantDetail = (id) => request.get(`/merchant/${id}`)
export const updateMerchantCredit = (data) => request.post('/merchant/credit', data)
export const freezeMerchant = (id, reason) => request.put(`/merchant/${id}/freeze`, { reason })
export const unfreezeMerchant = (id) => request.put(`/merchant/${id}/unfreeze`)
export const delistMerchant = (id) => request.post(`/merchant/${id}/delist`)
export const getMerchantCreditLogs = (id) => request.get(`/merchant/${id}/credit-logs`)
export const getMerchantAuditLogs = (id) => request.get(`/merchant/${id}/audit-logs`)
