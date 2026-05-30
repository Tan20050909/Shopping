<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Goods, Present, Star, TrendCharts, User } from '@element-plus/icons-vue'
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
  { label: '商品', value: `${total.value || goods.value.length || 0}+`, icon: Goods },
  { label: '精选专题', value: `${homeData.value.sections?.length || 0}+`, icon: Star },
  { label: '热搜词', value: `${hotKeywords.value.length || 0}+`, icon: TrendCharts },
  { label: '优惠券', value: '每日可领', icon: Present },
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
  const presets = [
    'https://images.unsplash.com/photo-1518779578993-ec3579fee39f?auto=format&fit=crop&w=1600&q=80',
    'https://images.unsplash.com/photo-1520975869018-47c0a5da8b35?auto=format&fit=crop&w=1600&q=80',
    'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1600&q=80'
  ]
  if (banner?.imageUrl?.startsWith('http')) return banner.imageUrl
  return presets[Number(banner?.bannerId || 0) % presets.length]
}

function handleBannerClick(banner) {
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
                <h1>{{ banner.bannerTitle || 'Allmart 精选好物' }}</h1>
                <p>{{ banner.bannerIntro || '每日上新，发现更好的生活。' }}</p>
                <div class="row">
                  <el-button type="primary" @click.stop="handleBannerClick(banner)">去看看</el-button>
                  <el-button plain @click.stop="router.push('/recommend')">推荐</el-button>
                </div>
              </div>
            </article>
          </el-carousel-item>
        </el-carousel>
        <div v-else class="hero-slide hero-fallback">
          <div class="container hero-copy">
            <span class="hero-kicker">Allmart Select</span>
            <h1>Allmart 精选好物</h1>
            <p>欢迎来到 Allmart，探索更多优质商品。</p>
            <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
          </div>
        </div>
      </section>

      <section class="section stats-section">
        <div class="container">
          <h2 class="brand-slogan">Allmart 精选好物<span>每天都有新发现</span></h2>
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

      <section class="section-gray">
        <div class="container">
          <div class="section-head">
            <div>
              <h2 class="section-title">热门商品</h2>
              <p class="muted">平台热卖，大家都在买</p>
            </div>
            <router-link class="panel-link" to="/products">查看更多</router-link>
          </div>
          <div class="product-grid">
            <ProductCard v-for="item in displayedGoods" :key="item.goodsId" :item="item" mode="compact" />
          </div>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="page-hero">
        <div class="container">
          <span class="page-kicker">Products</span>
          <h1>商品列表</h1>
          <p>搜索、筛选并找到你喜欢的商品。</p>
        </div>
      </section>

      <section class="section">
        <div class="container product-list-layout">
          <div class="filter-panel">
            <div class="search-toolbar">
              <el-input v-model="keyword" placeholder="搜索商品名称/关键词" clearable @keyup.enter="handleSearch" />
              <el-select v-model="categoryId" placeholder="选择分类" clearable>
                <el-option v-for="c in topLevelCategories" :key="c.cateId" :label="c.cateName" :value="c.cateId" />
              </el-select>
              <div class="price-range">
                <el-input-number v-model="minPrice" :min="0" :precision="2" :controls="false" placeholder="最低价" />
                <span>~</span>
                <el-input-number v-model="maxPrice" :min="0" :precision="2" :controls="false" placeholder="最高价" />
              </div>
              <el-select v-model="sort" placeholder="排序" clearable>
                <el-option label="默认" value="" />
                <el-option label="价格升序" value="priceAsc" />
                <el-option label="价格降序" value="priceDesc" />
                <el-option label="评分" value="score" />
                <el-option label="新品" value="new" />
              </el-select>
              <el-button type="primary" :loading="loading" @click="handleSearch">搜索</el-button>
            </div>
            <span class="result-tip">共 {{ total }} 件商品</span>
          </div>

          <div class="product-grid list-grid">
            <ProductCard v-for="item in goods" :key="item.goodsId" :item="item" mode="compact" />
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
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-wash {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.72), rgba(0, 0, 0, 0.15));
}

.hero-copy {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  color: #fff;
}

.hero-kicker {
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.9;
}

.brand-slogan {
  margin: 0 0 18px;
  font-size: 28px;
  line-height: 1.25;
}

.brand-slogan span {
  margin-left: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: #fff;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
}

.category-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.category-tab {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.category-tab small {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
  margin-top: 14px;
}

.filter-panel {
  display: grid;
  gap: 10px;
}

.search-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-tip {
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .category-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
