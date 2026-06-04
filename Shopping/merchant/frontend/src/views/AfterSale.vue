<template>
  <div class="after-page">
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">AFTER SALE</span>
          <h1 class="merchant-page-title">售后管理</h1>
          <p class="merchant-page-desc">统一查看和处理退款售后申请，及时跟进买家服务进度</p>
        </div>
      </div>
    </section>
    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">售后管理</div>
        <div class="panel-tools">
          <el-input v-model="keyword" class="search" placeholder="搜索售后单号/原因/订单号" clearable />
          <el-button plain @click="loadList">刷新</el-button>
        </div>
      </div>

      <div class="tabs">
        <div class="tab" :class="{ active: tabKey === 'all' }" @click="setTab('all')">全部</div>
        <div class="tab" :class="{ active: tabKey === '0' }" @click="setTab('0')">待处理<span v-if="countPending" class="badge">{{ countPending }}</span></div>
        <div class="tab" :class="{ active: tabKey === '1' }" @click="setTab('1')">已同意</div>
        <div class="tab" :class="{ active: tabKey === '2' }" @click="setTab('2')">已拒绝</div>
        <div class="tab" :class="{ active: tabKey === '3' }" @click="setTab('3')">已完成</div>
      </div>

      <div v-if="displayList.length" class="card-list">
        <div v-for="a in displayList" :key="a.id" class="card">
          <div class="card-head">
            <div class="meta">
              <span class="date">{{ formatDate(a.createTime) }}</span>
              <span class="split">|</span>
              <span class="no">售后单号：{{ a.afterSaleNo || a.id }}</span>
              <span v-if="orderNoOf(a)" class="order-no">订单号：{{ orderNoOf(a) }}</span>
            </div>
            <div class="status-wrap">
              <span class="status-tag" :class="statusClass(a)">{{ statusText(a) }}</span>
            </div>
          </div>

          <div class="card-body">
            <div class="thumbs">
              <div v-if="a.displayPic" class="thumb">
                <img class="thumb-img" :src="resolveImg(a.displayPic)" alt="" @error="onImgError" />
              </div>
              <div v-else-if="firstEvidence(a)" class="thumb">
                <img class="thumb-img" :src="resolveImg(firstEvidence(a))" alt="" @error="onImgError" />
              </div>
              <div v-else class="thumb"></div>
            </div>

            <div class="info">
              <div class="goods">{{ goodsSummaryOf(a) }}</div>
              <div class="line">
                <span class="k">类型</span>
                <span class="v">{{ typeText(a.type) }}</span>
              </div>
              <div class="line">
                <span class="k">退款金额</span>
                <span class="v strong">¥ {{ toMoney(a.refundAmount) }}</span>
              </div>
              <div class="reason">{{ a.reason || '-' }}</div>
            </div>

            <div class="actions">
              <el-button size="small" class="btn" @click="openDetail(a.id)">详情</el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty">
        <el-empty description="暂无售后数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { afterSaleApi, orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const router = useRouter()
const list = ref([])
const tabKey = ref('all')
const keyword = ref('')
const merchantId = ref(getMerchantId())
const orderNoMap = ref({})
const orderGoodsMap = ref({})

const loadList = async () => {
  try {
    merchantId.value = getMerchantId()
    const [asRes, orderRes] = await Promise.all([
      afterSaleApi.list(merchantId.value, null),
      orderApi.list(merchantId.value, null)
    ])
    list.value = asRes?.data || []
    const orders = Array.isArray(orderRes?.data) ? orderRes.data : []
    const map = {}
    const goodsMap = {}
    for (const o of orders) {
      if (o?.id != null && o.orderNo) map[`id:${o.id}`] = o.orderNo
      if (o?.groupId != null && o.orderNo) map[`group:${o.groupId}`] = o.orderNo
      const names = Array.isArray(o?.itemNames) ? o.itemNames.map(v => String(v || '').trim()).filter(Boolean) : []
      const summary = names.length ? names.length === 1 ? `商品：${names[0]}` : `商品：${names[0]} 等${names.length}件` : '商品：-'
      if (o?.id != null) goodsMap[`id:${o.id}`] = summary
      if (o?.groupId != null) goodsMap[`group:${o.groupId}`] = summary
    }
    orderNoMap.value = map
    orderGoodsMap.value = goodsMap
  } catch (error) {
    list.value = []
    ElMessage.error(error?.response?.data?.message || '售后列表加载失败')
  }
}

const openDetail = (id) => {
  router.push(`/after-sale/${id}`)
}

const setTab = (k) => {
  tabKey.value = k
}

const orderNoOf = (a) => {
  if (!a) return ''
  const byId = a.orderId != null ? orderNoMap.value[`id:${a.orderId}`] : ''
  if (byId) return byId
  const byGroup = a.groupId != null ? orderNoMap.value[`group:${a.groupId}`] : ''
  return byGroup || ''
}

const goodsSummaryOf = (a) => {
  if (!a) return '商品：-'
  const byId = a.orderId != null ? orderGoodsMap.value[`id:${a.orderId}`] : ''
  if (byId) return byId
  const byGroup = a.groupId != null ? orderGoodsMap.value[`group:${a.groupId}`] : ''
  return byGroup || '商品：-'
}

const matchKeyword = (a) => {
  const k = String(keyword.value || '').trim()
  if (!k) return true
  const fields = [a.afterSaleNo, a.reason, orderNoOf(a), goodsSummaryOf(a)].filter(Boolean).map(String)
  return fields.some(v => v.includes(k))
}

const displayList = computed(() => {
  const base = (list.value || []).filter(matchKeyword)
  if (tabKey.value === 'all') return base
  const s = Number(tabKey.value)
  if (!Number.isFinite(s)) return base
  return base.filter(a => derivedStatus(a) === s)
})

const countPending = computed(() => (list.value || []).filter(a => derivedStatus(a) === 0).length)

const toMoney = (value) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const formatDate = (t) => {
  const s = String(t || '').trim()
  if (!s) return '-'
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return s
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const derivedStatus = (a) => {
  const t = Number(a?.type)
  const s = Number(a?.status)
  if (t === 1 && s === 1) return 3
  return Number.isFinite(s) ? s : 0
}

const statusText = (a) => {
  const s = derivedStatus(a)
  return s === 1 ? '已同意' : s === 2 ? '已拒绝' : s === 3 ? '已完成' : '待处理'
}

const statusClass = (a) => {
  const s = derivedStatus(a)
  return s === 1 ? 'ok' : s === 2 ? 'reject' : s === 3 ? 'done' : 'pending'
}

const typeText = (t) => {
  const v = Number(t)
  if (v === 1) return '退款'
  if (v === 2) return '换货'
  if (v === 3) return '补发'
  if (v === 4) return '退货退款'
  return '售后'
}

const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2280%22%20height%3D%2280%22%20viewBox%3D%220%200%2080%2080%22%3E%3Crect%20width%3D%2280%22%20height%3D%2280%22%20rx%3D%2214%22%20fill%3D%22%23f3f4f6%22/%3E%3Cpath%20d%3D%22M22%2028h36v24H22z%22%20fill%3D%22%23e5e7eb%22/%3E%3Cpath%20d%3D%22M26%2048l10-10%208%208%2010-12%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22/%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2236%22%20r%3D%223.5%22%20fill%3D%22%23cbd5e1%22/%3E%3C/svg%3E'

const resolveImg = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return defaultImage
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  if (v.startsWith('/goods/')) return `/uploads${v}`
  if (v.startsWith('goods/')) return `/uploads/${v}`
  const idx = v.indexOf('/uploads/')
  if (idx > 0) return v.slice(idx)
  if (v.startsWith('/')) return v
  return defaultImage
}

/** 从对象中按优先级读取证据字段 */
const pickEvidenceField = (obj) => {
  if (!obj) return ''
  return String(obj.applyEvidence ?? obj.apply_evidence ?? obj.evidence ?? '').trim()
}

const firstEvidence = (a) => {
  const raw = pickEvidenceField(a)
  if (!raw) return ''
  const u = raw.split(',').map(v => String(v || '').trim()).filter(Boolean)[0] || ''
  return u || ''
}

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

onMounted(loadList)
</script>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 900;
  color: #111827;
}

.panel-tools {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search {
  width: 320px;
}

.tabs {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 10px;
}

.tab {
  position: relative;
  font-weight: 800;
  font-size: 13px;
  color: #6b7280;
  padding: 8px 2px;
  cursor: pointer;
}

.tab.active {
  color: #e11d48;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -11px;
  height: 2px;
  background: #e11d48;
}

.badge {
  margin-left: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  border-radius: 999px;
  background: #e11d48;
  color: #fff;
}

.card-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 12px;
  flex-wrap: wrap;
}

.no,
.order-no {
  color: #111827;
  font-weight: 800;
}

.status-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  background: #f3f4f6;
  color: #374151;
}

.status-tag.pending { background: #fff7ed; color: #c2410c; }
.status-tag.ok { background: #ecfdf5; color: #047857; }
.status-tag.reject { background: #fee2e2; color: #b91c1c; }
.status-tag.done { background: #eff6ff; color: #1d4ed8; }

.card-body {
  display: grid;
  grid-template-columns: 90px 1fr 140px;
  gap: 12px;
  padding: 12px;
  align-items: center;
}

.thumb {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  background: #f3f4f6;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info .line {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
}

.info .line + .line {
  margin-top: 8px;
}

.info .v {
  font-weight: 900;
  color: #111827;
}

.info .v.strong {
  color: #e11d48;
}

.goods {
  font-size: 12px;
  color: #111827;
  font-weight: 800;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reason {
  margin-top: 10px;
  font-size: 12px;
  color: #111827;
  font-weight: 800;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn {
  border-radius: 10px;
}

.empty {
  padding: 20px 0;
}
</style>
