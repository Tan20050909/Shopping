import request from '../utils/request'

export const getGoodsList = (params) => request.get('/goods/list', { params })
export const auditGoods = (goodsIdOrData, auditStatus, auditRemark = '') => {
  const data = typeof goodsIdOrData === 'object'
    ? goodsIdOrData
    : { goodsId: goodsIdOrData, auditStatus, auditRemark }
  return request.post('/goods/audit', data)
}
export const updateGoodsStatus = (id, status, reason = '') => request.put(`/goods/${id}/status`, { status, reason })
export const getGoodsDetail = (id) => request.get(`/goods/${id}`)
