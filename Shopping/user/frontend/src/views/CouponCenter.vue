<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { api } from '../api/client'

const router = useRouter()
const coupons = ref([])
const loading = ref(false)
const receivingId = ref(null)

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
  return '先领券再下单，平台券、店铺券、商品券都在这里集中领取。'
})
const availableCount = computed(() => {
  if (isMerchantRole.value) return coupons.value.length
  return coupons.value.filter((item) => item.canReceive).length
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
  if (coupon.canReceive) return '立即领取'
  return coupon.cannotReceiveReason || '不可领取'
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

onMounted(load)
</script>

<template>
  <main class="coupon-center">
    <section class="page-hero coupon-hero">
      <div class="container">
        <span class="page-kicker">Coupons</span>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageDesc }}</p>
        <div class="row page-hero-actions">
          <span class="coupon-count">{{ isMerchantRole ? `共 ${availableCount} 张（平台+本店）` : `当前 ${availableCount} 张可领取` }}</span>
        </div>
      </div>
    </section>

    <section class="page stack">
      <div class="coupon-grid" v-loading="loading">
        <article v-for="coupon in coupons" :key="coupon.couponId || coupon.coupon_id" class="coupon-card">
          <div class="coupon-value">
            <strong>{{ discountText(coupon) }}</strong>
            <span>满 {{ money(coupon.minAmount ?? coupon.min_amount) }} 可用</span>
          </div>
          <div class="coupon-info stack">
            <div>
              <h2>{{ coupon.couponName || coupon.coupon_name }}</h2>
              <p>{{ rangeText(coupon) }}</p>
            </div>
            <div class="coupon-meta">
              <span>有效期至 {{ coupon.endTime || coupon.end_time }}</span>
              <span>剩余 {{ coupon.surplusNum ?? coupon.surplus_num }} 张</span>
            </div>
            <el-button
              type="primary"
              :disabled="!coupon.canReceive"
              :loading="receivingId === (coupon.couponId || coupon.coupon_id)"
              @click="receive(coupon)"
            >
              {{ buttonText(coupon) }}
            </el-button>
          </div>
        </article>
        <section v-if="!loading && !coupons.length" class="band stack empty-state">
          <strong>{{ needsUserLogin ? '请先登录再查看可领取优惠券' : '暂时没有优惠券' }}</strong>
          <p class="muted">{{ needsUserLogin ? '登录后可领取平台券与店铺券。' : '稍后再来看看。' }}</p>
          <el-button v-if="needsUserLogin" type="primary" @click="router.push('/login')">去登录</el-button>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.coupon-hero {
  padding-bottom: 80px;
}

.coupon-count {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-pill);
  background: #ffffff;
  border: 1px solid var(--border-light);
  color: var(--brand-red);
  font-weight: 800;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 18px;
}

.coupon-card {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  overflow: hidden;
  min-height: 176px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #fff;
  box-shadow: none;
}

.coupon-value {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  background: var(--bg-soft);
  color: var(--brand-red);
  border-right: 1px dashed var(--border-light);
}

.coupon-value strong {
  font-size: 34px;
  line-height: 1;
}

.coupon-value span {
  color: var(--text-secondary);
  font-size: 14px;
}

.coupon-info {
  padding: 20px;
}

.coupon-info h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.coupon-info p {
  margin: 0;
  color: var(--text-secondary);
}

.coupon-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 14px;
}

.empty-state {
  padding: 28px 20px;
}

:deep(.coupon-info .el-button.is-disabled) {
  color: var(--text-muted);
  background: var(--bg-soft);
  border-color: var(--border-light);
}

@media (max-width: 720px) {
  .coupon-card {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
