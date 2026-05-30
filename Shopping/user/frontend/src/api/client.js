const API_BASE = import.meta.env.VITE_API_BASE || ''
const USER_TOKEN_KEY = 'shopping_user_token'
const LOCAL_API_FALLBACK = 'http://localhost:8082'

// #region debug-point A:report
const DEBUG_SERVER_URL = 'http://127.0.0.1:7777/event'
const DEBUG_SESSION_ID = 'order-items-missing'
function dbg(hypothesisId, msg, data = {}) {
  fetch(DEBUG_SERVER_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: DEBUG_SESSION_ID,
      runId: 'pre-fix',
      hypothesisId,
      location: 'client.js',
      msg: `[DEBUG] ${msg}`,
      data,
      ts: Date.now()
    })
  }).catch(() => {})
}
// #endregion

export function imageOf(item) {
  const id = Number(item?.goodsId || item?.goods_id || item?.liveId || item?.live_id || 1)
  const name = String(item?.goodsName || item?.goods_name || item?.title || '')
  const rawSrc = item?.goodsPic || item?.goods_pic || item?.liveCover || item?.live_cover
  const normalized = String(rawSrc || '').trim().replaceAll('\\', '/')
  const asset = (relativePath) => new URL(`../../public/brand-assets/${relativePath}`, import.meta.url).href
  const bannerA = asset('digital-banner.png')
  const bannerB = asset('spring-banner.png')
  const demoGoodsCovers = {
    3001: asset('cable.png'),
    3002: asset('mouse.png'),
    3003: asset('laptop.png'),
    3004: asset('dress.png'),
    3005: asset('canvas-bag.png')
  }
  if (normalized && normalized !== '[object Object]') {
    const looksLikePath = normalized.includes('/') || normalized.includes('.') || normalized.startsWith('uploads/')
    if (!looksLikePath) {
      // fall through
    } else {
      if (normalized.startsWith('http://') || normalized.startsWith('https://') || normalized.startsWith('data:')) return normalized
      if (normalized.startsWith('/uploads/')) return normalized
      if (normalized.startsWith('uploads/')) return `/${normalized}`
      if (normalized.startsWith('/goods/')) return `/uploads${normalized}`
      if (normalized.startsWith('goods/')) return `/uploads/${normalized}`
      if (normalized.startsWith('/images/') || normalized.startsWith('/videos/')) return `/uploads${normalized}`
      if (normalized.startsWith('images/') || normalized.startsWith('videos/')) return `/uploads/${normalized}`
      const idx = normalized.indexOf('/uploads/')
      if (idx > 0) return normalized.slice(idx)
      if (normalized.startsWith('/')) return normalized
      return `/${normalized}`
    }
  }
  const localGoodsImages = {
    3001: demoGoodsCovers[3001],
    3002: demoGoodsCovers[3002],
    3003: demoGoodsCovers[3003],
    3004: demoGoodsCovers[3004],
    3005: demoGoodsCovers[3005]
  }
  if (localGoodsImages[id]) return localGoodsImages[id]
  if (name.includes('连衣裙')) return localGoodsImages[3004]
  if (name.includes('快充线') || name.includes('充电线')) return localGoodsImages[3001]
  if (name.includes('帆布包')) return localGoodsImages[3005]
  if (name.includes('鼠标')) return localGoodsImages[3002]
  if (name.includes('轻薄本') || name.includes('电脑')) return localGoodsImages[3003]
  const images = [
    bannerA,
    bannerB,
    bannerA,
    bannerB
  ]
  return images[id % images.length]
}

export function getUserToken() {
  try {
    return localStorage.getItem(USER_TOKEN_KEY) || ''
  } catch (e) {
    return ''
  }
}

export function setUserToken(token) {
  try {
    if (!token) {
      localStorage.removeItem(USER_TOKEN_KEY)
    } else {
      localStorage.setItem(USER_TOKEN_KEY, String(token))
    }
  } finally {
    window.dispatchEvent(new Event('shopping-user-token'))
  }
}

export async function api(path, options = {}) {
  const requestOptions = buildRequestOptions(path, options)
  const fallbackUrl = shouldUseLocalFallback(path) ? `${LOCAL_API_FALLBACK}${path}` : null
  // #region debug-point A:api-url
  if (path?.startsWith?.('/api/user/orders')) {
    dbg('A', 'api call prepared', { path, apiBase: API_BASE, initialUrl: `${API_BASE}${path}`, fallbackUrl, pagePort: typeof window !== 'undefined' ? window.location.port : '' })
  }
  // #endregion
  try {
    return await requestApi(`${API_BASE}${path}`, requestOptions)
  } catch (error) {
    if (fallbackUrl && shouldRetryWithLocalApi(error)) {
      // #region debug-point A:api-fallback
      if (path?.startsWith?.('/api/user/orders')) {
        dbg('A', 'api fallback retry', { path, fallbackUrl, reason: { networkError: Boolean(error?.networkError), invalidJson: Boolean(error?.invalidJson), status: error?.status ?? null } })
      }
      // #endregion
      return requestApi(fallbackUrl, requestOptions)
    }
    throw error
  }
}

function buildRequestOptions(path, options = {}) {
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
  const token = getUserToken()
  const headers = {
    ...(token ? { Authorization: token } : {}),
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(options.headers || {})
  }
  return {
    ...options,
    headers,
    body: options.body && !isFormData && typeof options.body !== 'string' ? JSON.stringify(options.body) : options.body
  }
}

async function requestApi(url, options) {
  let response
  try {
    response = await fetch(url, options)
  } catch (error) {
    error.networkError = true
    error.message = '无法连接后端服务，请确认 Spring Boot 已启动在 8082 端口'
    throw error
  }
  const payload = await response.json().catch(() => {
    const error = new Error('接口没有返回有效 JSON')
    error.invalidJson = true
    error.status = response.status
    throw error
  })
  // #region debug-point A:api-response
  if (String(url).includes('/api/user/orders')) {
    dbg('A', 'api response', { url, httpStatus: response.status, code: payload?.code, message: payload?.message, ok: Boolean(response.ok) })
  }
  // #endregion
  if (!response.ok || payload.code !== 'SUCCESS') {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data
}

function shouldUseLocalFallback(path) {
  return !API_BASE && path.startsWith('/api') && typeof window !== 'undefined'
    && window.location.port !== '8082'
}

function shouldRetryWithLocalApi(error) {
  return Boolean(error?.networkError || error?.invalidJson || error?.status === 404)
}
