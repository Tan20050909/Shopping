// 默认商家仅用于显式 demo 场景，普通页面不得隐式使用
const DEFAULT_MERCHANT = {
  merchantId: 2001,
  merchantName: '数码优选旗舰店',
  username: '数码优选旗舰店',
  shopName: '数码优选旗舰店',
  avatarUrl: '/brand-assets/avatars/default-avatar-01.png',
  source: 'demo'
}

/**
 * 读取已登录的商家用户信息。
 * options.forceDefault 仅在明确需要 demo 数据的场景传入 true。
 * 普通业务页面不传此参数，未登录时返回 null。
 */
export function ensureMerchantUser(options = {}) {
  const forceDefault = Boolean(options.forceDefault)
  try {
    const raw = localStorage.getItem('merchantUser')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') {
        // 拒绝非登录来源的默认账号
        if (parsed.source === 'demo' && !forceDefault) return null
        if (forceDefault && parsed.source !== 'login') {
          const id = Number(parsed.merchantId || parsed.merchant_id || parsed.id) || DEFAULT_MERCHANT.merchantId
          const merged = { ...DEFAULT_MERCHANT, ...parsed, merchantId: id }
          localStorage.setItem('merchantUser', JSON.stringify(merged))
          return merged
        }
        return parsed
      }
    }
  } catch (error) {
    // 解析失败
  }

  if (forceDefault) {
    localStorage.setItem('merchantUser', JSON.stringify(DEFAULT_MERCHANT))
    return DEFAULT_MERCHANT
  }
  return null
}

/**
 * 获取当前登录商家 ID。
 * 未登录或无有效商家用户时返回 null，不回退到默认值。
 */
export function getMerchantId() {
  const user = ensureMerchantUser()
  if (!user) return null
  const id = Number(user.merchantId || user.merchant_id || user.id)
  return Number.isFinite(id) && id > 0 ? id : null
}

export function getMerchantName() {
  const user = ensureMerchantUser()
  if (!user) return null
  return user.merchantName || user.merchant_name || user.shopName || user.username || null
}
