<template>
  <main class="page data-center-page">
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">DATA CENTER</span>
          <h1 class="merchant-page-title">数据中心</h1>
          <p class="merchant-page-desc">全面掌握店铺销售数据，驱动精细化运营增长</p>
          <div class="merchant-page-actions dc-tools">
          <el-date-picker
            v-model="dateRange"
            class="dc-range"
            size="small"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            clearable
            @change="handleRangeChange"
          />
          <div class="range-tabs">
            <div class="tab" :class="{ active: !hasCustomRange && days === 7 }" @click="setDays(7)">近7天</div>
            <div class="tab" :class="{ active: !hasCustomRange && days === 30 }" @click="setDays(30)">近30天</div>
          </div>
          <el-button size="small" plain :disabled="loading || !merchantId" @click="exportData">导出数据</el-button>
          <el-button type="primary" size="small" :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>
      </div>
    </section>

    <div class="dc-page">
      <el-empty v-if="!merchantId" description="仅商家账号可查看数据中心" />

      <template v-else>
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-icon">¥</div>
            <div><div class="kpi-label">今日营业额</div>
            <div class="kpi-value">¥ {{ toMoney(kpi.turnoverToday) }}</div>
            <div class="kpi-sub">累计 ¥ {{ toMoney(kpi.turnoverTotal) }}</div></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon">单</div>
            <div>
            <div class="kpi-label">今日支付订单</div>
            <div class="kpi-value">{{ Number(kpi.paidOrderToday || 0) }}</div>
            <div class="kpi-sub">累计 {{ Number(kpi.paidOrderTotal || 0) }}</div></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon">运</div>
            <div><div class="kpi-label">待发货订单</div>
            <div class="kpi-value">{{ Number(kpi.pendingShip || 0) }}</div>
            <div class="kpi-sub">待收货 {{ Number(kpi.pendingReceive || 0) }}</div></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon">售</div>
            <div><div class="kpi-label">售后待处理</div>
            <div class="kpi-value">{{ Number(kpi.pendingAfterSale || 0) }}</div>
            <div class="kpi-sub">已完成订单 {{ Number(kpi.completed || 0) }}</div></div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon">✓</div>
            <div><div class="kpi-label">已完成订单</div>
            <div class="kpi-value">{{ Number(kpi.completed || 0) }}</div>
            <div class="kpi-sub">累计支付 {{ Number(kpi.paidOrderTotal || 0) }}</div></div>
          </div>
        </div>

        <div class="panel-grid">
          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">营业额趋势（{{ days }}天）</div>
              <div class="panel-sub">更新：{{ formatTime(updateTime) }}</div>
            </div>

            <div class="chart-wrap">
              <svg class="chart" viewBox="0 0 720 240" preserveAspectRatio="none">
                <defs>
                  <linearGradient id="dcLine" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0" stop-color="rgba(239,68,68,0.55)" />
                    <stop offset="1" stop-color="rgba(239,68,68,0.05)" />
                  </linearGradient>
                </defs>

                <path class="area" :d="areaPath" fill="url(#dcLine)" />
                <path class="line" :d="linePath" />

                <g class="grid">
                  <line x1="36" y1="22" x2="36" y2="210" />
                  <line x1="36" y1="210" x2="704" y2="210" />
                  <line v-for="tick in trendYTicks" :key="`grid-${tick.y}`" x1="36" :y1="tick.y" x2="704" :y2="tick.y" />
                </g>
                <g class="axis-labels">
                  <text v-for="tick in trendYTicks" :key="`y-${tick.y}`" x="30" :y="tick.y + 4" text-anchor="end">{{ tick.label }}</text>
                  <text v-for="tick in trendXLabels" :key="tick.key" :x="tick.x" y="232" text-anchor="middle">{{ tick.label }}</text>
                </g>

                <g class="dots">
                  <circle v-for="p in dotPoints" :key="p.key" :cx="p.x" :cy="p.y" r="3" />
                </g>
              </svg>
            </div>

            <div class="chart-foot">
              <div class="foot-item">
                <div class="foot-label">最高值</div>
                <div class="foot-value">¥ {{ toMoney(maxTurnover) }}</div>
              </div>
              <div class="foot-item">
                <div class="foot-label">最低值</div>
                <div class="foot-value">¥ {{ toMoney(minTurnover) }}</div>
              </div>
              <div class="foot-item">
                <div class="foot-label">均值</div>
                <div class="foot-value">¥ {{ toMoney(avgTurnover) }}</div>
              </div>
              <div class="foot-item">
                <div class="foot-label">支付订单</div>
                <div class="foot-value">{{ rangeOrderCount }}</div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">热销TOP10（{{ days }}天）</div>
              <div class="panel-sub">按销量排序</div>
            </div>

            <div v-if="topGoods.length" class="top-list">
              <div v-for="(g, idx) in topGoods.slice(0, 5)" :key="String(g.goodsId || g.goods_id || idx)" class="top-row">
                <div class="rank">{{ idx + 1 }}</div>
                <div class="name" :title="g.goodsName || g.goods_name || ''">{{ g.goodsName || g.goods_name || '商品' }}</div>
                <div class="qty">{{ Number(g.quantity || 0) }}</div>
                <div class="bar">
                  <div class="bar-inner" :style="{ width: `${qtyPercent(g)}%` }"></div>
                </div>
                <div class="amt">¥ {{ toMoney(g.amount) }}</div>
              </div>
            </div>
            <el-empty v-else-if="!loading" description="暂无热销数据" />
            <div v-else class="loading-pad"></div>
            <div v-if="topGoods.length" class="panel-link">查看全部 {{ Math.min(10, topGoods.length) }} 件商品 &gt;</div>
          </div>
        </div>

        <div class="panel-grid-3">
          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">订单时段分布（{{ days }}天）</div>
              <div class="panel-sub">高峰判断</div>
            </div>

            <div class="bar-chart-wrap">
              <svg class="bar-chart" viewBox="0 0 360 200" preserveAspectRatio="none">
                <g class="bar-grid">
                  <line x1="24" y1="16" x2="24" y2="156" />
                  <line x1="24" y1="156" x2="344" y2="156" />
                </g>

                <g class="bars">
                  <rect
                    v-for="b in timeBars"
                    :key="b.label"
                    :x="b.x"
                    :y="b.y"
                    :width="b.w"
                    :height="b.h"
                    rx="8"
                    ry="8"
                  />
                </g>

                <g class="x-axis">
                  <text
                    v-for="(b, idx) in timeBars"
                    :key="`lbl-${b.label}`"
                    class="x-label"
                    :x="b.x + b.w / 2"
                    y="186"
                    text-anchor="middle"
                  >
                    {{ timeAxisLabels[idx] || '' }}
                  </text>
                </g>
              </svg>
            </div>

            <div class="bar-legend">
              <div v-for="b in timeDistributionSafe" :key="b.label" class="bar-legend-item">
                <div class="bar-legend-label">{{ b.label }}</div>
                <div class="bar-legend-value">{{ Number(b.count || 0) }} 单</div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">商品分类销售占比（{{ days }}天）</div>
              <div class="panel-sub metric-tabs">
                <button
                  type="button"
                  class="metric-tab"
                  :class="{ active: categoryMetric === 'amount' }"
                  @click="categoryMetric = 'amount'"
                >
                  按销售额
                </button>
                <button
                  type="button"
                  class="metric-tab"
                  :class="{ active: categoryMetric === 'quantity' }"
                  @click="categoryMetric = 'quantity'"
                >
                  按销量
                </button>
              </div>
            </div>

            <div class="donut">
              <div class="donut-left">
                <div class="donut-chart">
                  <svg viewBox="0 0 120 120" class="donut-svg" aria-hidden="true">
                    <circle class="donut-track" cx="60" cy="60" :r="donut.r" />
                    <circle
                      v-for="seg in donutSegments"
                      :key="seg.key"
                      class="donut-seg"
                      cx="60"
                      cy="60"
                      :r="donut.r"
                      :stroke="seg.color"
                      :stroke-dasharray="seg.dasharray"
                      :stroke-dashoffset="seg.dashoffset"
                    />
                  </svg>
                </div>
                <div class="donut-metric">
                  <div class="donut-metric-main">{{ categoryMetricMain }}</div>
                  <div class="donut-metric-sub">{{ categoryMetricSub }}</div>
                </div>
              </div>

              <div class="donut-legend">
                <div v-for="it in categoryLegend" :key="it.name" class="donut-legend-item">
                  <span class="dot" :style="{ background: it.color }"></span>
                  <span class="name" :title="it.name">{{ it.name }}</span>
                  <span class="pct">{{ it.pct }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">用户新老客分析（{{ days }}天）</div>
              <div class="panel-sub">拉新与留存</div>
            </div>

            <div class="split-cards">
              <div class="split-card">
                <div class="split-label">新客</div>
                <div class="split-value">{{ Number(customerSplitSafe.newUserCount || 0) }} 人</div>
                <div class="split-sub">订单 {{ Number(customerSplitSafe.newOrderCount || 0) }} · ¥ {{ toMoney(customerSplitSafe.newTurnover) }}</div>
              </div>
              <div class="split-card">
                <div class="split-label">老客</div>
                <div class="split-value">{{ Number(customerSplitSafe.oldUserCount || 0) }} 人</div>
                <div class="split-sub">订单 {{ Number(customerSplitSafe.oldOrderCount || 0) }} · ¥ {{ toMoney(customerSplitSafe.oldTurnover) }}</div>
              </div>
            </div>

            <div class="stackbar">
              <div class="stackbar-inner">
                <div class="stack new" :style="{ width: `${customerPct.newPct}%` }"></div>
                <div class="stack old" :style="{ width: `${customerPct.oldPct}%` }"></div>
              </div>
              <div class="stackbar-foot">
                <span>新客占比 {{ customerPct.newPct }}%</span>
                <span>老客占比 {{ customerPct.oldPct }}%</span>
              </div>
            </div>
          </div>
        </div>

        <div class="panel-grid-3">
          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">用户画像：性别</div>
              <div class="panel-sub">按下单用户</div>
            </div>

            <div class="donut">
              <div class="donut-left">
                <div class="donut-chart">
                  <svg viewBox="0 0 120 120" class="donut-svg" aria-hidden="true">
                    <circle class="donut-track" cx="60" cy="60" :r="donut.r" />
                    <circle
                      v-for="seg in genderDonutSegments"
                      :key="seg.key"
                      class="donut-seg"
                      cx="60"
                      cy="60"
                      :r="donut.r"
                      :stroke="seg.color"
                      :stroke-dasharray="seg.dasharray"
                      :stroke-dashoffset="seg.dashoffset"
                    />
                  </svg>
                </div>
                <div class="donut-metric">
                  <div class="donut-metric-main">{{ genderTotal }}</div>
                  <div class="donut-metric-sub">用户数</div>
                </div>
              </div>

              <div class="donut-legend">
                <div v-for="it in genderLegend" :key="it.name" class="donut-legend-item">
                  <span class="dot" :style="{ background: it.color }"></span>
                  <span class="name">{{ it.name }}</span>
                  <span class="pct">{{ it.pct }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">用户画像：年龄段</div>
              <div class="panel-sub">生日优先</div>
            </div>

            <div class="bar-chart-wrap">
              <svg class="bar-chart" viewBox="0 0 360 200" preserveAspectRatio="none">
                <g class="bar-grid">
                  <line x1="24" y1="16" x2="24" y2="156" />
                  <line x1="24" y1="156" x2="344" y2="156" />
                </g>

                <g class="bars age">
                  <rect
                    v-for="b in ageBars"
                    :key="b.label"
                    :x="b.x"
                    :y="b.y"
                    :width="b.w"
                    :height="b.h"
                    rx="8"
                    ry="8"
                  />
                </g>

                <g class="x-axis">
                  <text
                    v-for="b in ageBars"
                    :key="`age-${b.label}`"
                    class="x-label"
                    :x="b.x + b.w / 2"
                    y="186"
                    text-anchor="middle"
                  >
                    {{ b.label }}
                  </text>
                </g>
              </svg>
            </div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">用户画像：地区</div>
              <div class="panel-sub">订单收货地址省份（按最近一笔订单）</div>
            </div>

            <div class="hbar-wrap">
              <svg class="hbar" viewBox="0 0 360 220" preserveAspectRatio="none">
                <g class="bar-grid">
                  <line x1="120" y1="18" x2="120" y2="200" />
                  <line x1="120" y1="200" x2="344" y2="200" />
                </g>

                <g class="hbar-items">
                  <template v-for="b in regionBars" :key="b.key">
                    <text class="hbar-label" :x="112" :y="b.ty" text-anchor="end">{{ b.label }}</text>
                    <rect class="hbar-bar region" :x="120" :y="b.y" :width="b.w" :height="b.h" rx="7" ry="7" />
                    <text class="hbar-value" :x="120 + b.w + 6" :y="b.tv">{{ b.value }}</text>
                  </template>
                </g>
              </svg>
            </div>
          </div>

        </div>

        <div class="panel-grid-ops">
          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">库存预警</div>
              <div class="panel-sub">阈值 {{ Number(stockSafe.warningStock || 0) }}</div>
            </div>
            <div v-if="stockItems.length" class="stock-list">
              <div v-for="(item, idx) in stockItems.slice(0, 4)" :key="String(item?.skuId || item?.goodsId || idx)" class="stock-row">
                <div class="stock-thumb">货</div>
                <div class="stock-name">{{ item?.goodsName || '商品' }}<small>{{ item?.skuSpec || '默认规格' }}</small></div>
                <div class="stock-value">库存 {{ Number(item?.available || 0) }}<strong>低于阈值</strong></div>
              </div>
            </div>
            <el-empty v-else-if="!loading" :image-size="54" description="暂无库存预警" />
            <div class="panel-link">查看全部库存预警 &gt;</div>
          </div>

          <div class="panel">
            <div class="panel-head">
              <div class="panel-title">售后 / 退款风险</div>
              <div class="panel-sub">售后/差评风险</div>
            </div>

            <div class="gauge-grid">
              <div class="gauge-card">
                <div class="gauge-title">售后待处理</div>
                <div class="gauge-ring">
                  <svg viewBox="0 0 120 120" class="gauge-svg" aria-hidden="true">
                    <circle class="gauge-track" cx="60" cy="60" r="42" />
                    <circle
                      class="gauge-prog"
                      cx="60"
                      cy="60"
                      r="42"
                      :stroke-dasharray="afterSaleGauge.dasharray"
                      :stroke-dashoffset="afterSaleGauge.dashoffset"
                    />
                  </svg>
                </div>
                <div class="gauge-metric">
                  <div class="gauge-metric-main">{{ afterSaleCount }}</div>
                  <div class="gauge-metric-sub">件</div>
                </div>
              </div>

              <div class="gauge-card">
                <div class="gauge-title">差评待回复</div>
                <div class="gauge-ring">
                  <svg viewBox="0 0 120 120" class="gauge-svg" aria-hidden="true">
                    <circle class="gauge-track" cx="60" cy="60" r="42" />
                    <circle
                      class="gauge-prog warn"
                      cx="60"
                      cy="60"
                      r="42"
                      :stroke-dasharray="badReviewGauge.dasharray"
                      :stroke-dashoffset="badReviewGauge.dashoffset"
                    />
                  </svg>
                </div>
                <div class="gauge-metric">
                  <div class="gauge-metric-main">{{ badReviewCount }}</div>
                  <div class="gauge-metric-sub">条</div>
                </div>
              </div>

              <div class="gauge-card">
                <div class="gauge-title">待发货</div>
                <div class="gauge-ring">
                  <svg viewBox="0 0 120 120" class="gauge-svg" aria-hidden="true">
                    <circle class="gauge-track" cx="60" cy="60" r="42" />
                    <circle
                      class="gauge-prog ok"
                      cx="60"
                      cy="60"
                      r="42"
                      :stroke-dasharray="shipGauge.dasharray"
                      :stroke-dashoffset="shipGauge.dashoffset"
                    />
                  </svg>
                </div>
                <div class="gauge-metric">
                  <div class="gauge-metric-main">{{ shipCount }}</div>
                  <div class="gauge-metric-sub">单</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const days = ref(7)
const dateRange = ref([])

const authRole = computed(() => String(sessionStorage.getItem('shopping_auth_role') || ''))
const merchantId = computed(() => {
  if (authRole.value !== 'merchant') return null
  try {
    const raw = localStorage.getItem('merchantUser')
    const parsed = raw ? JSON.parse(raw) : null
    const id = parsed?.merchantId ?? parsed?.merchant_id ?? parsed?.id
    const num = Number(id)
    return Number.isFinite(num) && num > 0 ? num : null
  } catch (e) {
    return null
  }
})

const kpi = ref({
  rangeDays: 7,
  turnoverTotal: 0,
  turnoverToday: 0,
  paidOrderTotal: 0,
  paidOrderToday: 0,
  pendingShip: 0,
  pendingReceive: 0,
  completed: 0,
  pendingAfterSale: 0
})
const trend = ref([])
const topGoods = ref([])
const updateTime = ref('')
const timeDistribution = ref([])
const categoryShare = ref([])
const categoryMetric = ref('amount')
const customerSplit = ref({})
const stock = ref({})
const afterSaleTodos = ref([])
const badReviews = ref({})
const compare = ref({})
const month = ref({})
const userProfile = ref({})

const toMoney = (v) => {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const hasCustomRange = computed(() => {
  const v = dateRange.value
  return Array.isArray(v) && v.length === 2 && Boolean(v[0]) && Boolean(v[1])
})

const rangeLabel = computed(() => {
  if (hasCustomRange.value) {
    const v = dateRange.value
    return `${v[0]}~${v[1]}`
  }
  return `${days.value}天`
})

const calcDaysByDateRange = (start, end) => {
  const s = typeof start === 'string' ? start : ''
  const e = typeof end === 'string' ? end : ''
  const ds = new Date(`${s}T00:00:00`)
  const de = new Date(`${e}T00:00:00`)
  const ts = ds.getTime()
  const te = de.getTime()
  if (!Number.isFinite(ts) || !Number.isFinite(te)) return 7
  const diff = Math.floor((te - ts) / (24 * 60 * 60 * 1000))
  return Math.max(1, Math.min(90, diff + 1))
}

const pad2 = (n) => String(n).padStart(2, '0')
const formatTime = (v) => {
  const d = v ? new Date(v) : null
  if (!d || Number.isNaN(d.getTime())) return '-'
  const y = d.getFullYear()
  const m = pad2(d.getMonth() + 1)
  const day = pad2(d.getDate())
  const hh = pad2(d.getHours())
  const mm = pad2(d.getMinutes())
  return `${y}-${m}-${day} ${hh}:${mm}`
}

const safeTrend = computed(() => (Array.isArray(trend.value) ? trend.value : []))

const turnovers = computed(() => safeTrend.value.map((x) => Number(x?.turnover ?? 0)).map((x) => (Number.isFinite(x) ? x : 0)))
const maxTurnover = computed(() => (turnovers.value.length ? Math.max(...turnovers.value, 0) : 0))
const minTurnover = computed(() => (turnovers.value.length ? Math.min(...turnovers.value) : 0))
const avgTurnover = computed(() => {
  const list = turnovers.value
  if (!list.length) return 0
  const sum = list.reduce((a, b) => a + (Number.isFinite(b) ? b : 0), 0)
  return sum / Math.max(1, list.length)
})
const rangeOrderCount = computed(() => {
  const list = safeTrend.value.map((x) => Number(x?.orderCount ?? 0)).map((x) => (Number.isFinite(x) ? x : 0))
  return list.reduce((a, b) => a + b, 0)
})

const chartGeometry = computed(() => {
  const w = 720
  const h = 240
  const left = 36
  const right = 16
  const top = 22
  const bottom = 30
  const innerW = w - left - right
  const innerH = h - top - bottom
  const count = Math.max(1, safeTrend.value.length)
  const step = count <= 1 ? 0 : innerW / (count - 1)
  return { w, h, left, right, top, bottom, innerW, innerH, step }
})

const trendYTicks = computed(() => {
  const max = maxTurnover.value
  return [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
    y: 210 - ratio * 188,
    label: max <= 0 ? '0' : Math.round(max * ratio).toLocaleString()
  }))
})

const trendXLabels = computed(() => {
  const list = safeTrend.value
  if (!list.length) return []
  const keepEvery = list.length > 10 ? Math.ceil(list.length / 7) : 1
  return list.map((it, idx) => ({
    key: String(it?.date || idx),
    x: chartGeometry.value.left + chartGeometry.value.step * idx,
    label: String(it?.date || '').slice(5)
  })).filter((_, idx) => idx % keepEvery === 0 || idx === list.length - 1)
})

const dotPoints = computed(() => {
  const g = chartGeometry.value
  const max = maxTurnover.value
  const list = safeTrend.value
  if (!list.length) {
    return [{ key: '0', x: g.left, y: g.top + g.innerH }]
  }
  return list.map((it, idx) => {
    const v = Number(it?.turnover ?? 0)
    const vv = Number.isFinite(v) ? v : 0
    const ratio = max <= 0 ? 0 : vv / max
    const x = g.left + g.step * idx
    const y = g.top + (1 - ratio) * g.innerH
    return { key: String(it?.date || idx), x, y }
  })
})

const linePath = computed(() => {
  const pts = dotPoints.value
  if (!pts.length) return ''
  const first = pts[0]
  const parts = [`M ${first.x.toFixed(2)} ${first.y.toFixed(2)}`]
  for (let i = 1; i < pts.length; i++) {
    const p = pts[i]
    parts.push(`L ${p.x.toFixed(2)} ${p.y.toFixed(2)}`)
  }
  return parts.join(' ')
})

const areaPath = computed(() => {
  const pts = dotPoints.value
  if (!pts.length) return ''
  const g = chartGeometry.value
  const baseY = g.top + g.innerH
  const first = pts[0]
  const parts = [`M ${first.x.toFixed(2)} ${baseY.toFixed(2)}`, `L ${first.x.toFixed(2)} ${first.y.toFixed(2)}`]
  for (let i = 1; i < pts.length; i++) {
    const p = pts[i]
    parts.push(`L ${p.x.toFixed(2)} ${p.y.toFixed(2)}`)
  }
  const last = pts[pts.length - 1]
  parts.push(`L ${last.x.toFixed(2)} ${baseY.toFixed(2)}`, 'Z')
  return parts.join(' ')
})

const maxQty = computed(() => {
  const list = Array.isArray(topGoods.value) ? topGoods.value : []
  const nums = list.map((x) => Number(x?.quantity ?? 0)).filter((x) => Number.isFinite(x))
  return nums.length ? Math.max(...nums, 0) : 0
})

const qtyPercent = (g) => {
  const max = maxQty.value
  const q = Number(g?.quantity ?? 0)
  const qq = Number.isFinite(q) ? q : 0
  if (max <= 0) return 0
  return Math.max(0, Math.min(100, Math.round((qq / max) * 100)))
}

const timeDistributionSafe = computed(() => (Array.isArray(timeDistribution.value) ? timeDistribution.value : []))
const timeAxisLabels = computed(() => {
  return timeDistributionSafe.value.map((it) => {
    const raw = String(it?.label || '').trim()
    if (!raw) return ''
    const idx = raw.indexOf('(')
    return idx > 0 ? raw.slice(0, idx) : raw
  })
})
const timeBars = computed(() => {
  const list = timeDistributionSafe.value
  const n = Math.max(1, list.length)
  const left = 24
  const right = 16
  const top = 18
  const base = 156
  const innerW = 360 - left - right
  const gap = 14
  const w = Math.max(18, Math.floor((innerW - gap * (n - 1)) / n))
  const max = list.map(x => Number(x?.count ?? 0)).filter(x => Number.isFinite(x)).reduce((a, b) => Math.max(a, b), 0)
  const usableH = base - top
  return list.map((it, idx) => {
    const v = Number(it?.count ?? 0)
    const vv = Number.isFinite(v) ? v : 0
    const ratio = max <= 0 ? 0 : vv / max
    const h = Math.max(6, Math.round(ratio * usableH))
    const x = left + idx * (w + gap)
    const y = base - h
    return { label: String(it?.label || idx), x, y, w, h }
  })
})

const categoryShareSafe = computed(() => (Array.isArray(categoryShare.value) ? categoryShare.value : []))
const categoryValueOf = (it) => {
  if (categoryMetric.value === 'quantity') {
    const q = Number(it?.quantity ?? 0)
    return Number.isFinite(q) ? q : 0
  }
  const a = Number(it?.amount ?? 0)
  return Number.isFinite(a) ? a : 0
}
const categoryTotal = computed(() => categoryShareSafe.value.map(categoryValueOf).reduce((a, b) => a + b, 0))
const categoryMetricMain = computed(() => {
  if (categoryMetric.value === 'quantity') {
    return `${Math.round(categoryTotal.value)}`
  }
  return `¥ ${toMoney(categoryTotal.value)}`
})
const categoryMetricSub = computed(() => (categoryMetric.value === 'quantity' ? '区间销量' : '区间销售额'))
const colorPalette = ['#e60012', '#f97316', '#f59e0b', '#fb7185', '#a855f7', '#22c55e', '#0ea5e9', '#64748b']
const categoryLegend = computed(() => {
  const total = categoryTotal.value
  return categoryShareSafe.value.map((it, idx) => {
    const v = categoryValueOf(it)
    const pct = total <= 0 ? 0 : (v / total) * 100
    return {
      name: String(it?.name || '分类'),
      pct: `${pct.toFixed(pct >= 10 ? 0 : 1)}%`,
      color: colorPalette[idx % colorPalette.length]
    }
  })
})
const donut = computed(() => {
  const r = 42
  const c = 2 * Math.PI * r
  return { r, c }
})
const donutSegments = computed(() => {
  const total = categoryTotal.value
  const c = donut.value.c
  let offset = 0
  return categoryShareSafe.value.map((it, idx) => {
    const v = categoryValueOf(it)
    const pct = total <= 0 ? 0 : v / total
    const len = Math.max(0, Math.min(c, pct * c))
    const seg = {
      key: `${String(it?.name || idx)}-${idx}`,
      color: colorPalette[idx % colorPalette.length],
      dasharray: `${len} ${c - len}`,
      dashoffset: `${-offset}`
    }
    offset += len
    return seg
  })
})

const customerSplitSafe = computed(() => (customerSplit.value && typeof customerSplit.value === 'object' ? customerSplit.value : {}))
const customerPct = computed(() => {
  const n = Number(customerSplitSafe.value?.newTurnover ?? 0)
  const o = Number(customerSplitSafe.value?.oldTurnover ?? 0)
  const nn = Number.isFinite(n) ? n : 0
  const oo = Number.isFinite(o) ? o : 0
  const sum = nn + oo
  if (sum <= 0) return { newPct: 0, oldPct: 0 }
  const newPct = Math.round((nn / sum) * 100)
  return { newPct, oldPct: 100 - newPct }
})

const stockSafe = computed(() => (stock.value && typeof stock.value === 'object' ? stock.value : {}))
const stockItems = computed(() => (Array.isArray(stockSafe.value.items) ? stockSafe.value.items : []))
const afterSaleItems = computed(() => (Array.isArray(afterSaleTodos.value) ? afterSaleTodos.value : []))
const badReviewsSafe = computed(() => (badReviews.value && typeof badReviews.value === 'object' ? badReviews.value : {}))
const afterSaleCount = computed(() => (Array.isArray(afterSaleTodos.value) ? afterSaleTodos.value.length : 0))
const badReviewCount = computed(() => {
  const v = Number(badReviewsSafe.value?.count ?? 0)
  return Number.isFinite(v) ? v : 0
})
const shipCount = computed(() => {
  const v = Number(kpi.value?.pendingShip ?? 0)
  return Number.isFinite(v) ? v : 0
})

const stockBars = computed(() => {
  const list = (Array.isArray(stockItems.value) ? stockItems.value : []).slice(0, 6)
  const baseList = list.length
    ? list
    : [
        { goodsName: '—', skuSpec: '', available: 0 },
        { goodsName: '—', skuSpec: '', available: 0 },
        { goodsName: '—', skuSpec: '', available: 0 },
        { goodsName: '—', skuSpec: '', available: 0 }
      ]
  const max = baseList
    .map((x) => Number(x?.available ?? 0))
    .filter((x) => Number.isFinite(x))
    .reduce((a, b) => Math.max(a, b), 0)
  const maxV = Math.max(1, max)
  const left = 120
  const maxW = 210
  const startY = 26
  const row = 28
  const h = 14
  return baseList.map((it, idx) => {
    const v0 = Number(it?.available ?? 0)
    const v = Number.isFinite(v0) ? v0 : 0
    const w = Math.max(0, Math.round((v / maxV) * maxW))
    const goodsName = String(it?.goodsName || '商品')
    const sku = String(it?.skuSpec || '').trim()
    const label = sku ? `${goodsName}·${sku}` : goodsName
    const shortLabel = label.length > 10 ? `${label.slice(0, 10)}…` : label
    const y = startY + idx * row
    return {
      key: `${String(it?.skuId || it?.goodsId || idx)}-${idx}`,
      label: shortLabel,
      value: String(v),
      x: left,
      y,
      w,
      h,
      ty: y + 12,
      tv: y + 12
    }
  })
})

const buildGauge = (value, cap) => {
  const r = 42
  const c = 2 * Math.PI * r
  const v = Math.max(0, Number(value || 0))
  const max = Math.max(1, Number(cap || 1))
  const pct = Math.min(1, v / max)
  const len = pct * c
  return { dasharray: `${len} ${c - len}`, dashoffset: '0' }
}

const afterSaleGauge = computed(() => buildGauge(afterSaleCount.value, 20))
const badReviewGauge = computed(() => buildGauge(badReviewCount.value, 20))
const shipGauge = computed(() => buildGauge(shipCount.value, 30))

const userProfileSafe = computed(() => (userProfile.value && typeof userProfile.value === 'object' ? userProfile.value : {}))
const genderDistSafe = computed(() => {
  const list = userProfileSafe.value?.gender
  return Array.isArray(list) ? list : []
})
const ageDistSafe = computed(() => {
  const list = userProfileSafe.value?.age
  return Array.isArray(list) ? list : []
})
const regionDistSafe = computed(() => {
  const list = userProfileSafe.value?.region
  return Array.isArray(list) ? list : []
})

const genderTotal = computed(() => {
  return genderDistSafe.value
    .map((x) => Number(x?.count ?? 0))
    .filter((x) => Number.isFinite(x))
    .reduce((a, b) => a + b, 0)
})

const genderLegend = computed(() => {
  const total = genderTotal.value
  const colors = ['#e60012', '#f97316', '#64748b']
  const fallback = [
    { name: '男', count: 0 },
    { name: '女', count: 0 },
    { name: '未知', count: 0 }
  ]
  const list = genderDistSafe.value.length ? genderDistSafe.value : fallback
  return list.map((it, idx) => {
    const v0 = Number(it?.count ?? 0)
    const v = Number.isFinite(v0) ? v0 : 0
    const pct = total <= 0 ? 0 : (v / total) * 100
    return { name: String(it?.name || '未知'), pct: `${pct.toFixed(pct >= 10 ? 0 : 1)}%`, color: colors[idx % colors.length] }
  })
})

const genderDonutSegments = computed(() => {
  const total = genderTotal.value
  const c = donut.value.c
  const list = genderLegend.value.map((x) => ({ name: x.name, count: genderDistSafe.value.find((d) => String(d?.name) === x.name)?.count ?? 0, color: x.color }))
  let offset = 0
  return list.map((it, idx) => {
    const v0 = Number(it?.count ?? 0)
    const v = Number.isFinite(v0) ? v0 : 0
    const pct = total <= 0 ? 0 : v / total
    const len = Math.max(0, Math.min(c, pct * c))
    const seg = {
      key: `${String(it?.name || idx)}-${idx}`,
      color: it.color,
      dasharray: `${len} ${c - len}`,
      dashoffset: `${-offset}`
    }
    offset += len
    return seg
  })
})

const ageBars = computed(() => {
  const list = ageDistSafe.value.length
    ? ageDistSafe.value
    : [
        { name: '0-17', count: 0 },
        { name: '18-24', count: 0 },
        { name: '25-34', count: 0 },
        { name: '35-44', count: 0 },
        { name: '45+', count: 0 },
        { name: '未知', count: 0 }
      ]
  const n = Math.max(1, list.length)
  const left = 24
  const right = 16
  const top = 18
  const base = 156
  const innerW = 360 - left - right
  const gap = 10
  const w = Math.max(14, Math.floor((innerW - gap * (n - 1)) / n))
  const max = list.map(x => Number(x?.count ?? 0)).filter(x => Number.isFinite(x)).reduce((a, b) => Math.max(a, b), 0)
  const usableH = base - top
  return list.map((it, idx) => {
    const v = Number(it?.count ?? 0)
    const vv = Number.isFinite(v) ? v : 0
    const ratio = max <= 0 ? 0 : vv / max
    const h = Math.max(6, Math.round(ratio * usableH))
    const x = left + idx * (w + gap)
    const y = base - h
    return { label: String(it?.name || idx), x, y, w, h }
  })
})

const regionBars = computed(() => {
  const list = (Array.isArray(regionDistSafe.value) ? regionDistSafe.value : []).slice(0, 6)
  const baseList = list.length
    ? list
    : [
        { name: '—', count: 0 },
        { name: '—', count: 0 },
        { name: '—', count: 0 },
        { name: '—', count: 0 }
      ]
  const max = baseList
    .map((x) => Number(x?.count ?? 0))
    .filter((x) => Number.isFinite(x))
    .reduce((a, b) => Math.max(a, b), 0)
  const maxV = Math.max(1, max)
  const maxW = 210
  const startY = 26
  const row = 28
  const h = 14
  return baseList.map((it, idx) => {
    const v0 = Number(it?.count ?? 0)
    const v = Number.isFinite(v0) ? v0 : 0
    const w = Math.max(0, Math.round((v / maxV) * maxW))
    const name = String(it?.name || '未知')
    const label = name.length > 10 ? `${name.slice(0, 10)}…` : name
    const y = startY + idx * row
    return {
      key: `${name}-${idx}`,
      label,
      value: String(v),
      y,
      w,
      h,
      ty: y + 12,
      tv: y + 12
    }
  })
})

const fetchJson = async (url) => {
  let res
  try {
    res = await fetch(url, { method: 'GET' })
  } catch (e) {
    throw new Error('无法连接后端服务（请确认商家端后端 8081 已启动）')
  }

  const text = await res.text().catch(() => '')
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch (e) {
    data = null
  }

  if (!res.ok) {
    const msg = String(data?.message || data?.error || '').trim()
    throw new Error(msg || `请求失败（HTTP ${res.status}）`)
  }

  if (!data || typeof data !== 'object') {
    throw new Error('接口返回异常')
  }
  return data
}

const load = async () => {
  if (loading.value || !merchantId.value) return
  loading.value = true
  try {
    const query = new URLSearchParams({ merchantId: String(merchantId.value), days: String(days.value) })
    if (hasCustomRange.value) {
      query.set('startDate', String(dateRange.value[0]))
      query.set('endDate', String(dateRange.value[1]))
    }
    const data = await fetchJson(`/api/data-center/overview?${query.toString()}`)
    kpi.value = data?.kpi || kpi.value
    days.value = Number(data?.kpi?.rangeDays || days.value)
    trend.value = Array.isArray(data?.trend) ? data.trend : []
    topGoods.value = Array.isArray(data?.topGoods) ? data.topGoods : []
    timeDistribution.value = Array.isArray(data?.timeDistribution) ? data.timeDistribution : []
    categoryShare.value = Array.isArray(data?.categoryShare) ? data.categoryShare : []
    customerSplit.value = data?.customerSplit || {}
    stock.value = data?.stock || {}
    afterSaleTodos.value = Array.isArray(data?.afterSaleTodos) ? data.afterSaleTodos : []
    badReviews.value = data?.badReviews || {}
    compare.value = data?.compare || {}
    month.value = data?.month || {}
    userProfile.value = data?.userProfile || {}
    updateTime.value = data?.updateTime || ''
  } catch (e) {
    ElMessage.error(e?.message || '数据中心加载失败')
  } finally {
    loading.value = false
  }
}

const setDays = (v) => {
  if (days.value === v) return
  dateRange.value = []
  days.value = v
  load()
}

const handleRangeChange = () => {
  if (!hasCustomRange.value) return
  const v = dateRange.value
  days.value = calcDaysByDateRange(v[0], v[1])
  load()
}

const csvCell = (value) => {
  const raw = value == null ? '' : String(value)
  const normalized = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const escaped = normalized.replace(/"/g, '""')
  return `"${escaped}"`
}

const downloadTextFile = (filename, content, mime = 'text/plain;charset=utf-8') => {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const exportData = () => {
  const k = kpi.value || {}
  const t = Array.isArray(trend.value) ? trend.value : []
  const top = Array.isArray(topGoods.value) ? topGoods.value : []
  const label = rangeLabel.value

  const lines = []
  lines.push([`数据中心导出（${label}）`].map(csvCell).join(','))
  lines.push('')
  lines.push(['指标', '值'].map(csvCell).join(','))
  lines.push(['今日营业额', toMoney(k.turnoverToday)].map(csvCell).join(','))
  lines.push(['累计营业额', toMoney(k.turnoverTotal)].map(csvCell).join(','))
  lines.push(['今日支付订单', Number(k.paidOrderToday || 0)].map(csvCell).join(','))
  lines.push(['累计支付订单', Number(k.paidOrderTotal || 0)].map(csvCell).join(','))
  lines.push(['待发货', Number(k.pendingShip || 0)].map(csvCell).join(','))
  lines.push(['待收货', Number(k.pendingReceive || 0)].map(csvCell).join(','))
  lines.push(['已完成订单', Number(k.completed || 0)].map(csvCell).join(','))
  lines.push(['售后待处理', Number(k.pendingAfterSale || 0)].map(csvCell).join(','))
  lines.push('')
  lines.push(['日期', '营业额', '支付订单'].map(csvCell).join(','))
  for (const row of t) {
    lines.push([row?.date || '', toMoney(row?.turnover), Number(row?.orderCount || 0)].map(csvCell).join(','))
  }
  lines.push('')
  lines.push(['TOP', '商品', '销量', '销售额'].map(csvCell).join(','))
  for (let i = 0; i < top.length; i++) {
    const g = top[i] || {}
    lines.push([i + 1, g.goodsName || g.goods_name || '商品', Number(g.quantity || 0), toMoney(g.amount)].map(csvCell).join(','))
  }

  const filename = `数据中心_${label}_${formatTime(updateTime.value).replace(/[:\\s]/g, '-')}.csv`
  downloadTextFile(filename, `\ufeff${lines.join('\n')}`, 'text/csv;charset=utf-8')
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.dc-page {
  background: #fff;
  min-height: 520px;
}

.dc-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.dc-main {
  font-size: 16px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.dc-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.dc-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.dc-range {
  width: 280px;
}

.range-tabs {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.tab {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: rgba(255, 255, 255, 0.75);
  color: #334155;
  cursor: pointer;
  user-select: none;
  font-size: 12px;
  font-weight: 700;
}

.tab.active {
  border-color: rgba(230, 0, 18, 0.55);
  background: rgba(230, 0, 18, 0.10);
  color: var(--brand-red);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.kpi-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 14px;
  padding: 12px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.kpi-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 700;
}

.kpi-value {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: 0.3px;
}

.kpi-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #475569;
}

.panel-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 10px;
}

.panel-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}

.panel-grid-2 {
  display: grid;
  grid-template-columns: 1.1fr 1.4fr;
  gap: 10px;
  margin-top: 10px;
}

.panel {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 14px;
  padding: 12px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.panel-sub {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.metric-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.metric-tab {
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: rgba(255, 255, 255, 0.7);
  color: #334155;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.metric-tab.active {
  border-color: rgba(230, 0, 18, 0.55);
  background: rgba(230, 0, 18, 0.10);
  color: var(--brand-red);
}

.chart-wrap {
  height: 240px;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.65);
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.chart {
  width: 100%;
  height: 100%;
}

.grid line {
  stroke: rgba(148, 163, 184, 0.45);
  stroke-width: 1;
}

.line {
  fill: none;
  stroke: rgba(230, 0, 18, 0.95);
  stroke-width: 2.2;
}

.dots circle {
  fill: rgba(230, 0, 18, 0.95);
  stroke: rgba(255, 255, 255, 0.9);
  stroke-width: 1.2;
}

.chart-foot {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.foot-item {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  padding: 10px;
}

.foot-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 700;
}

.foot-value {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 900;
  color: #0f172a;
}

.top-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.top-row {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) 56px 1fr 110px;
  align-items: center;
  gap: 10px;
}

.rank {
  width: 22px;
  height: 22px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(230, 0, 18, 0.10);
  color: var(--brand-red);
  font-weight: 900;
  font-size: 12px;
}

.name {
  font-size: 12px;
  color: #0f172a;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qty {
  font-size: 12px;
  color: #475569;
  font-weight: 800;
  text-align: right;
}

.bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.bar-inner {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(230, 0, 18, 0.95), rgba(249, 115, 22, 0.80));
}

.amt {
  font-size: 12px;
  color: #0f172a;
  font-weight: 900;
  text-align: right;
}

.loading-pad {
  height: 260px;
}

.bar-chart-wrap {
  height: 200px;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.65);
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.bar-chart {
  width: 100%;
  height: 100%;
}

.bar-grid line {
  stroke: rgba(148, 163, 184, 0.45);
  stroke-width: 1;
}

.bars rect {
  fill: rgba(230, 0, 18, 0.85);
}

.bars.age rect {
  fill: rgba(249, 115, 22, 0.85);
}

.x-label {
  font-size: 11px;
  fill: rgba(100, 116, 139, 0.95);
  font-weight: 800;
}

.bar-legend {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.bar-legend-item {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  padding: 10px;
}

.bar-legend-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 800;
}

.bar-legend-value {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
}

.hbar-wrap {
  height: 220px;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.65);
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.hbar {
  width: 100%;
  height: 100%;
}

.hbar-label {
  font-size: 11px;
  fill: rgba(15, 23, 42, 0.85);
  font-weight: 800;
}

.hbar-bar {
  fill: rgba(230, 0, 18, 0.85);
}

.hbar-bar.region {
  fill: rgba(100, 116, 139, 0.70);
}

.hbar-value {
  font-size: 11px;
  fill: rgba(100, 116, 139, 0.95);
  font-weight: 900;
}

.gauge-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.gauge-card {
  background: rgba(255, 255, 255, 0.70);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  padding: 10px;
}

.gauge-title {
  font-size: 12px;
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 10px;
}

.gauge-ring {
  position: relative;
  height: 140px;
  display: grid;
  place-items: center;
}

.gauge-metric {
  margin-top: 4px;
  text-align: center;
}

.gauge-metric-main {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.gauge-metric-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
  font-weight: 800;
}

.gauge-svg {
  width: 140px;
  height: 140px;
}

.gauge-track {
  fill: none;
  stroke: rgba(226, 232, 240, 0.95);
  stroke-width: 16;
}

.gauge-prog {
  fill: none;
  stroke: rgba(230, 0, 18, 0.92);
  stroke-width: 16;
  transform: rotate(-90deg);
  transform-origin: 60px 60px;
}

.gauge-prog.warn {
  stroke: rgba(249, 115, 22, 0.92);
}

.gauge-prog.ok {
  stroke: rgba(34, 197, 94, 0.85);
}

.gauge-center {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
  pointer-events: none;
}

.gauge-num {
  font-size: 18px;
  font-weight: 900;
  color: #0f172a;
}

.gauge-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
  font-weight: 800;
}

.donut {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.donut-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.donut-chart {
  position: relative;
  width: 160px;
  height: 160px;
  display: grid;
  place-items: center;
}

.donut-metric {
  text-align: center;
}

.donut-metric-main {
  font-size: 14px;
  font-weight: 900;
  color: #0f172a;
}

.donut-metric-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
  font-weight: 800;
}

.donut-svg {
  width: 140px;
  height: 140px;
}

.donut-track {
  fill: none;
  stroke: rgba(226, 232, 240, 0.95);
  stroke-width: 16;
}

.donut-seg {
  fill: none;
  stroke-width: 16;
  transform: rotate(-90deg);
  transform-origin: 60px 60px;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  text-align: center;
  pointer-events: none;
}

.donut-total {
  font-size: 14px;
  font-weight: 900;
  color: #0f172a;
}

.donut-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.donut-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.donut-legend-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.donut-legend-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.donut-legend-item .name {
  font-size: 12px;
  color: #0f172a;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.donut-legend-item .pct {
  font-size: 12px;
  color: #64748b;
  font-weight: 800;
}

.split-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.split-card {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  padding: 10px;
}

.split-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 800;
}

.split-value {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 900;
  color: #0f172a;
}

.split-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #475569;
  font-weight: 700;
}

.stackbar-inner {
  height: 12px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(226, 232, 240, 0.9);
  overflow: hidden;
  display: flex;
}

.stackbar .stack {
  height: 100%;
}

.stackbar .new {
  background: rgba(230, 0, 18, 0.9);
}

.stackbar .old {
  background: rgba(249, 115, 22, 0.85);
}

.stackbar-foot {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.todo-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.todo-box {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  padding: 10px;
}

.todo-box-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.todo-box-title {
  font-size: 12px;
  color: #0f172a;
  font-weight: 900;
}

.todo-box-count {
  font-size: 12px;
  color: var(--brand-red);
  font-weight: 900;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-list.small {
  gap: 8px;
}

.todo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.todo-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.todo-title {
  font-size: 12px;
  font-weight: 900;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-sub {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-badge {
  font-size: 12px;
  font-weight: 900;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.85);
  color: #0f172a;
  white-space: nowrap;
}

.todo-badge.warn {
  border-color: rgba(230, 0, 18, 0.25);
  background: rgba(230, 0, 18, 0.06);
  color: var(--brand-red);
}

.panel-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.quick-title {
  font-size: 12px;
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 10px;
}

.quick-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .panel-grid {
    grid-template-columns: 1fr;
  }
  .panel-grid-3 {
    grid-template-columns: 1fr;
  }
  .panel-grid-2 {
    grid-template-columns: 1fr;
  }
  .top-row {
    grid-template-columns: 22px minmax(0, 1fr) 56px 1fr 90px;
  }
  .donut {
    grid-template-columns: 1fr;
  }
  .donut-chart {
    width: 100%;
  }
  .gauge-grid {
    grid-template-columns: 1fr;
  }
}

/* 数据中心宽版经营看板 */
.page { width: 100%; }
.data-center-page { background:#fff; }
:global(.data-center-shell .main-content) {
  padding: 0 0 40px;
}
.dc-page {
  width: calc(100% - 64px);
  max-width: 1360px;
  margin: 0 auto;
  padding: 24px 0 32px;
  border-radius: 0;
  background: #fff;
}
.dc-tools {
  justify-content: flex-start;
  width: 100%;
}
.dc-tools :deep(.el-button) { height: 34px; margin-left: 0; padding: 0 16px; border-radius: 999px; }
.dc-tools :deep(.el-input__wrapper) { border-radius: 999px; background:#fff; box-shadow: 0 0 0 1px #eee inset; }
.tab { padding: 8px 14px; background: #fff; border-color: #eee; font-size: 13px; }
.tab.active { border-color: rgba(230,0,18,.35); background: #fff4f4; color: #e60012; }
.kpi-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-bottom: 16px; }
.kpi-card {
  display: flex; align-items: center; gap: 12px; min-height: 92px; padding: 16px;
  background: #fff; border: 1px solid #eee; border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15,23,42,.055);
}
.kpi-icon {
  width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center;
  border-radius: 50%; background: #fff1f2; color: #e60012; font-size: 16px; font-weight: 900;
}
.kpi-card:nth-child(2) .kpi-icon { background:#fff6ed; color:#f97316; }
.kpi-card:nth-child(3) .kpi-icon { background:#eef5ff; color:#3b82f6; }
.kpi-card:nth-child(4) .kpi-icon { background:#f5f0ff; color:#8b5cf6; }
.kpi-card:nth-child(5) .kpi-icon { background:#eefbf4; color:#22a06b; }
.kpi-label { font-size: 13px; color: #525b6b; }
.kpi-value { margin-top: 5px; font-size: 27px; line-height: 1.15; }
.kpi-sub { margin-top: 6px; color: #8a94a4; }
.panel-grid, .panel-grid-3, .panel-grid-ops { gap: 15px; margin-bottom: 15px; }
.panel-grid { grid-template-columns: minmax(0, 1.45fr) minmax(360px, 1fr); }
.panel-grid-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.panel-grid-ops { display:grid; grid-template-columns: .9fr 1.5fr; }
.panel-grid-ops .gauge-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.panel {
  background:#fff; border:1px solid #eee; border-radius:18px; padding:16px;
  box-shadow:0 12px 32px rgba(15,23,42,.055);
}
.panel-head { margin-bottom: 14px; }
.panel-title { font-size: 14px; color:#172033; }
.panel-sub { color:#8993a4; }
.chart-wrap { height: 270px; background:#fff; }
.chart { overflow:visible; }
.grid line { stroke:#edf0f4; stroke-dasharray:4 4; }
.axis-labels text { fill:#9aa3b2; font-size:9px; }
.line { stroke:#e60012; stroke-width:2.5; }
.dots circle { fill:#e60012; stroke:#fff; stroke-width:2; }
.chart-foot { grid-template-columns:repeat(4,1fr); margin-top:12px; border:1px solid #eee; border-radius:14px; overflow:hidden; }
.foot-item { border:0; border-right:1px solid #eee; border-radius:0; background:#fff; }
.foot-item:last-child { border-right:0; }
.top-list { min-height:270px; }
.top-row { grid-template-columns:24px minmax(90px,1fr) 36px minmax(90px,1fr) 82px; gap:10px; padding:13px 0; }
.rank { border:0; background:#fff1f2; color:#e60012; }
.top-row:nth-child(n+4) .rank { background:#f5f6f8; color:#667085; }
.bar { height:7px; background:#f0f2f5; }
.bar-inner { background:linear-gradient(90deg,#e60012,#ff7a45); }
.panel-link { margin-top:12px; padding-top:12px; border-top:1px solid #f1f2f4; text-align:center; color:#667085; font-size:12px; }
.bar-chart-wrap { height:230px; background:#fff; }
.bar-chart { height:230px; }
.bars rect { fill:#ef3340; }
.bars.age rect { fill:#ff7043; }
.bar-legend { gap:8px; }
.bar-legend-item,.split-card { background:#fff; border:1px solid #eee; border-radius:13px; }
.donut { min-height:220px; align-items:center; }
.donut-chart { min-height:170px; }
.donut-svg { width:160px; height:160px; }
.donut-track { stroke:#f0f2f5; }
.donut-legend-item { grid-template-columns:10px minmax(0,1fr) 44px; }
.stackbar-inner { height:10px; }
.stack.new { background:#e60012; } .stack.old { background:#ff8747; }
.hbar { height:230px; }
.hbar-bar.region { fill:#e60012; }
.stock-list { display:flex; flex-direction:column; gap:9px; }
.stock-row {
  display:grid; grid-template-columns:38px minmax(0,1fr) auto; gap:9px; align-items:center;
  padding:9px; border:1px solid #f0f1f3; border-radius:12px;
}
.stock-thumb { width:36px; height:36px; display:grid; place-items:center; border-radius:9px; background:#f5f6f8; color:#98a2b3; font-size:12px; }
.stock-name { min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#344054; font-size:12px; font-weight:700; }
.stock-name small { display:block; margin-top:3px; color:#98a2b3; font-weight:400; }
.stock-value { text-align:right; color:#667085; font-size:11px; }
.stock-value strong { display:block; margin-top:4px; color:#e60012; }
.gauge-grid { gap:10px; }
.gauge-card { border-color:#eee; border-radius:14px; background:#fff; }
.gauge-prog { stroke:#e60012; } .gauge-prog.warn { stroke:#ff5a36; } .gauge-prog.ok { stroke:#ff8a3d; }

@media (max-width: 1280px) {
  .kpi-grid { grid-template-columns:repeat(3,minmax(0,1fr)); }
}
@media (max-width: 900px) {
  .dc-page { width:calc(100% - 28px); padding:20px 0 24px; }
  .dc-range { width:100%; }
  .kpi-grid,.panel-grid,.panel-grid-3,.panel-grid-ops { grid-template-columns:1fr; }
  .panel-grid-ops .gauge-grid { grid-template-columns:1fr; }
}
</style>
