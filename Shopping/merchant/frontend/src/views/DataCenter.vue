<template>
  <div class="dc-page">
    <div class="dc-head">
      <div class="dc-title">
        <div class="dc-main">数据中心</div>
        <div class="dc-sub">营业额、订单与热销趋势分析</div>
      </div>
      <div class="dc-tools">
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
        <el-button size="small" plain :disabled="loading" @click="exportData">导出数据</el-button>
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">今日营业额</div>
        <div class="kpi-value">¥ {{ toMoney(kpi.turnoverToday) }}</div>
        <div class="kpi-sub">累计：¥ {{ toMoney(kpi.turnoverTotal) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">今日支付订单</div>
        <div class="kpi-value">{{ Number(kpi.paidOrderToday || 0) }}</div>
        <div class="kpi-sub">累计：{{ Number(kpi.paidOrderTotal || 0) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">待发货</div>
        <div class="kpi-value">{{ Number(kpi.pendingShip || 0) }}</div>
        <div class="kpi-sub">待收货：{{ Number(kpi.pendingReceive || 0) }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">售后待处理</div>
        <div class="kpi-value">{{ Number(kpi.pendingAfterSale || 0) }}</div>
        <div class="kpi-sub">已完成订单：{{ Number(kpi.completed || 0) }}</div>
      </div>
    </div>

    <div class="panel-grid">
      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">营业额趋势（{{ rangeLabel }}）</div>
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
            </g>

            <g class="dots">
              <circle v-for="p in dotPoints" :key="p.key" :cx="p.x" :cy="p.y" r="3" />
            </g>
          </svg>
        </div>

        <div class="chart-foot">
          <div class="foot-item">
            <div class="foot-label">峰值</div>
            <div class="foot-value">¥ {{ toMoney(maxTurnover) }}</div>
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
          <div class="panel-title">热销TOP10（{{ rangeLabel }}）</div>
          <div class="panel-sub">按销量排序</div>
        </div>

        <div v-if="topGoods.length" class="top-list">
          <div v-for="(g, idx) in topGoods" :key="String(g.goodsId || g.goods_id || idx)" class="top-row">
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dataCenterApi } from '@/api'
import { getMerchantId } from '@/utils/merchant'

const loading = ref(false)
const days = ref(7)
const dateRange = ref([])

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

const toMoney = (v) => {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
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

const safeTrend = computed(() => (Array.isArray(trend.value) ? trend.value : []))

const turnovers = computed(() => safeTrend.value.map((x) => Number(x?.turnover ?? 0)).map((x) => (Number.isFinite(x) ? x : 0)))
const maxTurnover = computed(() => (turnovers.value.length ? Math.max(...turnovers.value, 0) : 0))
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

const load = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = hasCustomRange.value
      ? await dataCenterApi.overview(getMerchantId(), { days: days.value, startDate: dateRange.value[0], endDate: dateRange.value[1] })
      : await dataCenterApi.overview(getMerchantId(), { days: days.value })
    const data = res?.data || {}
    kpi.value = data.kpi || kpi.value
    days.value = Number(data?.kpi?.rangeDays || days.value)
    trend.value = Array.isArray(data.trend) ? data.trend : []
    topGoods.value = Array.isArray(data.topGoods) ? data.topGoods : []
    updateTime.value = data.updateTime || ''
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '数据中心加载失败')
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

onMounted(load)
</script>

<style scoped>
.dc-page {
  background: radial-gradient(1200px 500px at 30% 0%, rgba(239, 68, 68, 0.16), rgba(255, 255, 255, 0) 60%),
    radial-gradient(900px 420px at 90% 20%, rgba(249, 115, 22, 0.10), rgba(255, 255, 255, 0) 55%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(249, 250, 251, 0.9));
  border-radius: 16px;
  padding: 14px;
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
  border-color: rgba(239, 68, 68, 0.55);
  background: rgba(239, 68, 68, 0.10);
  color: #ef4444;
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
  stroke: rgba(239, 68, 68, 0.95);
  stroke-width: 2.2;
}

.dots circle {
  fill: rgba(239, 68, 68, 0.95);
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
  background: rgba(239, 68, 68, 0.10);
  color: #ef4444;
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
  background: linear-gradient(90deg, rgba(239, 68, 68, 0.95), rgba(249, 115, 22, 0.80));
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

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .panel-grid {
    grid-template-columns: 1fr;
  }
  .top-row {
    grid-template-columns: 22px minmax(0, 1fr) 56px 1fr 90px;
  }
}
</style>
