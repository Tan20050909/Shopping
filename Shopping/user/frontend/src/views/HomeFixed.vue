<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Goods, Present, Star, TrendCharts, User, VideoCamera } from '@element-plus/icons-vue'
import { api, getUserToken } from '../api/client'
import ProductCard from '../components/ProductCard.vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const categoryId = ref()
const minPrice = ref()
const maxPrice = ref()
const sort = ref('')
const categories = ref([])
const goods = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const homeLoading = ref(false)

const platformCouponPopupVisible = ref(false)
const platformCouponPopupCoupon = ref(null)
const platformCouponReceiving = ref(false)

const homeData = ref({
  banners: [],
  sections: [],
  categories: [],
  hotKeywords: [],
  freshProducts: []
})

const isHome = computed(() => route.path === '/')
const authRole = computed(() => String(sessionStorage.getItem('shopping_auth_role') || ''))
const userToken = computed(() => getUserToken())
const merchantId = computed(() => {
  if (authRole.value !== 'merchant') return null
  try {
    const raw = localStorage.getItem('merchantUser')
    const parsed = raw ? JSON.parse(raw) : null
    const id = parsed?.merchantId ?? parsed?.id
    const num = Number(id)
    return Number.isFinite(num) && num > 0 ? num : null
  } catch (e) {
    return null
  }
})
const displayedGoods = computed(() => (isHome.value ? goods.value.slice(0, 8) : goods.value))
const hotKeywords = computed(() => homeData.value.hotKeywords || [])
const visibleHomeSections = computed(() => (homeData.value.sections || []).filter((section) => {
  const name = String(section.sectionName || '')
  return !name.includes('热门推荐') && !name.includes('数码专区')
}))
const featuredGoods = computed(() => {
  const sectionGoods = visibleHomeSections.value.flatMap(section => section.goods || [])
  return sectionGoods.length ? sectionGoods.slice(0, 4) : goods.value.slice(0, 4)
})
const topicProduct = computed(() => featuredGoods.value[0] || goods.value[0])

const groupedCategories = computed(() => {
  const list = categories.value || []
  const topLevel = list.filter(item => Number(item.parentCateId || 0) === 0)
  return topLevel.map(parent => ({
    ...parent,
    children: list.filter(item => Number(item.parentCateId || 0) === Number(parent.cateId))
  }))
})
const topLevelCategories = computed(() => groupedCategories.value.map(({ children, ...category }) => category))

const platformStats = computed(() => [
  { label: '精选商品', value: `${total.value || goods.value.length || 0}+`, icon: Goods },
  { label: '内容推荐', value: `${homeData.value.sections?.length || 0}+`, icon: Star },
  { label: '热门关键词', value: `${hotKeywords.value.length || 0}+`, icon: TrendCharts },
  { label: '优惠活动', value: '持续上新', icon: Present },
  { label: '活跃用户', value: '持续增长', icon: User }
])

function numberOf(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function moneyText(value) {
  return numberOf(value, 0).toFixed(0)
}

function discountText(coupon) {
  const discountType = numberOf(coupon?.discount_type ?? coupon?.discountType, 1)
  if (discountType === 2) {
    const rate = numberOf(coupon?.discount_rate ?? coupon?.discountRate, 1)
    return `${rate * 10}x`
  }
  return `CNY ${moneyText(coupon?.denomination)}`
}

function localDayKey() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function platformPopupStorageKey() {
  return 'shopping_platform_coupon_popup_day'
}

async function maybeShowPlatformCouponPopup() {
  if (!isHome.value) return
  if (authRole.value === 'merchant') return
  if (!userToken.value) return
  if (platformCouponPopupVisible.value) return

  const today = localDayKey()
  try {
    const last = String(localStorage.getItem(platformPopupStorageKey()) || '')
    if (last === today) return
  } catch (e) {
  }

  try {
    const list = await api('/api/user/coupon-center')
    if (!Array.isArray(list) || !list.length) return

    const candidate = list.find((item) => {
      const grantType = numberOf(item?.grant_type ?? item?.grantType, 1)
      const couponType = numberOf(item?.coupon_type ?? item?.couponType, 0)
      return grantType === 2 && couponType === 1 && Boolean(item?.canReceive)
    })
    if (!candidate) return

    platformCouponPopupCoupon.value = candidate
    platformCouponPopupVisible.value = true
    try {
      localStorage.setItem(platformPopupStorageKey(), today)
    } catch (e) {
    }
  } catch (error) {
  }
}

async function receivePlatformCoupon() {
  if (platformCouponReceiving.value) return
  const couponId = platformCouponPopupCoupon.value?.coupon_id ?? platformCouponPopupCoupon.value?.couponId
  if (!couponId) return
  platformCouponReceiving.value = true
  try {
    await api(`/api/user/coupons/${couponId}/receive`, { method: 'POST' })
    ElMessage.success('领取成功，已放入我的券包')
    platformCouponPopupVisible.value = false
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    platformCouponReceiving.value = false
  }
}

function bannerImage(banner) {
  const title = String(banner?.bannerTitle || '')
  if (title.includes('数码')) return '/brand-assets/digital-banner.png'
  if (title.includes('春季') || title.includes('穿搭')) return '/brand-assets/spring-banner.png'
  const presets = [
    '/brand-assets/digital-banner.png',
    '/brand-assets/spring-banner.png',
    'https://images.unsplash.com/photo-1518779578993-ec3579fee39f?auto=format&fit=crop&w=1600&q=80',
    'https://images.unsplash.com/photo-1520975869018-47c0a5da8b35?auto=format&fit=crop&w=1600&q=80'
  ]
  if (banner?.imageUrl?.startsWith('http')) return banner.imageUrl
  return presets[Number(banner?.bannerId || 0) % presets.length]
}

function topicImage() {
  const id = Number(topicProduct.value?.goodsId || topicProduct.value?.goods_id || 1)
  const presets = [
    'https://images.unsplash.com/photo-1556742502-ec7c0e9f34b1?auto=format&fit=crop&w=1600&q=80',
    'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1600&q=80',
    'https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&w=1600&q=80'
  ]
  return presets[id % presets.length]
}

function handleBannerClick(banner) {
  const title = String(banner?.bannerTitle || '')
  if (title.includes('数码')) {
    router.push('/products?categoryId=1')
    return
  }
  if (title.includes('春季') || title.includes('穿搭') || title.includes('服装')) {
    router.push('/products?categoryId=4')
    return
  }
  const type = Number(banner?.jumpType || 0)
  const value = banner?.jumpValue
  if (!value) {
    router.push('/products')
    return
  }
  if (type === 2) {
    router.push(`/products?categoryId=${value}`)
    return
  }
  if (type === 3) {
    const numericValue = Number(value)
    if (Number.isFinite(numericValue) && numericValue > 0 && numericValue < 1000) {
      router.push(`/products?categoryId=${numericValue}`)
      return
    }
    router.push(`/products/${value}`)
    return
  }
  if (type === 4) {
    router.push(`/live/${value}`)
    return
  }
  router.push('/products')
}

function sectionAction(section) {
  if (section.sectionType === 4) {
    router.push('/live')
    return
  }
  if (section.goods?.[0]?.categoryId) {
    router.push(`/products?categoryId=${section.goods[0].categoryId}`)
    return
  }
  router.push('/products')
}

function syncQuery() {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  categoryId.value = route.query.categoryId ? Number(route.query.categoryId) : undefined
  minPrice.value = route.query.minPrice ? Number(route.query.minPrice) : undefined
  maxPrice.value = route.query.maxPrice ? Number(route.query.maxPrice) : undefined
  sort.value = typeof route.query.sort === 'string' ? route.query.sort : ''
}

async function loadHome() {
  if (!isHome.value) return
  homeLoading.value = true
  try {
    homeData.value = await api('/api/user/home')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    homeLoading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await api('/api/user/categories')
  } catch (error) {
    ElMessage.error(error.message)
    categories.value = []
  }
}

async function loadGoods() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (keyword.value) params.set('keyword', keyword.value)
    if (categoryId.value) params.set('categoryId', categoryId.value)
    if (minPrice.value != null && minPrice.value !== '') params.set('minPrice', minPrice.value)
    if (maxPrice.value != null && maxPrice.value !== '') params.set('maxPrice', maxPrice.value)
    if (sort.value) params.set('sort', sort.value)
    if (merchantId.value) params.set('merchantId', String(merchantId.value))
    params.set('pageNum', pageNum.value)
    params.set('pageSize', pageSize.value)
    const data = await api(`/api/user/products?${params.toString()}`)
    goods.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadPage() {
  syncQuery()
  try {
    await Promise.all([loadCategories(), loadHome(), loadGoods()])
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function goCategory(cateId) {
  categoryId.value = cateId
  handleSearch()
}

async function handleSearch() {
  pageNum.value = 1
  await router.replace({
    path: '/products',
    query: {
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(categoryId.value ? { categoryId: categoryId.value } : {}),
      ...(minPrice.value != null && minPrice.value !== '' ? { minPrice: minPrice.value } : {}),
      ...(maxPrice.value != null && maxPrice.value !== '' ? { maxPrice: maxPrice.value } : {}),
      ...(sort.value ? { sort: sort.value } : {})
    }
  })
}

async function handlePageChange(nextPage) {
  pageNum.value = nextPage
  await loadGoods()
}

watch(
  () => route.fullPath,
  async () => {
    syncQuery()
    if (isHome.value) {
      await Promise.all([loadHome(), loadGoods()])
      await maybeShowPlatformCouponPopup()
      return
    }
    await loadGoods()
  }
)

onMounted(async () => {
  await loadPage()
  await maybeShowPlatformCouponPopup()
})
</script>

<template>
  <main class="home-page">
    <template v-if="isHome">
      <section class="home-hero">
        <el-carousel v-if="homeData.banners?.length" :interval="4200" trigger="click" height="520px">
          <el-carousel-item v-for="banner in homeData.banners" :key="banner.bannerId">
            <article
              class="hero-slide"
              role="button"
              tabindex="0"
              @click="handleBannerClick(banner)"
              @keydown.enter="handleBannerClick(banner)"
              @keydown.space.prevent="handleBannerClick(banner)"
            >
              <img class="hero-slide-image" :src="bannerImage(banner)" :alt="banner.bannerTitle" />
              <span class="hero-wash"></span>
              <div class="container hero-copy">
                <span class="hero-kicker">Allmart Select</span>
                <h1>{{ banner.bannerTitle || '精选好物，点亮生活' }}</h1>
                <p>{{ banner.bannerIntro || '保留真实商品、分类、购物车和订单链路，用更清爽的品牌官网方式呈现。' }}</p>
                <div class="row">
                  <el-button type="primary" @click.stop="handleBannerClick(banner)">立即选购</el-button>
                  <el-button plain @click.stop="router.push('/recommend')">查看推荐</el-button>
                </div>
              </div>
            </article>
          </el-carousel-item>
        </el-carousel>
        <div v-else class="hero-slide hero-fallback">
          <div class="container hero-copy">
            <span class="hero-kicker">Allmart Select</span>
            <h1>精选好物，点亮生活</h1>
            <p>首页 Banner 将优先展示数据库中的活动轮播图。</p>
            <el-button type="primary" @click="router.push('/products')">立即选购</el-button>
          </div>
        </div>
      </section>

      <section class="section stats-section">
        <div class="container">
          <h2 class="brand-slogan">精选好物，从日常开始 <span>Allmart</span></h2>
          <div class="stats-grid">
            <article v-for="stat in platformStats" :key="stat.label" class="stat-card">
              <el-icon><component :is="stat.icon" /></el-icon>
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
            </article>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="container feature-layout">
          <article class="topic-cover" :style="{ backgroundImage: `url(${topicImage()})` }">
            <span class="topic-shade"></span>
            <div class="topic-copy">
              <span>Featured Story</span>
              <h2>用真实数据搭起一间干净好逛的生活商店</h2>
              <p>商品、优惠券、订单和直播入口都来自现有接口，只把页面节奏拉回 Allmart 的轻品牌感。</p>
              <el-button plain @click="router.push('/products')">浏览全部商品</el-button>
            </div>
          </article>
          <aside class="quick-feature">
            <router-link to="/coupons">
              <strong>优惠券</strong>
              <span>先领券，再下单</span>
            </router-link>
            <router-link to="/rankings">
              <strong>热门榜单</strong>
              <span>看看大家最近在买什么</span>
            </router-link>
            <router-link to="/live">
              <strong>直播精选</strong>
              <span>边看边逛，发现实时好物</span>
            </router-link>
          </aside>
        </div>
      </section>

      <section class="section-gray">
        <div class="container">
          <div class="section-head">
            <div>
              <h2 class="section-title">分类精选</h2>
              <p class="muted">按分类快速找到你想要的商品</p>
            </div>
            <router-link class="panel-link" to="/products">全部商品</router-link>
          </div>
          <div class="category-tabs">
            <button
              v-for="group in groupedCategories"
              :key="group.cateId"
              class="category-tab"
              :class="{ active: Number(categoryId) === Number(group.cateId) }"
              @click="goCategory(group.cateId)"
            >
              <span>{{ group.cateName }}</span>
              <small>{{ group.children?.map(item => item.cateName).slice(0, 2).join(' / ') || '更多' }}</small>
            </button>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="container">
          <div class="section-head">
            <div>
              <h2 class="section-title">热门商品</h2>
              <p class="muted">平台热卖，大家都在买</p>
            </div>
            <router-link class="panel-link" to="/products">查看更多</router-link>
          </div>
          <div class="product-grid compact">
            <ProductCard v-for="item in displayedGoods" :key="item.goodsId" :item="item" mode="compact" />
          </div>
        </div>
      </section>

      <section v-if="visibleHomeSections.length" class="section-gray">
        <div class="container home-sections">
          <article v-for="section in visibleHomeSections" :key="section.sectionId" class="content-section">
            <div class="section-head">
              <div>
                <h2 class="section-title">{{ section.sectionName }}</h2>
                <p class="muted">{{ section.sectionType === 4 ? '直播、内容和商品联动展示' : '来自首页楼层配置的精选商品' }}</p>
              </div>
              <button class="ghost-link panel-link" @click="sectionAction(section)">进入专区</button>
            </div>
            <div v-if="section.goods?.length" class="product-grid compact">
              <ProductCard
                v-for="item in section.goods.slice(0, 4)"
                :key="`${section.sectionId}-${item.goodsId}`"
                :item="item"
                mode="compact"
              />
            </div>
            <div v-else class="empty-section">
              <el-icon><VideoCamera /></el-icon>
              <strong>{{ section.sectionType === 4 ? '直播入口已保留' : '楼层商品待配置' }}</strong>
              <span class="muted">这里不新增无关功能，只等待后端已有数据驱动展示。</span>
            </div>
          </article>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="page-hero">
        <div class="container">
          <span class="page-kicker">Products</span>
          <h1>商品分类</h1>
          <p>发现更多适合你的生活好物。</p>
        </div>
      </section>

      <section class="section">
        <div class="container product-list-layout">
          <div class="filter-panel">
            <div class="search-toolbar">
              <el-input v-model="keyword" placeholder="搜商品、品牌、关键词" clearable @keyup.enter="handleSearch" />
              <el-select v-model="categoryId" placeholder="分类" clearable>
                <el-option v-for="c in topLevelCategories" :key="c.cateId" :label="c.cateName" :value="c.cateId" />
              </el-select>
              <div class="price-range">
                <el-input-number v-model="minPrice" :min="0" :precision="2" :controls="false" placeholder="最低价" />
                <span>至</span>
                <el-input-number v-model="maxPrice" :min="0" :precision="2" :controls="false" placeholder="最高价" />
              </div>
              <el-select v-model="sort" placeholder="排序" clearable>
                <el-option label="销量优先" value="salesDesc" />
                <el-option label="价格从低到高" value="priceAsc" />
                <el-option label="价格从高到低" value="priceDesc" />
                <el-option label="评分优先" value="ratingDesc" />
                <el-option label="最新上架" value="newest" />
              </el-select>
              <el-button type="primary" :loading="loading" @click="handleSearch">搜索商品</el-button>
            </div>
            <div class="category-tabs slim">
              <button
                v-for="group in groupedCategories"
                :key="group.cateId"
                class="category-tab"
                :class="{ active: Number(categoryId) === Number(group.cateId) }"
                @click="goCategory(group.cateId)"
              >
                <span>{{ group.cateName }}</span>
              </button>
            </div>
            <span class="result-tip">当前共 {{ total }} 件商品</span>
          </div>

          <div class="product-grid list-grid">
            <ProductCard v-for="item in goods" :key="item.goodsId" :item="item" />
          </div>
          <el-pagination
            background
            layout="prev, pager, next"
            :page-size="pageSize"
            :current-page="pageNum"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </template>

    <el-dialog v-model="platformCouponPopupVisible" title="领取平台优惠券" width="420px">
      <div v-if="platformCouponPopupCoupon" class="stack" style="gap: 10px;">
        <strong>{{ platformCouponPopupCoupon.coupon_name || platformCouponPopupCoupon.couponName }}</strong>
        <div class="row" style="justify-content: space-between; width: 100%;">
          <span>优惠：{{ discountText(platformCouponPopupCoupon) }}</span>
          <span>满 {{ moneyText(platformCouponPopupCoupon.min_amount ?? platformCouponPopupCoupon.minAmount) }} 可用</span>
        </div>
        <span class="muted">有效期至 {{ platformCouponPopupCoupon.end_time || platformCouponPopupCoupon.endTime }}</span>
      </div>
      <template #footer>
        <el-button @click="platformCouponPopupVisible = false">稍后再说</el-button>
        <el-button plain @click="router.push('/coupons')">去领券中心</el-button>
        <el-button type="primary" :loading="platformCouponReceiving" @click="receivePlatformCoupon">一键领取</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.home-page {
  background: #fff;
}

.home-hero {
  width: 100%;
  background: #fff;
}

.home-hero :deep(.el-carousel__container) {
  height: 520px !important;
}

.hero-slide {
  position: relative;
  display: block;
  width: 100%;
  height: 520px;
  padding: 0;
  border: 0;
  overflow: hidden;
  cursor: pointer;
  text-align: left;
  background: #f2f2f2;
}

.hero-slide-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-wash {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.94) 0%, rgba(255, 255, 255, 0.74) 34%, rgba(255, 255, 255, 0.08) 76%);
}

.hero-copy {
  position: relative;
  z-index: 2;
  display: grid;
  align-content: center;
  gap: 20px;
  height: 100%;
  color: var(--text-main);
}

.hero-kicker {
  width: fit-content;
  color: var(--brand-red);
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1,
.page-hero h1 {
  max-width: 560px;
  margin: 0;
  font-size: 56px;
  font-weight: 900;
  line-height: 1.05;
}

.hero-copy p,
.page-hero p {
  max-width: 500px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 17px;
  line-height: 1.8;
}

.hero-fallback {
  background:
    radial-gradient(circle at 82% 30%, rgba(230, 0, 18, 0.14), transparent 28%),
    linear-gradient(135deg, #fff 0%, #fff4f5 100%);
}

.brand-slogan {
  margin: 0 0 42px;
  text-align: center;
  font-size: 30px;
  font-weight: 900;
}

.brand-slogan span {
  color: var(--brand-red);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 28px;
}

.stat-card {
  display: grid;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.stat-card :deep(.el-icon) {
  color: #777;
  font-size: 34px;
}

.stat-card span {
  color: var(--text-secondary);
  font-size: 14px;
}

.stat-card strong {
  color: var(--brand-red);
  font-size: 24px;
  font-weight: 900;
}

.feature-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
}

.topic-cover {
  position: relative;
  min-height: 380px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background-position: center;
  background-size: cover;
}

.topic-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.62), rgba(0, 0, 0, 0.08));
}

.topic-copy {
  position: absolute;
  left: 36px;
  bottom: 36px;
  display: grid;
  gap: 14px;
  max-width: 460px;
  color: #fff;
}

.topic-copy span {
  font-size: 13px;
  font-weight: 800;
}

.topic-copy h2 {
  margin: 0;
  font-size: 38px;
}

.topic-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.8;
}

.topic-copy :deep(.el-button) {
  width: fit-content;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.82);
  background: transparent;
}

.quick-feature {
  display: grid;
  gap: 16px;
}

.quick-feature a {
  display: grid;
  align-content: center;
  gap: 10px;
  min-height: 116px;
  padding: 24px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #fff;
}

.quick-feature a:hover {
  border-color: var(--brand-red);
}

.quick-feature strong {
  font-size: 22px;
}

.quick-feature span {
  color: var(--text-muted);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.section-head p {
  margin: 0;
}

.category-tabs {
  display: flex;
  gap: 18px;
  overflow-x: auto;
  padding-bottom: 6px;
}

.category-tab {
  position: relative;
  display: grid;
  gap: 8px;
  min-width: 160px;
  padding: 18px 0;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
}

.category-tab span {
  font-size: 18px;
  font-weight: 800;
}

.category-tab small {
  color: var(--text-muted);
}

.category-tab:hover,
.category-tab.active {
  color: var(--brand-red);
  border-bottom-color: var(--brand-red);
}

.category-tabs.slim .category-tab {
  min-width: auto;
  padding: 8px 0 10px;
}

.category-tabs.slim .category-tab span {
  font-size: 14px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 26px;
}

.product-grid.compact {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.home-sections {
  display: grid;
  gap: 64px;
}

.content-section {
  display: grid;
  gap: 6px;
}

.empty-section {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 180px;
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-soft);
  text-align: center;
}

.empty-section :deep(.el-icon) {
  color: var(--brand-red);
  font-size: 32px;
}

.product-list-layout {
  display: grid;
  gap: 32px;
}

.filter-panel {
  display: grid;
  gap: 20px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--border-light);
}

.search-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 160px minmax(260px, 0.8fr) 160px 130px;
  gap: 12px;
  align-items: center;
}

.price-range {
  display: grid;
  grid-template-columns: minmax(110px, 1fr) auto minmax(110px, 1fr);
  gap: 8px;
  align-items: center;
}

.price-range span {
  color: var(--text-muted);
  font-size: 13px;
}

.price-range :deep(.el-input-number) {
  width: 100%;
}

.result-tip {
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 1080px) {
  .hero-copy h1,
  .page-hero h1 {
    font-size: 44px;
  }

  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .feature-layout {
    grid-template-columns: 1fr;
  }

  .product-grid,
  .product-grid.compact {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .search-toolbar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home-hero :deep(.el-carousel__container) {
    height: 260px !important;
  }

  .hero-slide {
    height: 260px;
  }

  .hero-copy {
    gap: 12px;
  }

  .hero-copy h1,
  .page-hero h1 {
    font-size: 32px;
  }

  .hero-copy p {
    display: none;
  }

  .brand-slogan {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-grid,
  .product-grid.compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .search-toolbar {
    grid-template-columns: 1fr;
  }

  .topic-cover {
    min-height: 300px;
  }

  .topic-copy {
    left: 22px;
    right: 22px;
    bottom: 22px;
  }
}

</style>
