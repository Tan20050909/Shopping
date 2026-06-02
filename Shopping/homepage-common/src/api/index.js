import axios from 'axios'

// 统一公开接口客户端，默认指向商家后端 8081
const publicRequest = axios.create({
  baseURL: import.meta.env.VITE_PUBLIC_API_BASE || 'http://localhost:8081/api',
  timeout: 10000
})

export const bannerApi = {
  active: () => publicRequest.get('/banner/active')
}

export const goodsApi = {
  public: (userId) => publicRequest.get('/goods/public', { params: { userId } }),
  toggleFavorite: (goodsId, userId) => publicRequest.post(`/goods/${goodsId}/favorite`, null, { params: { userId } })
}

export const merchantApi = {
  publicInfo: (merchantId, userId) => publicRequest.get(`/merchant/${merchantId}/public`, { params: { userId } }),
  toggleFollow: (merchantId, userId) => publicRequest.post(`/merchant/${merchantId}/follow`, null, { params: { userId } })
}
