<template>
  <div class="merchant-dashboard">
    <section class="dashboard-hero">
      <div class="hero-copy">
        <span class="hero-kicker">ALLMART MERCHANT</span>
        <h1>商家中心</h1>
        <h2>欢迎回来，{{ displayMerchantName }}</h2>
        <p>实时掌握店铺经营数据，管理商品、订单与客户服务</p>
        <div class="hero-actions">
          <button class="hero-primary" type="button" @click="goTo('/order')">查看订单</button>
          <button class="hero-secondary" type="button" @click="goTo('/goods')">发布商品</button>
        </div>
      </div>
    </section>

    <div class="status-alerts">
      <el-alert v-if="merchantStatus === 0" title="店铺未营业" description="您的店铺尚未通过审核或已停业，部分功能受限。请前往店铺信息页查看审核状态。" type="warning" show-icon :closable="false" />
      <el-alert v-if="merchantStatus === 3" title="店铺已被平台冻结" :description="'您的店铺已被平台冻结，原因：' + (merchantAuditRemark || '未知') + '。无法进行经营操作。'" type="error" show-icon :closable="false" />
      <el-alert v-if="merchantAuditStatus === 2" title="审核未通过" :description="'原因：' + (merchantAuditRemark || '未知')" type="error" show-icon :closable="false" />
    </div>

    <section class="metric-grid">
      <article v-for="item in metricCards" :key="item.label" class="metric-card">
        <div class="metric-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small :class="item.changeClass">{{ item.changeText }}</small>
      </article>
    </section>

    <section class="dashboard-section section-soft">
      <div class="chart-grid">
        <article class="dashboard-card chart-card">
          <CardHead title="营业额趋势" action="更多" @click="goTo('/data-center')" />
        <div v-if="salesTrendHasEnoughData" class="line-chart">
            <svg viewBox="0 0 320 160" role="img" aria-label="营业额趋势">
              <defs>
                <linearGradient id="salesGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#E60012" stop-opacity="0.24" />
                  <stop offset="100%" stop-color="#E60012" stop-opacity="0" />
                </linearGradient>
              </defs>
              <g class="grid-lines">
                <line v-for="y in [28, 58, 88, 118]" :key="y" x1="18" :y1="y" x2="306" :y2="y" />
              </g>
              <polyline class="trend-area" :points="areaPoints" />
              <polyline class="trend-line" :points="linePoints" />
              <circle v-for="point in linePointObjects" :key="point.label" :cx="point.x" :cy="point.y" r="3.8" />
            </svg>
            <div class="chart-x-labels">
              <span v-for="item in salesTrend" :key="item.label">{{ item.label }}</span>
            </div>
          </div>
          <EmptyMini v-else text="暂无近 7 日营业额数据" />
        </article>

        <article class="dashboard-card chart-card">
          <CardHead title="订单时段分布" action="更多" @click="goTo('/order')" />
          <div v-if="timeBuckets.some(item => item.value > 0)" class="bar-chart">
            <div v-for="bucket in timeBuckets" :key="bucket.label" class="bar-item">
              <div class="bar-track">
                <span :style="{ height: `${bucket.percent}%` }"></span>
              </div>
              <small>{{ bucket.label }}</small>
            </div>
          </div>
          <div v-else class="time-empty-state">
            <div class="ghost-bars">
              <i v-for="height in [28, 44, 62, 86, 68, 38]" :key="height" :style="{ height: `${height}%` }"></i>
            </div>
            <p>暂无今日订单时段数据</p>
            <span>产生支付订单后，这里会展示不同时段的下单分布。</span>
          </div>
        </article>

        <article class="dashboard-card chart-card">
          <CardHead title="商品分类销售占比" action="更多" @click="goTo('/goods')" />
          <div v-if="categoryShares.length" class="donut-layout">
            <div class="donut" :style="{ background: categoryConic }">
              <div class="donut-center">销售占比</div>
            </div>
            <div class="legend-list">
              <div v-for="item in categoryShares" :key="item.name" class="legend-row">
                <i :style="{ background: item.color }"></i>
                <span>{{ item.name }}</span>
                <strong>{{ item.percent.toFixed(1) }}%</strong>
              </div>
            </div>
          </div>
          <EmptyMini v-else text="暂无商品分类数据" />
        </article>
      </div>
    </section>

    <section class="dashboard-section section-soft operations-grid">
      <article class="dashboard-card order-card">
        <CardHead title="待处理订单" action="查看全部" @click="goTo('/order')" />
        <div class="light-tabs" aria-label="订单筛选">
          <button class="active">全部</button>
          <button>待付款</button>
          <button>待发货 <i v-if="pendingShip">{{ pendingShip }}</i></button>
          <button>退换/售后</button>
        </div>
        <div v-if="pendingOrderRows.length" class="order-table">
          <div class="order-row order-row-head">
            <span>订单号</span>
            <span>商品</span>
            <span>买家</span>
            <span>金额</span>
            <span>状态</span>
            <span>操作</span>
          </div>
          <div v-for="order in pendingOrderRows" :key="order.key" class="order-row">
            <span class="mono" :title="order.no">{{ order.no }}</span>
            <span :title="order.product">{{ order.product }}</span>
            <span :title="order.buyer">{{ order.buyer }}</span>
            <span>¥{{ toMoney(order.amount) }}</span>
            <span class="status-pill">{{ order.statusText }}</span>
            <button type="button" @click="goTo(order.path)">查看</button>
          </div>
        </div>
        <EmptyMini v-else text="当前没有待处理订单" />
      </article>

      <article class="dashboard-card stock-card">
        <CardHead title="库存预警" action="更多" @click="goTo('/stock')" />
        <div v-if="stockWarnings.length" class="stock-list">
          <div v-for="item in stockWarnings" :key="item.id" class="stock-item">
            <img :src="resolveImg(item.pic)" :alt="item.name" @error="onImgError" />
            <div>
              <strong>{{ item.name }}</strong>
              <span>库存仅剩 <b>{{ item.stock }}</b> 件</span>
            </div>
          </div>
        </div>
        <EmptyMini v-else text="暂无库存预警" />
      </article>
    </section>

    <section class="dashboard-section analysis-grid">
      <article class="dashboard-card">
        <CardHead title="运营预警" action="更多" @click="goTo('/data-center')" />
        <div class="warning-list">
          <div v-for="item in operationWarnings" :key="item.title" class="warning-item">
            <span :class="item.level">
              <el-icon><WarningFilled /></el-icon>
            </span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="dashboard-card">
        <CardHead title="新老客分析" action="更多" @click="goTo('/data-center')" />
        <div v-if="customerMix.total" class="customer-mix">
          <div class="mini-donut" :style="{ background: customerConic }">
            <span>访客占比</span>
          </div>
          <div class="customer-lines">
            <div>
              <i class="new-dot"></i>
              <span>新客</span>
              <strong>{{ customerMix.newPercent.toFixed(1) }}%</strong>
            </div>
            <div>
              <i class="old-dot"></i>
              <span>老客</span>
              <strong>{{ customerMix.oldPercent.toFixed(1) }}%</strong>
            </div>
          </div>
        </div>
        <EmptyMini v-else text="暂无成交用户数据" />
      </article>

      <article class="dashboard-card">
        <CardHead title="用户画像分析" action="更多" @click="goTo('/data-center')" />
        <div class="portrait-panel">
          <div class="portrait-group-title">性别分布</div>
          <div class="portrait-row">
            <span>女性</span>
            <div class="progress-track"><i :style="{ width: `${genderProfile.female}%` }"></i></div>
            <strong>{{ genderProfile.hasData ? `${genderProfile.female}%` : '暂无' }}</strong>
          </div>
          <div class="portrait-row male">
            <span>男性</span>
            <div class="progress-track"><i :style="{ width: `${genderProfile.male}%` }"></i></div>
            <strong>{{ genderProfile.hasData ? `${genderProfile.male}%` : '暂无' }}</strong>
          </div>
          <div class="portrait-group-title">年龄分布</div>
          <div class="age-chips">
            <span>18-24岁</span>
            <span class="active">25-34岁</span>
            <span>35-44岁</span>
            <span>45岁以上</span>
          </div>
        </div>
      </article>
    </section>

    <section class="dashboard-section section-soft bottom-grid">
      <article class="dashboard-card">
        <CardHead title="客服消息" action="更多" @click="goTo('/chat')" />
        <div v-if="customerMessages.length" class="message-list">
          <div v-for="message in customerMessages" :key="message.key" class="message-item">
            <img :src="resolveAvatar(message.avatar)" :alt="message.name" @error="onAvatarError" />
            <div>
              <strong>{{ message.name }}</strong>
              <span>{{ message.text }}</span>
            </div>
            <time>{{ message.time }}</time>
            <i v-if="message.unread">{{ message.unread }}</i>
          </div>
        </div>
        <EmptyMini v-else text="暂无客服消息" />
      </article>

      <article class="dashboard-card hot-card">
        <CardHead title="热销商品" action="更多" @click="goTo('/goods')" />
        <div v-if="hotGoods.length" class="hot-grid">
          <div v-for="(item, index) in hotGoods" :key="item.id || index" class="hot-item" @click="goTo(`/goods/${item.id}`)">
            <span>{{ index + 1 }}</span>
            <img :src="resolveImg(item.pic)" :alt="item.name" @error="onImgError" />
            <strong>{{ item.name }}</strong>
            <div>
              <b>¥{{ toMoney(item.price) }}</b>
              <small>销量 {{ item.sales }}</small>
            </div>
          </div>
        </div>
        <EmptyMini v-else text="暂无热销商品数据" />
      </article>
    </section>

    <footer class="dashboard-footer">
      <div class="footer-brand">
        <img src="/brand-assets/allmart-logo-full.png" alt="AllMart" />
        <span>{{ displayMerchantName }}</span>
      </div>
      <i class="footer-divider" aria-hidden="true"></i>
      <nav>
        <a href="#" @click.prevent="goTo('/platform-support')">帮助中心</a>
        <a href="#" @click.prevent="goTo('/platform-support')">平台规则</a>
        <a href="#" @click.prevent="goTo('/merchant-setting')">隐私政策</a>
        <a href="#" @click.prevent="goTo('/platform-support')">联系我们</a>
      </nav>
      <p>© 2025 AllMart，保留所有权利。</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatLineRound,
  Coin,
  DataAnalysis,
  Document,
  Histogram,
  PieChart,
  ShoppingBag,
  TrendCharts,
  User,
  WarningFilled
} from '@element-plus/icons-vue'
import { afterSaleApi, chatApi, goodsApi, merchantApi, orderApi } from '@/api'
import { getMerchantId } from '@/utils/merchant'

const CardHead = defineComponent({
  props: {
    title: { type: String, required: true },
    action: { type: String, default: '更多' }
  },
  emits: ['click'],
  setup(props, { emit }) {
    return () => h('div', { class: 'card-head' }, [
      h('h3', props.title),
      h('button', { type: 'button', onClick: () => emit('click') }, props.action)
    ])
  }
})

const EmptyMini = defineComponent({
  props: {
    text: { type: String, required: true }
  },
  setup(props) {
    return () => h('div', { class: 'empty-mini' }, props.text)
  }
})

const router = useRouter()

const orders = ref([])
const afterSales = ref([])
const goodsList = ref([])
const merchantInfo = ref({})
const chatSessions = ref([])
const merchantStatus = ref(1)
const merchantAuditStatus = ref(1)
const merchantAuditRemark = ref('')
const platformRankings = ref([])

const pendingShip = ref(0)
const pendingAfterSale = ref(0)
const pendingGoods = ref(0)

const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2280%22%20height%3D%2280%22%20viewBox%3D%220%200%2080%2080%22%3E%3Crect%20width%3D%2280%22%20height%3D%2280%22%20rx%3D%2214%22%20fill%3D%22%23f5f5f5%22/%3E%3Cpath%20d%3D%22M22%2030h36v24H22z%22%20fill%3D%22%23e9e9e9%22/%3E%3Cpath%20d%3D%22M27%2050l10-10%208%208%2010-12%22%20stroke%3D%22%23cccccc%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22/%3E%3Ccircle%20cx%3D%2233%22%20cy%3D%2237%22%20r%3D%223.5%22%20fill%3D%22%23cccccc%22/%3E%3C/svg%3E'
const defaultAvatar = '/brand-assets/avatars/default-avatar-01.png'
const chartColors = ['#ff2d35', '#69a7e8', '#56c78b', '#f6b94b', '#b8aaa0', '#d9d9d9']

const displayMerchantName = computed(() => {
  const raw = merchantInfo.value?.merchantName || merchantInfo.value?.merchant_name || merchantInfo.value?.shopName || merchantInfo.value?.username || ''
  return String(raw || 'AllMart旗舰店').trim()
})

const parseDate = (value) => {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const s = String(value).trim()
  if (!s) return null
  const d = new Date(s.includes(' ') ? s.replace(' ', 'T') : s)
  return Number.isNaN(d.getTime()) ? null : d
}

const dateKey = (date) => {
  const d = date instanceof Date ? date : parseDate(date)
  if (!d) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const shortDate = (key) => key ? key.slice(5).replace('-', '-') : '-'

const addDays = (date, offset) => {
  const d = new Date(date)
  d.setDate(d.getDate() + offset)
  return d
}

const orderDate = (order) => parseDate(order?.payTime || order?.pay_time || order?.createTime || order?.create_time)

const orderAmount = (order) => {
  const num = Number(order?.payAmount ?? order?.pay_amount ?? order?.totalAmount ?? order?.total_amount ?? 0)
  return Number.isFinite(num) ? num : 0
}

const isPaidOrder = (order) => {
  const payStatus = Number(order?.payStatus ?? order?.pay_status)
  const status = Number(order?.status ?? order?.orderStatus ?? order?.order_status)
  return payStatus === 1 || [1, 2, 3, 5].includes(status)
}

const isSameDay = (value, target) => {
  const date = parseDate(value)
  if (!date) return false
  return dateKey(date) === dateKey(target)
}

const extract = (result) => {
  if (result.status !== 'fulfilled') return []
  const data = result.value?.data
  return Array.isArray(data) ? data : []
}

const today = computed(() => new Date())
const yesterday = computed(() => addDays(today.value, -1))
const paidOrders = computed(() => orders.value.filter(isPaidOrder))
const todayPaidOrders = computed(() => paidOrders.value.filter(o => isSameDay(orderDate(o), today.value)))
const yesterdayPaidOrders = computed(() => paidOrders.value.filter(o => isSameDay(orderDate(o), yesterday.value)))
const todayAfterSales = computed(() => afterSales.value.filter(a => isSameDay(a?.createTime || a?.create_time || a?.applyTime || a?.apply_time, today.value)))
const yesterdayAfterSales = computed(() => afterSales.value.filter(a => isSameDay(a?.createTime || a?.create_time || a?.applyTime || a?.apply_time, yesterday.value)))

const todaySalesAmount = computed(() => todayPaidOrders.value.reduce((sum, order) => sum + orderAmount(order), 0))
const yesterdaySalesAmount = computed(() => yesterdayPaidOrders.value.reduce((sum, order) => sum + orderAmount(order), 0))
const todayOrderCount = computed(() => todayPaidOrders.value.length)
const yesterdayOrderCount = computed(() => yesterdayPaidOrders.value.length)

const uniqueUsers = (list) => {
  const ids = list.map(o => String(o?.userId ?? o?.user_id ?? o?.buyerId ?? '')).filter(Boolean)
  return Array.from(new Set(ids))
}

const todayVisitorCount = computed(() => uniqueUsers(todayPaidOrders.value).length)
const yesterdayVisitorCount = computed(() => uniqueUsers(yesterdayPaidOrders.value).length)
const conversionRate = computed(() => todayVisitorCount.value ? todayOrderCount.value / todayVisitorCount.value : 0)
const yesterdayConversionRate = computed(() => yesterdayVisitorCount.value ? yesterdayOrderCount.value / yesterdayVisitorCount.value : 0)
const refundRate = computed(() => todayOrderCount.value ? todayAfterSales.value.length / todayOrderCount.value : 0)
const yesterdayRefundRate = computed(() => yesterdayOrderCount.value ? yesterdayAfterSales.value.length / yesterdayOrderCount.value : 0)
const hasStableConversion = computed(() => todayVisitorCount.value > todayOrderCount.value)

const formatCount = (value) => Number(value || 0).toLocaleString('zh-CN')

const toMoney = (value) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const formatPercent = (value) => `${(Number(value || 0) * 100).toFixed(2)}%`

const changeMeta = (current, previous, options = {}) => {
  const positiveDown = Boolean(options.positiveDown)
  if (!current) return { text: '暂无对比', cls: 'flat' }
  if (!previous) return { text: '暂无对比', cls: 'flat' }
  const diff = current - previous
  const ratio = Math.abs(diff / previous) * 100
  if (Math.abs(diff) < 0.00001) return { text: '较昨日 持平', cls: 'flat' }
  const up = diff > 0
  const cls = positiveDown ? (up ? 'down' : 'up') : (up ? 'up' : 'down')
  return { text: `较昨日 ${up ? '↑' : '↓'} ${ratio.toFixed(1)}%`, cls }
}

const metricCards = computed(() => {
  const saleChange = changeMeta(todaySalesAmount.value, yesterdaySalesAmount.value)
  const orderChange = changeMeta(todayOrderCount.value, yesterdayOrderCount.value)
  const visitorChange = changeMeta(todayVisitorCount.value, yesterdayVisitorCount.value)
  const conversionChange = changeMeta(conversionRate.value, yesterdayConversionRate.value)
  const refundChange = changeMeta(refundRate.value, yesterdayRefundRate.value, { positiveDown: true })

  return [
    { label: '今日营业额', value: `¥ ${toMoney(todaySalesAmount.value)}`, icon: Coin, changeText: saleChange.text, changeClass: saleChange.cls },
    { label: '支付订单数', value: formatCount(todayOrderCount.value), icon: Document, changeText: orderChange.text, changeClass: orderChange.cls },
    { label: '访客数', value: formatCount(todayVisitorCount.value), icon: User, changeText: visitorChange.text, changeClass: visitorChange.cls },
    { label: '转化率', value: hasStableConversion.value ? formatPercent(conversionRate.value) : '暂无稳定数据', icon: TrendCharts, changeText: hasStableConversion.value ? conversionChange.text : '需访客数据', changeClass: hasStableConversion.value ? conversionChange.cls : 'flat' },
    { label: '退款率', value: formatPercent(refundRate.value), icon: WarningFilled, changeText: refundChange.text, changeClass: refundChange.cls }
  ]
})

const salesTrend = computed(() => {
  return Array.from({ length: 7 }, (_, index) => {
    const key = dateKey(addDays(today.value, index - 6))
    const value = paidOrders.value
      .filter(order => dateKey(orderDate(order)) === key)
      .reduce((sum, order) => sum + orderAmount(order), 0)
    return { key, label: shortDate(key), value }
  })
})

const salesTrendHasEnoughData = computed(() => salesTrend.value.filter(item => item.value > 0).length >= 2)

const trendMax = computed(() => Math.max(...salesTrend.value.map(item => item.value), 1))
const linePointObjects = computed(() => salesTrend.value.map((item, index) => {
  const x = 24 + index * 48
  const y = 128 - (item.value / trendMax.value) * 96
  return { ...item, x, y }
}))
const linePoints = computed(() => linePointObjects.value.map(p => `${p.x},${p.y}`).join(' '))
const areaPoints = computed(() => {
  const points = linePointObjects.value.map(p => `${p.x},${p.y}`).join(' ')
  return `24,136 ${points} 312,136`
})

const timeBuckets = computed(() => {
  const buckets = [
    { label: '00-04', start: 0, end: 4, value: 0 },
    { label: '04-08', start: 4, end: 8, value: 0 },
    { label: '08-12', start: 8, end: 12, value: 0 },
    { label: '12-16', start: 12, end: 16, value: 0 },
    { label: '16-20', start: 16, end: 20, value: 0 },
    { label: '20-24', start: 20, end: 24, value: 0 }
  ]
  todayPaidOrders.value.forEach(order => {
    const d = orderDate(order)
    if (!d) return
    const hour = d.getHours()
    const bucket = buckets.find(item => hour >= item.start && hour < item.end)
    if (bucket) bucket.value += 1
  })
  const max = Math.max(...buckets.map(item => item.value), 1)
  return buckets.map(item => ({ ...item, percent: item.value ? Math.max((item.value / max) * 100, 8) : 0 }))
})

const goodsStock = (goods) => {
  const candidates = [goods?.totalStock, goods?.total_stock, goods?.stock, goods?.skuStock, goods?.sku_stock]
  const value = candidates.map(Number).find(num => Number.isFinite(num))
  return value == null ? 0 : value
}

const goodsSales = (goods) => {
  const value = Number(goods?.buyCount ?? goods?.buy_count ?? goods?.sellCount ?? goods?.sell_count ?? 0)
  return Number.isFinite(value) ? value : 0
}

const goodsPrice = (goods) => {
  const value = Number(goods?.price ?? goods?.displayPrice ?? goods?.minPrice ?? goods?.min_price ?? 0)
  return Number.isFinite(value) ? value : 0
}

const goodsName = (goods) => String(goods?.name || goods?.goodsName || goods?.goods_name || '商品').trim()
const goodsId = (goods) => goods?.id || goods?.goodsId || goods?.goods_id
const goodsPic = (goods) => goods?.pic || goods?.goodsPic || goods?.goods_pic || goods?.mainPic || goods?.main_pic || goods?.cover || ''

const categoryShares = computed(() => {
  const map = new Map()
  goodsList.value.forEach(goods => {
    const name = String(goods?.categoryName || goods?.cateName || goods?.cate_name || '其他').trim() || '其他'
    const value = Math.max(goodsSales(goods), 1)
    map.set(name, (map.get(name) || 0) + value)
  })
  const total = Array.from(map.values()).reduce((sum, value) => sum + value, 0)
  if (!total) return []
  return Array.from(map.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, value], index) => ({
      name,
      value,
      color: chartColors[index],
      percent: (value / total) * 100
    }))
})

const categoryConic = computed(() => {
  let cursor = 0
  const segments = categoryShares.value.map(item => {
    const start = cursor
    cursor += item.percent
    return `${item.color} ${start}% ${cursor}%`
  })
  return `conic-gradient(${segments.join(', ')})`
})

const ORDER_STATUS_TEXT = ['待支付', '待发货', '待收货', '已完成', '已取消', '售后中']

const orderStatusText = (order) => {
  const status = Number(order?.status ?? order?.orderStatus ?? order?.order_status)
  return ORDER_STATUS_TEXT[status] || '待处理'
}

const orderNo = (order) => String(order?.orderNo || order?.order_no || order?.id || '-')

const orderProduct = (order) => {
  if (Array.isArray(order?.itemNames) && order.itemNames.length) return order.itemNames.filter(Boolean).join('、')
  return String(order?.goodsName || order?.goods_name || order?.productName || '订单商品').trim()
}

const pendingOrderRows = computed(() => {
  return orders.value
    .filter(order => [0, 1, 5].includes(Number(order?.status ?? order?.orderStatus ?? order?.order_status)))
    .slice()
    .sort((a, b) => (orderDate(b)?.getTime() || 0) - (orderDate(a)?.getTime() || 0))
    .slice(0, 5)
    .map(order => {
      const no = orderNo(order)
      return {
        key: no,
        no,
        product: orderProduct(order),
        buyer: String(order?.userNickname || order?.buyerName || order?.consignee || `用户${order?.userId || ''}`).trim(),
        amount: orderAmount(order),
        statusText: orderStatusText(order),
        path: `/order?keyword=${encodeURIComponent(no)}`
      }
    })
})

const stockWarnings = computed(() => {
  return goodsList.value
    .map(goods => ({
      id: goodsId(goods),
      name: goodsName(goods),
      stock: goodsStock(goods),
      pic: goodsPic(goods)
    }))
    .filter(item => item.stock > 0 && item.stock <= 20)
    .sort((a, b) => a.stock - b.stock)
    .slice(0, 4)
})

const operationWarnings = computed(() => {
  const warnings = []
  if (conversionRate.value > 0 && conversionRate.value < 0.1) {
    warnings.push({ level: 'danger', title: `今日转化率 ${formatPercent(conversionRate.value)}`, desc: '建议优化商品详情和营销活动。' })
  } else {
    warnings.push({ level: 'info', title: '转化率监测', desc: '持续关注商品详情页和活动入口的成交效率。' })
  }
  if (stockWarnings.value.length) {
    warnings.push({ level: 'warn', title: `${stockWarnings.value.length} 个商品库存偏低`, desc: '建议及时补货，避免影响成交。' })
  } else {
    warnings.push({ level: 'info', title: '库存状态稳定', desc: '当前没有低库存预警，建议定期检查热销商品备货。' })
  }
  if (pendingGoods.value) {
    warnings.push({ level: 'info', title: `${pendingGoods.value} 个商品待审核`, desc: '可前往商品管理查看审核状态。' })
  }
  if (refundRate.value > 0.05) {
    warnings.push({ level: 'danger', title: `退款率 ${formatPercent(refundRate.value)}`, desc: '建议关注售后原因和商品评价。' })
  } else if (pendingAfterSale.value) {
    warnings.push({ level: 'warn', title: `${pendingAfterSale.value} 个售后待处理`, desc: '建议尽快响应售后申请，降低投诉风险。' })
  } else {
    warnings.push({ level: 'info', title: '售后响应良好', desc: '暂无待处理售后，继续保持服务响应节奏。' })
  }
  const score = Number(merchantInfo.value?.shopScore ?? merchantInfo.value?.shop_score ?? 0)
  if (score > 0 && score < 4.6) {
    warnings.push({ level: 'warn', title: `店铺评分 ${score.toFixed(1)}`, desc: '建议关注近期待评价和差评回复质量。' })
  }
  return warnings.slice(0, 3)
})

const customerMix = computed(() => {
  const map = new Map()
  paidOrders.value.forEach(order => {
    const id = String(order?.userId ?? order?.user_id ?? order?.buyerId ?? '').trim()
    if (!id) return
    map.set(id, (map.get(id) || 0) + 1)
  })
  const total = map.size
  const oldCount = Array.from(map.values()).filter(count => count > 1).length
  const newCount = Math.max(total - oldCount, 0)
  return {
    total,
    newCount,
    oldCount,
    newPercent: total ? (newCount / total) * 100 : 0,
    oldPercent: total ? (oldCount / total) * 100 : 0
  }
})

const customerConic = computed(() => `conic-gradient(#69a7e8 0 ${customerMix.value.newPercent}%, #ff2d35 ${customerMix.value.newPercent}% 100%)`)

const genderProfile = computed(() => {
  return {
    hasData: false,
    female: 0,
    male: 0
  }
})

const customerMessages = computed(() => {
  return chatSessions.value.slice(0, 4).map((session, index) => {
    const userId = session?.userId ?? session?.user_id ?? ''
    return {
      key: session?.sessionId || session?.id || `${userId}-${index}`,
      name: String(session?.userNickname || session?.nickname || `用户${userId}`).trim(),
      text: String(session?.lastMessageContent || session?.last_message_content || '暂无消息内容').trim(),
      avatar: session?.userAvatar || session?.avatar || '',
      unread: Number(session?.unreadCount ?? session?.unread_count ?? 0) || 0,
      time: formatMessageTime(session?.lastMessageTime || session?.updateTime || session?.createTime)
    }
  })
})

const hotGoods = computed(() => {
  const source = platformRankings.value.length ? platformRankings.value : goodsList.value
  return source
    .slice()
    .sort((a, b) => goodsSales(b) - goodsSales(a))
    .slice(0, 4)
    .map(goods => ({
      id: goodsId(goods),
      name: goodsName(goods),
      pic: goodsPic(goods),
      price: goodsPrice(goods),
      sales: goodsSales(goods)
    }))
})

const formatMessageTime = (value) => {
  const d = parseDate(value)
  if (!d) return '-'
  const now = new Date()
  if (dateKey(d) === dateKey(now)) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return shortDate(dateKey(d))
}

const resolveImg = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return defaultImage
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/brand-assets/')) return v
  if (v.startsWith('brand-assets/')) return `/${v}`
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

const resolveAvatar = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return defaultAvatar
  return resolveImg(raw)
}

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

const onAvatarError = (e) => {
  if (e?.target) e.target.src = defaultAvatar
}

const loadDashboardData = async () => {
  const merchantId = getMerchantId()
  const results = await Promise.allSettled([
    orderApi.list(merchantId, null),
    afterSaleApi.list(merchantId, null),
    goodsApi.list(merchantId),
    merchantApi.info(merchantId),
    chatApi.listSessions(merchantId)
  ])

  orders.value = extract(results[0])
  afterSales.value = extract(results[1])
  goodsList.value = extract(results[2])
  merchantInfo.value = results[3].status === 'fulfilled' ? (results[3].value?.data || {}) : {}
  chatSessions.value = extract(results[4])

  merchantStatus.value = Number(merchantInfo.value?.status ?? 1)
  merchantAuditStatus.value = Number(merchantInfo.value?.auditStatus ?? merchantInfo.value?.audit_status ?? 1)
  merchantAuditRemark.value = merchantInfo.value?.auditRemark || merchantInfo.value?.audit_remark || ''

  pendingShip.value = orders.value.filter(order => Number(order?.status ?? order?.orderStatus ?? order?.order_status) === 1).length
  pendingAfterSale.value = afterSales.value.filter(item => Number(item?.status) === 0).length
  pendingGoods.value = goodsList.value.filter(goods => Number(goods?.auditStatus ?? goods?.audit_status ?? 1) !== 1).length

  try {
    const resp = await fetch('/api/user/rankings?type=sales')
    const payload = await resp.json().catch(() => null)
    if (resp.ok && payload?.code === 'SUCCESS') {
      const list = Array.isArray(payload.data) ? payload.data : []
      platformRankings.value = list.filter(item => Number(item?.merchantId || item?.merchant_id) === Number(merchantId))
    } else {
      platformRankings.value = []
    }
  } catch (e) {
    platformRankings.value = []
  }
}

const goTo = (path) => {
  router.push(path)
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.merchant-dashboard {
  display: grid;
  width: 100%;
  gap: 38px;
  color: var(--text-main);
}

.merchant-dashboard,
.merchant-dashboard * {
  box-sizing: border-box;
}

.dashboard-hero {
  position: relative;
  min-height: clamp(370px, 33vw, 430px);
  overflow: hidden;
  border-radius: 0;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.94) 31%, rgba(255, 255, 255, 0.72) 48%, rgba(255, 255, 255, 0.1) 70%),
    linear-gradient(180deg, rgba(255, 255, 255, 0) 76%, rgba(255, 255, 255, 0.72) 100%),
    url("/brand-assets/merchant.png");
  background-size: cover;
  background-position: center right;
  box-shadow: none;
}

.dashboard-hero::after {
  content: "";
  position: absolute;
  inset: 0;
  height: 100%;
  background:
    radial-gradient(circle at 18% 35%, rgba(255, 255, 255, 0.74), transparent 34%),
    linear-gradient(90deg, rgba(255, 255, 255, 0.82) 0%, transparent 58%);
  pointer-events: none;
}

.hero-copy {
  position: relative;
  z-index: 2;
  display: grid;
  align-content: center;
  justify-items: start;
  max-width: 560px;
  min-height: clamp(370px, 33vw, 430px);
  gap: 14px;
  padding: 78px 0 74px 64px;
}

.hero-kicker {
  color: var(--brand-red);
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
  line-height: 1;
}

.hero-copy h1 {
  margin: 0;
  color: #050505;
  font-size: clamp(50px, 4.6vw, 64px);
  font-weight: 900;
  line-height: 1;
}

.hero-copy h2 {
  margin: 0;
  color: #111;
  font-size: clamp(22px, 2vw, 28px);
  font-weight: 800;
}

.hero-copy p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
}

.hero-actions button {
  min-width: 118px;
  height: 42px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 800;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.hero-actions button:hover {
  transform: translateY(-1px);
}

.hero-primary {
  border: 1px solid var(--brand-red);
  background: var(--brand-red);
  color: #fff;
}

.hero-secondary {
  border: 1px solid rgba(230, 0, 18, 0.55);
  background: rgba(255, 255, 255, 0.92);
  color: var(--brand-red);
}

.status-alerts {
  display: grid;
  gap: 10px;
}

.dashboard-section {
  display: grid;
  gap: 22px;
}

.section-soft {
  position: relative;
  margin-inline: 0;
  padding: 34px 24px;
  background: #f7f7f7;
}

.section-soft::before,
.section-soft::after {
  content: "";
  position: absolute;
  left: 24px;
  right: 24px;
  height: 1px;
  background: rgba(238, 238, 238, 0.72);
}

.section-soft::before {
  top: 0;
}

.section-soft::after {
  bottom: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22px;
  padding: 2px 0 4px;
}

.metric-card,
.dashboard-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 22px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.035);
}

.metric-card {
  display: grid;
  align-content: center;
  gap: 12px;
  min-height: 154px;
  padding: 24px 25px 22px;
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.028);
}

.metric-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: var(--brand-red);
  font-size: 28px;
}

.metric-card span {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 700;
}

.metric-card strong {
  color: #111;
  font-size: clamp(24px, 2.1vw, 32px);
  font-weight: 900;
  line-height: 1.1;
}

.metric-card small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.metric-card small.up {
  color: var(--brand-red);
}

.metric-card small.down {
  color: #35a96d;
}

.metric-card small.flat {
  color: var(--text-muted);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.dashboard-card {
  min-width: 0;
  padding: 24px;
}

:deep(.card-head),
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

:deep(.card-head h3),
.card-head h3 {
  margin: 0;
  color: #111;
  font-size: 18px;
  font-weight: 900;
}

:deep(.card-head button),
.card-head button {
  border: 0;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
}

:deep(.card-head button:hover),
.card-head button:hover {
  color: var(--brand-red);
}

.chart-card {
  min-height: 360px;
}

.line-chart svg {
  display: block;
  width: 100%;
  height: 248px;
}

.grid-lines line {
  stroke: #eeeeee;
  stroke-width: 1;
}

.trend-area {
  fill: url(#salesGradient);
  stroke: none;
}

.trend-line {
  fill: none;
  stroke: #ff2d35;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.line-chart circle {
  fill: #fff;
  stroke: #ff2d35;
  stroke-width: 3;
}

.chart-x-labels {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  color: var(--text-muted);
  font-size: 12px;
}

.bar-chart {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  align-items: end;
  min-height: 252px;
  padding: 22px 4px 0;
}

.bar-item {
  display: grid;
  gap: 10px;
  justify-items: center;
}

.bar-track {
  display: flex;
  align-items: end;
  width: 24px;
  height: 198px;
  border-radius: 999px;
  background: #f5f5f5;
  overflow: hidden;
}

.bar-track span {
  width: 100%;
  border-radius: 999px 999px 0 0;
  background: linear-gradient(180deg, #ff2d35, rgba(255, 45, 53, 0.3));
}

.bar-item small {
  color: var(--text-muted);
  font-size: 12px;
}

.donut-layout {
  display: grid;
  grid-template-columns: 186px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
  min-height: 252px;
}

.donut,
.mini-donut {
  position: relative;
  display: grid;
  place-items: center;
  aspect-ratio: 1;
  border-radius: 50%;
}

.donut::after,
.mini-donut::after {
  content: "";
  position: absolute;
  inset: 34px;
  border-radius: 50%;
  background: #fff;
}

.donut-center,
.mini-donut span {
  position: relative;
  z-index: 1;
  color: #111;
  font-size: 14px;
  font-weight: 900;
  text-align: center;
}

.legend-list {
  display: grid;
  gap: 14px;
}

.legend-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  color: var(--text-secondary);
  font-size: 13px;
}

.legend-row i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-row strong {
  color: #111;
}

.operations-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 0.9fr);
  gap: 22px;
}

.light-tabs {
  display: flex;
  gap: 28px;
  margin: 2px 0 14px;
  border-bottom: 1px solid var(--border-light);
}

.light-tabs button {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-weight: 700;
}

.light-tabs button.active {
  color: var(--brand-red);
}

.light-tabs button.active::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--brand-red);
}

.light-tabs i {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--brand-red);
  color: #fff;
  font-size: 11px;
  font-style: normal;
  line-height: 18px;
}

.order-table {
  display: grid;
}

.order-row {
  display: grid;
  grid-template-columns: minmax(120px, 1.25fr) minmax(120px, 1.2fr) minmax(72px, 0.8fr) minmax(72px, 0.72fr) minmax(80px, 0.72fr) 66px;
  gap: 16px;
  align-items: center;
  min-height: 56px;
  border-bottom: 1px solid var(--border-soft);
  color: var(--text-secondary);
  font-size: 13px;
}

.order-row:last-child {
  border-bottom: 0;
}

.order-row-head {
  min-height: 38px;
  color: var(--text-muted);
  font-size: 12px;
}

.order-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: "Helvetica Neue", Arial, sans-serif;
  color: #333;
}

.status-pill {
  width: fit-content;
  padding: 5px 11px;
  border-radius: 999px;
  background: #fff5f6;
  color: var(--brand-red);
  font-weight: 800;
}

.order-row button {
  height: 30px;
  border: 1px solid rgba(230, 0, 18, 0.5);
  border-radius: 999px;
  background: #fff;
  color: var(--brand-red);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.stock-list,
.message-list,
.warning-list {
  display: grid;
}

.stock-item {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 78px;
  border-bottom: 1px solid var(--border-soft);
}

.stock-item:last-child,
.message-item:last-child,
.warning-item:last-child {
  border-bottom: 0;
}

.stock-item img {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  object-fit: cover;
  background: #f6f6f6;
}

.stock-item div {
  display: grid;
  gap: 5px;
}

.stock-item strong {
  overflow: hidden;
  color: #111;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-item span {
  color: var(--text-secondary);
  font-size: 13px;
}

.stock-item b {
  color: var(--brand-red);
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.warning-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 12px;
  min-height: 72px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-soft);
}

.warning-item > span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
}

.warning-item .danger {
  background: #ff2d35;
}

.warning-item .warn {
  background: #f5b23b;
}

.warning-item .info {
  background: #69a7e8;
}

.warning-item strong {
  color: #111;
}

.warning-item p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.customer-mix {
  display: grid;
  grid-template-columns: 142px minmax(0, 1fr);
  gap: 20px;
  align-items: center;
  min-height: 164px;
}

.mini-donut::after {
  inset: 30px;
}

.customer-lines {
  display: grid;
  gap: 18px;
}

.customer-lines div {
  display: grid;
  grid-template-columns: 9px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  color: var(--text-secondary);
}

.customer-lines i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.new-dot {
  background: #69a7e8;
}

.old-dot {
  background: #ff2d35;
}

.customer-lines strong {
  color: #111;
}

.portrait-panel {
  display: grid;
  gap: 14px;
}

.portrait-group-title {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.portrait-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 46px;
  gap: 12px;
  align-items: center;
  color: var(--text-secondary);
  font-size: 13px;
}

.progress-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #eeeeee;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--brand-red), #ff686f);
}

.portrait-row.male .progress-track i {
  background: linear-gradient(90deg, #69a7e8, #9cc8ef);
}

.age-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.age-chips span {
  padding: 6px 12px;
  border: 1px solid var(--border-light);
  border-radius: 999px;
  color: var(--text-secondary);
  font-size: 12px;
}

.age-chips .active {
  border-color: var(--brand-red);
  background: var(--brand-red);
  color: #fff;
}

.bottom-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.8fr) minmax(0, 1.8fr);
  gap: 22px;
}

.message-item {
  position: relative;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 68px;
  border-bottom: 1px solid var(--border-soft);
}

.message-item img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.message-item div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.message-item strong,
.message-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item strong {
  color: #111;
  font-size: 13px;
}

.message-item span,
.message-item time {
  color: var(--text-muted);
  font-size: 12px;
}

.message-item i {
  position: absolute;
  right: 0;
  bottom: 12px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--brand-red);
  color: #fff;
  font-size: 11px;
  font-style: normal;
  line-height: 18px;
  text-align: center;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.hot-item {
  position: relative;
  display: grid;
  gap: 11px;
  cursor: pointer;
  padding: 0 0 2px;
}

.hot-item > span {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1;
  display: grid;
  place-items: center;
  min-width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #b18234;
  font-size: 12px;
  font-weight: 900;
}

.hot-item img {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 12px;
  object-fit: cover;
  background: #f6f6f6;
}

.hot-item strong {
  overflow: hidden;
  color: #111;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-item div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.hot-item b {
  color: var(--brand-red);
  font-size: 14px;
}

.hot-item small {
  color: var(--text-muted);
}

.dashboard-footer {
  display: grid;
  grid-template-columns: auto 1px minmax(0, 1fr);
  gap: 12px 36px;
  align-items: center;
  margin-top: 8px;
  padding: 44px 48px 52px;
  border-top: 1px solid var(--border-soft);
  color: var(--text-muted);
  background: #fff;
}

.footer-brand {
  display: grid;
  gap: 8px;
}

.footer-brand img {
  width: 142px;
  height: auto;
}

.footer-brand span {
  font-size: 13px;
}

.footer-divider {
  width: 1px;
  height: 56px;
  background: var(--border-light);
}

.dashboard-footer nav {
  display: flex;
  gap: 30px;
  justify-content: center;
  color: var(--text-secondary);
}

.dashboard-footer a {
  color: var(--text-secondary);
  text-decoration: none;
  font-weight: 700;
}

.dashboard-footer a:hover {
  color: var(--brand-red);
}

.dashboard-footer p {
  grid-column: 3;
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: #aaa;
}

.empty-mini {
  display: grid;
  min-height: 184px;
  place-items: center;
  border: 1px dashed var(--border-light);
  border-radius: 16px;
  background: #fafafa;
  color: var(--text-muted);
  font-size: 13px;
}

.time-empty-state {
  display: grid;
  gap: 16px;
  min-height: 252px;
  align-content: center;
  padding: 22px;
  border: 1px dashed var(--border-light);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(250, 250, 250, 0.92)),
    radial-gradient(circle at 76% 24%, rgba(230, 0, 18, 0.06), transparent 34%);
  text-align: center;
}

.ghost-bars {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  align-items: end;
  height: 112px;
  max-width: 250px;
  margin: 0 auto;
}

.ghost-bars i {
  display: block;
  border-radius: 999px 999px 0 0;
  background: linear-gradient(180deg, #f0f0f0, #fafafa);
}

.time-empty-state p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 800;
}

.time-empty-state span {
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .metric-grid,
  .chart-grid,
  .analysis-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-grid .metric-card:last-child,
  .analysis-grid .dashboard-card:last-child {
    grid-column: 1 / -1;
  }

  .operations-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-hero {
    background-position: center right 20%;
  }
}

@media (max-width: 760px) {
  .merchant-dashboard {
    gap: 18px;
  }

  .dashboard-hero {
    min-height: 360px;
    background-position: center right 34%;
  }

  .hero-copy {
    min-height: 360px;
    padding: 42px 24px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .hero-copy h2 {
    font-size: 20px;
  }

  .section-soft {
    margin-inline: -16px;
    padding-inline: 16px;
  }

  .metric-grid,
  .chart-grid,
  .analysis-grid,
  .hot-grid {
    grid-template-columns: 1fr;
  }

  .order-row {
    grid-template-columns: 1fr 1fr;
    padding: 12px 0;
  }

  .order-row-head {
    display: none;
  }

  .donut-layout,
  .customer-mix {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .dashboard-footer {
    grid-template-columns: 1fr;
    padding: 34px 0 42px;
  }

  .dashboard-footer p {
    grid-column: auto;
  }

  .dashboard-footer nav {
    flex-wrap: wrap;
    gap: 14px 22px;
  }
}
</style>
