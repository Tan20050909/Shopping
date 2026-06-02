<template>
  <div class="workspace-page">
    <!-- 通用Banner轮播 -->
    <section class="banner-section">
      <div class="banner-slider" @mouseenter="pauseBanner" @mouseleave="resumeBanner">
        <div class="banner-slide"
             v-for="(slide, idx) in roleBanners" :key="idx"
             :class="{ active: currentBanner === idx }"
             :style="{ background: slide.bg }"
             @click="router.push(slide.link)">
          <div class="banner-inner">
            <div class="banner-content">
              <div class="banner-tag">{{ slide.tag }}</div>
              <h1 class="banner-title">{{ slide.title }}</h1>
              <p class="banner-desc">{{ slide.desc }}</p>
            </div>
            <div class="banner-visual">
              <div class="banner-visual-circle"></div>
              <span class="banner-icon-text">{{ slide.icon }}</span>
            </div>
          </div>
          <div class="banner-click-hint">点击进入 →</div>
        </div>
      </div>
      <div class="banner-dots">
        <span v-for="(_, idx) in roleBanners" :key="idx"
              :class="{ active: currentBanner === idx }"
              @click="currentBanner = idx"></span>
      </div>
    </section>

    <!-- ==================== 超级管理员工作台 ==================== -->
    <template v-if="currentRole === 'SUPER_ADMIN'">
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>平台运营<span>总览</span></h2><p>全局视角掌控平台关键指标</p></div>
          <div class="stat-grid stat-grid-5" v-loading="statsLoading">
            <div class="stat-card" v-for="item in adminStats" :key="item.label" @click="router.push(item.path)">
              <div class="stat-icon" :style="{ background: item.iconBg }"><el-icon :size="24"><component :is="item.icon" /></el-icon></div>
              <div class="stat-info"><div class="stat-label">{{ item.label }}</div><div class="stat-value" :style="{ color: item.color || '#E60012' }">{{ item.value }}</div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-row">
            <div class="ws-col-8">
              <div class="ws-header"><h2>订单趋势</h2>
                <div class="trend-tabs"><span :class="{ active: trendDays === 7 }" @click="trendDays = 7; loadTrend()">近7天</span><span :class="{ active: trendDays === 30 }" @click="trendDays = 30; loadTrend()">近30天</span></div>
              </div>
              <div class="chart-box"><div ref="orderChartRef" style="height:340px"></div></div>
            </div>
            <div class="ws-col-4">
              <div class="ws-header"><h2>订单状态</h2></div>
              <div class="chart-box"><div ref="pieChartRef" style="height:280px"></div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>待处理<span>紧急事项</span></h2></div>
          <div class="action-grid">
            <div class="action-card" v-for="item in allPendingItems" :key="item.label" @click="router.push(item.path)">
              <div class="action-num" :class="{ danger: item.count > 0 }">{{ item.count }}</div>
              <div class="action-label">{{ item.label }}</div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>SLA<span>风险预警</span></h2></div>
          <div class="sla-grid">
            <div v-for="item in slaItems" :key="item.label" class="sla-card" @click="router.push(item.path)">
              <div class="sla-title">{{ item.label }}</div>
              <div class="sla-value" :class="{ danger: item.count > 0 }">{{ item.count }}</div>
              <div class="sla-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section" v-if="insights.length">
        <div class="ws-inner">
          <div class="ws-header"><h2>智能<span>洞察</span></h2></div>
          <div class="insight-list">
            <div v-for="ins in insights" :key="ins.insightId" class="insight-card">
              <span class="insight-icon">{{ severityMap[ins.severity]?.icon || '💡' }}</span>
              <div class="insight-body">
                <div class="insight-top"><el-tag :type="ins.severity===3?'danger':ins.severity===2?'warning':'info'" size="small" effect="dark" round>{{ severityMap[ins.severity]?.label }}</el-tag><span class="insight-title">{{ ins.title }}</span></div>
                <div class="insight-text">{{ ins.content }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 运营专员工作台 ==================== -->
    <template v-if="currentRole === 'OPERATOR'">
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>运营<span>核心指标</span></h2><p>今日运营数据一目了然</p></div>
          <div class="stat-grid stat-grid-4">
            <div class="stat-card" v-for="item in operatorStats" :key="item.label">
              <div class="stat-icon" :style="{ background: item.iconBg }"><el-icon :size="24"><component :is="item.icon" /></el-icon></div>
              <div class="stat-info"><div class="stat-label">{{ item.label }}</div><div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>审核<span>队列</span></h2><p>待审核商户与商品，优先处理</p></div>
          <div class="queue-row">
            <div class="queue-card queue-merchant" @click="router.push('/merchant')">
              <div class="queue-badge">待审核</div>
              <div class="queue-num">{{ stats.merchantPendingAudit || 0 }}</div>
              <div class="queue-label">商户入驻申请</div>
              <div class="queue-action">立即审核 →</div>
            </div>
            <div class="queue-card queue-goods" @click="router.push('/goods')">
              <div class="queue-badge">待审核</div>
              <div class="queue-num">{{ stats.goodsPendingAudit || 0 }}</div>
              <div class="queue-label">商品上架申请</div>
              <div class="queue-action">立即审核 →</div>
            </div>
            <div class="queue-card queue-coupon" @click="router.push('/coupon')">
              <div class="queue-badge">管理</div>
              <div class="queue-num">{{ stats.expiringCoupons || 0 }}</div>
              <div class="queue-label">临期优惠券</div>
              <div class="queue-action">去管理 →</div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>营销<span>工作台</span></h2></div>
          <div class="marketing-grid">
            <div class="mk-card" v-for="card in marketingCards" :key="card.title" @click="router.push(card.path)">
              <div class="mk-icon" :style="{ background: card.bg }">{{ card.emoji }}</div>
              <div class="mk-info"><div class="mk-title">{{ card.title }}</div><div class="mk-desc">{{ card.desc }}</div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>运营<span>快捷入口</span></h2></div>
          <div class="quick-row">
            <div class="quick-btn" v-for="q in operatorQuick" :key="q.label" @click="router.push(q.path)">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 客服专员工作台 ==================== -->
    <template v-if="currentRole === 'SERVICE'">
      <section class="ws-section service-hero">
        <div class="ws-inner">
          <div class="service-hero-grid">
            <div class="sh-card sh-after-sale" @click="router.push('/after-sale')">
              <div class="sh-emoji">🔄</div>
              <div class="sh-num">{{ stats.afterSalePending || 0 }}</div>
              <div class="sh-label">待处理售后</div>
              <div class="sh-sub">需在48小时内响应</div>
            </div>
            <div class="sh-card sh-dispute" @click="router.push('/dispute')">
              <div class="sh-emoji">⚖️</div>
              <div class="sh-num">{{ stats.disputePending || 0 }}</div>
              <div class="sh-label">待处理纠纷</div>
              <div class="sh-sub">需在72小时内裁决</div>
            </div>
            <div class="sh-card sh-chat" @click="router.push('/chat')">
              <div class="sh-emoji">💬</div>
              <div class="sh-num">—</div>
              <div class="sh-label">客服消息</div>
              <div class="sh-sub">实时响应用户咨询</div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>SLA<span>超时预警</span></h2><p>优先处理即将超时的工单</p></div>
          <div class="sla-grid">
            <div class="sla-card sla-urgent" @click="router.push('/after-sale')">
              <div class="sla-title">售后超48小时</div>
              <div class="sla-value danger">{{ stats.afterSaleOverdue || 0 }}</div>
              <div class="sla-desc">已超时，用户可能投诉升级</div>
            </div>
            <div class="sla-card sla-warn" @click="router.push('/after-sale')">
              <div class="sla-title">售后36小时预警</div>
              <div class="sla-value" style="color:#E6A23C">{{ stats.afterSaleDueSoon || 0 }}</div>
              <div class="sla-desc">即将触达48小时处理线</div>
            </div>
            <div class="sla-card sla-urgent" @click="router.push('/dispute')">
              <div class="sla-title">纠纷超72小时</div>
              <div class="sla-value danger">{{ stats.disputeOverdue || 0 }}</div>
              <div class="sla-desc">建议尽快仲裁或给出结论</div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>订单<span>催发货</span></h2><p>提醒商家及时发货</p></div>
          <div class="remind-row">
            <div class="remind-card" @click="router.push('/order')">
              <div class="remind-icon">📦</div>
              <div class="remind-info"><div class="remind-label">待发货订单</div><div class="remind-hint">前往订单管理提醒商家发货</div></div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>客服<span>快捷操作</span></h2></div>
          <div class="quick-row">
            <div class="quick-btn" v-for="q in serviceQuick" :key="q.label" @click="router.push(q.path)">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 审核专员工作台 ==================== -->
    <template v-if="currentRole === 'AUDITOR'">
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>审计<span>概览</span></h2><p>平台运营合规与数据审计</p></div>
          <div class="stat-grid stat-grid-4">
            <div class="stat-card" v-for="item in auditorStats" :key="item.label" @click="router.push(item.path)">
              <div class="stat-icon" :style="{ background: item.iconBg }"><el-icon :size="24"><component :is="item.icon" /></el-icon></div>
              <div class="stat-info"><div class="stat-label">{{ item.label }}</div><div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-row">
            <div class="ws-col-6">
              <div class="ws-header"><h2>销售额趋势</h2>
                <div class="trend-tabs"><span :class="{ active: trendDays === 7 }" @click="trendDays = 7; loadTrend()">7天</span><span :class="{ active: trendDays === 30 }" @click="trendDays = 30; loadTrend()">30天</span></div>
              </div>
              <div class="chart-box"><div ref="orderChartRef" style="height:300px"></div></div>
            </div>
            <div class="ws-col-6">
              <div class="ws-header"><h2>订单状态分布</h2></div>
              <div class="chart-box"><div ref="pieChartRef" style="height:300px"></div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>审计<span>快捷入口</span></h2></div>
          <div class="quick-row">
            <div class="quick-btn" v-for="q in auditorQuick" :key="q.label" @click="router.push(q.path)">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 风控专员工作台 ==================== -->
    <template v-if="currentRole === 'RISK_OFFICER'">
      <section class="ws-section risk-hero">
        <div class="ws-inner">
          <div class="ws-header"><h2>风控<span>态势感知</span></h2><p>实时监控平台风险指标</p></div>
          <div class="risk-indicator-row">
            <div class="ri-card" @click="router.push('/abnormal')">
              <div class="ri-ring ri-danger"><div class="ri-ring-inner">{{ stats.abnormalPending || 0 }}</div></div>
              <div class="ri-label">异常订单</div>
            </div>
            <div class="ri-card" @click="router.push('/merchant')">
              <div class="ri-ring ri-warn"><div class="ri-ring-inner">{{ stats.frozenMerchants || 0 }}</div></div>
              <div class="ri-label">平台冻结商户</div>
            </div>
            <div class="ri-card" @click="router.push('/goods')">
              <div class="ri-ring ri-info"><div class="ri-ring-inner">{{ stats.lowStockGoods || 0 }}</div></div>
              <div class="ri-label">低库存商品</div>
            </div>
            <div class="ri-card" @click="router.push('/user')">
              <div class="ri-ring ri-warn"><div class="ri-ring-inner">—</div></div>
              <div class="ri-label">风险标签用户</div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>异常<span>订单队列</span></h2><p>需要重点关注和处理</p></div>
          <div class="action-grid">
            <div class="action-card" @click="router.push('/abnormal')">
              <div class="action-num danger">{{ stats.abnormalPending || 0 }}</div>
              <div class="action-label">待处理异常</div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="action-card" @click="router.push('/dispute')">
              <div class="action-num danger">{{ stats.disputePending || 0 }}</div>
              <div class="action-label">待处理纠纷</div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="action-card" @click="router.push('/after-sale')">
              <div class="action-num">{{ stats.afterSaleOverdue || 0 }}</div>
              <div class="action-label">售后超时</div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>风控<span>快捷操作</span></h2></div>
          <div class="quick-row">
            <div class="quick-btn" v-for="q in riskQuick" :key="q.label" @click="router.push(q.path)">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 财务专员工作台 ==================== -->
    <template v-if="currentRole === 'FINANCE_OFFICER'">
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>财务<span>总览</span></h2><p>平台资金与退款数据</p></div>
          <div class="stat-grid stat-grid-4">
            <div class="stat-card" v-for="item in financeStats" :key="item.label">
              <div class="stat-icon" :style="{ background: item.iconBg }"><el-icon :size="24"><component :is="item.icon" /></el-icon></div>
              <div class="stat-info"><div class="stat-label">{{ item.label }}</div><div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-row">
            <div class="ws-col-8">
              <div class="ws-header"><h2>销售额趋势</h2>
                <div class="trend-tabs"><span :class="{ active: trendDays === 7 }" @click="trendDays = 7; loadTrend()">7天</span><span :class="{ active: trendDays === 30 }" @click="trendDays = 30; loadTrend()">30天</span></div>
              </div>
              <div class="chart-box"><div ref="orderChartRef" style="height:320px"></div></div>
            </div>
            <div class="ws-col-4">
              <div class="ws-header"><h2>订单状态</h2></div>
              <div class="chart-box"><div ref="pieChartRef" style="height:280px"></div></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>退款<span>队列</span></h2></div>
          <div class="action-grid">
            <div class="action-card" @click="router.push('/after-sale')">
              <div class="action-num danger">{{ stats.afterSalePending || 0 }}</div>
              <div class="action-label">待处理售后</div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
            <div class="action-card" @click="router.push('/order')">
              <div class="action-num">{{ stats.refundRate || 0 }}%</div>
              <div class="action-label">退款率</div>
              <el-icon class="action-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>财务<span>快捷操作</span></h2></div>
          <div class="quick-row">
            <div class="quick-btn" v-for="q in financeQuick" :key="q.label" @click="router.push(q.path)">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ==================== 内容审核工作台 ==================== -->
    <template v-if="currentRole === 'CONTENT_REVIEWER'">
      <section class="ws-section">
        <div class="ws-inner">
          <div class="ws-header"><h2>内容<span>审核中心</span></h2><p>商品与评论内容合规审查</p></div>
          <div class="review-hero-row">
            <div class="rh-card" @click="router.push('/goods')">
              <div class="rh-emoji">🔍</div>
              <div class="rh-num">{{ stats.goodsPendingAudit || 0 }}</div>
              <div class="rh-label">待审核商品</div>
              <div class="rh-action">去审核 →</div>
            </div>
            <div class="rh-card" @click="router.push('/review')">
              <div class="rh-emoji">📝</div>
              <div class="rh-num">—</div>
              <div class="rh-label">待审核评论</div>
              <div class="rh-action">去审核 →</div>
            </div>
            <div class="rh-card" @click="router.push('/merchant')">
              <div class="rh-emoji">🏪</div>
              <div class="rh-num">{{ stats.merchantPendingAudit || 0 }}</div>
              <div class="rh-label">待审核商户</div>
              <div class="rh-action">去审核 →</div>
            </div>
          </div>
        </div>
      </section>
      <section class="ws-section ws-alt">
        <div class="ws-inner">
          <div class="ws-header"><h2>内容<span>快捷操作</span></h2></div>
          <div class="quick-row">
            <div class="quick-btn" v-for="q in contentQuick" :key="q.label" @click="router.push(q.path)">
              <el-icon :size="20"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

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
  // 需要图表的角色
  if (['SUPER_ADMIN', 'AUDITOR', 'FINANCE_OFFICER'].includes(currentRole.value)) {
    setTimeout(async () => {
      initCharts()
      await loadTrend()
    }, 100)
  }
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
.workspace-page { margin: 0; }

/* ===== Banner ===== */
.banner-section {
  position: relative;
  width: 100%;
  height: 420px;
  overflow: hidden;
}
.banner-slider {
  position: relative;
  width: 100%;
  height: 100%;
}
.banner-slide {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  display: flex;
  align-items: center;
  opacity: 0;
  transition: opacity 0.8s ease;
  cursor: pointer;
}
.banner-slide.active { opacity: 1; }
.banner-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.banner-content { max-width: 520px; color: #fff; }
.banner-tag {
  display: inline-block;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  padding: 6px 20px;
  border-radius: 999px;
  font-size: 13px;
  margin-bottom: 20px;
  letter-spacing: 1px;
}
.banner-title {
  font-size: 44px;
  font-weight: 800;
  line-height: 1.25;
  margin-bottom: 14px;
  white-space: pre-line;
}
.banner-desc {
  font-size: 16px;
  opacity: 0.85;
  line-height: 1.6;
}
.banner-visual {
  position: relative;
  width: 240px;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-visual-circle {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
}
.banner-visual-circle::before {
  content: '';
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  background: rgba(255,255,255,0.06);
}
.banner-icon-text {
  font-size: 72px;
  position: relative;
  z-index: 1;
}
.banner-click-hint {
  position: absolute;
  bottom: 20px;
  right: 32px;
  color: rgba(255,255,255,0.6);
  font-size: 13px;
  letter-spacing: 1px;
}
.banner-slide:hover .banner-click-hint { color: #fff; }
.banner-dots {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}
.banner-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.4);
  cursor: pointer;
  transition: all 0.3s;
}
.banner-dots span.active {
  background: #fff;
  width: 28px;
  border-radius: 5px;
}

/* ===== 通用区块 ===== */
.ws-section { padding: 56px 0; background: #fff; }
.ws-alt { background: #FAFAFA; }
.ws-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.ws-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 28px; flex-wrap: wrap; gap: 12px; }
.ws-header h2 { font-size: 24px; font-weight: 700; margin: 0; }
.ws-header h2 span { color: #E60012; }
.ws-header p { font-size: 14px; color: var(--text-muted, #999); margin: 0; }

/* ===== 统计卡片 ===== */
.stat-grid { display: grid; gap: 20px; }
.stat-grid-5 { grid-template-columns: repeat(5, 1fr); }
.stat-grid-4 { grid-template-columns: repeat(4, 1fr); }
.stat-card {
  background: #FAFAFA;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;
}
.stat-card:hover { background: #fff; border-color: #FFE8EA; box-shadow: 0 4px 16px rgba(230,0,18,0.08); transform: translateY(-2px); }
.stat-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-label { font-size: 13px; color: #999; margin-bottom: 6px; }
.stat-value { font-size: 26px; font-weight: 700; }

/* ===== 图表 ===== */
.ws-row { display: flex; gap: 24px; }
.ws-col-8 { flex: 2; }
.ws-col-4 { flex: 1; }
.ws-col-6 { flex: 1; }
.chart-box {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  padding: 20px;
}
.trend-tabs { display: flex; gap: 4px; background: #fff; border-radius: 999px; padding: 3px; border: 1px solid #f0f0f0; }
.trend-tabs span { padding: 6px 18px; font-size: 13px; border-radius: 999px; cursor: pointer; color: #999; transition: all 0.2s; }
.trend-tabs span.active { background: #E60012; color: #fff; font-weight: 600; }

/* ===== 待处理卡片 ===== */
.action-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.action-card {
  background: #FAFAFA;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.action-card:hover { background: #fff; border-color: #FFE8EA; box-shadow: 0 2px 12px rgba(230,0,18,0.06); transform: translateY(-2px); }
.action-num { font-size: 28px; font-weight: 700; color: #333; }
.action-num.danger { color: #E60012; }
.action-label { font-size: 14px; color: #666; flex: 1; }
.action-arrow { color: #ccc; transition: color 0.2s; }
.action-card:hover .action-arrow { color: #E60012; }

/* ===== SLA ===== */
.sla-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.sla-card {
  border: 1px solid #FFE8EA;
  border-radius: 14px;
  background: #FFF8F8;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.2s;
}
.sla-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(230,0,18,0.06); background: #FFF5F5; }
.sla-card.sla-urgent { border-color: #FFCDD2; background: #FFF5F5; }
.sla-card.sla-warn { border-color: #FFE0B2; background: #FFF8F0; }
.sla-title { font-size: 13px; color: #999; margin-bottom: 8px; }
.sla-value { font-size: 28px; font-weight: 700; color: #333; margin-bottom: 6px; }
.sla-value.danger { color: #E60012; }
.sla-desc { font-size: 12px; line-height: 1.5; color: #bbb; }

/* ===== 洞察 ===== */
.insight-list { display: flex; flex-direction: column; gap: 12px; }
.insight-card {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px 18px;
  background: #FFF8F8; border-radius: 14px; border: 1px solid #FFE8EA;
  transition: all 0.2s;
}
.insight-card:hover { background: #FFF5F5; }
.insight-icon { font-size: 20px; flex-shrink: 0; margin-top: 2px; }
.insight-body { flex: 1; }
.insight-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.insight-title { font-weight: 600; font-size: 14px; }
.insight-text { font-size: 13px; color: #999; line-height: 1.5; }

/* ===== 审核队列（运营专员） ===== */
.queue-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.queue-card {
  border-radius: 20px;
  padding: 32px 28px;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}
.queue-card::before {
  content: '';
  position: absolute;
  top: -30px; right: -30px;
  width: 120px; height: 120px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
}
.queue-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
.queue-merchant { background: linear-gradient(135deg, #FF6B35, #FF8C42); }
.queue-goods { background: linear-gradient(135deg, #E65100, #FF6D00); }
.queue-coupon { background: linear-gradient(135deg, #BF360C, #E65100); }
.queue-badge {
  display: inline-block;
  background: rgba(255,255,255,0.25);
  backdrop-filter: blur(8px);
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 12px;
  margin-bottom: 16px;
}
.queue-num { font-size: 48px; font-weight: 800; margin-bottom: 8px; }
.queue-label { font-size: 16px; opacity: 0.9; margin-bottom: 16px; }
.queue-action {
  display: inline-block;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 999px;
  padding: 8px 20px;
  font-size: 14px;
  transition: all 0.2s;
}
.queue-card:hover .queue-action { background: #fff; color: #E60012; border-color: #fff; }

/* ===== 营销卡片（运营专员） ===== */
.marketing-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.mk-card {
  background: #FAFAFA;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;
}
.mk-card:hover { background: #fff; border-color: #FFE8EA; box-shadow: 0 4px 16px rgba(230,0,18,0.08); transform: translateY(-2px); }
.mk-icon {
  width: 56px; height: 56px;
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}
.mk-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.mk-desc { font-size: 13px; color: #999; }

/* ===== 客服Hero ===== */
.service-hero { background: linear-gradient(135deg, #E3F2FD, #F5F5F5) !important; }
.service-hero-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.sh-card {
  background: #fff;
  border-radius: 20px;
  padding: 32px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}
.sh-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }
.sh-after-sale:hover { border-color: #29B6F6; }
.sh-dispute:hover { border-color: #0288D1; }
.sh-chat:hover { border-color: #0277BD; }
.sh-emoji { font-size: 40px; margin-bottom: 12px; }
.sh-num { font-size: 42px; font-weight: 800; color: #E60012; margin-bottom: 8px; }
.sh-label { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
.sh-sub { font-size: 13px; color: #999; }

/* ===== 催发货 ===== */
.remind-row { display: flex; flex-direction: column; gap: 12px; }
.remind-card {
  background: #FAFAFA;
  border-radius: 14px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.remind-card:hover { background: #fff; border-color: #FFE8EA; box-shadow: 0 2px 12px rgba(230,0,18,0.06); }
.remind-icon { font-size: 32px; }
.remind-info { flex: 1; }
.remind-label { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.remind-hint { font-size: 13px; color: #999; }

/* ===== 风控Hero ===== */
.risk-hero { background: linear-gradient(135deg, #FFEBEE, #F5F5F5) !important; }
.risk-indicator-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.ri-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.ri-card:hover { transform: translateY(-4px); }
.ri-ring {
  width: 110px; height: 110px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 12px;
  position: relative;
}
.ri-ring::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 4px solid transparent;
}
.ri-danger { background: rgba(230,0,18,0.08); }
.ri-danger::before { border-color: rgba(230,0,18,0.3); }
.ri-warn { background: rgba(230,162,60,0.08); }
.ri-warn::before { border-color: rgba(230,162,60,0.3); }
.ri-info { background: rgba(69,39,160,0.08); }
.ri-info::before { border-color: rgba(69,39,160,0.3); }
.ri-ring-inner {
  font-size: 32px;
  font-weight: 800;
  color: #333;
}
.ri-danger .ri-ring-inner { color: #E60012; }
.ri-warn .ri-ring-inner { color: #E6A23C; }
.ri-info .ri-ring-inner { color: #4527A0; }
.ri-label { font-size: 14px; color: #666; font-weight: 500; }

/* ===== 内容审核Hero ===== */
.review-hero-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.rh-card {
  background: #FAFAFA;
  border-radius: 20px;
  padding: 36px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}
.rh-card:hover { background: #fff; border-color: #FFE8EA; box-shadow: 0 4px 20px rgba(230,0,18,0.1); transform: translateY(-4px); }
.rh-emoji { font-size: 44px; margin-bottom: 12px; }
.rh-num { font-size: 40px; font-weight: 800; color: #E60012; margin-bottom: 8px; }
.rh-label { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.rh-action { font-size: 14px; color: #E60012; font-weight: 500; }

/* ===== 快捷按钮 ===== */
.quick-row { display: flex; flex-wrap: wrap; gap: 12px; }
.quick-btn {
  display: flex; align-items: center; gap: 8px;
  background: #FAFAFA;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 12px 24px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-btn:hover { background: #fff; border-color: #E60012; color: #E60012; box-shadow: 0 2px 8px rgba(230,0,18,0.08); }

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .banner-section { height: 340px; }
  .banner-title { font-size: 32px; }
  .banner-visual { width: 160px; height: 160px; }
  .banner-icon-text { font-size: 48px; }
  .stat-grid-5 { grid-template-columns: repeat(3, 1fr); }
  .stat-grid-4 { grid-template-columns: repeat(2, 1fr); }
  .ws-row { flex-direction: column; }
  .ws-col-4, .ws-col-6 { width: 100%; }
  .sla-grid { grid-template-columns: repeat(2, 1fr); }
  .queue-row { grid-template-columns: 1fr; }
  .marketing-grid { grid-template-columns: repeat(2, 1fr); }
  .service-hero-grid { grid-template-columns: 1fr; }
  .risk-indicator-row { grid-template-columns: repeat(2, 1fr); }
  .review-hero-row { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .banner-section { height: 280px; }
  .banner-inner { flex-direction: column; text-align: center; }
  .banner-title { font-size: 24px; }
  .banner-visual { display: none; }
  .stat-grid-5 { grid-template-columns: repeat(2, 1fr); }
  .stat-grid-4 { grid-template-columns: 1fr 1fr; }
  .action-grid { grid-template-columns: repeat(2, 1fr); }
  .sla-grid { grid-template-columns: 1fr; }
  .ws-section { padding: 40px 0; }
}
</style>
