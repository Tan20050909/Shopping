const DEFAULT_MERCHANT = {
  merchantId: 2001,
  merchantName: '数码优选旗舰店',
  username: '数码优选旗舰店',
  shopName: '数码优选旗舰店',
  avatarUrl: 'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-default',
  source: 'demo'
}

export function ensureMerchantUser(options = {}) {
  const forceDefault = Boolean(options.forceDefault)
  try {
    const raw = localStorage.getItem('merchantUser')
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') {
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
  }

  if (forceDefault) {
    localStorage.setItem('merchantUser', JSON.stringify(DEFAULT_MERCHANT))
    return DEFAULT_MERCHANT
  }
  return null
}

export function getMerchantId() {
  const user = ensureMerchantUser()
  if (!user) return DEFAULT_MERCHANT.merchantId
  return Number(user.merchantId || user.merchant_id || user.id) || DEFAULT_MERCHANT.merchantId
}

export function getMerchantName() {
  const user = ensureMerchantUser()
  if (!user) return DEFAULT_MERCHANT.merchantName
  return user.merchantName || user.merchant_name || user.shopName || user.username || DEFAULT_MERCHANT.merchantName
}
