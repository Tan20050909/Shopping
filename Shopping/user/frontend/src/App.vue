<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ShoppingCart, ArrowUp } from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const keyword = ref('')
const headerAvatar = computed(() => user.profile?.avatar || 'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-user')
const standalone = computed(() => route.meta?.standalone || route.query.standalone === '1')
const brandLogo = new URL('../public/brand-assets/allmart-logo-full.png', import.meta.url).href
const authRole = ref('')
const isUserRole = computed(() => authRole.value === 'user')
const isMerchantRole = computed(() => authRole.value === 'merchant')
const merchantProfile = computed(() => {
  try {
    if (!isMerchantRole.value) return null
    const raw = localStorage.getItem('merchantUser')
    const parsed = raw ? JSON.parse(raw) : null
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch (e) {
    return null
  }
})
const merchantName = computed(() => String(merchantProfile.value?.merchantName || merchantProfile.value?.username || '商家').trim())
const merchantAvatar = computed(() => String(merchantProfile.value?.avatarUrl || merchantProfile.value?.shopLogo || '').trim())
const showUserAccount = computed(() => isUserRole.value && user.isLoggedIn)
const showMerchantAccount = computed(() => isMerchantRole.value && Boolean(merchantProfile.value))
const showLoginLink = computed(() => !showUserAccount.value && !showMerchantAccount.value)

const navItems = computed(() => {
  const base = [
    { label: '首页', to: '/' },
    { label: '商品分类', to: '/products' },
    { label: '精选推荐', to: '/recommend' },
    { label: '热门榜单', to: '/rankings' },
    { label: '直播', to: '/live' },
    { label: '优惠券', to: '/coupons' },
    { label: '消息中心', to: '/messages', newTab: true }
  ]
  if (isMerchantRole.value) return [...base, { label: '数据中心', to: '/data-center' }]
  return [
    ...base,
    { label: '购物车', to: '/cart' },
    { label: '我的订单', to: '/orders' },
    { label: '我的售后', to: '/after-sales' },
    { label: '个人中心', to: '/profile' }
  ]
})

function syncKeyword() {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
}

function syncAuthRole() {
  authRole.value = String(sessionStorage.getItem('shopping_auth_role') || '')
}

function goSearch() {
  router.push({
    path: '/products',
    query: keyword.value ? { keyword: keyword.value } : {}
  })
}

function backTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openMessageCenter() {
  const target = router.resolve(
    isMerchantRole.value
      ? { path: '/chat', query: { standalone: '1', shell: '1', role: 'merchant' } }
      : { path: '/messages', query: { standalone: '1', shell: '1', role: 'user' } }
  )
  window.open(target.href, '_blank', 'noopener,noreferrer')
}

function logoutMerchant() {
  localStorage.removeItem('merchantUser')
  sessionStorage.removeItem('shopping_auth_role')
  router.push('/')
}

watch(() => route.fullPath, syncKeyword, { immediate: true })
watch(() => route.fullPath, syncAuthRole, { immediate: true })
onMounted(() => {
  if (!sessionStorage.getItem('shopping_auth_role') && localStorage.getItem('shopping_user_token')) {
    sessionStorage.setItem('shopping_auth_role', 'user')
  }
  syncAuthRole()
  user.syncToken()
  if (isUserRole.value && user.isLoggedIn) {
    user.loadMe().catch(() => {})
  }
  window.addEventListener('shopping-user-token', () => {
    user.syncToken()
    syncAuthRole()
    if (isUserRole.value && user.isLoggedIn) {
      user.loadMe().catch(() => {})
    } else {
      user.profile = null
    }
  })
})
</script>

<template>
  <div class="app-shell">
    <header v-if="!standalone" class="site-header">
      <div class="container site-header-inner">
        <router-link class="brand-lockup" to="/">
          <img class="brand-logo-img" :src="brandLogo" alt="All Mart" />
          <span class="brand-name">AllMart</span>
        </router-link>

        <nav class="site-nav">
          <template v-for="item in navItems" :key="item.to">
            <a
              v-if="item.newTab"
              class="site-nav-link"
              href="#"
              @click.prevent="openMessageCenter"
            >
              {{ item.label }}
            </a>
            <router-link
              v-else
              class="site-nav-link"
              :class="{ active: route.path === item.to || (item.to !== '/' && route.path.startsWith(item.to)) }"
              :to="item.to"
            >
              {{ item.label }}
            </router-link>
          </template>
        </nav>

        <div class="site-actions">
          <el-input
            v-model="keyword"
            class="site-search"
            placeholder="搜索好物"
            clearable
            @keyup.enter="goSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <router-link v-if="!isMerchantRole" class="header-icon-link" to="/cart" aria-label="购物车">
            <el-icon><ShoppingCart /></el-icon>
          </router-link>
          <div class="auth-actions">
            <router-link v-if="showUserAccount" class="user-chip" to="/profile">
              <img
                :src="headerAvatar"
                alt="用户头像"
              />
              <span>{{ user.profile?.nickname || '我的' }}</span>
            </router-link>
            <button v-if="showUserAccount" class="ghost-link header-logout-link" @click="user.logout()">退出</button>

            <router-link v-if="showMerchantAccount" class="user-chip" to="/dashboard">
              <img
                v-if="merchantAvatar"
                :src="merchantAvatar"
                alt="商家头像"
              />
              <span>{{ merchantName || '商家' }}</span>
            </router-link>
            <button v-if="showMerchantAccount" class="ghost-link header-logout-link" @click="logoutMerchant">退出</button>

            <router-link v-if="showLoginLink" class="header-login-link" to="/login">登录</router-link>
          </div>
        </div>
      </div>
    </header>

    <router-view />

    <footer v-if="!standalone" class="site-footer">
      <div class="container site-footer-inner">
        <div class="stack">
          <img class="footer-logo-img" :src="brandLogo" alt="All Mart" />
          <span class="brand-name">AllMart</span>
          <span class="muted">你的全品类购物新选择</span>
        </div>
        <div class="footer-info">
          <span>客服热线：400-000-0000</span>
          <span>售后服务：400-000-0001</span>
          <span>技术栈：Vue 3 / Element Plus / Spring Boot</span>
          <span>© 2026 AllMart</span>
        </div>
        <button class="back-top" @click="backTop" aria-label="返回顶部">
          <el-icon><ArrowUp /></el-icon>
        </button>
      </div>
    </footer>
  </div>
</template>
