export function shopLogo(logo) {
  const raw = typeof logo === 'string' ? logo : (logo?.shopLogo || logo?.shop_logo || '')
  const value = String(raw || '').trim().replaceAll('\\', '/')
  if (!value) return 'https://api.dicebear.com/9.x/shapes/svg?seed=allmart-shop'
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) return value
  if (value.startsWith('/shop/')) return `/uploads${value}`
  if (value.startsWith('shop/')) return `/uploads/${value}`
  if (value.startsWith('/')) return value
  if (value.startsWith('uploads/')) return `/${value}`
  return `/${value}`
}

export function scoreOf(value, fallback = '5.0') {
  const numberValue = Number(value || fallback)
  return Number.isFinite(numberValue) ? numberValue.toFixed(1) : fallback
}
