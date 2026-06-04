<template>
  <div class="brand-layout">
    <!-- Header -->
    <header class="brand-header" :class="{ sticky: isSticky }">
      <div class="header-inner">
        <!-- Logo -->
        <div class="header-logo" @click="router.push('/dashboard')">
          <img src="/brand-assets/allmart-logo-full.png" alt="AllMart" class="logo-image" />
          <span class="header-platform-name">平台中心</span>
        </div>

        <!-- Nav - 分组下拉式导航 -->
        <nav class="header-nav">
          <!-- 核心入口始终显示 -->
          <div v-for="item in topNavItems" :key="item.path"
               :class="['nav-item', { active: isActive(item.path) }]"
               @click="handleNav(item)">
            <span>{{ item.label }}</span>
            <div class="nav-underline"></div>
          </div>

          <!-- 分组下拉菜单 -->
          <div v-for="group in navGroups" :key="group.label"
               class="nav-dropdown" @mouseenter="hoverGroup = group.label" @mouseleave="hoverGroup = null">
            <div :class="['nav-item nav-item-group', { active: group.active, 'nav-open': hoverGroup === group.label }]">
              <span>{{ group.label }}</span>
              <el-icon :size="11" style="margin-left:2px;transition:transform 0.2s"><ArrowDown /></el-icon>
              <div class="nav-underline"></div>
            </div>
            <transition name="dropdown-fade">
              <div v-if="hoverGroup === group.label" class="nav-dropdown-menu">
                <div v-for="item in group.items" :key="item.path"
                     :class="['dropdown-item', { 'dropdown-active': isActive(item.path) }]"
                     @click="handleNav(item)">
                  <span>{{ item.label }}</span>
                  <el-icon v-if="isActive(item.path)" :size="12" color="#E60012"><Select /></el-icon>
                </div>
              </div>
            </transition>
          </div>
        </nav>

        <!-- Right -->
        <div class="header-right">
          <div class="header-search">
            <el-icon :size="15"><Search /></el-icon>
            <input v-model="searchKeyword" placeholder="搜索商家/订单" class="search-input" @keyup.enter="handleSearch" />
          </div>
          <button type="button" class="header-search-btn" aria-label="搜索" @click="handleSearch"><el-icon :size="16"><Search /></el-icon></button>
          <el-badge :value="unreadCount" :hidden="unreadCount===0" :max="99">
            <div class="header-icon-btn" @click="router.push('/notification')">
              <el-icon :size="18"><Bell /></el-icon>
            </div>
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="header-user">
              <el-avatar :size="32" :src="adminAvatar" style="background:#E60012;font-weight:700;font-size:14px">
                {{ (adminInfo.realName || adminInfo.username || 'A')[0] }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="dashboard"><el-icon><DataAnalysis /></el-icon>仪表盘</el-dropdown-item>
                <el-dropdown-item command="ai"><el-icon><MagicStick /></el-icon>AI助手</el-dropdown-item>
                <el-dropdown-item command="notification"><el-icon><Bell /></el-icon>通知中心</el-dropdown-item>
                <el-dropdown-item v-if="hasPermission('ADMIN_MGMT')" command="admin"><el-icon><User /></el-icon>员工身份</el-dropdown-item>

                <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- Mobile Menu Button -->
    <div class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen">
      <el-icon :size="22"><component :is="mobileMenuOpen ? 'Close' : 'Menu'" /></el-icon>
    </div>

    <!-- Mobile Menu Overlay -->
    <transition name="fade">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false">
        <div class="mobile-menu" @click.stop>
        <div v-for="item in filteredNavItems" :key="item.path"
             :class="['mobile-nav-item', { active: isActive(item.path) }]"
             @click="handleNav(item); mobileMenuOpen = false">
            <span>{{ item.label }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </transition>

    <!-- Main Content -->
    <main class="brand-main">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="brand-footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand-col">
            <div class="footer-logo">
              <img src="/brand-assets/allmart-logo-full.png" alt="AllMart" class="footer-logo-image" />
            </div>
            <p class="footer-desc">AllMart 平台运营中心<br/>统一服务商家、订单与平台治理。</p>
          </div>
          <div class="footer-links-group">
            <div class="footer-col">
              <h4>平台服务</h4>
              <a v-if="hasPermission('GOODS_MGMT')" @click="router.push('/goods')">商品管理</a>
              <a v-if="hasPermission('ORDER_MGMT')" @click="router.push('/order')">订单管理</a>
              <a v-if="hasPermission('MARKETING_MGMT')" @click="router.push('/coupon')">优惠券</a>
              <a v-if="hasPermission('MARKETING_ACTIVITY')" @click="router.push('/activity')">营销活动</a>
            </div>
            <div class="footer-col">
              <h4>商家规则</h4>
              <a v-if="hasPermission('MERCHANT_MGMT')" @click="router.push('/merchant')">商户管理</a>
              <a v-if="hasPermission('GOODS_MGMT')" @click="router.push('/goods')">商品规范</a>
              <a v-if="hasPermission('DISPUTE_MGMT')" @click="router.push('/dispute')">纠纷规则</a>
            </div>
            <div class="footer-col">
              <h4>运营管理</h4>
              <a v-if="hasPermission('USER_MGMT')" @click="router.push('/user')">用户管理</a>
              <a v-if="hasPermission('DATA_MGMT')" @click="router.push('/report')">数据报表</a>
              <a v-if="hasPermission('MARKETING_MGMT')" @click="router.push('/coupon')">营销管理</a>
            </div>
            <div class="footer-col">
              <h4>帮助中心</h4>
              <a>帮助中心</a>
              <a>服务条款</a>
              <a>隐私政策</a>
              <a>关于我们</a>
            </div>
          </div>
        </div>
        <div class="footer-divider"></div>
        <div class="footer-bottom">
          <div class="footer-info">
            <span>客服热线：400-888-8888</span>
            <span class="footer-sep">|</span>
            <span>售后服务：7天无理由退换</span>
            <span class="footer-sep">|</span>
            <span>工作时间：9:00-22:00</span>
          </div>
          <div class="footer-copy">© 2026 AllMart. All rights reserved.</div>
        </div>
      </div>
      <!-- Back to top -->
      <transition name="fade">
        <div v-if="showBackTop" class="back-to-top" @click="scrollToTop">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 12V4M4 7L8 3L12 7" stroke="#E60012" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </transition>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Select } from '@element-plus/icons-vue'
import request from '../utils/request'
import { hasPermission } from '../utils/permission'


const route = useRoute()
const router = useRouter()
const adminInfo = JSON.parse(localStorage.getItem('admin_info') || '{}')
const adminAvatar = adminInfo.avatar || '/brand-assets/avatars/default-avatar-01.png'
const isSticky = ref(false)
const unreadCount = ref(0)
const searchExpanded = ref(false)
const searchKeyword = ref('')
const mobileMenuOpen = ref(false)
const showBackTop = ref(false)
const hoverGroup = ref(null)

// 原始完整导航项
const allNavItems = [
  { label: '首页', path: '/dashboard', permission: 'DASHBOARD_VIEW' },
  { label: '商品', path: '/goods', permission: 'GOODS_MGMT' },
  { label: '订单监督', path: '/order', permission: 'ORDER_MGMT' },
  { label: '商家管理', path: '/merchant', permission: 'MERCHANT_MGMT' },
  { label: '用户管理', path: '/user', permission: 'USER_MGMT' },
  { label: '售后', path: '/after-sale', permission: 'AFTER_SALE_MGMT' },
  { label: '纠纷', path: '/dispute', permission: 'DISPUTE_MGMT' },
  { label: '异常', path: '/abnormal', permission: 'ORDER_ABNORMAL' },
  { label: '服务市场', path: '/chat', permission: 'CHAT_MGMT' },
  { label: '营销管理', path: '/coupon', permission: 'MARKETING_MGMT' },
  { label: '轮播', path: '/banner', permission: 'CONTENT_BANNER' },
  { label: '评论', path: '/review', permission: 'REVIEW_MGMT' },
  { label: '数据中心', path: '/report', permission: 'DATA_MGMT' },
  { label: '日志', path: '/log', permission: 'LOG_VIEW' },
  { label: '系统', path: '/admin', permission: 'ADMIN_MGMT' },
]

// 过滤后的导航项（按权限）
const filteredNavItems = computed(() => allNavItems.filter(item => hasPermission(item.permission)))

// 核心顶部导航 - 只显示最常用的4个
const topNavItems = computed(() => {
  const topPaths = ['/dashboard', '/merchant', '/order', '/user']
  return filteredNavItems.value.filter(item => topPaths.includes(item.path))
})

// 分组定义
const navGroups = computed(() => {
  const items = filteredNavItems.value
  const isTop = (p) => topPaths.includes(p)

  // 订单相关
  const orderItems = items.filter(i => !isTop(i.path) && ['/after-sale','/dispute','/abnormal'].includes(i.path))
  // 运营相关
  const opsItems = items.filter(i => !isTop(i.path) && ['/chat','/coupon','/banner','/review'].includes(i.path))
  // 系统相关
  const sysItems = items.filter(i => !isTop(i.path) && ['/report','/log','/admin'].includes(i.path))

  return [
    ...(orderItems.length ? [{ label: '交易', items: orderItems, active: orderItems.some(i => isActive(i.path)) }] : []),
    ...(opsItems.length ? [{ label: '运营', items: opsItems, active: opsItems.some(i => isActive(i.path)) }] : []),
    ...(sysItems.length ? [{ label: '更多', items: sysItems, active: sysItems.some(i => isActive(i.path)) }] : []),
  ]
})

const topPaths = ['/dashboard', '/merchant', '/order', '/user']


const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const handleNav = (item) => {
  if (item.path) router.push(item.path)
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: hasPermission('GOODS_MGMT') ? '/goods' : '/dashboard', query: { keyword: searchKeyword.value } })

    searchExpanded.value = false
    searchKeyword.value = ''
  }
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    router.push('/login')
  } else if (cmd === 'ai') {
    router.push('/ai-assistant')
  } else if (cmd === 'notification') {
    router.push('/notification')
  } else if (cmd === 'dashboard') {
    router.push('/dashboard')
  } else if (cmd === 'admin') {
    router.push('/admin')
  }
}


const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const loadUnreadCount = async () => {
  try {
    const res = await request.get('/notification/unread-count', { params: { adminId: adminInfo.adminId || 1 } })
    unreadCount.value = res.data.count
  } catch {}
}

const onScroll = () => {
  isSticky.value = window.scrollY > 10
  showBackTop.value = window.scrollY > 400
}

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  loadUnreadCount()
  setInterval(loadUnreadCount, 60000)
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.brand-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

/* ===== Header ===== */
.brand-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #fff;
  border-bottom: 1px solid var(--border-light);
  transition: box-shadow 0.3s;
}
.brand-header.sticky {
  box-shadow: var(--shadow-header);
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}
.logo-image { display: block; width: 138px; height: auto; }
.header-platform-name {
  position: relative;
  padding-left: 10px;
  color: #555;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.header-platform-name::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  width: 1px;
  height: 14px;
  background: #eee;
  transform: translateY(-50%);
}

/* Nav */
.header-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}
.nav-item {
  padding: 8px 16px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  white-space: nowrap;
  display: flex;
  align-items: center;
  font-weight: 600;
}
.nav-item:hover {
  color: var(--brand-red);
}
.nav-item.active {
  color: var(--brand-red);
  font-weight: 600;
}
.nav-item-group .nav-underline { display: none; }
.nav-item-group.nav-open > .nav-underline { display: block; }
.nav-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: #E60012;
  border-radius: 1px;
  transition: width 0.25s ease;
}
.nav-item:hover .nav-underline,
.nav-item.active .nav-underline {
  width: 20px;
}

/* Nav Dropdown */
.nav-dropdown {
  position: relative;
}
.nav-dropdown-menu {
  position: absolute;
  top: 100%;
  left: -8px;
  min-width: 130px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06);
  border: 1px solid rgba(0,0,0,0.06);
  padding: 6px 0;
  z-index: 1001;
  margin-top: 4px;
}
.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 18px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.dropdown-item:hover {
  background: var(--brand-red-light, #FFF1F0);
  color: var(--brand-red, #E60012);
}
.dropdown-active {
  color: var(--brand-red, #E60012);
  font-weight: 500;
}

/* Right */
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 190px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid #eee;
  border-radius: 999px;
  color: #999;
  background: #fafafa;
}
.header-search:hover {
  border-color: #ddd;
}
.search-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-main);
  width: 100%;
  font-weight: 500;
}
.search-input::placeholder {
  color: var(--text-muted);
}
.header-search-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #f7f7f7;
  color: #222;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.header-search-btn:hover {
  background: #fff5f5;
  color: #e60012;
}
.header-icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
}
.header-icon-btn:hover {
  background: var(--brand-red-light);
  color: var(--brand-red);
}
.header-user {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 2px;
  border-radius: 50%;
  transition: all 0.2s;
}
.header-user:hover {
  box-shadow: 0 0 0 2px var(--brand-red-light);
}

/* Mobile menu */
.mobile-menu-btn {
  display: none;
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
  width: 48px;
  height: 48px;
  background: #E60012;
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(230,0,18,0.3);
}
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 1001;
}
.mobile-menu {
  position: fixed;
  top: 0;
  right: 0;
  width: 260px;
  height: 100vh;
  background: #fff;
  padding: 24px 0;
  overflow-y: auto;
}
.mobile-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  font-size: 15px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.mobile-nav-item:hover,
.mobile-nav-item.active {
  color: #E60012;
  background: #FFE8EA;
}

/* Main */
.brand-main {
  flex: 1;
  width: 100%;
}

/* ===== Footer ===== */
.brand-footer {
  background: var(--bg-section);
  margin-top: auto;
  position: relative;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 56px 24px 32px;
}
.footer-top {
  display: flex;
  justify-content: space-between;
  gap: 60px;
  margin-bottom: 40px;
}
.footer-brand-col {
  max-width: 280px;
}
.footer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.footer-logo-text {
  font-size: 18px;
  font-weight: 700;
}
.footer-logo-image { display: block; width: 126px; height: auto; }
.footer-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.8;
}
.footer-links-group {
  display: flex;
  gap: 60px;
}
.footer-col h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 16px;
}
.footer-col a {
  display: block;
  font-size: 13px;
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.2s;
  margin-bottom: 10px;
}
.footer-col a:hover {
  color: #E60012;
}
.footer-divider {
  height: 1px;
  background: var(--border-light);
  margin-bottom: 24px;
}
.footer-bottom {
  text-align: center;
}
.footer-info {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 12px;
}
.footer-sep {
  margin: 0 12px;
  color: var(--border-light);
}
.footer-copy {
  color: var(--text-muted);
  font-size: 12px;
}

/* Back to top */
.back-to-top {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 44px;
  height: 44px;
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-soft);
  transition: all 0.25s;
  z-index: 100;
}
.back-to-top:hover {
  border-color: #E60012;
  box-shadow: 0 4px 16px rgba(230,0,18,0.15);
}

.fade-enter-active,
.fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }

.dropdown-fade-enter-active { transition: all 0.2s ease; }
.dropdown-fade-leave-active { transition: all 0.15s ease; }
.dropdown-fade-enter-from { opacity: 0; transform: translateY(-4px); }
.dropdown-fade-leave-to { opacity: 0; transform: translateY(-4px); }

@media (max-width: 1024px) {
  .header-nav { display: none; }
  .mobile-menu-btn { display: flex; }
  .footer-top { flex-direction: column; gap: 32px; }
  .footer-links-group { gap: 32px; flex-wrap: wrap; }
}

@media (max-width: 768px) {
  .header-inner { height: 60px; }
  .footer-inner { padding: 40px 16px 24px; }
  .footer-links-group { flex-direction: column; gap: 24px; }
  .footer-brand-col { max-width: 100%; }
  .back-to-top { bottom: 80px; right: 16px; }
}
</style>
