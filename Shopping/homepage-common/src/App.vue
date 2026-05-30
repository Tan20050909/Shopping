<template>
  <div class="homepage">
    <header class="site-header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-logo">
            <span class="brand-all">All</span><span class="brand-mart">Mart</span>
          </div>
        </div>

        <nav class="nav-links">
          <a href="#" class="active" @click.prevent="goSection('top')">首页</a>
          <a href="#" @click.prevent="goSection('product')">商品分类</a>
          <a href="#" @click.prevent="goSection('product')">精选推荐</a>
          <a href="#" @click.prevent="goSection('rank')">热门榜单</a>
          <a href="#" @click.prevent="toast('优惠功能开发中')">优惠</a>
          <a href="#" @click.prevent="toast('我的功能开发中')">我的</a>
        </nav>

        <div class="header-right">
          <div class="search">
            <el-input v-model="searchText" placeholder="搜索好物" class="search-input" />
          </div>
          <div class="user">
            <div class="user-avatar">{{ userAvatarText }}</div>
            <div class="user-name">{{ userName }}</div>
          </div>
          <div class="merchant-entry" @click="goMerchantPortal">商家入口</div>
        </div>
      </div>
    </header>

    <div class="hero">
      <el-carousel height="420px" indicator-position="outside" v-if="heroBanners.length > 0">
        <el-carousel-item v-for="item in heroBanners" :key="item.id">
          <div class="hero-slide" :style="getHeroStyle(item)">
            <div class="hero-mask"></div>
            <div class="hero-content">
              <div class="hero-kicker">SHOPPING SELECT</div>
              <div class="hero-title">{{ item.title || '春季穿搭上新' }}</div>
              <div class="hero-sub">{{ item.description || '精选好物，开心生活' }}</div>
              <div class="hero-actions">
                <div class="hero-btn primary" @click="goSection('product')">立即购买</div>
                <div class="hero-btn" @click="goSection('product')">查看推荐</div>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="headline">
      <div class="headline-text">
        <span class="headline-strong">Shopping</span>
        <span class="headline-sub">精选好物</span>
        <span class="headline-red">开心生活</span>
      </div>
    </div>

    <div class="platform-stats">
      <div class="stats-inner">
        <div class="stat" v-for="s in platformStats" :key="s.label" @click="onStatClick(s)">
          <div class="stat-icon">{{ s.short }}</div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <div class="feature-row">
      <div class="feature" v-for="s in featureList" :key="s.label" @click="onFeatureClick(s)">
        <div class="feature-icon">{{ s.short }}</div>
        <div class="feature-label">{{ s.label }}</div>
      </div>
    </div>

    <div ref="rankRef" class="product-section">
      <div class="section-head">
        <div class="section-title">热销榜 TOP3</div>
        <div class="section-desc">根据购买量推荐</div>
      </div>

      <div class="rank-grid">
        <div class="rank-card" v-for="(item, idx) in topSelling" :key="item.id" @click="toast(`已选择：${item.name}`)">
          <div class="rank-no">{{ idx + 1 }}</div>
          <img :src="item.image || defaultProductImage" :alt="item.name" class="rank-img" />
          <div class="rank-meta">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-sub">
              <span>已售 {{ item.buyCount ?? 0 }}</span>
              <span class="rank-split">|</span>
              <span>粉丝 {{ item.followerCount ?? 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div ref="productRef" class="product-section">
      <div class="section-head">
        <div class="section-title">猜你喜欢</div>
        <div class="section-desc">精选好物推荐</div>
      </div>

      <div class="product-grid">
        <div class="product-card" v-for="item in displayProducts" :key="item.id" @click="toast(`已选择：${item.name}`)">
          <div class="product-image-wrapper">
            <img :src="item.image || defaultProductImage" :alt="item.name" class="product-image" />
          </div>
          <div class="product-info">
            <div class="product-name">{{ item.name }}</div>
            <div class="product-price">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ item.price || '99.00' }}</span>
            </div>
            <div class="product-sold">已售 {{ item.buyCount ?? 0 }}</div>
            <div class="product-social" @click.stop>
              <div class="shop-line">
                <div class="shop-name">{{ item.merchantName || `店铺 ${item.merchantId}` }}</div>
                <div class="shop-count">粉丝 {{ item.followerCount ?? 0 }}</div>
                <div class="shop-action" :class="{ active: item.followed }" @click="toggleFollow(item)">
                  {{ item.followed ? '已关注' : '关注店铺' }}
                </div>
              </div>
              <div class="fav-line">
                <div class="fav-count">收藏 {{ item.favoriteCount ?? 0 }}</div>
                <div class="fav-action" :class="{ active: item.favorited }" @click="toggleFavorite(item)">
                  {{ item.favorited ? '已收藏' : '收藏' }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { bannerApi, goodsApi, merchantApi } from './api'
import { ElMessage } from 'element-plus'

const searchText = ref('')
const banners = ref([])
const goods = ref([])

const readUserFromStorage = () => {
  const keys = ['buyerUser', 'user', 'currentUser', 'profile', 'merchantUser']
  for (const k of keys) {
    try {
      const raw = localStorage.getItem(k)
      if (!raw) continue
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') return parsed
    } catch (e) {
    }
  }
  return null
}

const currentUserId = computed(() => {
  const u = readUserFromStorage()
  const id = Number(u?.userId ?? u?.id ?? u?.uid ?? u?.accountId ?? 0)
  return Number.isFinite(id) && id > 0 ? id : 1001
})

const productRef = ref(null)
const rankRef = ref(null)

const toast = (message) => {
  ElMessage.info(String(message || ''))
}

const goSection = (key) => {
  if (key === 'top') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  if (key === 'rank') {
    const el = rankRef.value
    if (el && typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    } else {
      window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
    }
    return
  }
  if (key === 'product') {
    const el = productRef.value
    if (el && typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    } else {
      window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
    }
  }
}

const onStatClick = (s) => {
  const key = String(s?.key || '')
  if (key === 'goods') {
    goSection('product')
    return
  }
  toast(`${s?.label || '模块'}暂无数据`)
}

const onFeatureClick = (s) => {
  const key = String(s?.key || '')
  if (key === 'featured' || key === 'recommend') {
    goSection('product')
    return
  }
  toast(`${s?.label || '功能'}开发中`)
}

const userName = computed(() => {
  const u = readUserFromStorage()
  const name = u?.nickname || u?.username || u?.name || u?.account
  return String(name || '游客')
})

const userAvatarText = computed(() => {
  const n = String(userName.value || '').trim()
  return n ? n.slice(0, 1) : '游'
})

const merchantPortalUrl = import.meta.env.VITE_MERCHANT_PORTAL_URL || 'http://localhost:3000/'

const goMerchantPortal = () => {
  window.location.href = merchantPortalUrl
}

const defaultProductImage = 'https://via.placeholder.com/320x320?text=Goods'

const resolveProductImage = (src) => {
  const v = String(src || '').trim()
  if (!v) return defaultProductImage
  if (v.startsWith('/uploads/')) return `http://localhost:8080${v}`
  if (v.startsWith('http://')) return v.replace('http://', 'https://')
  if (v.startsWith('https://')) return v
  return v
}

const resolveMediaUrl = (src) => {
  const v = String(src || '').trim()
  if (!v) return ''
  if (v.startsWith('/uploads/')) return `http://localhost:8080${v}`
  if (v.startsWith('http://')) return v.replace('http://', 'https://')
  if (v.startsWith('https://')) return v
  return v
}

const featureList = ref([
  { key: 'featured', short: '精', label: '精选好物' },
  { key: 'recommend', short: '推', label: '精选推荐' },
  { key: 'rank', short: '榜', label: '热门榜单' },
  { key: 'coupon', short: '券', label: '优惠活动' },
  { key: 'member', short: '会', label: '会员中心' }
])

const platformStats = computed(() => {
  const goodsCount = Array.isArray(goods.value) ? goods.value.length : 0
  const bannerCount = Array.isArray(banners.value) ? banners.value.length : 0
  return [
    { key: 'goods', short: '品', value: String(goodsCount), label: '精选商品' },
    { key: 'banner', short: '活', value: String(bannerCount), label: '活动 Banner' },
    { key: 'new', short: '新', value: String(Math.min(goodsCount, 99)), label: '今日上新' },
    { key: 'rank', short: '榜', value: '0', label: '热门榜单' },
    { key: 'coupon', short: '券', value: '0', label: '优惠活动' }
  ]
})

const heroBanners = computed(() => {
  if (Array.isArray(banners.value) && banners.value.length) {
    return banners.value.map((b, idx) => ({
      id: b.id ?? idx,
      title: b.title || '品牌主视觉',
      description: b.description || '全新系列灵感上新',
      image: b.image || ''
    }))
  }
  const fallback = goods.value.slice(0, 3).map((g, idx) => ({
    id: g.id ?? idx,
    title: g.name || '新品上新',
    description: g.description || '精选好物推荐',
    image: g.goodsPic || ''
  }))
  return fallback
})

const getHeroStyle = (item) => {
  const img = String(item?.image || '').trim()
  if (img) {
    const url = resolveProductImage(img)
    return { backgroundImage: `url(${url})` }
  }
  return { background: 'linear-gradient(135deg, #ff8aa0 0%, #ff4d6d 100%)' }
}

const displayProducts = computed(() => {
  return Array.isArray(goods.value) ? goods.value : []
})

const topSelling = computed(() => {
  return (displayProducts.value || [])
    .slice()
    .sort((a, b) => Number(b?.buyCount ?? 0) - Number(a?.buyCount ?? 0))
    .slice(0, 3)
})

const loadBanners = async () => {
  try {
    const res = await bannerApi.active()
    if (res.data && res.data.length > 0) {
      banners.value = res.data
    }
  } catch (e) {
    console.log('加载轮播图失败，使用默认数据')
  }
}

const loadGoods = async () => {
  try {
    const res = await goodsApi.public(currentUserId.value)
    if (res.data && res.data.length > 0) {
      goods.value = res.data.map(g => ({
        ...g,
        image: resolveProductImage(g.goodsPic),
        price: '99.00',
        buyCount: Number(g.buyCount ?? 0) || 0,
        followerCount: Number(g.followerCount ?? 0) || 0,
        favoriteCount: Number(g.favoriteCount ?? 0) || 0,
        followed: Boolean(g.followed),
        favorited: Boolean(g.favorited)
      }))
    }
  } catch (e) {
    console.log('加载商品失败，使用默认数据')
  }
}

const toggleFavorite = async (item) => {
  try {
    const res = await goodsApi.toggleFavorite(item.id, currentUserId.value)
    const data = res?.data || {}
    item.favorited = Boolean(data.favorited)
    item.favoriteCount = Number(data.favoriteCount ?? item.favoriteCount ?? 0) || 0
  } catch (e) {
    toast('操作失败')
  }
}

const toggleFollow = async (item) => {
  try {
    const res = await merchantApi.toggleFollow(item.merchantId, currentUserId.value)
    const data = res?.data || {}
    const followed = Boolean(data.followed)
    const count = Number(data.followerCount ?? 0) || 0
    for (const g of goods.value) {
      if (Number(g.merchantId) === Number(item.merchantId)) {
        g.followed = followed
        g.followerCount = count
      }
    }
    item.followed = followed
    item.followerCount = count
  } catch (e) {
    toast('操作失败')
  }
}

onMounted(() => {
  loadBanners()
  loadGoods()
})
</script>

<style>
:root {
  --brand-red: #E60012;
  --brand-red-dark: #C4000F;
  --brand-red-light: #FFE8EA;

  --bg-page: #FFFFFF;
  --bg-section: #F7F7F7;
  --bg-soft: #FAFAFA;
  --bg-card: #FFFFFF;

  --text-main: #111111;
  --text-secondary: #555555;
  --text-muted: #999999;

  --border-light: #EEEEEE;
  --border-soft: #F3F3F3;

  --shadow-soft: 0 8px 24px rgba(0, 0, 0, 0.05);
  --shadow-card: 0 12px 32px rgba(0, 0, 0, 0.08);

  --radius-lg: 16px;
  --radius-md: 10px;
  --radius-sm: 6px;
  --radius-pill: 999px;

  --el-color-primary: var(--brand-red);
  --el-color-primary-dark-2: var(--brand-red-dark);
  --el-color-primary-light-3: #FFB9BF;
  --el-color-primary-light-5: #FFD6D9;
  --el-color-primary-light-7: #FFE9EA;
  --el-color-primary-light-8: #FFF0F0;
  --el-color-primary-light-9: #FFF7F7;
}

html,
body {
  background: var(--bg-page);
  color: var(--text-main);
}

.el-button--primary {
  background: var(--brand-red);
  border-color: var(--brand-red);
  border-radius: var(--radius-pill);
}

.el-button--primary:hover {
  background: var(--brand-red-dark);
  border-color: var(--brand-red-dark);
}

.el-input .el-input__wrapper {
  border-radius: var(--radius-pill);
}

.homepage {
  min-height: 100vh;
  background: var(--bg-page);
  color: var(--text-main);
}

.site-header {
  background: var(--bg-page);
  border-bottom: 1px solid var(--border-light);
  position: sticky;
  top: 0;
  z-index: 20;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand-logo {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0.2px;
  line-height: 1;
}

.brand-all {
  color: var(--text-main);
}

.brand-mart {
  color: var(--brand-red);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}

.nav-links a {
  color: var(--text-main);
  text-decoration: none;
  font-weight: 800;
  font-size: 13px;
  padding: 8px 10px;
  border-radius: 10px;
}

.nav-links a.active {
  color: var(--brand-red);
}

.nav-links a:hover {
  background: var(--brand-red-light);
  color: var(--brand-red);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.search-input {
  width: 220px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 999px;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  color: var(--text-secondary);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--bg-soft);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: var(--text-main);
}

.user-name {
  font-size: 13px;
}

.merchant-entry {
  font-size: 12px;
  font-weight: 900;
  color: var(--brand-red);
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--brand-red-light);
  border: 1px solid #ffd6d9;
}

.hero {
  max-width: 1200px;
  margin: 0 auto;
  padding: 14px 20px 0;
}

.hero :deep(.el-carousel__container) {
  border-radius: 18px;
  overflow: hidden;
}

.hero-slide {
  width: 100%;
  height: 100%;
  position: relative;
  background-size: cover;
  background-position: center;
}

.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.22) 55%, rgba(255, 255, 255, 0.08) 100%);
}

.hero-content {
  position: absolute;
  left: 54px;
  top: 50%;
  transform: translateY(-50%);
  color: #111827;
  max-width: 560px;
}

.hero-kicker {
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 1px;
  color: var(--brand-red);
}

.hero-title {
  margin-top: 10px;
  font-size: 44px;
  font-weight: 900;
  line-height: 1.05;
}

.hero-sub {
  margin-top: 14px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  max-width: 520px;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.hero-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid rgba(17, 24, 39, 0.16);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
}

.hero-btn.primary {
  background: var(--brand-red);
  border-color: var(--brand-red);
  color: #fff;
}

.headline {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px 0;
  text-align: center;
}

.headline-text {
  font-size: 18px;
  font-weight: 900;
}

.headline-strong {
  margin-right: 6px;
}

.headline-sub {
  margin-right: 6px;
  color: var(--text-main);
}

.headline-red {
  color: var(--brand-red);
}

.platform-stats {
  max-width: 1200px;
  margin: 0 auto;
  padding: 26px 24px 0;
}

.stats-inner {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.stat {
  padding: 18px 14px;
  text-align: center;
  background: var(--bg-page);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  margin: 0 auto;
  background: var(--brand-red-light);
  color: var(--brand-red);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.stat-value {
  margin-top: 12px;
  font-size: 22px;
  font-weight: 900;
  color: var(--brand-red);
}

.stat-label {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
}

.feature-row {
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 24px 0;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  text-align: center;
}

.feature {
  padding: 12px 8px;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  margin: 0 auto;
  background: var(--bg-page);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-red);
  font-weight: 900;
}

.feature-label {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 800;
  color: var(--text-secondary);
}

.product-section {
  background: var(--bg-page);
  border-radius: 14px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 18px 18px 20px;
  margin-top: 18px;
  border: 1px solid var(--border-soft);
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 900;
}

.section-desc {
  font-size: 13px;
  color: var(--text-muted);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 28px;
}

.product-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid var(--border-soft);
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card);
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: var(--bg-soft);
}

.product-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 14px 14px 16px;
}

.product-name {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.5;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  margin-top: 12px;
  color: var(--brand-red);
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.product-sold {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 800;
  color: var(--text-muted);
}

.rank-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.rank-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-soft);
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.rank-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.rank-no {
  width: 26px;
  height: 26px;
  border-radius: 10px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  flex: none;
}

.rank-img {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid var(--border-soft);
  background: var(--bg-soft);
  flex: none;
}

.rank-meta {
  min-width: 0;
  flex: 1;
}

.rank-name {
  font-weight: 900;
  color: var(--text-main);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rank-split {
  color: var(--border-light);
}

.price-symbol {
  font-size: 12px;
  font-weight: 900;
}

.price-value {
  font-size: 20px;
  font-weight: 900;
}

.product-social {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-soft);
}

.shop-line,
.fav-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.shop-name {
  color: var(--text-secondary);
  font-weight: 800;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-action,
.fav-action {
  margin-left: auto;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #ffd6d9;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 900;
  cursor: pointer;
  user-select: none;
}

.shop-action.active,
.fav-action.active {
  background: var(--brand-red);
  border-color: var(--brand-red);
  color: #fff;
}

@media (max-width: 960px) {
  .nav-links {
    display: none;
  }

  .stats-inner {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .search-input {
    width: 160px;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .rank-grid {
    grid-template-columns: 1fr;
  }
}
</style>
