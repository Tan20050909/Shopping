import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
})

export const bannerApi = {
  active: () => request.get('/banner/active')
}

export const goodsApi = {
  public: (userId) => request.get('/goods/public', { params: { userId } }),
  toggleFavorite: (goodsId, userId) => request.post(`/goods/${goodsId}/favorite`, null, { params: { userId } })
}

export const merchantApi = {
  publicInfo: (merchantId, userId) => request.get(`/merchant/${merchantId}/public`, { params: { userId } }),
  toggleFollow: (merchantId, userId) => request.post(`/merchant/${merchantId}/follow`, null, { params: { userId } })
}
