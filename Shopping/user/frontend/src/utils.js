export function shopLogo(logo) {
  const raw = typeof logo === 'string' ? logo : (logo?.shopLogo || logo?.shop_logo || '')
  const value = String(raw || '').trim().replaceAll('\\', '/')
  const merchantBase = 'http://localhost:8081'
  const fallback = '/brand-assets/avatars/default-avatar-01.png'
  if (!value || value.startsWith('/default/')) return fallback
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) return value
  if (value.startsWith('/brand-assets/')) return value
  if (value.startsWith('brand-assets/')) return `/${value}`
  if (value.startsWith('/shop/')) return `${merchantBase}/uploads${value}`
  if (value.startsWith('shop/')) return `${merchantBase}/uploads/${value}`
  if (value.startsWith('/uploads/')) return `${merchantBase}${value}`
  if (value.startsWith('uploads/')) return `${merchantBase}/${value}`
  if (value.startsWith('/')) return `${merchantBase}${value}`
  return `${merchantBase}/${value}`
}

export function scoreOf(value, fallback = '5.0') {
  const numberValue = Number(value || fallback)
  return Number.isFinite(numberValue) ? numberValue.toFixed(1) : fallback
}
