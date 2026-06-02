const API_BASE = import.meta.env.VITE_API_BASE || ''
const USER_TOKEN_KEY = 'shopping_user_token'
const LOCAL_API_FALLBACK = 'http://localhost:8082'

// #region debug-point A:report
// 调试上报默认禁用，需要显式配置 VITE_DEBUG_REPORT_ENABLED=true 才会启用
const DEBUG_REPORT_ENABLED = import.meta.env.VITE_DEBUG_REPORT_ENABLED === 'true'
const DEBUG_SERVER_URL = 'http://127.0.0.1:7777/event'
const DEBUG_SESSION_ID = 'order-items-missing'
function dbg(hypothesisId, msg, data = {}) {
  if (!DEBUG_REPORT_ENABLED) return
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

function brandAsset(relativePath) {
  return `/brand-assets/${relativePath}`
}

export function fallbackImageOf(item) {
  const id = Number(item?.goodsId || item?.goods_id || item?.liveId || item?.live_id || 1)
  const name = String(item?.goodsName || item?.goods_name || item?.liveTitle || item?.live_title || item?.liveTheme || item?.live_theme || item?.title || '')
  const bannerA = brandAsset('digital-banner.png')
  const bannerB = brandAsset('spring-banner.png')
  const demoGoodsCovers = {
    3001: brandAsset('cable.png'),
    3002: brandAsset('mouse.png'),
    3003: brandAsset('laptop.png'),
    3004: brandAsset('dress.png'),
    3005: brandAsset('canvas-bag.png'),
    3006: brandAsset('earphone.webp')
  }
  if (demoGoodsCovers[id]) return demoGoodsCovers[id]
  if (name.includes('耳机')) return demoGoodsCovers[3006]
  if (name.includes('连衣裙') || name.includes('春季') || name.includes('穿搭') || name.includes('服饰')) return demoGoodsCovers[3004]
  if (name.includes('快充线') || name.includes('充电线') || name.includes('数据线')) return demoGoodsCovers[3001]
  if (name.includes('帆布包')) return demoGoodsCovers[3005]
  if (name.includes('鼠标')) return demoGoodsCovers[3002]
  if (name.includes('轻薄本') || name.includes('电脑')) return demoGoodsCovers[3003]
  const images = [
    bannerA,
    bannerB,
    bannerA,
    bannerB
  ]
  return images[Math.abs(id) % images.length]
}

export function imageOf(item) {
  const rawSrc = item?.goodsPic || item?.goods_pic || item?.picUrl || item?.pic_url || item?.mainImage || item?.main_image || item?.imageUrl || item?.image_url || item?.image || item?.goodsImage || item?.goods_image || item?.productPic || item?.product_pic || item?.cover || item?.thumbnail || item?.liveCover || item?.live_cover
  const normalized = String(rawSrc || '').trim().replaceAll('\\', '/')
  if (normalized && normalized !== '[object Object]') {
    // http(s) 开头：原样使用
    if (normalized.startsWith('http://') || normalized.startsWith('https://') || normalized.startsWith('data:')) return normalized
    // SQL 历史样例里的 /goods/... 未由当前商家端静态目录托管，优先用同商品本地兜底图。
    if (normalized.startsWith('/goods/') || normalized.startsWith('goods/')) return fallbackImageOf(item)
    // 商品图片按商家端静态资源地址解析（默认 http://localhost:8081）
    const merchantBase = 'http://localhost:8081'
    // /uploads/...：拼到商家端
    if (normalized.startsWith('/uploads/')) return `${merchantBase}${normalized}`
    if (normalized.startsWith('uploads/')) return `${merchantBase}/${normalized}`
    // 其他相对路径
    if (normalized.startsWith('/images/') || normalized.startsWith('/videos/')) return `${merchantBase}/uploads${normalized}`
    if (normalized.startsWith('images/') || normalized.startsWith('videos/')) return `${merchantBase}/uploads/${normalized}`
    const idx = normalized.indexOf('/uploads/')
    if (idx > 0) return `${merchantBase}${normalized.slice(idx)}`
    // 裸文件名（如 abc.jpg）或普通路径，统一拼到商家端
    if (normalized.startsWith('/')) return `${merchantBase}${normalized}`
    return `${merchantBase}/${normalized}`
  }
  return fallbackImageOf(item)
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
