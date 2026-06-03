<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '../api/client'

const router = useRouter()
const coupons = ref([])
const loading = ref(false)
const receivingId = ref(null)
const activeScene = ref('platform')
const activeFilter = ref('all')
const couponSceneChips = [
  { key: 'platform', label: '平台满减券' },
  { key: 'merchant', label: '店铺折扣券' },
  { key: 'category', label: '品类券' },
  { key: 'stackable', label: '可叠加优惠' }
]
const filterChips = [
  { key: 'all', label: '全部' },
  { key: 'platform', label: '平台券' },
  { key: 'merchant', label: '店铺券' },
  { key: 'category', label: '品类券' },
  { key: 'expiring', label: '即将过期' }
]

const authRole = computed(() => String(sessionStorage.getItem('shopping_auth_role') || ''))
const isMerchantRole = computed(() => authRole.value === 'merchant')
const merchantId = computed(() => {
  if (!isMerchantRole.value) return null
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
const userToken = computed(() => {
  try {
    return String(localStorage.getItem('shopping_user_token') || '')
  } catch (e) {
    return ''
  }
})
const needsUserLogin = computed(() => !isMerchantRole.value && !userToken.value)

const pageTitle = computed(() => (isMerchantRole.value ? '优惠券' : '领券中心'))
const pageDesc = computed(() => {
  if (isMerchantRole.value) return '展示平台券和本店优惠券（商品端查看用）。'
  return '先领券再下单，平台券、店铺券、品类券和商品券都在这里集中领取。'
})

const normalizedCoupons = computed(() => coupons.value.map((coupon) => normalizeCoupon(coupon)))
const availableCount = computed(() => normalizedCoupons.value.filter((item) => item.state !== 'disabled').length)
const expiringSoonCount = computed(() => normalizedCoupons.value.filter((item) => item.isExpiringSoon).length)
const filteredCoupons = computed(() => {
  if (activeFilter.value === 'all') return normalizedCoupons.value
  if (activeFilter.value === 'expiring') return normalizedCoupons.value.filter((item) => item.isExpiringSoon)
  return normalizedCoupons.value.filter((item) => item.filterType === activeFilter.value)
})

function money(value) {
  return Number(value || 0).toFixed(0)
}

function discountText(coupon) {
  if (Number(coupon.discountType ?? coupon.discount_type) === 2) {
    return `${Number((coupon.discountRate ?? coupon.discount_rate) || 1) * 10}折`
  }
  return `¥${money(coupon.denomination)}`
}

function rangeText(coupon) {
  const couponType = Number(coupon.couponType ?? coupon.coupon_type ?? 0)
  const merchantName = coupon.merchantName || coupon.merchant_name
  if (couponType === 1) return '全场通用'
  return merchantName ? `仅限 ${merchantName}` : (coupon.scopeText || coupon.scope_text || '本店可用')
}

function buttonText(coupon) {
  if (coupon.state === 'receive') return '立即领取'
  if (coupon.state === 'use') return '去使用'
  return coupon.cannotReceiveReason || '不可领取'
}

function parseTime(value) {
  const raw = String(value || '').trim()
  if (!raw) return null
  const normalized = raw.includes(' ') ? raw.replace(' ', 'T') : raw
  const time = new Date(normalized)
  return Number.isNaN(time.getTime()) ? null : time
}

function couponFilterType(coupon) {
  const couponType = Number(coupon.couponType ?? coupon.coupon_type ?? 0)
  const scopeText = String(coupon.scopeText || coupon.scope_text || '')
  if (scopeText.includes('指定分类') || scopeText.includes('指定商品')) return 'category'
  return couponType === 1 ? 'platform' : 'merchant'
}

function couponState(coupon) {
  if (coupon.canReceive) return 'receive'
  if (!isMerchantRole.value && String(coupon.cannotReceiveReason || '') === '已领取') return 'use'
  return 'disabled'
}

function normalizeCoupon(coupon) {
  const endTime = coupon.endTime || coupon.end_time
  const endAt = parseTime(endTime)
  const now = Date.now()
  const diff = endAt ? endAt.getTime() - now : Number.POSITIVE_INFINITY
  return {
    ...coupon,
    endTimeText: endTime,
    scopeTextResolved: rangeText(coupon),
    filterType: couponFilterType(coupon),
    state: couponState(coupon),
    isExpiringSoon: diff > 0 && diff <= 1000 * 60 * 60 * 24 * 3
  }
}

function toCenterCoupon(coupon, kind) {
  const isPlatform = kind === 'platform'
  const discountType = Number(coupon?.discountType ?? coupon?.discount_type ?? 1)
  const discountAmount = Number(coupon?.discountAmount ?? coupon?.discount_amount ?? coupon?.denomination ?? 0)
  const discountRate = Number(coupon?.discountRate ?? coupon?.discount_rate ?? 1)
  const minAmount = Number(coupon?.minAmount ?? coupon?.min_amount ?? 0)
  const surplusNum = Number(coupon?.surplusNum ?? coupon?.surplus_num ?? 0)
  const endTime = coupon?.endTime ?? coupon?.end_time ?? coupon?.end_time_str ?? ''
  return {
    couponId: coupon?.id ?? coupon?.couponId ?? coupon?.coupon_id,
    couponName: coupon?.name ?? coupon?.couponName ?? coupon?.coupon_name ?? '优惠券',
    couponType: isPlatform ? 1 : 0,
    merchantName: isPlatform ? '' : (coupon?.merchantName ?? coupon?.merchant_name ?? merchantNameFromLocal()),
    discountType,
    denomination: discountAmount,
    discountRate,
    minAmount,
    endTime,
    surplusNum,
    canReceive: !isMerchantRole.value,
    cannotReceiveReason: isMerchantRole.value ? '仅展示' : ''
  }
}

function merchantNameFromLocal() {
  try {
    const raw = localStorage.getItem('merchantUser')
    const parsed = raw ? JSON.parse(raw) : null
    return String(parsed?.merchantName || parsed?.username || '').trim()
  } catch (e) {
    return ''
  }
}

async function merchantJson(url) {
  const resp = await fetch(url, { method: 'GET' })
  const payload = await resp.json().catch(() => null)
  if (!resp.ok) throw new Error('优惠券加载失败')
  if (!Array.isArray(payload)) return []
  return payload
}

async function load() {
  loading.value = true
  try {
    if (isMerchantRole.value) {
      if (!merchantId.value) {
        coupons.value = []
        return
      }
      const [platformList, myList] = await Promise.all([
        merchantJson('/api/coupon/platform/list'),
        merchantJson(`/api/coupon/list?merchantId=${encodeURIComponent(String(merchantId.value))}`)
      ])
      const merged = [
        ...(platformList || []).filter((c) => Number(c?.status ?? 1) === 1).map((c) => toCenterCoupon(c, 'platform')),
        ...(myList || []).filter((c) => Number(c?.status ?? 1) === 1).map((c) => toCenterCoupon(c, 'merchant'))
      ]
      coupons.value = merged
      return
    }
    if (needsUserLogin.value) {
      coupons.value = []
      return
    }
    coupons.value = await api('/api/user/coupon-center')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function receive(coupon) {
  if (isMerchantRole.value) return
  if (needsUserLogin.value) {
    router.push('/login')
    return
  }
  receivingId.value = coupon.couponId || coupon.coupon_id
  try {
    await api(`/api/user/coupons/${receivingId.value}/receive`, { method: 'POST' })
    ElMessage.success('领取成功，已放入我的券包')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    receivingId.value = null
  }
}

function handleCouponAction(coupon) {
  if (coupon.state === 'receive') {
    receive(coupon)
    return
  }
  if (coupon.state === 'use') {
    router.push('/products')
  }
}

function chooseScene(sceneKey) {
  activeScene.value = sceneKey
  activeFilter.value = sceneKey === 'stackable' ? 'all' : sceneKey
}

onMounted(load)
</script>

<template>
  <main class="coupon-center">
    <section class="allmart-page-hero coupon-hero">
      <div class="container">
        <div class="allmart-hero-inner">
          <span class="allmart-page-kicker">COUPONS</span>
          <h1 class="allmart-page-title">{{ pageTitle }}</h1>
          <p class="allmart-page-subtitle">{{ pageDesc }}</p>
          <div class="allmart-chip-tabs allmart-hero-actions coupon-scene-tabs" aria-label="优惠券功能入口">
            <button
              v-for="scene in couponSceneChips"
              :key="scene.key"
              type="button"
              class="allmart-chip"
              :class="{ active: activeScene === scene.key }"
              @click="chooseScene(scene.key)"
            >
              {{ scene.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="page coupon-content allmart-after-hero-page">
      <div class="coupon-toolbar">
        <div class="coupon-stats allmart-card">
          <span>可用优惠券 <strong>{{ availableCount }}</strong> 张</span>
          <i></i>
          <span>即将过期 <strong>{{ expiringSoonCount }}</strong> 张</span>
        </div>
        <div class="coupon-filter-tabs" aria-label="优惠券筛选">
          <button
            v-for="chip in filterChips"
            :key="chip.key"
            type="button"
            class="allmart-chip"
            :class="{ active: activeFilter === chip.key }"
            :aria-selected="activeFilter === chip.key"
            @click="activeFilter = chip.key"
          >
            {{ chip.label }}
          </button>
        </div>
      </div>

      <div class="coupon-grid" v-loading="loading">
        <article v-for="coupon in filteredCoupons" :key="coupon.couponId || coupon.coupon_id" class="coupon-ticket">
          <div class="coupon-ticket-value">
            <strong>{{ discountText(coupon) }}</strong>
            <span>满 {{ money(coupon.minAmount ?? coupon.min_amount) }} 可用</span>
          </div>
          <div class="coupon-ticket-divider" aria-hidden="true">
            <span></span>
          </div>
          <div class="coupon-ticket-main">
            <div class="coupon-ticket-copy">
              <h2>{{ coupon.couponName || coupon.coupon_name }}</h2>
              <p>{{ coupon.scopeTextResolved }}</p>
            </div>
            <div class="coupon-ticket-meta">
              <span>有效期至 {{ coupon.endTimeText }}</span>
              <span>剩余 {{ coupon.surplusNum ?? coupon.surplus_num }} 张</span>
            </div>
            <el-button
              class="coupon-ticket-action"
              :class="{
                'is-outline': coupon.state === 'receive',
                'is-solid': coupon.state === 'use',
                'is-muted': coupon.state === 'disabled'
              }"
              :disabled="coupon.state === 'disabled'"
              :loading="receivingId === (coupon.couponId || coupon.coupon_id)"
              @click="handleCouponAction(coupon)"
            >
              {{ buttonText(coupon) }}
            </el-button>
          </div>
        </article>
        <section v-if="!loading && !filteredCoupons.length" class="allmart-empty-state coupon-empty-state">
          <strong>{{ needsUserLogin ? '请先登录再查看可领取优惠券' : '暂无可领取优惠券' }}</strong>
          <p class="muted">{{ needsUserLogin ? '登录后可领取平台券与店铺券。' : '稍后再来看看。' }}</p>
          <div class="row">
            <el-button v-if="needsUserLogin" type="primary" @click="router.push('/login')">去登录</el-button>
            <el-button v-else @click="router.push('/products')">去逛逛</el-button>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.coupon-content {
  display: grid;
  gap: 26px;
}

.coupon-toolbar {
  display: flex;
  gap: 18px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.coupon-stats {
  display: inline-flex;
  align-items: center;
  gap: 18px;
  min-height: 54px;
  padding: 0 20px;
  border-radius: var(--radius-pill);
  color: var(--text-secondary);
  box-shadow: none;
}

.coupon-stats strong {
  color: var(--brand-red);
  font-size: 20px;
}

.coupon-stats i {
  width: 1px;
  height: 18px;
  background: var(--border-light);
}

.coupon-filter-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.coupon-ticket {
  display: grid;
  grid-template-columns: 128px 18px minmax(0, 1fr);
  min-height: 184px;
  border: 1px solid var(--border-light);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.coupon-ticket-value {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 18px 12px;
  background: linear-gradient(180deg, #fff7f8 0%, #fff1f2 100%);
  color: var(--brand-red);
}

.coupon-ticket-value strong {
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
}

.coupon-ticket-value span {
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 700;
}

.coupon-ticket-divider {
  position: relative;
  background:
    radial-gradient(circle at top, #ffffff 9px, transparent 10px) top center / 18px 18px no-repeat,
    radial-gradient(circle at bottom, #ffffff 9px, transparent 10px) bottom center / 18px 18px no-repeat;
}

.coupon-ticket-divider::before {
  content: "";
  position: absolute;
  inset: 12px 8px;
  border-left: 1px dashed #f0d3d7;
}

.coupon-ticket-main {
  display: grid;
  align-content: space-between;
  gap: 16px;
  padding: 20px 22px 18px 16px;
}

.coupon-ticket-copy {
  display: grid;
  gap: 10px;
}

.coupon-ticket-copy h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
}

.coupon-ticket-copy p,
.coupon-ticket-meta {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.coupon-ticket-copy p {
  margin: 0;
}

.coupon-ticket-meta {
  display: grid;
  gap: 4px;
}

.coupon-ticket-action {
  justify-self: start;
  min-width: 154px;
}

.coupon-ticket-action.is-outline {
  border-color: #ff6b78;
  color: var(--brand-red);
  background: #fff;
}

.coupon-ticket-action.is-solid {
  border-color: var(--brand-red);
  background: var(--brand-red);
  color: #fff;
}

.coupon-ticket-action.is-muted {
  border-color: var(--border-light);
  background: #fafafa;
  color: var(--text-muted);
}

.coupon-empty-state {
  grid-column: 1 / -1;
}

:deep(.coupon-ticket-action.is-outline:hover) {
  border-color: var(--brand-red);
  color: var(--brand-red);
  background: #fff7f8;
}

:deep(.coupon-ticket-action.is-solid:hover) {
  border-color: var(--brand-red-dark);
  background: var(--brand-red-dark);
}

:deep(.coupon-ticket-action.is-muted:hover) {
  border-color: var(--border-light);
  color: var(--text-muted);
  background: #fafafa;
}

:deep(.coupon-ticket-action.is-muted.is-disabled) {
  border-color: var(--border-light);
  color: var(--text-muted);
  background: #fafafa;
  box-shadow: none;
  opacity: 1;
}

@media (max-width: 1080px) {
  .coupon-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .coupon-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .coupon-stats {
    min-height: auto;
    padding: 14px 18px;
  }

  .coupon-grid {
    grid-template-columns: 1fr;
  }

  .coupon-ticket {
    grid-template-columns: 1fr;
  }

  .coupon-ticket-divider {
    display: none;
  }

  .coupon-ticket-main {
    padding-left: 20px;
  }
}
</style>
