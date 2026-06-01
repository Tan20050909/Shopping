import request from '../utils/request'

export const getOrderList = (params) => request.get('/order/list', { params })
export const getOrderDetail = (id) => request.get(`/order/${id}`)
export const remindMerchantShip = (id, data = {}) => request.post(`/order/${id}/remind-ship`, data)
export const refundOrder = (id, data = {}) => request.put(`/order/${id}/refund`, {
  refundAmount: data.refundAmount || data.amount || 0,
  refundReason: data.refundReason || data.reason || '平台后台退款'
})
export const updateOrderNote = (id, data) => request.put(`/order/${id}/note`, data)
export const cancelOrder = (id) => request.put(`/order/${id}/cancel`)
