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

import { resolveProductImage } from '../utils/image.js'

export function imageOf(item) {
  if (!item || typeof item !== 'object') return resolveProductImage(item, '')
  const rawSrc = item.goodsPic || item.goods_pic || item.liveCover || item.live_cover || ''
  return resolveProductImage(rawSrc, '')
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
