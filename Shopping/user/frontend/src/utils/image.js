/**
 * 商品图片统一解析模块
 * 两端（用户端 + 商家端）共用此文件，保证同一商品显示同一张图片。
 *
 * 解析优先级：
 *   1. 硬编码映射表（demo 商品 3001-3006）→ brand-assets/products/ 下的本地文件
 *   2. 完整 URL（http/https/data）→ 直接返回
 *   3. /uploads/ 路径 → 直接返回（由后端提供）
 *   4. /goods/ 路径 → 尝试从映射表匹配，否则拼接 /uploads 前缀
 *   5. 关键词兜底（中文/英文商品名）
 *   6. 默认占位图
 */

const productImages = {
  3001: '/brand-assets/products/cable.png',
  3002: '/brand-assets/products/mouse.png',
  3003: '/brand-assets/products/laptop.png',
  3004: '/brand-assets/products/dress.png',
  3005: '/brand-assets/products/canvas-bag.png',
  3006: '/brand-assets/products/earphone.png'
}

/**
 * 从图片路径中提取商品 ID，并尝试返回对应的本地映射图。
 */
export function productImageFallback(source, fallback = '') {
  const raw = String(source || '').trim().replaceAll('\\', '/')
  const goodsId = Number(raw.match(/(?:^|\/)goods\/(\d+)\//)?.[1])
  if (Number.isFinite(goodsId) && productImages[goodsId]) return productImages[goodsId]
  const lower = raw.toLowerCase()
  if (raw.includes('耳机') || lower.includes('earphone') || lower.includes('headset')) return productImages[3006]
  return fallback
}

/**
 * 根据任意格式的图片路径，返回可直接用于 <img src> 的 URL。
 * @param {string} source  - 数据库返回的图片路径或完整 URL
 * @param {string} fallback - 无法解析时的兜底值
 * @returns {string}
 */
export function resolveProductImage(source, fallback = '') {
  const mapped = productImageFallback(source, '')
  if (mapped) return mapped

  const raw = String(source || '').trim().replaceAll('\\', '/')
  if (!raw) return fallback

  // 完整 URL → 直接返回
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw

  // /uploads/ 路径 → 直接返回
  if (raw.startsWith('/uploads/')) return raw
  if (raw.startsWith('uploads/')) return `/${raw}`

  // /goods/ 路径 → 再尝试映射一次（兜底），不行就拼 /uploads
  if (raw.startsWith('/goods/') || raw.startsWith('goods/')) return productImageFallback(raw, fallback || `/uploads${raw.startsWith('/') ? raw : '/' + raw}`)

  // /images/ /videos/ → 补 /uploads 前缀
  if (raw.startsWith('/images/') || raw.startsWith('/videos/')) return `/uploads${raw}`
  if (raw.startsWith('images/') || raw.startsWith('videos/')) return `/uploads/${raw}`

  // 包含 /uploads/ 的复合路径 → 截取 /uploads/ 开头部分
  const uploadsIndex = raw.indexOf('/uploads/')
  if (uploadsIndex > 0) return raw.slice(uploadsIndex)

  return raw.startsWith('/') ? raw : fallback
}

/**
 * 通用商品图片解析入口（适配对象和字符串两种调用方式）。
 * @param {object|string} item  - 商品对象（含 goodsPic/goods_pic）或图片路径字符串
 * @param {string} fallback      - 兜底值
 * @returns {string}
 */
export function imageOfItem(item, fallback = '') {
  if (item && typeof item === 'object') {
    const rawSrc = item.goodsPic || item.goods_pic || item.liveCover || item.live_cover || ''
    return resolveProductImage(rawSrc, fallback)
  }
  return resolveProductImage(item, fallback)
}
