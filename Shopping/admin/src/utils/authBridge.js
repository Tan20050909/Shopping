const ALLOWED_ORIGINS = ['http://localhost:3002', 'http://127.0.0.1:3002']

/**
 * 从 URL 中提取 adminToken 并完成登录态写入
 * 顺序：读取 adminToken → 校验 origin → 写 localStorage.admin_token → 调用 /api/admin/info → 写 localStorage.admin_info → 清理 URL
 * @returns {Promise<boolean>} 是否成功消费了 adminToken
 */
export async function consumeAdminToken() {
  const url = new URL(window.location.href)
  const adminToken = url.searchParams.get('adminToken')

  if (!adminToken) {
    return false
  }

  // 校验当前页面 origin 白名单
  if (!ALLOWED_ORIGINS.includes(url.origin)) {
    console.warn('非法的 adminToken 来源，已忽略')
    cleanUrlParams()
    return false
  }

  // 第一步：先写 token
  localStorage.setItem('admin_token', adminToken)

  // 第二步：调用 /api/admin/info 获取管理员信息（必须等待完成）
  const infoSuccess = await fetchAdminInfo(adminToken)

  if (!infoSuccess) {
    // info 获取失败，清理 token
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    cleanUrlParams()
    return false
  }

  // 第三步：清理 URL 中的 adminToken 参数
  cleanUrlParams()

  return true
}

/**
 * 用原生 fetch 获取管理员信息并写入 localStorage（解除与 request.js 的循环依赖）
 * @returns {Promise<boolean>} 是否成功
 */
async function fetchAdminInfo(token) {
  try {
    const resp = await fetch('/api/admin/info', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    })

    if (!resp.ok) {
      console.error('获取管理员信息失败，HTTP 状态:', resp.status)
      return false
    }

    const res = await resp.json()

    if (res?.code === 200 && res?.data) {
      localStorage.setItem('admin_info', JSON.stringify(res.data))
      return true
    }

    console.error('获取管理员信息失败:', res?.message)
    return false
  } catch (e) {
    console.error('获取管理员信息异常:', e)
    return false
  }
}

/**
 * 清理 URL 中的 adminToken 参数
 */
function cleanUrlParams() {
  const url = new URL(window.location.href)
  url.searchParams.delete('adminToken')
  const cleanUrl = url.pathname + (url.search ? url.search : '') + url.hash
  window.history.replaceState({}, '', cleanUrl)
}

/**
 * 获取统一登录页地址
 * @param {string} currentPath 当前页面路径（可选，默认取当前 URL）
 * @returns {string} 统一登录页完整 URL
 */
export function getUnifiedLoginUrl(currentPath) {
  // 避免 back 指向 /login，统一用 /dashboard 或 /
  let backUrl = currentPath || window.location.href

  try {
    const u = new URL(backUrl, window.location.origin)
    // 如果 back 是 /login 页面，改为 /dashboard
    if (u.pathname === '/login') {
      backUrl = u.origin + '/dashboard'
    }
  } catch {
    backUrl = window.location.origin + '/dashboard'
  }

  // 确保 tab=admin 参数始终存在
  return `http://localhost:3000/login?tab=admin&back=${encodeURIComponent(backUrl)}`
}

/**
 * 跳转到统一登录页
 */
export function redirectToLogin() {
  window.location.href = getUnifiedLoginUrl()
}
