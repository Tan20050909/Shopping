// 商家端本地头像工具，替代原来对 @user/avatar 的跨项目引用
export const DEFAULT_AVATARS = Array.from(
  { length: 20 },
  (_, index) => `/brand-assets/avatars/default-avatar-${String(index + 1).padStart(2, '0')}.png`
)

export const DEFAULT_USER_AVATAR = DEFAULT_AVATARS[0]
export const DEFAULT_ANONYMOUS_AVATAR = '/brand-assets/avatars/default-avatar.png'

export function defaultAvatarByIndex(value) {
  const n = Number(value)
  const idx = Number.isFinite(n) ? Math.abs(Math.floor(n)) % DEFAULT_AVATARS.length : 0
  return DEFAULT_AVATARS[idx]
}

export function resolveAvatar(src, fallback = DEFAULT_USER_AVATAR, uploadBase = 'http://localhost:8081') {
  const raw = String(src || '').trim().replaceAll('\\', '/')
  if (!raw || raw === '[object Object]') return fallback
  if (raw.startsWith('https://api.dicebear.com/')) return fallback
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  if (raw.startsWith('/brand-assets/')) return raw
  if (raw.startsWith('brand-assets/')) return `/${raw}`
  if (raw.startsWith('/uploads/')) return `${uploadBase}${raw}`
  if (raw.startsWith('uploads/')) return `${uploadBase}/${raw}`
  if (raw.startsWith('/')) return raw
  return `/${raw}`
}
