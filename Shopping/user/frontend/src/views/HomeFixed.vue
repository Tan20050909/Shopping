<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Goods, Present, Star, TrendCharts, VideoCamera, User } from '@element-plus/icons-vue'
import { api, imageOf } from '../api/client'
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
const sortOptions = [
  { label: '综合推荐', value: '' },
  { label: '销量优先', value: 'salesDesc' },
  { label: '价格从低到高', value: 'priceAsc' },
  { label: '价格从高到低', value: 'priceDesc' },
  { label: '评分优先', value: 'ratingDesc' },
  { label: '最新上架', value: 'newest' }
]
const homeData = ref({
  banners: [],
  sections: [],
  categories: [],
  hotKeywords: [],
  freshProducts: []
})
const lives = ref([])

const isHome = computed(() => route.path === '/')
const hotKeywords = computed(() => homeData.value.hotKeywords || [])
const displayedGoods = computed(() => (isHome.value ? goods.value.slice(0, 8) : goods.value))
const currentCategoryName = computed(() => {
  if (!categoryId.value) return '全部商品'
  const match = topLevelCategories.value.find((item) => Number(item.cateId) === Number(categoryId.value))
  return match?.cateName || '当前分类'
})
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
const topLive = computed(() => {
  const list = Array.isArray(lives.value) ? lives.value : []
  const sorted = [...list]
    .filter(r => Number(r.live_status) !== 2 && Number(r.live_status) !== 3)
    .sort((a, b) => Number(b.watch_num || 0) - Number(a.watch_num || 0))
  return sorted[0] || null
})
const platformStats = computed(() => [
  { label: '精选商品', value: `${total.value || goods.value.length || 0}+`, icon: Goods },
  { label: '内容推荐', value: `${homeData.value.sections?.length || 0}+`, icon: Star },
  { label: '热门关键词', value: `${hotKeywords.value.length || 0}+`, icon: TrendCharts },
  { label: '优惠活动', value: '持续上新', icon: Present },
  { label: '活跃用户', value: '1001 / 1002', icon: User }
])

function bannerImage(banner) {
  const title = String(banner?.bannerTitle || '')
  if (title.includes('数码')) return '/brand-assets/digital-banner.png'
  if (title.includes('春季') || title.includes('穿搭')) return '/brand-assets/spring-banner.png'
  const presets = [
    '/brand-assets/digital-banner.png',
    '/brand-assets/spring-banner.png',
    'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1600&q=80'
  ]
  return banner?.imageUrl?.startsWith('http')
    ? banner.imageUrl
    : presets[Number(banner?.bannerId || 0) % presets.length]
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
    const [homeRes, liveRes] = await Promise.all([
      api('/api/user/home'),
      api('/api/user/live-rooms').catch(() => [])
    ])
    homeData.value = homeRes
    lives.value = liveRes
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    homeLoading.value = false
  }
}

async function loadCategories() {
  categories.value = await api('/api/user/categories')
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
    params.set('pageNum', pageNum.value)
    params.set('pageSize', pageSize.value)
    const data = await api(`/api/user/products?${params.toString()}`)
    goods.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadPage() {
  syncQuery()
  await Promise.all([loadCategories(), loadHome(), loadGoods()])
}

function goToTopLive() {
  if (!topLive.value) { router.push('/live'); return }
  const url = String(topLive.value.live_url || '').trim()
  if (url.startsWith('http://') || url.startsWith('https://')) {
    window.open(url, '_blank', 'noopener,noreferrer')
  } else {
    router.push(`/live/${topLive.value.live_id}`)
  }
}
function sectionAction(section) {
  if (section.sectionType === 4) {
    if (topLive.value) {
      const url = String(topLive.value.live_url || '').trim()
      if (url.startsWith('http://') || url.startsWith('https://')) {
        window.open(url, '_blank', 'noopener,noreferrer')
      } else {
        router.push(`/live/${topLive.value.live_id}`)
      }
    } else {
      router.push('/live')
    }
    return
  }
  if (section.goods?.[0]?.categoryId) {
    router.push(`/products?categoryId=${section.goods[0].categoryId}`)
    return
  }
  router.push('/products')
}

function goCategory(cateId) {
  categoryId.value = cateId
  handleSearch()
}

function chooseCategory(cateId) {
  categoryId.value = cateId
  handleSearch()
}

function chooseSort(value) {
  sort.value = value
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
      return
    }
    await loadGoods()
  }
)

onMounted(loadPage)
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
                <p>保留真实商品、分类、购物车和订单链路，用更清爽的品牌官网方式呈现。</p>
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
          <h2 class="brand-slogan">Allmart 精选好物 <span>开心生活</span></h2>
          <div class="stats-grid">
            <article v-for="stat in platformStats" :key="stat.label" class="stat-card">
              <el-icon><component :is="stat.icon" /></el-icon>
              <span>{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
            </article>
          </div>
        </div>
      </section>

      <section class="section-gray">
        <div class="container feature-layout">
          <article class="topic-cover" :style="{ backgroundImage: `url(${topicImage()})` }">
            <span class="topic-shade"></span>
            <div class="topic-copy">
              <span>精选专题</span>
              <h2>今日值得买</h2>
              <p>从推荐商品、热门榜单到直播内容，把你项目已有功能组织成品牌型购物动线。</p>
              <el-button plain @click="router.push('/recommend')">查看专题</el-button>
            </div>
          </article>
          <aside class="quick-feature">
            <router-link to="/rankings">
              <strong>热门榜单</strong>
              <span>看真实排行和高分好物</span>
            </router-link>
            <router-link to="/live">
              <strong>直播精选</strong>
              <span>进入直播推荐场景</span>
            </router-link>
            <router-link to="/coupons">
              <strong>优惠专区</strong>
              <span>领取可用优惠券</span>
            </router-link>
          </aside>
        </div>
      </section>

      <section class="section">
        <div class="container">
          <div class="section-head">
            <div>
              <h2 class="section-title">商品分类</h2>
              <p class="muted">保留原分类筛选逻辑，换成更干净的官网入口。</p>
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
              <small>{{ group.children?.map(item => item.cateName).slice(0, 2).join(' / ') || '查看好物' }}</small>
            </button>
          </div>
        </div>
      </section>

      <section class="section-gray">
        <div class="container">
          <div class="section-head">
            <div>
              <h2 class="section-title">精选商品</h2>
              <p class="muted">图片优先、信息克制，仍然支持收藏、加购和立即购买。</p>
            </div>
            <router-link class="panel-link" to="/products">筛选更多</router-link>
          </div>
          <div class="product-grid">
            <ProductCard v-for="item in displayedGoods" :key="item.goodsId" :item="item" mode="compact" />
          </div>
        </div>
      </section>

      <section v-if="visibleHomeSections.length" class="section">
        <div class="container home-sections">
          <article
            v-for="section in visibleHomeSections"
            :key="section.sectionId"
            class="content-section"
          >
            <div class="section-head">
              <div>
                <h2 class="section-title">{{ section.sectionName }}</h2>
                <p class="muted">{{ section.sectionType === 4 ? '直播内容入口，保持项目原功能。' : '由首页楼层数据驱动的商品内容。' }}</p>
              </div>
              <el-button text @click="sectionAction(section)">
                {{ section.sectionType === 4 ? '进入直播' : '查看更多' }}
              </el-button>
            </div>
            <div v-if="section.goods?.length" class="product-grid compact">
              <ProductCard
                v-for="item in section.goods"
                :key="`${section.sectionId}-${item.goodsId}`"
                :item="item"
                mode="compact"
              />
            </div>
            <div v-else-if="section.sectionType === 4 && topLive" class="live-card" tabindex="0" role="button" @click="goToTopLive" @keydown.enter="goToTopLive" @keydown.space.prevent="goToTopLive">
              <img class="live-card-cover" :src="imageOf(topLive)" :alt="topLive.live_title" @error="(e) => { e.target.style.display='none' }" />
              <div class="live-card-info">
                <strong>{{ topLive.live_title }}</strong>
                <span class="muted">观看 {{ topLive.watch_num || 0 }} 人 · {{ topLive.merchant_name || '' }}</span>
              </div>
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
      <section class="allmart-page-hero">
        <div class="container">
          <span class="allmart-page-kicker">CATEGORIES</span>
          <h1 class="allmart-page-title">商品分类</h1>
          <p class="allmart-page-subtitle">按分类、关键词和排序发现 AllMart 精选好物，保留真实商品数据与完整购买链路。</p>
          <div class="allmart-chip-tabs" aria-label="商品分类筛选">
            <button
              type="button"
              class="allmart-chip"
              :class="{ active: !categoryId }"
              :aria-selected="!categoryId"
              @click="chooseCategory(undefined)"
            >
              全部
            </button>
            <button
              v-for="group in groupedCategories"
              :key="group.cateId"
              type="button"
              class="allmart-chip"
              :class="{ active: Number(categoryId) === Number(group.cateId) }"
              :aria-selected="Number(categoryId) === Number(group.cateId)"
              @click="chooseCategory(group.cateId)"
            >
              {{ group.cateName }}
            </button>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="container product-list-layout product-browser">
          <div class="product-toolbar">
            <div class="product-toolbar-meta">
              <span>{{ currentCategoryName }}</span>
              <strong>{{ total }} 件商品</strong>
            </div>
            <div class="product-toolbar-search">
              <el-input v-model="keyword" placeholder="搜商品、品牌、关键词" clearable @keyup.enter="handleSearch" />
              <el-button class="toolbar-action" :loading="loading" @click="handleSearch">搜索</el-button>
            </div>
          </div>

          <div class="product-light-filters">
            <div class="product-sort-tabs">
              <button
                v-for="option in sortOptions"
                :key="option.value || 'default'"
                type="button"
                class="allmart-chip"
                :class="{ active: sort === option.value }"
                :aria-selected="sort === option.value"
                @click="chooseSort(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <div class="price-mini-filter">
              <span>价格</span>
              <el-input-number v-model="minPrice" :min="0" :precision="2" :controls="false" placeholder="最低" />
              <em>至</em>
              <el-input-number v-model="maxPrice" :min="0" :precision="2" :controls="false" placeholder="最高" />
              <el-button class="toolbar-action" :loading="loading" @click="handleSearch">应用</el-button>
            </div>
          </div>

          <div v-if="goods.length" class="allmart-product-grid list-grid" v-loading="loading">
            <ProductCard v-for="item in goods" :key="item.goodsId" :item="item" />
          </div>
          <section v-else class="allmart-empty-state" v-loading="loading">
            <strong>暂时没有找到商品</strong>
            <p>换个关键词、分类或价格范围试试，商品列表仍会优先读取真实接口数据。</p>
            <el-button type="primary" @click="router.push('/products')">查看全部商品</el-button>
          </section>
          <el-pagination
            class="product-pagination"
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

.live-card {
  position: relative;
  min-height: 200px;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
}

.live-card-cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.live-card-info {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 6px;
  padding: 24px;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
  color: #fff;
  min-height: 200px;
  align-content: end;
}

.live-card-info .muted {
  color: rgba(255,255,255,0.7);
}

.product-list-layout {
  display: grid;
  gap: 24px;
}

.product-browser {
  margin-top: -18px;
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

.product-toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.product-toolbar-meta {
  display: flex;
  gap: 12px;
  align-items: baseline;
  min-width: max-content;
}

.product-toolbar-meta span {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 900;
}

.product-toolbar-meta strong {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.product-toolbar-search {
  display: grid;
  grid-template-columns: minmax(240px, 360px) auto;
  gap: 10px;
  align-items: center;
}

.product-toolbar-search :deep(.el-input__wrapper) {
  min-height: 40px;
  background: var(--bg-soft);
}

.toolbar-action {
  min-height: 36px;
  height: 36px;
  padding: 0 18px;
  border-color: var(--border-light);
  color: var(--text-main);
  background: #fff;
}

.toolbar-action:hover {
  border-color: var(--brand-red);
  color: var(--brand-red);
  background: #fff;
}

.product-light-filters {
  display: flex;
  gap: 14px 18px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-soft);
}

.product-sort-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.product-sort-tabs .allmart-chip {
  min-height: 34px;
  padding-inline: 17px;
  box-shadow: none;
}

.price-mini-filter {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--text-muted);
  font-size: 13px;
}

.price-mini-filter span {
  color: var(--text-secondary);
  font-weight: 800;
}

.price-mini-filter em {
  color: var(--text-muted);
  font-style: normal;
}

.price-mini-filter :deep(.el-input-number) {
  width: 92px;
}

.price-mini-filter :deep(.el-input__wrapper) {
  min-height: 36px;
  border-radius: var(--radius-pill) !important;
  background: #fff;
}

.price-mini-filter :deep(.el-input__inner) {
  font-size: 12px;
}

.product-pagination {
  justify-self: center;
  margin-top: 4px;
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

  .product-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-toolbar-search {
    width: 100%;
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .product-light-filters {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 720px) {
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

  .product-toolbar-search {
    grid-template-columns: 1fr;
  }

  .price-mini-filter {
    flex-wrap: wrap;
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
