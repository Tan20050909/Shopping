<template>
  <div class="dashboard">
    <div class="page-title">
      <div class="title-left">
        <div class="title-main">商家主页</div>
        <div class="title-sub">你的订单与经营数据都在这里</div>
      </div>
      <div class="title-actions">
        <el-button class="action-btn" type="primary" @click="goTo('/goods')">发布商品</el-button>
        <el-button class="action-btn" plain @click="goTo('/order')">查看订单</el-button>
        <el-button class="action-btn" plain @click="goTo('/after-sale')">处理售后</el-button>
      </div>
    </div>

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
          <div class="todo" @click="goTo('/order')">
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

    <div class="order-panel">
      <div class="order-head">
        <div class="order-title">我的订单</div>
        <div class="order-tools">
          <el-input v-model="orderSearch" class="order-search" placeholder="搜索订单号/收货人" clearable />
        </div>
      </div>

      <div class="order-tabs">
        <div class="tab" :class="{ active: activeTab === 'all' }" @click="setTab('all')">所有订单</div>
        <div class="tab" :class="{ active: activeTab === 'pay' }" @click="setTab('pay')">待付款</div>
        <div class="tab" :class="{ active: activeTab === 'ship' }" @click="setTab('ship')">
          待发货<span v-if="pendingShip" class="tab-badge">{{ pendingShip }}</span>
        </div>
        <div class="tab" :class="{ active: activeTab === 'deliver' }" @click="setTab('deliver')">待收货</div>
        <div class="tab" :class="{ active: activeTab === 'done' }" @click="setTab('done')">已完成</div>
        <div class="tab" :class="{ active: activeTab === 'after' }" @click="setTab('after')">售后</div>
      </div>

      <div v-if="activeTab !== 'after' && filteredOrders.length" class="order-list">
        <div v-for="o in filteredOrders" :key="o.id || o.orderId || o.orderNo" class="order-card">
          <div class="order-card-head">
            <div class="order-meta">
              <span class="order-date">{{ formatTime(o.payTime || o.createTime) }}</span>
              <span class="order-split">|</span>
              <span class="order-no">订单号：{{ o.orderNo || '-' }}</span>
            </div>
            <div class="order-status" :class="statusClass(o.status)">{{ statusText(o.status) }}</div>
          </div>

          <div class="order-card-body">
            <div class="order-goods-block">
              <div class="order-items">
                <div v-if="getThumbs(o).length" class="item-thumb" v-for="(it, idx) in getThumbs(o)" :key="idx">
                  <img class="thumb-img" :src="resolveImg(it.goodsPic)" alt="" @error="onImgError" />
                </div>
                <div v-else class="item-thumb" v-for="n in 5" :key="n"></div>
              </div>
              <div class="order-goods">{{ orderGoodsSummary(o) }}</div>
            </div>

            <div class="order-amount">
              <div class="amount-line">
                <div class="amount-label">商品金额</div>
                <div class="amount-value">¥ {{ toMoney(o.totalAmount) }}</div>
              </div>
              <div class="amount-line">
                <div class="amount-label">实付款</div>
                <div class="amount-value strong">¥ {{ toMoney(o.payAmount || o.totalAmount) }}</div>
              </div>
            </div>

            <div class="order-actions">
              <el-button size="small" class="btn" @click="openDetail(o.id || o.orderId)">订单详情</el-button>
              <el-button v-if="o.status === 1" size="small" type="primary" class="btn primary" @click="goTo('/order')">发货</el-button>
              <el-button v-if="o.status === 0" size="small" class="btn" @click="goTo('/order')">改价</el-button>
            </div>
          </div>

          <div class="order-card-foot">
            <div class="receiver">
              <span class="receiver-name">{{ o.consignee || '-' }}</span>
              <span class="receiver-phone">{{ o.consigneePhone || '' }}</span>
              <span class="receiver-addr">{{ o.receiveAddr || '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab !== 'after'" class="order-empty">
        <el-empty description="暂无订单数据" />
      </div>

      <div v-else-if="filteredAfterSales.length" class="order-list">
        <div v-for="a in filteredAfterSales" :key="a.id" class="order-card">
          <div class="order-card-head">
            <div class="order-meta">
              <span class="order-date">{{ formatTime(a.createTime) }}</span>
              <span class="order-split">|</span>
              <span class="order-no">售后单号：{{ a.afterSaleNo || a.id }}</span>
            </div>
            <div class="order-status" :class="Number(a.status) === 0 ? 's-warn' : Number(a.status) === 1 ? 's-success' : 's-default'">
              {{ Number(a.status) === 1 ? '已同意' : Number(a.status) === 2 ? '已拒绝' : '待处理' }}
            </div>
          </div>

          <div class="order-card-body">
            <div class="order-goods-block">
              <div class="order-items">
                <div v-if="(a.evidence || '').trim()" class="item-thumb">
                  <img class="thumb-img" :src="resolveImg(String(a.evidence).split(',')[0])" alt="" @error="onImgError" />
                </div>
                <div v-else class="item-thumb"></div>
              </div>
              <div class="order-goods">{{ afterSaleGoodsSummary(a) }}</div>
            </div>

            <div class="order-amount">
              <div class="amount-line">
                <div class="amount-label">类型</div>
                <div class="amount-value">{{ Number(a.type) === 1 ? '退款' : Number(a.type) === 2 ? '换货' : Number(a.type) === 3 ? '补发' : '退货退款' }}</div>
              </div>
              <div class="amount-line">
                <div class="amount-label">退款金额</div>
                <div class="amount-value strong">¥ {{ toMoney(a.refundAmount) }}</div>
              </div>
            </div>

            <div class="order-actions">
              <el-button size="small" class="btn" @click="openAfterSale(a.id)">售后详情</el-button>
            </div>
          </div>

          <div class="order-card-foot">
            <div class="receiver">
              <span class="receiver-name">{{ a.reason || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="order-empty">
        <el-empty description="暂无售后数据" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="860px" destroy-on-close>
      <div v-if="detail?.order">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.order.status) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.order.consignee }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.order.consigneePhone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail.order.receiveAddr }}</el-descriptions-item>
          <el-descriptions-item label="买家备注" :span="2">{{ detail.order.buyerRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">商品明细</el-divider>
        <el-table :data="detail.items || []" size="small" style="width: 100%">
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div class="item-info">
                <img class="item-pic" :src="resolveImg(row.goodsPic)" alt="" @error="onImgError" />
                <div class="item-meta">
                  <div class="item-name">{{ row.goodsName }}</div>
                  <div class="item-spec">{{ row.spec || '默认' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120" />
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="totalPrice" label="小计" width="120" />
        </el-table>

        <el-divider content-position="left">物流信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="快递公司">{{ detail.logistics?.company || '-' }}</el-descriptions-item>
          <el-descriptions-item label="快递单号">{{ detail.logistics?.trackingNo || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else>
        <el-empty description="暂无详情" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { afterSaleApi, goodsApi, merchantApi, orderApi } from '@/api'
import { getMerchantId } from '@/utils/merchant'

const router = useRouter()

const todaySalesAmount = ref(0)
const todayOrderCount = ref(0)
const visitorCount = ref(0)
const followerCount = ref(0)

const platformRankings = ref([])
const merchantIdRef = ref(getMerchantId())

const pendingShip = ref(0)
const pendingAfterSale = ref(0)
const pendingGoods = ref(0)
const orders = ref([])
const afterSales = ref([])
const orderThumbs = ref({})

const detailVisible = ref(false)
const detail = ref(null)

const activeTab = ref('all')
const orderSearch = ref('')

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

const loadDashboardData = async () => {
  const merchantId = getMerchantId()
  merchantIdRef.value = merchantId
  const today = new Date()

  const results = await Promise.allSettled([
    orderApi.list(merchantId, null),
    afterSaleApi.list(merchantId, null),
    goodsApi.list(merchantId),
    merchantApi.info(merchantId)
  ])

  const ordersRes = results[0].status === 'fulfilled' ? results[0].value : null
  const afterSaleRes = results[1].status === 'fulfilled' ? results[1].value : null
  const goodsRes = results[2].status === 'fulfilled' ? results[2].value : null
  const merchantRes = results[3].status === 'fulfilled' ? results[3].value : null

  const orderList = Array.isArray(ordersRes?.data) ? ordersRes.data : []
  const afterSaleList = Array.isArray(afterSaleRes?.data) ? afterSaleRes.data : []
  const goods = Array.isArray(goodsRes?.data) ? goodsRes.data : []
  followerCount.value = Number(merchantRes?.data?.followerCount ?? 0) || 0

  orders.value = orderList
    .slice()
    .sort((a, b) => {
      const ta = parseDate(a.payTime || a.createTime)?.getTime() || 0
      const tb = parseDate(b.payTime || b.createTime)?.getTime() || 0
      return tb - ta
    })

  pendingShip.value = orderList.filter(o => Number(o.status) === 1).length
  todayOrderCount.value = orderList.filter(o => isSameDay(o.payTime || o.createTime, today)).length
  todaySalesAmount.value = orderList
    .filter(o => isSameDay(o.payTime || o.createTime, today))
    .reduce((sum, o) => sum + Number(o.payAmount || o.totalAmount || 0), 0)

  pendingAfterSale.value = afterSaleList.filter(a => Number(a.status) === 0).length
  pendingGoods.value = goods.filter(g => Number(g.auditStatus) !== 1).length
  afterSales.value = afterSaleList
    .slice()
    .sort((a, b) => {
      const ta = parseDate(a.createTime)?.getTime() || 0
      const tb = parseDate(b.createTime)?.getTime() || 0
      return tb - ta
    })

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

const setTab = (key) => {
  activeTab.value = key
}

const matchSearch = (o, keyword) => {
  if (!keyword) return true
  const k = String(keyword).trim()
  if (!k) return true
  const itemNames = Array.isArray(o?.itemNames) ? o.itemNames.filter(Boolean).map(String).join(' ') : ''
  const fields = [o.orderNo, o.consignee, o.consigneePhone, o.receiveAddr, itemNames].filter(Boolean).map(String)
  return fields.some(v => v.includes(k))
}

const tabMatch = (o) => {
  if (activeTab.value === 'all') return true
  if (activeTab.value === 'pay') return o.status === 0
  if (activeTab.value === 'ship') return o.status === 1
  if (activeTab.value === 'deliver') return o.status === 2
  if (activeTab.value === 'done') return o.status === 3
  if (activeTab.value === 'after') return false
  return true
}

const filteredOrders = computed(() => {
  return orders.value.filter(o => tabMatch(o)).filter(o => matchSearch(o, orderSearch.value)).slice(0, 8)
})

const filteredAfterSales = computed(() => {
  if (activeTab.value !== 'after') return []
  const k = String(orderSearch.value || '').trim()
  const list = afterSales.value
  if (!k) return list.slice(0, 8)
  return list
    .filter(a => [a.afterSaleNo, a.reason, afterSaleGoodsSummary(a)].filter(Boolean).map(String).some(v => v.includes(k)))
    .slice(0, 8)
})

const orderGoodsSummary = (o) => {
  const names = Array.isArray(o?.itemNames) ? o.itemNames.map(v => String(v || '').trim()).filter(Boolean) : []
  if (!names.length) return '商品：-'
  const first = names[0]
  if (names.length === 1) return `商品：${first}`
  return `商品：${first} 等${names.length}件`
}

const afterSaleGoodsSummary = (a) => {
  if (!a) return '商品：-'
  const byId = a.orderId != null ? orders.value.find(o => Number(o?.id) === Number(a.orderId)) : null
  if (byId) return orderGoodsSummary(byId)
  const byGroup = a.groupId != null ? orders.value.find(o => Number(o?.groupId) === Number(a.groupId)) : null
  if (byGroup) return orderGoodsSummary(byGroup)
  return '商品：-'
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

const getThumbs = (o) => {
  const pics = o?.itemPics
  if (Array.isArray(pics) && pics.length) {
    return pics.slice(0, 5).map(p => ({ goodsPic: p }))
  }
  const id = o?.id || o?.orderId
  const key = id != null ? String(id) : ''
  const list = orderThumbs.value[key]
  if (!Array.isArray(list)) return []
  return list.slice(0, 5)
}

const prefetchThumbs = async (list) => {
  const ids = (list || [])
    .map(o => o?.id || o?.orderId)
    .filter(v => v != null)
    .map(v => String(v))

  const missing = ids.filter(id => !orderThumbs.value[id])
  if (!missing.length) return

  const picked = missing.slice(0, 8)
  const results = await Promise.allSettled(picked.map(id => orderApi.detail(id)))
  for (let i = 0; i < results.length; i++) {
    const id = picked[i]
    const r = results[i]
    if (r.status !== 'fulfilled') {
      orderThumbs.value[id] = []
      continue
    }
    const d = r.value?.data
    const items = Array.isArray(d?.items) ? d.items : []
    orderThumbs.value[id] = items
  }
}

const openDetail = (id) => {
  if (id == null) return
  router.push(`/order/${id}`)
}

const openAfterSale = (id) => {
  if (id == null) return
  router.push(`/after-sale/${id}`)
}

const statusText = (status) => {
  const texts = ['待付款', '待发货', '待收货', '已完成']
  return texts[status] || '未知'
}

const statusClass = (status) => {
  if (status === 0) return 's-warn'
  if (status === 1) return 's-primary'
  if (status === 2) return 's-info'
  if (status === 3) return 's-success'
  return 's-default'
}

const toMoney = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const formatTime = (value) => {
  if (!value) return '-'
  const d = parseDate(value)
  if (!d) return '-'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(() => {
  loadDashboardData()
})

watch([filteredOrders, activeTab, orderSearch], () => {
  prefetchThumbs(filteredOrders.value)
}, { immediate: true })

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

.order-panel {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
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

.order-search :deep(.el-input__wrapper) {
  border-radius: 999px;
}

.order-tabs {
  display: flex;
  gap: 18px;
  align-items: center;
  padding: 0 16px;
  height: 44px;
  border-bottom: 1px solid #f0f0f0;
}

.tab {
  position: relative;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  padding: 10px 0;
}

.tab.active {
  color: var(--brand-red);
  font-weight: 800;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  height: 2px;
  width: 100%;
  background: var(--brand-red);
  border-radius: 999px;
}

.tab-badge {
  margin-left: 6px;
  background: var(--brand-red);
  color: #fff;
  border-radius: 999px;
  padding: 1px 6px;
  font-size: 12px;
  font-weight: 800;
}

.order-list {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.order-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.order-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666;
  font-size: 12px;
}

.order-split {
  opacity: 0.6;
}

.order-status {
  font-size: 12px;
  font-weight: 800;
}

.order-status.s-warn {
  color: #e6a23c;
}

.order-status.s-primary {
  color: var(--brand-red);
}

.order-status.s-info {
  color: #409eff;
}

.order-status.s-success {
  color: #67c23a;
}

.order-card-body {
  display: grid;
  grid-template-columns: 1fr 240px 220px;
  gap: 12px;
  align-items: center;
  padding: 12px;
}

.order-goods-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.order-items {
  display: flex;
  gap: 10px;
  overflow: hidden;
}

.order-goods {
  font-size: 12px;
  color: #111827;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-thumb {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f2f2f2 0%, #e8e8e8 100%);
  border: 1px solid #f0f0f0;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-pic {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
  background: #f5f5f5;
}

.item-name {
  font-weight: 800;
  color: #111;
}

.item-spec {
  margin-top: 2px;
  font-size: 12px;
  color: #999;
}

.order-amount {
  border-left: 1px dashed #eee;
  padding-left: 12px;
}

.amount-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
  margin: 4px 0;
}

.amount-label {
  font-size: 12px;
  color: #999;
}

.amount-value {
  font-size: 12px;
  color: #333;
}

.amount-value.strong {
  font-weight: 800;
  color: #111;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  border-radius: 999px;
}

.btn.primary {
  background: var(--brand-red);
  border-color: var(--brand-red);
}

.order-card-foot {
  border-top: 1px solid #f0f0f0;
  padding: 10px 12px;
  font-size: 12px;
  color: #777;
}

.receiver {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.receiver-name {
  font-weight: 800;
  color: #333;
}

.order-empty {
  padding: 20px 0 30px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .order-card-body {
    grid-template-columns: 1fr;
  }

  .order-amount {
    border-left: none;
    padding-left: 0;
  }

  .order-actions {
    justify-content: flex-start;
  }
}
</style>
