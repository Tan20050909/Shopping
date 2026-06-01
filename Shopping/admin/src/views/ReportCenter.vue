<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">数据报表</h2>
        <p class="page-subtitle">平台经营数据可视化分析</p>
      </div>
    </div>
    <div class="content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="销售报表" name="sales">
          <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center">
            <div class="trend-tabs">
              <span :class="{ active: salesDays === 7 }" @click="salesDays = 7; loadSalesReport()">近7天</span>
              <span :class="{ active: salesDays === 30 }" @click="salesDays = 30; loadSalesReport()">近30天</span>
            </div>
          </div>
          <el-row :gutter="16" style="margin-bottom:20px">
            <el-col :span="6" v-for="item in salesCards" :key="item.label">
              <div class="stat-card"><div class="stat-label">{{ item.label }}</div><div class="stat-value" :class="{'brand-red':item.red}">{{ item.value }}</div></div>
            </el-col>
          </el-row>
          <div ref="salesChartRef" style="height:360px"></div>
        </el-tab-pane>
        <el-tab-pane label="用户分析" name="user">
          <div style="margin-bottom:16px">
            <div class="trend-tabs">
              <span :class="{ active: userDays === 7 }" @click="userDays = 7; loadUserAnalysis()">近7天</span>
              <span :class="{ active: userDays === 30 }" @click="userDays = 30; loadUserAnalysis()">近30天</span>
            </div>
          </div>
          <el-row :gutter="16" style="margin-bottom:20px">
            <el-col :span="6"><div class="stat-card"><div class="stat-label">用户总数</div><div class="stat-value brand-red">{{ userData.totalUsers || 0 }}</div></div></el-col>
            <el-col :span="6"><div class="stat-card"><div class="stat-label">期间新增</div><div class="stat-value">{{ userData.periodNewUsers || 0 }}</div></div></el-col>
            <el-col :span="6"><div class="stat-card"><div class="stat-label">日均新增</div><div class="stat-value">{{ userData.avgDailyNew || 0 }}</div></div></el-col>
          </el-row>
          <div ref="userChartRef" style="height:360px"></div>
        </el-tab-pane>
        <el-tab-pane label="经营概览" name="overview">
          <el-row :gutter="16" style="margin-bottom:20px">
            <el-col :span="4" v-for="item in overviewCards" :key="item.label">
              <div class="stat-card" style="text-align:center"><div class="stat-label">{{ item.label }}</div><div class="stat-value" :class="{'brand-red':item.red}" style="font-size:20px">{{ item.value }}</div></div>
            </el-col>
          </el-row>
          <div ref="overviewPieRef" style="height:320px"></div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getSalesReport, getUserAnalysis, getReportOverview } from '../api/common'

const activeTab = ref('sales')
const salesDays = ref(7), userDays = ref(30)
const salesData = ref({}), userData = ref({}), overviewData = ref({})
const salesChartRef = ref(null), userChartRef = ref(null), overviewPieRef = ref(null)
let salesChart = null, userChart = null, overviewPie = null

const salesCards = computed(() => [
  { label: '总销售额', value: '¥' + (salesData.value.totalSales || 0), red: true },
  { label: '总订单数', value: salesData.value.totalOrders || 0 },
  { label: '日均销售额', value: '¥' + (salesData.value.avgDaySales || 0) },
  { label: '客单价', value: '¥' + (salesData.value.avgOrderAmount || 0) },
])

const overviewCards = computed(() => [
  { label: '总订单', value: overviewData.value.totalOrders || 0, red: true },
  { label: '总用户', value: overviewData.value.totalUsers || 0 },
  { label: '总商户', value: overviewData.value.totalMerchants || 0 },
  { label: '总商品', value: overviewData.value.totalGoods || 0 },
  { label: '总销售额', value: '¥' + (overviewData.value.totalSales || 0), red: true },
  { label: '总评价', value: overviewData.value.totalReviews || 0 },
])

const loadSalesReport = async () => {
  const res = await getSalesReport({ days: salesDays.value })
  salesData.value = res.data
  await nextTick()
  if (!salesChart && salesChartRef.value) salesChart = echarts.init(salesChartRef.value)
  if (salesChart) {
    salesChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333' } }, legend: { data: ['销售额', '订单数'], textStyle: { color: '#999' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: (res.data.dates || []).map(d => d.slice(5)), axisLabel: { color: '#999' }, axisLine: { lineStyle: { color: '#eee' } } },
      yAxis: [{ type: 'value', name: '¥', axisLabel: { color: '#999' }, splitLine: { lineStyle: { color: '#f5f5f5' } } }, { type: 'value', name: '数量', axisLabel: { color: '#999' }, splitLine: { show: false } }],
      series: [
        { name: '销售额', type: 'bar', data: res.data.amounts || [], itemStyle: { color: '#E60012', borderRadius: [4,4,0,0] } },
        { name: '订单数', type: 'line', yAxisIndex: 1, data: res.data.orderCounts || [], itemStyle: { color: '#999' }, smooth: true, lineStyle: { width: 2 } }
      ]
    })
  }
}

const loadUserAnalysis = async () => {
  const res = await getUserAnalysis({ days: userDays.value })
  userData.value = res.data
  await nextTick()
  if (!userChart && userChartRef.value) userChart = echarts.init(userChartRef.value)
  if (userChart) {
    userChart.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: (res.data.dates || []).map(d => d.slice(5)), axisLabel: { color: '#999' } },
      yAxis: { type: 'value', name: '新增用户', axisLabel: { color: '#999' }, splitLine: { lineStyle: { color: '#f5f5f5' } } },
      series: [{ type: 'line', data: res.data.newUsers || [], smooth: true, itemStyle: { color: '#E60012' }, lineStyle: { width: 2 }, areaStyle: { color: 'rgba(230,0,18,0.06)' } }]
    })
  }
}

const loadOverview = async () => {
  const res = await getReportOverview({ days: 30 })
  overviewData.value = res.data
  await nextTick()
  if (!overviewPie && overviewPieRef.value) overviewPie = echarts.init(overviewPieRef.value)
  const s = res.data.orderStatusMap || {}
  if (overviewPie) {
    overviewPie.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['35%', '65%'], itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        data: [
          { value: s.pendingPayment || 0, name: '待付款', itemStyle: { color: '#FFB3B3' } },
          { value: s.pendingShip || 0, name: '待发货', itemStyle: { color: '#E60012' } },
          { value: s.pendingReceive || 0, name: '待收货', itemStyle: { color: '#FF5252' } },
          { value: s.completed || 0, name: '已完成', itemStyle: { color: '#333' } },
          { value: s.cancelled || 0, name: '已取消', itemStyle: { color: '#bbb' } },
          { value: s.refunded || 0, name: '已退款', itemStyle: { color: '#FF8A80' } },
        ].filter(d => d.value > 0)
      }]
    })
  }
}

watch(activeTab, (v) => { if (v === 'sales') loadSalesReport(); else if (v === 'user') loadUserAnalysis(); else if (v === 'overview') loadOverview() })
onMounted(() => { loadSalesReport(); window.addEventListener('resize', () => { salesChart?.resize(); userChart?.resize(); overviewPie?.resize() }) })
onUnmounted(() => { salesChart?.dispose(); userChart?.dispose(); overviewPie?.dispose() })
</script>

<style scoped>
.trend-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-soft);
  border-radius: var(--radius-pill);
  padding: 3px;
  border: 1px solid var(--border-light);
}
.trend-tabs span {
  padding: 6px 20px;
  font-size: 13px;
  border-radius: var(--radius-pill);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.trend-tabs span.active {
  background: #E60012;
  color: #fff;
  font-weight: 600;
}
</style>
