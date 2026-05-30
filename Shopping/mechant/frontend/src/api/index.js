import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const merchantApi = {
  apply: (data) => request.post('/merchant/apply', data),
  list: (merchantId) => request.get('/merchant/list', { params: { merchantId } }),
  info: (merchantId) => request.get('/merchant/info', { params: { merchantId } }),
  audit: (merchantId) => request.get('/merchant/audit', { params: { merchantId } }),
  updateProfile: (data) => request.put('/merchant/profile', data),
  updateLogo: (data) => request.put('/merchant/logo', data),
  updateMaterials: (data) => request.put('/merchant/materials', data)
}

export const merchantSettingApi = {
  get: (merchantId) => request.get('/merchant-setting', { params: { merchantId } }),
  update: (data) => request.put('/merchant-setting', data)
}

export const financeApi = {
  get: (merchantId) => request.get('/finance', { params: { merchantId } }),
  applyWithdraw: (data) => request.post('/finance/withdraw', data),
  listWithdraw: (merchantId) => request.get('/finance/withdraw/list', { params: { merchantId } }),
  listFlow: (merchantId, limit, flowType) => request.get('/finance/flow/list', { params: { merchantId, limit, flowType } })
}

export const dataCenterApi = {
  overview: (merchantId, options = 7) => {
    const params = { merchantId }
    if (options && typeof options === 'object') {
      const days = options.days
      const startDate = options.startDate
      const endDate = options.endDate
      if (days != null) params.days = days
      if (startDate) params.startDate = startDate
      if (endDate) params.endDate = endDate
    } else {
      params.days = options == null ? 7 : options
    }
    return request.get('/data-center/overview', { params })
  }
}

export const goodsApi = {
  create: (data) => request.post('/goods', data),
  list: (merchantId) => request.get('/goods/list', { params: { merchantId } }),
  detail: (id) => request.get(`/goods/${id}`),
  listPics: (id) => request.get(`/goods/${id}/pics`),
  replacePics: (id, pics) => request.put(`/goods/${id}/pics`, pics),
  update: (data) => request.put('/goods', data),
  delete: (id) => request.delete(`/goods/${id}`),
  updateStatus: (id, status) => request.put(`/goods/${id}/status`, null, { params: { status } }),
  batchUpdateStatus: (ids, status) => request.put('/goods/status/batch', ids, { params: { status } }),
  batchDelete: (ids) => request.delete('/goods/batch', { data: ids })
}

export const goodsSkuApi = {
  list: (goodsId) => request.get('/goods-sku/list', { params: { goodsId } }),
  updateStock: (id, stock) => request.put(`/goods-sku/${id}/stock`, null, { params: { stock } }),
  updatePrice: (id, price) => request.put(`/goods-sku/${id}/price`, null, { params: { price } }),
  listPriceLog: (id) => request.get(`/goods-sku/${id}/price-log`),
  replaceByGoodsId: (goodsId, skus) => request.put(`/goods-sku/goods/${goodsId}`, skus),
  getStockWarning: (merchantId) => request.get('/goods-sku/stock-warning', { params: { merchantId } }),
  setStockWarning: (merchantId, warningStock) => request.put('/goods-sku/stock-warning', null, { params: { merchantId, warningStock } }),
  listStockLog: (merchantId, goodsId, skuId, limit) => request.get('/goods-sku/stock-log/list', { params: { merchantId, goodsId, skuId, limit } })
}

export const orderApi = {
  list: (merchantId, status, userId) => request.get('/order/list', { params: { merchantId, status, userId } }),
  updateStatus: (id, status) => request.put(`/order/${id}/status`, null, { params: { status } }),
  updateFreight: (id, freight) => request.put(`/order/${id}/freight`, null, { params: { freight } }),
  updatePayAmount: (id, payAmount) => request.put(`/order/${id}/pay-amount`, null, { params: { payAmount } }),
  detail: (id) => request.get(`/order/${id}`),
  ship: (id, data) => request.post(`/order/${id}/ship`, data)
}

export const logisticsApi = {
  create: (data) => request.post('/logistics', data),
  updateStatus: (id, status) => request.put(`/logistics/${id}/status`, null, { params: { status } })
}

export const afterSaleApi = {
  list: (merchantId, status) => request.get('/after-sale/list', { params: { merchantId, status } }),
  detail: (id) => request.get(`/after-sale/${id}/detail`),
  handle: (id, status, remark, evidence) => request.put(`/after-sale/${id}/handle`, null, { params: { status, remark, evidence } }),
  complete: (id) => request.put(`/after-sale/${id}/complete`),
  buyerLogistics: (id, data) => request.post(`/after-sale/${id}/buyer-logistics`, data),
  merchantShip: (id, data) => request.post(`/after-sale/${id}/merchant-ship`, data)
}

export const goodsCommentApi = {
  list: (merchantId, includeInvalid = 0) => request.get('/goods-comment/list', { params: { merchantId, includeInvalid } }),
  reply: (id, reply) => request.put(`/goods-comment/${id}/reply`, null, { params: { reply } }),
  deleteReply: (id, index) => request.delete(`/goods-comment/${id}/reply`, { params: { index } }),
  setTop: (id, isTop) => request.put(`/goods-comment/${id}/top`, null, { params: { isTop } }),
  appeal: (id, merchantId, data) => request.post(`/goods-comment/${id}/appeal`, data, { params: { merchantId } })
}

export const couponApi = {
  create: (data) => request.post('/coupon', data),
  list: (merchantId) => request.get('/coupon/list', { params: { merchantId } }),
  listPlatform: () => request.get('/coupon/platform/list'),
  update: (id, data) => request.put(`/coupon/${id}`, data),
  updateStatus: (id, status) => request.put(`/coupon/${id}/status`, null, { params: { status } }),
  grant: (data) =>
    request.post('/coupon/grant', data).catch((e) => {
      if (e?.response?.status === 405) {
        return request.put('/coupon/grant', data)
      }
      throw e
    })
}

export const activityApi = {
  listPlatform: () => request.get('/activity/platform/list'),
  apply: (data) => request.post('/activity/apply', data),
  list: (merchantId) => request.get('/activity/list', { params: { merchantId } })
}

export const categoryApi = {
  list: () => request.get('/category/list')
}

export const uploadApi = {
  uploadImage: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/image', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  uploadVideo: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/video', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  uploadFile: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/upload/file', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const liveApi = {
  create: (data) => request.post('/live', data),
  list: (merchantId) => request.get('/live/list', { params: { merchantId } }),
  updateStatus: (id, status) => request.put(`/live/${id}/status`, null, { params: { status } }),
  listGoods: (id) => request.get(`/live/${id}/goods`),
  addGoods: (data) => request.post('/live/goods', data),
  removeGoods: (id) => request.delete(`/live/goods/${id}`)
}

export const bannerApi = {
  list: () => request.get('/banner/list'),
  active: () => request.get('/banner/active'),
  add: (data) => request.post('/banner/add', data),
  update: (data) => request.put('/banner/update', data),
  delete: (id) => request.delete(`/banner/delete/${id}`)
}

export const merchantUserApi = {
  login: (data) => request.post('/merchant-user/login', data),
  register: (data) => request.post('/merchant-user/register', data),
  getInfo: (id) => request.get(`/merchant-user/info/${id}`)
}

export const chatApi = {
  getSession: (merchantId, userId) => request.get('/chat/session', { params: { merchantId, userId } }),
  listSessions: (merchantId) => request.get('/chat/session/list', { params: { merchantId } }),
  listMessages: (sessionId, beforeId, limit) => request.get('/chat/message/list', { params: { sessionId, beforeId, limit } }),
  sendMessage: (data) => request.post('/chat/message/send', data),
  revokeMessage: (data) => request.post('/chat/message/revoke', data),
  markRead: (sessionId, receiverType, receiverId) => request.post('/chat/read', null, { params: { sessionId, receiverType, receiverId } })
}
