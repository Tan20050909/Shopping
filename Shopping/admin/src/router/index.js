import { createRouter, createWebHistory } from 'vue-router'
import { hasPermission } from '../utils/permission.js'
import { consumeAdminToken, getUnifiedLoginUrl } from '../utils/authBridge.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '首页', permission: 'DASHBOARD_VIEW' } },
      { path: 'admin', name: 'Admin', component: () => import('../views/AdminManage.vue'), meta: { title: '管理员管理', permission: 'ADMIN_MGMT' } },
      { path: 'role', name: 'Role', component: () => import('../views/RoleManage.vue'), meta: { title: '角色权限', permission: 'SYSTEM_ROLE' } },
      { path: 'user', name: 'User', component: () => import('../views/UserManage.vue'), meta: { title: '用户管理', permission: 'USER_MGMT' } },
      { path: 'merchant', name: 'Merchant', component: () => import('../views/MerchantManage.vue'), meta: { title: '商户管理', permission: 'MERCHANT_MGMT' } },
      { path: 'goods', name: 'Goods', component: () => import('../views/GoodsManage.vue'), meta: { title: '商品管理', permission: 'GOODS_MGMT' } },
      { path: 'order', name: 'Order', component: () => import('../views/OrderManage.vue'), meta: { title: '订单管理', permission: 'ORDER_MGMT' } },
      { path: 'after-sale', name: 'AfterSale', component: () => import('../views/AfterSaleManage.vue'), meta: { title: '售后管理', permission: 'AFTER_SALE_MGMT' } },
      { path: 'after-sale/:id', name: 'AfterSaleDetail', component: () => import('../views/AdminAfterSaleDetail.vue'), meta: { title: '售后监管详情', permission: 'AFTER_SALE_MGMT' } },
      { path: 'dispute', name: 'Dispute', component: () => import('../views/DisputeManage.vue'), meta: { title: '纠纷仲裁', permission: 'DISPUTE_MGMT' } },
      { path: 'abnormal', name: 'Abnormal', component: () => import('../views/AbnormalOrder.vue'), meta: { title: '异常订单', permission: 'ORDER_ABNORMAL' } },
      { path: 'review', name: 'Review', component: () => import('../views/ReviewManage.vue'), meta: { title: '评论管理', permission: 'REVIEW_MGMT' } },
      { path: 'category', name: 'Category', component: () => import('../views/CategoryManage.vue'), meta: { title: '分类管理', permission: 'GOODS_CATEGORY' } },
      { path: 'banner', name: 'Banner', component: () => import('../views/BannerManage.vue'), meta: { title: '轮播图管理', permission: 'CONTENT_BANNER' } },
      { path: 'coupon', name: 'Coupon', component: () => import('../views/CouponManage.vue'), meta: { title: '优惠券管理', permission: 'MARKETING_COUPON' } },
      { path: 'activity', name: 'Activity', component: () => import('../views/ActivityManage.vue'), meta: { title: '活动管理', permission: 'MARKETING_ACTIVITY' } },
      { path: 'group-buy', name: 'GroupBuy', component: () => import('../views/GroupBuyManage.vue'), meta: { title: '团购活动', permission: 'MARKETING_ACTIVITY' } },
      { path: 'chat', name: 'Chat', component: () => import('../views/ChatManage.vue'), meta: { title: '客服消息', permission: 'CHAT_MGMT' } },
      { path: 'log', name: 'Log', component: () => import('../views/OperationLog.vue'), meta: { title: '操作日志', permission: 'LOG_VIEW' } },
      { path: 'config', name: 'Config', component: () => import('../views/SystemConfig.vue'), meta: { title: '系统设置', permission: 'SYSTEM_CONFIG' } },
      { path: 'ai-assistant', name: 'AiAssistant', component: () => import('../views/AiAssistant.vue'), meta: { title: 'AI助手' } },
      { path: 'notification', name: 'Notification', component: () => import('../views/NotificationCenter.vue'), meta: { title: '通知中心' } },
      { path: 'report', name: 'Report', component: () => import('../views/ReportCenter.vue'), meta: { title: '数据报表', permission: 'DATA_MGMT' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  // 如果 URL 带 adminToken，先消费 token（必须等待完成，因为是异步写入 admin_info）
  const hasTokenInUrl = await consumeAdminToken()

  const token = localStorage.getItem('admin_token')

  // 如果直接访问 /login，跳统一登录页（back 指向 /dashboard，避免循环）
  if (to.path === '/login') {
    const loginUrl = getUnifiedLoginUrl(window.location.origin + '/dashboard')
    window.location.href = loginUrl
    return
  }

  // 如果没有 token，跳转统一登录页（带上当前完整 URL 作为 back）
  if (!token) {
    const loginUrl = getUnifiedLoginUrl(window.location.href)
    window.location.href = loginUrl
    return
  }

  // 权限检查
  if (token && to.meta?.permission && !hasPermission(to.meta.permission)) {
    next('/dashboard')
    return
  }

  next()
})

export default router
