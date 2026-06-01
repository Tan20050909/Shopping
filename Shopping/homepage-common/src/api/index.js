import axios from 'axios'

// 平台端 client（Banner 等）
const adminRequest = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_API_BASE || 'http://localhost:8080/api',
  timeout: 10000
})

// 商家端 client（商品、收藏、商家信息、关注）
const merchantRequest = axios.create({
  baseURL: import.meta.env.VITE_MERCHANT_API_BASE || 'http://localhost:8081/api',
  timeout: 10000
})

export const bannerApi = {
  active: () => adminRequest.get('/banner/active')
}

export const goodsApi = {
  public: (userId) => merchantRequest.get('/goods/public', { params: { userId } }),
  toggleFavorite: (goodsId, userId) => merchantRequest.post(`/goods/${goodsId}/favorite`, null, { params: { userId } })
}

export const merchantApi = {
  publicInfo: (merchantId, userId) => merchantRequest.get(`/merchant/${merchantId}/public`, { params: { userId } }),
  toggleFollow: (merchantId, userId) => merchantRequest.post(`/merchant/${merchantId}/follow`, null, { params: { userId } })
}
