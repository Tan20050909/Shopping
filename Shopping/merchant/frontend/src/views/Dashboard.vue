<template>
  <div class="dashboard">
    <div class="page-title">
      <div class="title-left">
        <div class="title-main">商家主页</div>
        <div class="title-sub">你的经营数据与待办事项都在这里</div>
      </div>
      <div class="title-actions">
        <el-button class="action-btn" type="primary" @click="goTo('/goods')">发布商品</el-button>
        <el-button class="action-btn" plain @click="goTo('/order')">查看订单</el-button>
        <el-button class="action-btn" plain @click="goTo('/after-sale')">处理售后</el-button>
      </div>
    </div>

    <el-alert v-if="merchantStatus === 0" title="店铺未营业" description="您的店铺尚未通过审核或已停业，部分功能受限。请前往店铺信息页查看审核状态。" type="warning" show-icon :closable="false" style="margin-bottom:16px" />
    <el-alert v-if="merchantStatus === 3" title="店铺已被平台冻结" :description="'您的店铺已被平台冻结，原因：' + (merchantAuditRemark || '未知') + '。无法进行经营操作。'" type="error" show-icon :closable="false" style="margin-bottom:16px" />
    <el-alert v-if="merchantAuditStatus === 2" title="审核未通过" :description="'原因：' + (merchantAuditRemark || '未知')" type="error" show-icon :closable="false" style="margin-bottom:16px" />

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-header">
          <div class="summary-title">店铺数据</div>
          <div class="summary-tip">今日</div>
        </div>
        <div class="summary-metrics">
          <div class="metric">
            <div class="metric-value">¥ {{ todaySalesAmount.toFixed(2) }}</div>
            <div class="metric-label">销售额</div>
          </div>
          <div class="metric">
            <div class="metric-value">{{ todayOrderCount }}</div>
            <div class="metric-label">订单数</div>
          </div>
          <div class="metric">
            <div class="metric-value">{{ followerCount }}</div>
            <div class="metric-label">粉丝数</div>
          </div>
          <div class="metric">
            <div class="metric-value">{{ visitorCount }}</div>
            <div class="metric-label">访客数</div>
          </div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-header">
          <div class="summary-title">待处理</div>
          <el-button text class="summary-link" @click="goTo('/order')">去处理</el-button>
        </div>
        <div class="summary-todo">
          <div class="todo" @click="goTo('/order?tab=1')">
            <div class="todo-label">待发货</div>
            <div class="todo-value">{{ pendingShip }}</div>
          </div>
          <div class="todo" @click="goTo('/after-sale')">
            <div class="todo-label">待售后</div>
            <div class="todo-value">{{ pendingAfterSale }}</div>
          </div>
          <div class="todo" @click="goTo('/goods')">
            <div class="todo-label">待审核</div>
            <div class="todo-value">{{ pendingGoods }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="platform-panel">
      <div class="order-head">
        <div class="order-title">店铺热销榜</div>
        <div class="order-tools">
          <el-button text class="summary-link" @click="goTo('/goods')">去商品管理</el-button>
        </div>
      </div>
      <div v-if="platformRankings.length" class="platform-grid">
        <div
          v-for="(g, idx) in platformRankings.slice(0, 6)"
          :key="g.goodsId || g.goods_id || idx"
          class="platform-item"
          @click="goTo(`/products/${g.goodsId || g.goods_id}`)"
        >
          <span class="platform-badge">TOP {{ idx + 1 }}</span>
          <img class="platform-pic" :src="resolveImg(g.goodsPic || g.goods_pic)" alt="" @error="onImgError" />
          <div class="platform-meta">
            <div class="platform-name">{{ g.goodsName || g.goods_name || '商品' }}</div>
            <div class="platform-sub">
              <span>{{ g.merchantName || g.merchant_name || '店铺' }}</span>
              <span class="platform-split">·</span>
              <span>销量 {{ Number(g.sellCount || g.sell_count || 0) }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无平台榜单数据" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { afterSaleApi, goodsApi, merchantApi, orderApi } from '@/api'
import { getMerchantId } from '@/utils/merchant'

const router = useRouter()

const todaySalesAmount = ref(0)
const todayOrderCount = ref(0)
const visitorCount = ref(0)
const followerCount = ref(0)
const merchantStatus = ref(1)
const merchantAuditStatus = ref(1)
const merchantAuditRemark = ref('')

const platformRankings = ref([])

const pendingShip = ref(0)
const pendingAfterSale = ref(0)
const pendingGoods = ref(0)

const parseDate = (value) => {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const s = String(value).trim()
  if (!s) return null
  const normalized = s.includes(' ') ? s.replace(' ', 'T') : s
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

const isSameDay = (value, target) => {
  const date = parseDate(value)
  if (!date) return false
  return date.toDateString() === target.toDateString()
}

const extract = (result) => {
  if (result.status !== 'fulfilled') return []
  const data = result.value?.data
  return Array.isArray(data) ? data : []
}

const loadDashboardData = async () => {
  const merchantId = getMerchantId()
  const today = new Date()

  const results = await Promise.allSettled([
    orderApi.list(merchantId, null),
    afterSaleApi.list(merchantId, null),
    goodsApi.list(merchantId),
    merchantApi.info(merchantId)
  ])

  const orderList = extract(results[0])
  const afterSaleList = extract(results[1])
  const goods = extract(results[2])
  const merchantRes = results[3].status === 'fulfilled' ? results[3].value?.data : null

  followerCount.value = Number(merchantRes?.followerCount ?? 0) || 0
  merchantStatus.value = Number(merchantRes?.status ?? 1)
  merchantAuditStatus.value = Number(merchantRes?.auditStatus ?? 1)
  merchantAuditRemark.value = merchantRes?.auditRemark || ''

  pendingShip.value = orderList.filter(o => Number(o.status) === 1).length
  todayOrderCount.value = orderList.filter(o => isSameDay(o.payTime || o.createTime, today)).length
  todaySalesAmount.value = orderList
    .filter(o => isSameDay(o.payTime || o.createTime, today))
    .reduce((sum, o) => sum + Number(o.payAmount || o.totalAmount || 0), 0)

  pendingAfterSale.value = afterSaleList.filter(a => Number(a.status) === 0).length
  pendingGoods.value = goods.filter(g => Number(g.auditStatus) !== 1).length

  const todayUsers = new Set(
    orderList
      .filter(o => isSameDay(o.payTime || o.createTime, today))
      .map(o => String(o.userId || ''))
      .filter(Boolean)
  )
  visitorCount.value = todayUsers.size

  try {
    const resp = await fetch('/api/user/rankings?type=sales')
    const payload = await resp.json().catch(() => null)
    if (resp.ok && payload && payload.code === 'SUCCESS') {
      const list = Array.isArray(payload.data) ? payload.data : []
      platformRankings.value = list.filter((g) => Number(g?.merchantId || g?.merchant_id) === Number(merchantId))
    } else {
      platformRankings.value = []
    }
  } catch (e) {
    platformRankings.value = []
  }
}

const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2264%22%20height%3D%2264%22%20viewBox%3D%220%200%2064%2064%22%3E%3Crect%20width%3D%2264%22%20height%3D%2264%22%20rx%3D%2212%22%20fill%3D%22%23f3f4f6%22/%3E%3Cpath%20d%3D%22M18%2022h28v20H18z%22%20fill%3D%22%23e5e7eb%22/%3E%3Cpath%20d%3D%22M22%2038l8-8%206%206%208-10%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22/%3E%3Ccircle%20cx%3D%2226%22%20cy%3D%2228%22%20r%3D%223%22%20fill%3D%22%23cbd5e1%22/%3E%3C/svg%3E'

const resolveImg = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return defaultImage
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  if (v.startsWith('/goods/')) return `/uploads${v}`
  if (v.startsWith('goods/')) return `/uploads/${v}`
  if (v.startsWith('/images/') || v.startsWith('/videos/')) return `/uploads${v}`
  if (v.startsWith('images/') || v.startsWith('videos/')) return `/uploads/${v}`
  const idx = v.indexOf('/uploads/')
  if (idx > 0) return v.slice(idx)
  if (v.startsWith('/')) return v
  return defaultImage
}

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

onMounted(() => {
  loadDashboardData()
})

const goTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
}

.title-main {
  font-size: 16px;
  font-weight: 800;
  color: #111;
}

.title-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.title-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.action-btn {
  border-radius: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.summary-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
}

.platform-panel {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.platform-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 16px 16px;
}

.platform-item {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
}

.platform-item:hover {
  border-color: rgba(239, 68, 68, 0.35);
  background: #fffafa;
}

.platform-badge {
  grid-column: 1 / -1;
  width: fit-content;
  font-size: 12px;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.18);
  border-radius: 999px;
  padding: 2px 10px;
  font-weight: 800;
}

.platform-pic {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.platform-meta {
  min-width: 0;
}

.platform-name {
  font-weight: 800;
  color: #111;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-split {
  margin: 0 6px;
  opacity: 0.6;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-title {
  font-weight: 800;
  color: #111;
}

.summary-tip {
  font-size: 12px;
  color: #999;
}

.summary-link {
  color: var(--brand-red);
}

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.metric {
  background: #fff7f2;
  border: 1px solid #ffe3d6;
  border-radius: 10px;
  padding: 10px 12px;
}

.metric-value {
  font-size: 18px;
  font-weight: 800;
  color: #111;
}

.metric-label {
  margin-top: 2px;
  font-size: 12px;
  color: #999;
}

.summary-todo {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.todo {
  background: #fff5f0;
  border: 1px solid #ffd2bf;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.todo:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(255, 80, 0, 0.08);
}

.todo-label {
  font-size: 12px;
  color: #999;
}

.todo-value {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
  color: var(--brand-red);
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.order-title {
  font-weight: 800;
  color: #111;
}

.order-tools {
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
