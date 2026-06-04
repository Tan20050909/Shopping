<template>
  <div class="platform-dashboard">
    <div class="dashboard-shell">
      <section class="platform-hero platform-card">
        <div class="hero-copy">
          <span class="hero-kicker">ALLMART OPERATIONS</span>
          <h1>平台中心</h1>
          <p class="hero-welcome">欢迎回来，AllMart 平台管理员</p>
          <p class="hero-desc">统一掌握商家、订单与平台运营情况，提升管理效率</p>
          <div class="hero-actions">
            <button class="hero-primary" @click="router.push('/report')">查看数据中心</button>
            <button class="hero-secondary" @click="router.push('/merchant')">管理商家</button>
          </div>
        </div>
        <div class="hero-visual" aria-hidden="true">
          <div class="hero-visual-head"><span>今日运营概览</span><span class="live-dot">实时</span></div>
          <div class="hero-visual-value">¥{{ stats.todayGMV || 0 }}</div>
          <div class="hero-bars"><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div>
          <div class="hero-visual-foot"><span>支付订单 {{ stats.todayOrders || 0 }}</span><span>退款率 {{ stats.refundRate || 0 }}%</span></div>
        </div>
      </section>

      <section class="metrics-grid" v-loading="statsLoading">
        <article v-for="item in coreMetrics" :key="item.label" class="metric-card" @click="router.push(item.path)">
          <div class="metric-top"><span class="metric-label">{{ item.label }}</span><span class="metric-mark"></span></div>
          <strong>{{ item.value }}</strong><span class="metric-note">{{ item.note }}</span>
        </article>
      </section>

      <section class="chart-grid">
        <article class="platform-card chart-card trend-card">
          <div class="section-head"><div><h2>平台交易趋势</h2><p>订单数量与销售额走势</p></div><div class="period-switch"><button :class="{ active: trendDays === 7 }" @click="trendDays = 7; loadTrend()">近7天</button><button :class="{ active: trendDays === 30 }" @click="trendDays = 30; loadTrend()">近30天</button></div></div>
          <div ref="orderChartRef" class="chart-canvas" v-loading="trendLoading"></div>
        </article>
        <article class="platform-card chart-card status-card">
          <div class="section-head"><div><h2>订单状态分布</h2><p>当前订单结构占比</p></div></div>
          <div ref="pieChartRef" class="chart-canvas" v-loading="trendLoading"></div>
        </article>
      </section>

      <section class="dual-grid">
        <article class="platform-card list-card">
          <div class="section-head"><div><h2>待处理事项</h2><p>优先处理平台运营任务</p></div><button class="text-button" @click="router.push('/notification')">查看通知</button></div>
          <div class="pending-grid"><button v-for="item in allPendingItems" :key="item.label" @click="router.push(item.path)"><span>{{ item.label }}</span><strong :class="{ alert: item.count > 0 }">{{ item.count }}</strong><el-icon><ArrowRight /></el-icon></button></div>
        </article>
        <article class="platform-card list-card">
          <div class="section-head"><div><h2>风险预警</h2><p>关注临期与异常运营事项</p></div><button class="text-button" @click="router.push('/abnormal')">风险管理</button></div>
          <div class="risk-list"><button v-for="item in slaItems" :key="item.label" @click="router.push(item.path)"><span class="risk-dot" :class="{ alert: item.count > 0 }"></span><span class="risk-copy"><b>{{ item.label }}</b><small>{{ item.desc }}</small></span><strong>{{ item.count }}</strong></button></div>
        </article>
      </section>

      <section class="bottom-grid">
        <article class="platform-card insight-panel">
          <div class="section-head"><div><h2>智能洞察 / 运营公告</h2><p>基于平台数据生成的运营提示</p></div></div>
          <div v-if="insights.length" class="insight-list"><div v-for="ins in insights" :key="ins.insightId" class="insight-item"><span class="insight-level" :class="'level-' + ins.severity">{{ severityMap[ins.severity]?.label || '提示' }}</span><div><b>{{ ins.title }}</b><p>{{ ins.content }}</p></div></div></div>
          <div v-else class="empty-state"><span>暂无新的运营洞察</span><small>平台运行平稳，后续提示将在这里展示</small></div>
        </article>
        <article class="platform-card quick-panel">
          <div class="section-head"><div><h2>快捷入口</h2><p>快速进入常用平台服务</p></div></div>
          <div class="quick-grid"><button v-for="item in quickEntries" :key="item.label" @click="router.push(item.path)"><span class="quick-icon"><el-icon><component :is="item.icon" /></el-icon></span><span>{{ item.label }}</span><el-icon class="quick-arrow"><ArrowRight /></el-icon></button></div>
        </article>
      </section>
    </div>
  </div>
</template>


<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDashboardStats, getOrderTrend, getSalesTrend, getCategoryStats, getInsights } from '../api/common'
import { getAdminInfo, isSuperAdmin, hasPermission, getPermissionCodes } from '../utils/permission'

const router = useRouter()
const stats = ref({})
const trendDays = ref(7)
const insights = ref([])
const orderChartRef = ref(null)
const pieChartRef = ref(null)
const statsLoading = ref(true)
const trendLoading = ref(false)
let orderChart = null, pieChart = null

// ============ 角色识别 ============
const currentRole = computed(() => {
  if (isSuperAdmin()) return 'SUPER_ADMIN'
  const codes = getPermissionCodes()
  // 客服专员：有售后+客服+无商户管理
  if (codes.includes('AFTER_SALE_MGMT') && codes.includes('CHAT_MGMT') && !codes.includes('MERCHANT_MGMT') && !codes.includes('MARKETING_MGMT')) return 'SERVICE'
  // 风控专员：有异常订单+风控用户标签
  if (codes.includes('ORDER_ABNORMAL') && codes.includes('USER_RISK')) return 'RISK_OFFICER'
  // 审计员：有日志+报表+无营销
  if (codes.includes('LOG_VIEW') && codes.includes('REPORT_VIEW') && !codes.includes('MARKETING_MGMT')) return 'AUDITOR'
  // 内容审核员：有评论管理+无订单管理
  if (codes.includes('REVIEW_MGMT') && !codes.includes('ORDER_MGMT')) return 'CONTENT_REVIEWER'
  // 财务专员：有退款+结算+无商户管理
  if (codes.includes('ORDER_REFUND') && codes.includes('FINANCE_MGMT') && !codes.includes('MERCHANT_MGMT')) return 'FINANCE_OFFICER'
  // 运营专员：有商户+商品+营销
  if (codes.includes('MERCHANT_MGMT') && codes.includes('GOODS_MGMT') && codes.includes('MARKETING_MGMT')) return 'OPERATOR'
  // 兜底：按权限推断
  if (codes.includes('MERCHANT_MGMT') || codes.includes('GOODS_MGMT')) return 'OPERATOR'
  if (codes.includes('ORDER_MGMT')) return 'FINANCE_OFFICER'
  return 'OPERATOR'
})

// ============ 角色专属Banner ============
const bannerPresets = {
  SUPER_ADMIN: [
    { tag: '运营日报', title: '智能运营\n管理中台', desc: '看得清、管得住、防得准', link: '/report', bg: 'linear-gradient(135deg, #E60012 0%, #FF2D2D 60%, #FF5252 100%)', icon: '📊' },
    { tag: '风控预警', title: '实时风控\n自动治理', desc: '智能风控引擎，从被动响应走向主动治理', link: '/abnormal', bg: 'linear-gradient(135deg, #C4000F 0%, #E60012 50%, #FF2D2D 100%)', icon: '🛡️' },
    { tag: '全局掌控', title: '一屏总览\n全平台数据', desc: 'GMV、订单、用户、商户全维度掌握', link: '/report', bg: 'linear-gradient(135deg, #A8000D 0%, #C4000F 50%, #E60012 100%)', icon: '📈' },
  ],
  OPERATOR: [
    { tag: '商户入驻', title: '商家审核\n品质把控', desc: '严格审核商户资质，保障平台质量', link: '/merchant', bg: 'linear-gradient(135deg, #FF6B35 0%, #FF8C42 60%, #FFA751 100%)', icon: '🏪' },
    { tag: '商品管理', title: '商品审核\n上下架管控', desc: '违规检测与品质把控', link: '/goods', bg: 'linear-gradient(135deg, #E65100 0%, #FF6D00 50%, #FF9100 100%)', icon: '🔍' },
    { tag: '营销中心', title: '活动运营\n优惠券管理', desc: '灵活配置营销工具，提升转化率', link: '/coupon', bg: 'linear-gradient(135deg, #BF360C 0%, #E65100 50%, #FF6D00 100%)', icon: '🎯' },
  ],
  SERVICE: [
    { tag: '售后处理', title: '售后工单\n快速响应', desc: '48小时内响应，保障用户权益', link: '/after-sale', bg: 'linear-gradient(135deg, #0288D1 0%, #29B6F6 60%, #4FC3F7 100%)', icon: '🔄' },
    { tag: '纠纷仲裁', title: '公正裁决\n证据闭环', desc: '证据链机制+赔付规则引擎', link: '/dispute', bg: 'linear-gradient(135deg, #0277BD 0%, #0288D1 50%, #29B6F6 100%)', icon: '⚖️' },
    { tag: '客服消息', title: '实时沟通\n贴心服务', desc: '即时响应用户咨询，提升满意度', link: '/chat', bg: 'linear-gradient(135deg, #01579B 0%, #0277BD 50%, #0288D1 100%)', icon: '💬' },
  ],
  AUDITOR: [
    { tag: '数据报表', title: '全维度\n数据审计', desc: '销售额、用户、商品报表一应俱全', link: '/report', bg: 'linear-gradient(135deg, #4527A0 0%, #7E57C2 60%, #9575CD 100%)', icon: '📊' },
    { tag: '操作日志', title: '合规审计\n操作追溯', desc: '全链路操作留痕，确保合规', link: '/log', bg: 'linear-gradient(135deg, #311B92 0%, #4527A0 50%, #7E57C2 100%)', icon: '📋' },
    { tag: '平台洞察', title: '智能分析\n风险预警', desc: '数据驱动的运营审计', link: '/report', bg: 'linear-gradient(135deg, #1A237E 0%, #311B92 50%, #4527A0 100%)', icon: '🔍' },
  ],
  RISK_OFFICER: [
    { tag: '异常监控', title: '异常订单\n实时监控', desc: '自动识别刷单、套利、欺诈行为', link: '/abnormal', bg: 'linear-gradient(135deg, #B71C1C 0%, #E53935 60%, #EF5350 100%)', icon: '🚨' },
    { tag: '商户风控', title: '商户信用\n风险预警', desc: '信用评分体系，自动降级高风险商户', link: '/merchant', bg: 'linear-gradient(135deg, #880E4F 0%, #B71C1C 50%, #E53935 100%)', icon: '⚠️' },
    { tag: '纠纷仲裁', title: '证据闭环\n公正裁决', desc: '证据链机制确保公平高效', link: '/dispute', bg: 'linear-gradient(135deg, #4A148C 0%, #880E4F 50%, #B71C1C 100%)', icon: '⚖️' },
  ],
  FINANCE_OFFICER: [
    { tag: '退款审核', title: '退款管理\n资金安全', desc: '严格审核退款，保障资金安全', link: '/after-sale', bg: 'linear-gradient(135deg, #1B5E20 0%, #4CAF50 60%, #66BB6A 100%)', icon: '💰' },
    { tag: '销售报表', title: '销售趋势\n资金流向', desc: '实时掌握平台经营数据', link: '/report', bg: 'linear-gradient(135deg, #0D47A1 0%, #1B5E20 50%, #4CAF50 100%)', icon: '📈' },
    { tag: '订单管理', title: '订单全链路\n资金对账', desc: '订单与退款全流程资金管理', link: '/order', bg: 'linear-gradient(135deg, #004D40 0%, #0D47A1 50%, #1B5E20 100%)', icon: '💳' },
  ],
  CONTENT_REVIEWER: [
    { tag: '商品审核', title: '商品内容\n合规审查', desc: '确保上架商品信息合规', link: '/goods', bg: 'linear-gradient(135deg, #E65100 0%, #FF9800 60%, #FFB74D 100%)', icon: '🔍' },
    { tag: '评论管理', title: '评论审核\n内容治理', desc: '识别违规评论，维护社区氛围', link: '/review', bg: 'linear-gradient(135deg, #BF360C 0%, #E65100 50%, #FF9800 100%)', icon: '📝' },
    { tag: '商户审核', title: '商户资质\n入驻审查', desc: '严格把关商户入驻资质', link: '/merchant', bg: 'linear-gradient(135deg, #8D6E63 0%, #BF360C 50%, #E65100 100%)', icon: '🏪' },
  ],
}

const roleBanners = computed(() => bannerPresets[currentRole.value] || bannerPresets.OPERATOR)

// Banner轮播
const currentBanner = ref(0)
let bannerTimer = null
const startBanner = () => {
  bannerTimer = setInterval(() => {
    currentBanner.value = (currentBanner.value + 1) % roleBanners.value.length
  }, 4000)
}
const pauseBanner = () => clearInterval(bannerTimer)
const resumeBanner = () => startBanner()

// 切换角色时重置banner
watch(currentRole, () => { currentBanner.value = 0 })

// ============ 通用数据 ============
const severityMap = { 1: { icon: '💡', label: '提示' }, 2: { icon: '⚠️', label: '警告' }, 3: { icon: '🚨', label: '紧急' } }

// ============ 超级管理员数据 ============
const adminStats = computed(() => [
  { label: '今日GMV', value: '¥' + (stats.value.todayGMV || 0), icon: 'Money', iconBg: '#FFF5F5', color: '#E60012', path: '/report' },
  { label: '今日订单', value: (stats.value.todayOrders || 0) + '+', icon: 'List', iconBg: '#FFF8F0', color: '#E65100', path: '/order' },
  { label: '注册用户', value: (stats.value.userTotal || 0) + '+', icon: 'User', iconBg: '#F0FFF0', color: '#1B5E20', path: '/user' },
  { label: '在售商品', value: (stats.value.goodsTotal || 0) + '+', icon: 'Goods', iconBg: '#F5F5FF', color: '#4527A0', path: '/goods' },
  { label: '退款率', value: (stats.value.refundRate || 0) + '%', icon: 'Warning', iconBg: '#FFF5F8', color: '#880E4F', path: '/after-sale' },
])

const coreMetrics = computed(() => [
  { label: '平台 GMV', value: '¥' + (stats.value.todayGMV || 0), note: '今日成交总额', path: '/report' },
  { label: '支付订单数', value: stats.value.todayOrders || 0, note: '今日支付订单', path: '/order' },
  { label: '活跃商家', value: stats.value.merchantTotal || stats.value.merchantPendingAudit || 0, note: '平台商户规模', path: '/merchant' },
  { label: '活跃用户', value: stats.value.userTotal || 0, note: '平台注册用户', path: '/user' },
  { label: '退款率', value: (stats.value.refundRate || 0) + '%', note: '平台退款占比', path: '/after-sale' },
])

const quickEntries = [
  { label: '商家管理', icon: 'Shop', path: '/merchant' },
  { label: '订单监督', icon: 'List', path: '/order' },
  { label: '用户管理', icon: 'User', path: '/user' },
  { label: '数据中心', icon: 'DataAnalysis', path: '/report' },
  { label: '客服消息', icon: 'ChatDotRound', path: '/chat' },
]

const allPendingItems = computed(() => [
  { label: '待审核商户', count: stats.value.merchantPendingAudit || 0, path: '/merchant' },
  { label: '待审核商品', count: stats.value.goodsPendingAudit || 0, path: '/goods' },
  { label: '待处理售后', count: stats.value.afterSalePending || 0, path: '/after-sale' },
  { label: '待处理争议', count: stats.value.disputePending || 0, path: '/dispute' },
  { label: '异常订单', count: stats.value.abnormalPending || 0, path: '/abnormal' },
])

const slaItems = computed(() => [
  { label: '售后超48小时', count: stats.value.afterSaleOverdue || 0, path: '/after-sale', desc: '需优先处理，避免用户投诉升级' },
  { label: '售后36小时预警', count: stats.value.afterSaleDueSoon || 0, path: '/after-sale', desc: '即将触达48小时处理线' },
  { label: '纠纷超72小时', count: stats.value.disputeOverdue || 0, path: '/dispute', desc: '建议尽快仲裁或给出处理结论' },
  { label: '低库存商品', count: stats.value.lowStockGoods || 0, path: '/goods', desc: '库存≤50，需提醒商家补货' },
  { label: '平台冻结商户', count: stats.value.frozenMerchants || 0, path: '/merchant', desc: 'status=3，需关注并决定是否解冻' },
  { label: '临期优惠券', count: stats.value.expiringCoupons || 0, path: '/coupon', desc: '7天内到期，需调整营销策略' },
])

// ============ 运营专员数据 ============
const operatorStats = computed(() => [
  { label: '今日GMV', value: '¥' + (stats.value.todayGMV || 0), icon: 'Money', iconBg: '#FFF5F5', color: '#E60012' },
  { label: '今日订单', value: (stats.value.todayOrders || 0) + '+', icon: 'List', iconBg: '#FFF8F0', color: '#E65100' },
  { label: '在售商品', value: (stats.value.goodsTotal || 0) + '+', icon: 'Goods', iconBg: '#F0FFF0', color: '#1B5E20' },
  { label: '待审核商户', value: stats.value.merchantPendingAudit || 0, icon: 'Shop', iconBg: '#FFF5F8', color: '#880E4F' },
])

const marketingCards = [
  { title: '优惠券管理', desc: '创建、管理优惠券及活动', emoji: '🎫', bg: 'linear-gradient(135deg, #FF6B35, #FFA751)', path: '/coupon' },
  { title: '团购活动', desc: '配置限时团购活动', emoji: '🛒', bg: 'linear-gradient(135deg, #E65100, #FF6D00)', path: '/group-buy' },
  { title: '活动管理', desc: '营销活动策划与执行', emoji: '🎉', bg: 'linear-gradient(135deg, #BF360C, #E65100)', path: '/activity' },
  { title: '分类管理', desc: '商品分类与标签配置', emoji: '📂', bg: 'linear-gradient(135deg, #FF8C42, #FFA751)', path: '/category' },
]

const operatorQuick = [
  { label: '商户管理', icon: 'Shop', path: '/merchant' },
  { label: '商品管理', icon: 'Goods', path: '/goods' },
  { label: '优惠券', icon: 'Ticket', path: '/coupon' },
  { label: '团购活动', icon: 'Goods', path: '/group-buy' },
  { label: '分类管理', icon: 'Menu', path: '/category' },
  { label: '数据报表', icon: 'DataAnalysis', path: '/report' },
]

// ============ 客服专员数据 ============
const serviceQuick = [
  { label: '售后管理', icon: 'RefreshRight', path: '/after-sale' },
  { label: '纠纷仲裁', icon: 'Warning', path: '/dispute' },
  { label: '客服消息', icon: 'ChatDotRound', path: '/chat' },
  { label: '订单管理', icon: 'List', path: '/order' },
  { label: '用户查询', icon: 'User', path: '/user' },
  { label: '通知中心', icon: 'Bell', path: '/notification' },
]

// ============ 审核专员数据 ============
const auditorStats = computed(() => [
  { label: '今日GMV', value: '¥' + (stats.value.todayGMV || 0), icon: 'Money', iconBg: '#F3E5F5', color: '#4527A0', path: '/report' },
  { label: '今日订单', value: (stats.value.todayOrders || 0) + '+', icon: 'List', iconBg: '#E8EAF6', color: '#1A237E', path: '/order' },
  { label: '退款率', value: (stats.value.refundRate || 0) + '%', icon: 'Warning', iconBg: '#FFF3E0', color: '#E65100', path: '/after-sale' },
  { label: '操作日志', value: '查看', icon: 'Document', iconBg: '#E0F2F1', color: '#004D40', path: '/log' },
])

const auditorQuick = [
  { label: '数据报表', icon: 'DataAnalysis', path: '/report' },
  { label: '操作日志', icon: 'Document', path: '/log' },
  { label: '订单管理', icon: 'List', path: '/order' },
  { label: '售后管理', icon: 'RefreshRight', path: '/after-sale' },
  { label: '通知中心', icon: 'Bell', path: '/notification' },
]

// ============ 风控专员数据 ============
const riskQuick = [
  { label: '异常订单', icon: 'Warning', path: '/abnormal' },
  { label: '纠纷仲裁', icon: 'ScaleToOriginal', path: '/dispute' },
  { label: '商户管理', icon: 'Shop', path: '/merchant' },
  { label: '用户管理', icon: 'User', path: '/user' },
  { label: '订单管理', icon: 'List', path: '/order' },
  { label: '通知中心', icon: 'Bell', path: '/notification' },
]

// ============ 财务专员数据 ============
const financeStats = computed(() => [
  { label: '今日GMV', value: '¥' + (stats.value.todayGMV || 0), icon: 'Money', iconBg: '#E8F5E9', color: '#1B5E20' },
  { label: '退款率', value: (stats.value.refundRate || 0) + '%', icon: 'Warning', iconBg: '#FFF3E0', color: '#E65100' },
  { label: '待处理售后', value: stats.value.afterSalePending || 0, icon: 'RefreshRight', iconBg: '#FFF5F5', color: '#E60012' },
  { label: '今日订单', value: (stats.value.todayOrders || 0) + '+', icon: 'List', iconBg: '#E3F2FD', color: '#0D47A1' },
])

const financeQuick = [
  { label: '订单管理', icon: 'List', path: '/order' },
  { label: '售后管理', icon: 'RefreshRight', path: '/after-sale' },
  { label: '数据报表', icon: 'DataAnalysis', path: '/report' },
  { label: '通知中心', icon: 'Bell', path: '/notification' },
]

// ============ 内容审核数据 ============
const contentQuick = [
  { label: '商品管理', icon: 'Goods', path: '/goods' },
  { label: '评论管理', icon: 'ChatLineSquare', path: '/review' },
  { label: '商户管理', icon: 'Shop', path: '/merchant' },
  { label: '通知中心', icon: 'Bell', path: '/notification' },
]

// ============ 数据加载 ============
const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch (e) {
    ElMessage.error('加载统计数据失败')
  } finally {
    statsLoading.value = false
  }
  try {
    const insRes = await getInsights(0)
    insights.value = (insRes.data || []).slice(0, 5)
  } catch {
    // insights加载失败不影响主流程
  }
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const [orderRes, salesRes, categoryRes] = await Promise.all([
      getOrderTrend(trendDays.value),
      getSalesTrend(trendDays.value),
      getCategoryStats()
    ])
  if (orderChart) {
    const dates = orderRes.data.dates || []
    const counts = orderRes.data.counts || []
    const amounts = salesRes.data.amounts || []
    orderChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333' } },
      legend: { data: ['订单数', '销售额'], right: 20, top: 0, textStyle: { color: '#999' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: dates.map(d => d.slice(5)), axisLabel: { color: '#999' }, axisLine: { lineStyle: { color: '#eee' } } },
      yAxis: [
        { type: 'value', name: '订单数', axisLabel: { color: '#999' }, splitLine: { lineStyle: { color: '#f5f5f5' } } },
        { type: 'value', name: '销售额(¥)', axisLabel: { color: '#999' }, splitLine: { show: false } }
      ],
      series: [
        { name: '订单数', type: 'bar', data: counts, itemStyle: { color: '#E60012', borderRadius: [4,4,0,0] }, barWidth: '40%' },
        { name: '销售额', type: 'line', yAxisIndex: 1, data: amounts, itemStyle: { color: '#999' }, smooth: true, lineStyle: { width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(153,153,153,0.1)' }, { offset: 1, color: 'rgba(153,153,153,0)' }]) } }
      ]
    })
  }
  if (pieChart) {
    const cat = categoryRes.data
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { color: '#666', fontSize: 12 },
        data: [
          { value: cat.pendingPayment, name: '待支付', itemStyle: { color: '#FFB3B3' } },
          { value: cat.pendingShip, name: '待发货', itemStyle: { color: '#E60012' } },
          { value: cat.pendingReceive, name: '待收货', itemStyle: { color: '#FF5252' } },
          { value: cat.completed, name: '已完成', itemStyle: { color: '#333' } },
          { value: cat.cancelled, name: '已取消', itemStyle: { color: '#bbb' } },
          { value: cat.refunded, name: '售后中', itemStyle: { color: '#FF8A80' } },
        ].filter(d => d.value > 0)
      }]
    })
  }
  } catch (e) {
    ElMessage.error('加载趋势数据失败')
  } finally {
    trendLoading.value = false
  }
}

const initCharts = () => {
  if (orderChartRef.value) {
    orderChart?.dispose()
    orderChart = echarts.init(orderChartRef.value)
  }
  if (pieChartRef.value) {
    pieChart?.dispose()
    pieChart = echarts.init(pieChartRef.value)
  }
}

onMounted(async () => {
  await loadStats()
  setTimeout(async () => {
    initCharts()
    await loadTrend()
  }, 100)
  startBanner()
  window.addEventListener('resize', () => { orderChart?.resize(); pieChart?.resize() })
})

onUnmounted(() => {
  orderChart?.dispose()
  pieChart?.dispose()
  clearInterval(bannerTimer)
})
</script>

<style scoped>
.platform-dashboard{background:#f7f7f7;padding:32px 24px 64px;min-height:100vh}.dashboard-shell{max-width:1280px;margin:0 auto;display:grid;gap:20px}.platform-card{background:#fff;border:1px solid #eee;border-radius:20px;box-shadow:0 8px 26px rgba(0,0,0,.035)}.platform-hero{min-height:270px;padding:42px 48px;display:flex;align-items:center;justify-content:space-between;gap:48px;overflow:hidden}.hero-copy{max-width:620px}.hero-kicker{font-size:11px;font-weight:700;letter-spacing:2px;color:#e60012}.hero-copy h1{font-size:38px;line-height:1.2;margin:12px 0 8px;color:#111}.hero-welcome{font-size:18px;font-weight:600;color:#333;margin-bottom:8px}.hero-desc{font-size:14px;color:#888}.hero-actions{display:flex;gap:10px;margin-top:26px}.hero-actions button,.period-switch button,.text-button{border:0;cursor:pointer}.hero-primary,.hero-secondary{height:40px;padding:0 20px;border-radius:999px;font-weight:600}.hero-primary{background:#e60012;color:#fff}.hero-secondary{background:#f7f7f7;color:#333}.hero-visual{width:360px;padding:24px;border:1px solid #eee;border-radius:18px;background:#fafafa}.hero-visual-head,.hero-visual-foot{display:flex;justify-content:space-between;color:#888;font-size:12px}.live-dot{color:#e60012;background:#ffe8ea;padding:2px 8px;border-radius:999px}.hero-visual-value{font-size:30px;font-weight:750;margin:14px 0;color:#111}.hero-bars{height:66px;display:flex;align-items:flex-end;gap:9px;margin-bottom:14px}.hero-bars i{flex:1;background:#dedede;border-radius:4px 4px 1px 1px}.hero-bars i:nth-child(1){height:30%}.hero-bars i:nth-child(2){height:48%}.hero-bars i:nth-child(3){height:40%}.hero-bars i:nth-child(4){height:70%;background:#e60012}.hero-bars i:nth-child(5){height:58%}.hero-bars i:nth-child(6){height:84%;background:#333}.hero-bars i:nth-child(7){height:72%;background:#e60012}.metrics-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:14px}.metric-card{background:#fff;border:1px solid #eee;border-radius:16px;padding:20px;cursor:pointer;transition:.2s}.metric-card:hover{transform:translateY(-2px);box-shadow:0 10px 24px rgba(0,0,0,.06)}.metric-top{display:flex;justify-content:space-between}.metric-label,.metric-note{font-size:12px;color:#999}.metric-mark{width:7px;height:7px;border-radius:50%;background:#e60012}.metric-card strong{display:block;font-size:25px;color:#111;margin:13px 0 5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.chart-grid{display:grid;grid-template-columns:minmax(0,2fr) minmax(320px,1fr);gap:20px}.chart-card,.list-card,.insight-panel,.quick-panel{padding:24px}.section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:18px}.section-head h2{font-size:17px;color:#111;margin:0}.section-head p{font-size:12px;color:#999;margin-top:3px}.period-switch{display:flex;background:#f7f7f7;padding:3px;border-radius:999px}.period-switch button{background:transparent;color:#888;padding:6px 12px;border-radius:999px;font-size:12px}.period-switch button.active{background:#fff;color:#e60012;box-shadow:0 2px 7px rgba(0,0,0,.08)}.chart-canvas{height:310px}.dual-grid,.bottom-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}.text-button{background:transparent;color:#e60012;font-size:12px}.pending-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:10px}.pending-grid button,.risk-list button,.quick-grid button{border:1px solid #eee;background:#fff;cursor:pointer}.pending-grid button{display:grid;grid-template-columns:1fr auto auto;align-items:center;gap:10px;padding:14px;border-radius:12px;text-align:left;color:#555}.pending-grid strong{font-size:18px;color:#333}.pending-grid strong.alert,.risk-list strong{color:#e60012}.risk-list{display:grid;gap:9px;max-height:230px;overflow:auto}.risk-list button{display:flex;align-items:center;gap:12px;padding:11px 12px;border-radius:12px;text-align:left}.risk-dot{width:7px;height:7px;background:#ccc;border-radius:50%;flex:none}.risk-dot.alert{background:#e60012}.risk-copy{display:grid;gap:2px;flex:1}.risk-copy b{font-size:13px;color:#333}.risk-copy small{font-size:11px;color:#aaa}.risk-list strong{font-size:17px}.bottom-grid{grid-template-columns:minmax(0,1.25fr) minmax(340px,.75fr)}.insight-list{display:grid;gap:10px}.insight-item{display:flex;gap:12px;padding:13px;background:#fafafa;border-radius:12px}.insight-level{height:24px;padding:3px 9px;border-radius:999px;font-size:11px;color:#666;background:#eee;white-space:nowrap}.insight-level.level-2,.insight-level.level-3{color:#e60012;background:#ffe8ea}.insight-item b{font-size:13px;color:#333}.insight-item p{font-size:12px;color:#999;margin-top:3px}.empty-state{height:150px;border:1px dashed #ddd;border-radius:14px;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#777}.empty-state small{color:#aaa;margin-top:4px}.quick-grid{display:grid;gap:9px}.quick-grid button{display:flex;align-items:center;gap:12px;padding:11px;border-radius:12px;color:#444;text-align:left}.quick-icon{width:32px;height:32px;display:grid;place-items:center;border-radius:9px;background:#f7f7f7;color:#e60012}.quick-arrow{margin-left:auto;color:#bbb}@media(max-width:1100px){.metrics-grid{grid-template-columns:repeat(3,1fr)}.platform-hero{padding:34px}.hero-visual{width:320px}}@media(max-width:900px){.platform-hero{align-items:flex-start}.hero-visual{display:none}.chart-grid,.dual-grid,.bottom-grid{grid-template-columns:1fr}.metrics-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:600px){.platform-dashboard{padding:18px 12px 42px}.platform-hero{min-height:auto;padding:26px}.hero-copy h1{font-size:30px}.hero-actions{flex-wrap:wrap}.metrics-grid{grid-template-columns:1fr}.pending-grid{grid-template-columns:1fr}.chart-card,.list-card,.insight-panel,.quick-panel{padding:18px}.chart-canvas{height:270px}.section-head{flex-wrap:wrap}}
</style>
