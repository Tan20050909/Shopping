import { createRouter, createWebHistory } from 'vue-router'

import Login from '@/views/Login.vue'
import Dashboard from '@/views/Dashboard.vue'
import Merchant from '@/views/Merchant.vue'
import MerchantSetting from '@/views/MerchantSetting.vue'
import Finance from '@/views/Finance.vue'
import Goods from '@/views/Goods.vue'
import GoodsDetail from '@/views/GoodsDetail.vue'
import Stock from '@/views/Stock.vue'
import CommentAppeal from '@/views/CommentAppeal.vue'
import Order from '@/views/Order.vue'
import OrderDetailPage from '@/views/OrderDetailPage.vue'
import AfterSale from '@/views/AfterSale.vue'
import AfterSaleDetailPage from '@/views/AfterSaleDetailPage.vue'
import Comment from '@/views/Comment.vue'
import Coupon from '@/views/Coupon.vue'
import Activity from '@/views/Activity.vue'
import Live from '@/views/Live.vue'
import Banner from '@/views/Banner.vue'
import PlatformSupport from '@/views/PlatformSupport.vue'
import MerchantDataCenter from '@/views/DataCenter.vue'

const MERCHANT_LAYOUT = 'merchant'
const AUTH_LAYOUT = 'auth'

function getMerchantUser() {
  try {
    const raw = localStorage.getItem('merchantUser')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') return parsed
  } catch (e) {
    // 解析失败
  }
  return null
}

function hasMerchantSession() {
  const user = getMerchantUser()
  if (!user) return false
  // 必须有真实登录标记，不接受默认演示账号
  if (user.source === 'demo') return false
  const id = Number(user.merchantId || user.merchant_id || user.id)
  return Number.isFinite(id) && id > 0
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { layout: AUTH_LAYOUT }
  },
  {
    path: '/data-center',
    component: MerchantDataCenter,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/merchant',
    name: 'Merchant',
    component: Merchant,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/merchant-setting',
    name: 'MerchantSetting',
    component: MerchantSetting,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/finance',
    name: 'Finance',
    component: Finance,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/goods',
    name: 'Goods',
    component: Goods,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/goods/new',
    name: 'GoodsCreate',
    component: GoodsDetail,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/goods/:id',
    name: 'GoodsDetail',
    component: GoodsDetail,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/stock',
    name: 'Stock',
    component: Stock,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/comment-appeal',
    name: 'CommentAppeal',
    component: CommentAppeal,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/order',
    name: 'Order',
    component: Order,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: OrderDetailPage,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/after-sale',
    name: 'AfterSale',
    component: AfterSale,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/after-sale/:id',
    name: 'AfterSaleDetail',
    component: AfterSaleDetailPage,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: Comment,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/comment',
    redirect: '/chat'
  },
  {
    path: '/coupon',
    name: 'Coupon',
    component: Coupon,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/activity',
    name: 'Activity',
    component: Activity,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/merchant-live',
    name: 'MerchantLive',
    component: Live,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/banner',
    name: 'Banner',
    component: Banner,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  {
    path: '/platform-support',
    name: 'PlatformSupport',
    component: PlatformSupport,
    meta: { layout: MERCHANT_LAYOUT, requiresAuth: true }
  },
  // 根路径和通配路由统一重定向到商家 Dashboard
  { path: '/', redirect: '/dashboard' },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.onError((error) => {
  const msg = String(error?.message || '')
  if (
    msg.includes('Failed to fetch dynamically imported module') ||
    msg.includes('Importing a module script failed') ||
    msg.includes('Loading chunk') ||
    msg.includes('ChunkLoadError')
  ) {
    window.location.reload()
  }
})

router.beforeEach((to, from, next) => {
  // 登录页：已有有效商家会话时跳转 Dashboard
  if (to.path === '/login') {
    const forceLogin = to.query?.forceLogin === '1'
    if (!forceLogin && hasMerchantSession()) {
      next('/dashboard')
      return
    }
    next()
    return
  }

  // 商家路由：需要有效商家会话
  if (to.meta.requiresAuth && !hasMerchantSession()) {
    next({ path: '/login', query: { tab: 'merchant', redirect: to.fullPath } })
    return
  }

  next()
})

export default router
