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

import UserHome from '@user/views/Home.vue'
import UserGoodsDetail from '@user/views/GoodsDetail.vue'
import Cart from '@user/views/Cart.vue'
import Addresses from '@user/views/Addresses.vue'
import OrderConfirm from '@user/views/OrderConfirm.vue'
import Orders from '@user/views/Orders.vue'
import UserOrderDetail from '@user/views/OrderDetail.vue'
import OrderLogistics from '@user/views/OrderLogistics.vue'
import AfterSaleApply from '@user/views/AfterSaleApply.vue'
import AfterSales from '@user/views/AfterSales.vue'
import AfterSaleDetail from '@user/views/AfterSaleDetail.vue'
import ReviewCreate from '@user/views/ReviewCreate.vue'
import CouponCenter from '@user/views/CouponCenter.vue'
import Profile from '@user/views/Profile.vue'
import Recommend from '@user/views/Recommend.vue'
import Rankings from '@user/views/Rankings.vue'
import LiveList from '@user/views/LiveList.vue'
import LiveRoom from '@user/views/LiveRoom.vue'
import Shop from '@user/views/Shop.vue'
import Chat from '@user/views/Chat.vue'
import UserDataCenter from '@user/views/DataCenter.vue'

const USER_LAYOUT = 'user'
const MERCHANT_LAYOUT = 'merchant'
const AUTH_LAYOUT = 'auth'

function getAuthRole() {
  return String(sessionStorage.getItem('shopping_auth_role') || '')
}

function hasMerchantSession() {
  return getAuthRole() === 'merchant' && Boolean(localStorage.getItem('merchantUser'))
}

function hasUserSession() {
  return getAuthRole() === 'user' && Boolean(localStorage.getItem('shopping_user_token'))
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { layout: AUTH_LAYOUT }
  },
  { path: '/', component: UserHome, meta: { layout: USER_LAYOUT } },
  { path: '/products', component: UserHome, meta: { layout: USER_LAYOUT } },
  { path: '/products/:id', component: UserGoodsDetail, meta: { layout: USER_LAYOUT } },
  { path: '/shops/:id', component: Shop, meta: { layout: USER_LAYOUT } },
  { path: '/messages', component: Chat, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/chat/:id', component: Chat, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/cart', component: Cart, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/coupons', component: CouponCenter, meta: { layout: USER_LAYOUT } },
  { path: '/profile', component: Profile, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/profile/addresses', component: Addresses, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/profile/coupons', component: Profile, meta: { layout: USER_LAYOUT } },
  { path: '/profile/favorites', component: Profile, meta: { layout: USER_LAYOUT } },
  { path: '/profile/history', component: Profile, meta: { layout: USER_LAYOUT } },
  { path: '/profile/reviews', component: Profile, meta: { layout: USER_LAYOUT } },
  { path: '/order/confirm', component: OrderConfirm, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/orders', component: Orders, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/orders/:id', component: UserOrderDetail, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/orders/:id/logistics', component: OrderLogistics, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/after-sales', component: AfterSales, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/after-sales/apply', component: AfterSaleApply, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/after-sales/:id', component: AfterSaleDetail, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/reviews/create', component: ReviewCreate, meta: { layout: USER_LAYOUT, requiresUserAuth: true } },
  { path: '/recommend', component: Recommend, meta: { layout: USER_LAYOUT } },
  { path: '/rankings', component: Rankings, meta: { layout: USER_LAYOUT } },
  { path: '/live', component: LiveList, meta: { layout: USER_LAYOUT } },
  { path: '/live/:id', component: LiveRoom, meta: { layout: USER_LAYOUT } },
  { path: '/data-center', component: UserDataCenter, meta: { layout: USER_LAYOUT } },
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
  { path: '/:pathMatch(.*)*', redirect: '/' }
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
  const forcedRole = typeof to.query?.role === 'string' ? String(to.query.role) : ''
  if (forcedRole === 'user' || forcedRole === 'merchant') {
    sessionStorage.setItem('shopping_auth_role', forcedRole)
  }
  if (to.meta.layout === USER_LAYOUT && to.meta.requiresUserAuth && !hasUserSession()) {
    next({ path: '/login', query: { tab: 'user', redirect: to.fullPath } })
    return
  }

  if (to.meta.layout === MERCHANT_LAYOUT && to.meta.requiresAuth && !hasMerchantSession()) {
    next({ path: '/login', query: { tab: 'merchant', redirect: to.fullPath } })
    return
  }

  if (to.path === '/login') {
    const tab = typeof to.query?.tab === 'string' ? String(to.query.tab) : ''
    const role = getAuthRole()
    if (tab !== 'user' && role === 'merchant' && hasMerchantSession()) {
      next('/dashboard')
      return
    }
    if (role === 'user' || hasUserSession()) {
      next('/')
      return
    }
  }

  next()
})

export default router
