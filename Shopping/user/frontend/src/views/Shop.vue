<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, getUserToken } from '../api/client'
import { shopLogo as resolveLogo, scoreOf } from '../utils'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const shop = ref(null)
const products = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const contacting = ref(false)
const keyword = ref('')
const shopCouponPopupVisible = ref(false)
const shopCouponPopupCoupon = ref(null)
const shopCouponReceiving = ref(false)

function localDayKey() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

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
    return String(rate * 10) + 'x'
  }
  return 'CNY ' + moneyText(coupon?.denomination)
}

function buildProductsQuery() {
  const params = new URLSearchParams()
  params.set('pageNum', String(pageNum.value))
  params.set('pageSize', String(pageSize.value))
  const kw = String(keyword.value || '').trim()
  if (kw) params.set('keyword', kw)
  return params.toString()
}

function shopLogo() {
  return resolveLogo(shop.value?.shopLogo || shop.value?.shop_logo || '')
}

async function loadShopInfo() {
  shop.value = await api(`/api/user/shops/${route.params.id}`)
}

async function loadProducts() {
  const data = await api(`/api/user/shops/${route.params.id}/products?${buildProductsQuery()}`)
  products.value = data.records || []
  total.value = data.total || 0
}

async function loadShop() {
  loading.value = true
  try {
    await Promise.all([loadShopInfo(), loadProducts()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function contactMerchant() {
  if (contacting.value) return
  contacting.value = true
  try {
    const session = await api('/api/user/chat/sessions', {
      method: 'POST',
      body: { merchantId: Number(route.params.id) }
    })
    const target = router.resolve({
      path: `/chat/${session.sessionId || session.session_id}`,
      query: { standalone: '1', shell: '1', role: 'user' }
    })
    window.open(target.href, '_blank', 'noopener,noreferrer')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    contacting.value = false
  }
}

async function handlePageChange(nextPage) {
  pageNum.value = nextPage
  loading.value = true
  try {
    await loadProducts()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pageNum.value = 1
  loading.value = true
  try {
    await loadProducts()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function shopPopupStorageKey(merchantId) {
  return `shopping_shop_coupon_popup_day_${merchantId}`
}

async function maybeShowShopCouponPopup() {
  const merchantId = Number(route.params.id)
  if (!Number.isFinite(merchantId) || merchantId <= 0) return
  if (!getUserToken()) return
  if (shopCouponPopupVisible.value) return

  const today = localDayKey()
  try {
    const last = String(localStorage.getItem(shopPopupStorageKey(merchantId)) || '')
    if (last === today) return
  } catch (e) {
    // ignore
  }

  try {
    const list = await api('/api/user/coupon-center')
    if (!Array.isArray(list) || !list.length) return
    const candidate = list.find((item) => {
      const grantType = numberOf(item?.grant_type ?? item?.grantType, 1)
      const couponType = numberOf(item?.coupon_type ?? item?.couponType, 0)
      const itemMerchantId = numberOf(item?.merchant_id ?? item?.merchantId, 0)
      return grantType === 2 && couponType !== 1 && itemMerchantId === merchantId && Boolean(item?.canReceive)
    })
    if (!candidate) return

    shopCouponPopupCoupon.value = candidate
    shopCouponPopupVisible.value = true
    try {
      localStorage.setItem(shopPopupStorageKey(merchantId), today)
    } catch (e) {
      // ignore
    }
  } catch (error) {
    // ignore
  }
}

async function receiveShopCoupon() {
  if (shopCouponReceiving.value) return
  const couponId = shopCouponPopupCoupon.value?.coupon_id ?? shopCouponPopupCoupon.value?.couponId
  if (!couponId) return
  shopCouponReceiving.value = true
  try {
    await api(`/api/user/coupons/${couponId}/receive`, { method: 'POST' })
    ElMessage.success('领取成功，已放入我的券包')
    shopCouponPopupVisible.value = false
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    shopCouponReceiving.value = false
  }
}

watch(() => route.params.id, async () => {
  pageNum.value = 1
  keyword.value = ''
  await loadShop()
  await maybeShowShopCouponPopup()
})

onMounted(async () => {
  await loadShop()
  await maybeShowShopCouponPopup()
})
</script>

<template>
  <main class="page stack shop-page">
    <section v-if="shop" class="shop-hero">
      <div class="container shop-hero-inner">
        <img class="shop-logo" :src="shopLogo()" :alt="shop.merchantName" />
        <div class="stack">
          <span class="muted">AllMart</span>
          <h1>{{ shop.merchantName }}</h1>
          <p>{{ shop.shopIntro || '精选商品持续上新，欢迎进店选购。' }}</p>
          <div class="row shop-metrics">
            <span>综合 {{ scoreOf(shop.shopScore) }}</span>
            <span>服务 {{ scoreOf(shop.serviceScore) }}</span>
            <span>物流 {{ scoreOf(shop.logisticsScore) }}</span>
            <span>{{ shop.productCount || total }} 件在售</span>
          </div>
        </div>
        <el-button type="primary" :loading="contacting" @click="contactMerchant">联系商家</el-button>
      </div>
    </section>

    <section class="container stack">
      <div class="section-head">
        <div>
          <h2 class="section-title">店铺商品</h2>
          <p class="muted">共 {{ total }} 件商品</p>
        </div>
        <div class="row" style="gap: 10px;">
          <el-input
            v-model="keyword"
            placeholder="搜索店内商品"
            clearable
            style="max-width: 320px;"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :loading="loading" @click="handleSearch">搜索</el-button>
        </div>
      </div>
      <div v-loading="loading" class="allmart-product-grid">
        <ProductCard v-for="item in products" :key="item.goodsId" :item="item" />
      </div>
      <el-empty v-if="!loading && !products.length" description="这个店铺暂时没有可售商品" />
      <el-pagination
        v-if="total > pageSize"
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="handlePageChange"
      />
    </section>

    <el-dialog v-model="shopCouponPopupVisible" title="领取店铺优惠券" width="420px">
      <div v-if="shopCouponPopupCoupon" class="stack" style="gap: 10px;">
        <strong>{{ shopCouponPopupCoupon.coupon_name || shopCouponPopupCoupon.couponName }}</strong>
        <div class="row" style="justify-content: space-between; width: 100%;">
          <span>优惠：{{ discountText(shopCouponPopupCoupon) }}</span>
          <span>满 {{ moneyText(shopCouponPopupCoupon.min_amount ?? shopCouponPopupCoupon.minAmount) }} 可用</span>
        </div>
        <span class="muted">有效期至 {{ shopCouponPopupCoupon.end_time || shopCouponPopupCoupon.endTime }}</span>
      </div>
      <template #footer>
        <el-button @click="shopCouponPopupVisible = false">稍后再说</el-button>
        <el-button plain @click="router.push('/coupons')">去领券中心</el-button>
        <el-button type="primary" :loading="shopCouponReceiving" @click="receiveShopCoupon">一键领取</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.shop-page {
  gap: 32px;
}

.shop-hero {
  padding: 34px 0;
  background: var(--bg-section);
}

.shop-hero-inner {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
}

.shop-logo {
  width: 86px;
  height: 86px;
  object-fit: cover;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid var(--border-light);
}

.shop-hero h1 {
  margin: 0;
  font-size: 36px;
  line-height: 1.15;
}

.shop-hero p {
  margin: 0;
  color: var(--text-secondary);
}

.shop-metrics span {
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--text-secondary);
  background: #ffffff;
  border: 1px solid var(--border-light);
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
}

@media (max-width: 720px) {
  .shop-hero-inner {
    grid-template-columns: 72px 1fr;
  }

  .shop-hero-inner :deep(.el-button) {
    grid-column: 1 / -1;
  }

  .shop-hero h1 {
    font-size: 28px;
  }
}
</style>



