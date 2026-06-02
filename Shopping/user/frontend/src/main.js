import { createApp, onMounted } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles.css'
import App from './App.vue'
import Home from './views/Home.vue'
import GoodsDetail from './views/GoodsDetail.vue'
import Cart from './views/Cart.vue'
import Addresses from './views/Addresses.vue'
import OrderConfirm from './views/OrderConfirm.vue'
import Orders from './views/Orders.vue'
import OrderDetail from './views/OrderDetail.vue'
import OrderLogistics from './views/OrderLogistics.vue'
import AfterSaleApply from './views/AfterSaleApply.vue'
import AfterSales from './views/AfterSales.vue'
import AfterSaleDetail from './views/AfterSaleDetail.vue'
import ReviewCreate from './views/ReviewCreate.vue'
import CouponCenter from './views/CouponCenter.vue'
import Profile from './views/Profile.vue'
import Recommend from './views/Recommend.vue'
import Rankings from './views/Rankings.vue'
import LiveList from './views/LiveList.vue'
import LiveRoom from './views/LiveRoom.vue'
import Shop from './views/Shop.vue'
import Chat from './views/Chat.vue'
import DataCenter from './views/DataCenter.vue'

const AUTH_ROLE_KEY = 'shopping_auth_role'
const USER_TOKEN_KEY = 'shopping_user_token'

try {
  const url = new URL(window.location.href)
  const token = String(url.searchParams.get('token') || '').trim()
  const role = String(url.searchParams.get('role') || '').trim()
  if (token) {
    localStorage.setItem(USER_TOKEN_KEY, token)
    window.dispatchEvent(new Event('shopping-user-token'))
  }
  if (role === 'user') {
    sessionStorage.setItem(AUTH_ROLE_KEY, 'user')
  }
  if (token || role) {
    url.searchParams.delete('token')
    url.searchParams.delete('role')
    window.history.replaceState({}, '', url.toString())
  }
} catch (e) {
}

if (sessionStorage.getItem(AUTH_ROLE_KEY) === 'user' && !localStorage.getItem(USER_TOKEN_KEY)) {
  sessionStorage.removeItem(AUTH_ROLE_KEY)
}

if (!sessionStorage.getItem(AUTH_ROLE_KEY) && localStorage.getItem(USER_TOKEN_KEY)) {
  sessionStorage.setItem(AUTH_ROLE_KEY, 'user')
}

const LoginRedirect = {
  name: 'LoginRedirect',
  setup() {
    onMounted(() => {
      const url = new URL(window.location.href)
      const redirect = typeof url.searchParams.get('redirect') === 'string' ? url.searchParams.get('redirect') : ''
      const backPath = redirect && redirect.startsWith('/') ? redirect : '/'
      const back = `${window.location.origin}${backPath}`
      const target = new URL('http://localhost:3000/login')
      target.searchParams.set('tab', 'user')
      target.searchParams.set('back', back)
      target.searchParams.set('forceLogin', '1')
      window.location.href = target.toString()
    })
    return () => null
  }
}

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: LoginRedirect },
  { path: '/products', component: Home },
  { path: '/products/:id', component: GoodsDetail },
  { path: '/shops/:id', component: Shop },
  { path: '/messages', component: Chat, meta: { requiresUserAuth: true } },
  { path: '/chat/:id', component: Chat, meta: { requiresUserAuth: true } },
  { path: '/cart', component: Cart, meta: { requiresUserAuth: true } },
  { path: '/coupons', component: CouponCenter },
  { path: '/profile', component: Profile, meta: { requiresUserAuth: true } },
  { path: '/profile/addresses', component: Addresses, meta: { requiresUserAuth: true } },
  { path: '/profile/coupons', component: Profile, meta: { requiresUserAuth: true } },
  { path: '/profile/favorites', component: Profile, meta: { requiresUserAuth: true } },
  { path: '/profile/history', component: Profile, meta: { requiresUserAuth: true } },
  { path: '/profile/reviews', component: Profile, meta: { requiresUserAuth: true } },
  { path: '/order/confirm', component: OrderConfirm, meta: { requiresUserAuth: true } },
  { path: '/orders', component: Orders, meta: { requiresUserAuth: true } },
  { path: '/orders/:id', component: OrderDetail, meta: { requiresUserAuth: true } },
  { path: '/orders/:id/logistics', component: OrderLogistics, meta: { requiresUserAuth: true } },
  { path: '/after-sales', component: AfterSales, meta: { requiresUserAuth: true } },
  { path: '/after-sales/apply', component: AfterSaleApply, meta: { requiresUserAuth: true } },
  { path: '/after-sales/:id', component: AfterSaleDetail, meta: { requiresUserAuth: true } },
  { path: '/reviews/create', component: ReviewCreate, meta: { requiresUserAuth: true } },
  { path: '/recommend', component: Recommend },
  { path: '/rankings', component: Rankings },
  { path: '/live', component: LiveList },
  { path: '/live/:id', component: LiveRoom },
  { path: '/data-center', component: DataCenter }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta?.requiresUserAuth && !localStorage.getItem(USER_TOKEN_KEY)) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
