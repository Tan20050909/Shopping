<template>
  <UserShell v-if="layout === 'user'" />

  <div v-else-if="isStandalonePage">
    <router-view></router-view>
  </div>

  <div v-else-if="layout === 'merchant'" class="merchant-portal">
    <div class="top-nav">
      <div class="top-nav-left">
        <a href="#" @click.prevent="goHome">平台首页</a>
        <a href="#" @click.prevent="goRoute('/products')">分类入口</a>
        <a href="#" @click.prevent="goRoute('/activity')">活动入口</a>
      </div>
      <div class="top-nav-right">
        <a href="#" @click.prevent="goRoute('/chat')">消息</a>
        <a href="#" @click.prevent="goRoute('/platform-support')">平台客服</a>
        <a href="#" class="merchant-tag" @click.prevent="goRoute('/dashboard')">商家中心</a>
        <a href="#" @click.prevent="logoutMerchant">退出</a>
      </div>
    </div>

    <header class="main-header">
      <div class="header-left">
        <div class="brand">
          <div class="logo">
            <div class="logo-text brand-logo">
              <span class="brand-all">All</span><span class="brand-mart">Mart</span>
            </div>
            <div class="logo-sub">AllMart</div>
          </div>
          <div class="brand-split">|</div>
          <div class="brand-page">商家中心</div>
        </div>
      </div>

      <div class="search-box">
        <el-select v-model="searchType" class="search-select">
          <el-option label="宝贝" value="baby" />
          <el-option label="订单" value="order" />
        </el-select>
        <el-input
          v-model="searchText"
          placeholder="搜索商品/订单/售后"
          class="search-input"
          clearable
          @keydown.enter.prevent="doSearch"
          @clear="clearSearch"
        />
        <el-button type="primary" class="search-btn" @click="doSearch">搜索</el-button>
      </div>

      <div class="header-right">
        <div class="shop-card" @click="openAvatarDialog">
          <div class="shop-avatar">
            <img v-if="currentAvatar" class="shop-avatar-img" :src="currentAvatar" alt="" />
            <span v-else>{{ shopAvatar }}</span>
          </div>
          <div class="shop-meta">
            <div class="shop-name">{{ currentUser?.merchantName || currentUser?.username || '商家店铺' }}</div>
            <div class="shop-sub">
              <span class="shop-level">信誉 {{ shopLevelText }}</span>
              <span class="shop-split">|</span>
              <span class="shop-rank">评分 {{ shopRatingText }}</span>
            </div>
          </div>
        </div>
        <div class="shop-badges">
          <el-badge :value="pendingOrders" :max="99" class="badge-item">
            <el-button class="badge-btn" @click="goRoute('/order')">待处理订单</el-button>
          </el-badge>
          <el-badge :value="unreadMessages" :max="99" class="badge-item">
            <el-button class="badge-btn" @click="goRoute('/chat')">消息通知</el-button>
          </el-badge>
        </div>
      </div>
    </header>

    <div class="main-nav">
      <el-menu :key="menuKey" :default-active="activeMenu" mode="horizontal" class="merchant-top-menu" @select="onMenuSelect">
        <el-menu-item index="/dashboard">工作台</el-menu-item>
        <el-menu-item index="/goods">商品管理</el-menu-item>
        <el-menu-item index="/stock">库存管理</el-menu-item>
        <el-menu-item index="/comment-appeal">评价申诉</el-menu-item>
        <el-menu-item index="/order">订单管理</el-menu-item>
        <el-menu-item index="/after-sale">售后管理</el-menu-item>
        <el-menu-item index="/merchant">入驻审核</el-menu-item>
        <el-menu-item index="/merchant-setting">店铺设置</el-menu-item>
        <el-menu-item index="/finance">财务结算</el-menu-item>
        <el-menu-item index="/activity">营销活动</el-menu-item>
        <el-menu-item index="/chat">客服管理</el-menu-item>
        <el-menu-item index="/platform-support">平台客服</el-menu-item>
        <el-menu-item index="/merchant-live">直播管理</el-menu-item>
      </el-menu>
    </div>

    <main class="main-content">
      <div class="content-wrapper">
        <section class="center-content">
          <router-view></router-view>
        </section>

        <aside class="right-sidebar">
          <div class="quick-toolbar">
            <div class="quick-tool" @click="goRoute('/chat')">
              <div class="quick-icon">💬</div>
              <div class="quick-label">消息</div>
            </div>
            <div class="quick-tool" @click="goRoute('/order')">
              <div class="quick-icon">📦</div>
              <div class="quick-label">订单</div>
            </div>
            <div class="quick-tool" @click="goRoute('/after-sale')">
              <div class="quick-icon">🛠️</div>
              <div class="quick-label">售后</div>
            </div>
            <div class="quick-tool" @click="goRoute('/goods')">
              <div class="quick-icon">🛒</div>
              <div class="quick-label">商品</div>
            </div>
          </div>
        </aside>
      </div>
    </main>

    <el-dialog v-model="avatarDialogVisible" title="设置头像" width="720px">
      <div class="avatar-panel">
        <div class="avatar-grid">
          <div
            v-for="url in avatarCandidates"
            :key="url"
            class="avatar-item"
            :class="{ active: url === selectedAvatar }"
            @click="selectAvatar(url)"
          >
            <img :src="url" alt="" />
          </div>
        </div>
        <div class="avatar-actions">
          <el-upload action="" :http-request="uploadAvatar" :show-file-list="false" accept="image/*">
            <el-button>上传头像</el-button>
          </el-upload>
          <el-button type="primary" :disabled="!selectedAvatar" @click="applyAvatar">保存</el-button>
        </div>
      </div>
    </el-dialog>
  </div>

  <div v-else>
    <router-view></router-view>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { merchantApi, orderApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { ensureMerchantUser, getMerchantId } from '@/utils/merchant'
import UserShell from '@user/App.vue'

const route = useRoute()
const router = useRouter()
const activeMenu = ref(route.path)
const menuKey = ref(0)

const layout = computed(() => String(route.meta?.layout || 'user'))
const isStandalonePage = computed(() => layout.value === 'merchant' && String(route.query?.standalone || '') === '1')

const currentUser = ref(null)

const loadUser = async () => {
  const local = ensureMerchantUser()
  currentUser.value = local
  const merchantId = getMerchantId()
  if (!merchantId) return
  try {
    const res = await merchantApi.info(merchantId)
    const data = res?.data || res
    const shopLogo = String(data?.shopLogo || data?.shop_logo || '').trim()
    const shopScore = Number(data?.shopScore ?? data?.shop_score ?? 0)
    const followerCount = Number(data?.followerCount ?? data?.follower_count ?? 0)
    const raw = localStorage.getItem('merchantUser')
    const parsed = raw ? JSON.parse(raw) : {}
    const merged = {
      ...(parsed || {}),
      ...(shopLogo ? { shopLogo, avatarUrl: shopLogo } : {}),
      ...(Number.isFinite(shopScore) ? { shopScore } : {}),
      ...(Number.isFinite(followerCount) ? { followerCount } : {})
    }
    localStorage.setItem('merchantUser', JSON.stringify(merged))
    currentUser.value = merged
  } catch (e) {
  }
}

const searchType = ref('baby')
const searchText = ref('')

const pendingOrders = ref(0)
const unreadMessages = ref(0)
const shopScore = computed(() => {
  const v = Number(currentUser.value?.shopScore ?? currentUser.value?.shop_score ?? currentUser.value?.rating ?? 0)
  return Number.isFinite(v) ? v : 0
})
const shopLevelText = computed(() => {
  const s = shopScore.value
  if (!(s > 0)) return '-'
  if (s >= 4.8) return '5'
  if (s >= 4.5) return '4'
  if (s >= 4.0) return '3'
  if (s >= 3.5) return '2'
  return '1'
})
const shopRatingText = computed(() => {
  const s = shopScore.value
  if (!(s > 0)) return '-'
  return s.toFixed(1)
})
const shopAvatar = computed(() => (currentUser.value?.merchantName || currentUser.value?.username || '商')[0])
const currentAvatar = computed(() => {
  const v = String(currentUser.value?.avatarUrl || '').trim()
  return v || ''
})

const avatarDialogVisible = ref(false)
const avatarCandidates = ref([
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-01',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-02',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-03',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-04',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-05',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-06',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-07',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-08',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-09',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-10',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-11',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-12',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-13',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-14',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-15',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-16',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-17',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-18',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-19',
  'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-20'
])
const selectedAvatar = ref('')

const openAvatarDialog = () => {
  selectedAvatar.value = currentAvatar.value || avatarCandidates.value[0] || ''
  avatarDialogVisible.value = true
}

const selectAvatar = (url) => {
  selectedAvatar.value = url
}

const persistMerchantUser = (patch) => {
  const raw = localStorage.getItem('merchantUser')
  const parsed = raw ? JSON.parse(raw) : {}
  const merged = { ...(parsed || {}), ...(patch || {}) }
  localStorage.setItem('merchantUser', JSON.stringify(merged))
  currentUser.value = merged
}

const applyAvatar = async () => {
  if (!selectedAvatar.value) return
  const url = selectedAvatar.value
  try {
    await merchantApi.updateLogo({ merchantId: getMerchantId(), shopLogo: url })
  } catch (e) {
  }
  persistMerchantUser({ avatarUrl: url, shopLogo: url })
  avatarDialogVisible.value = false
  ElMessage.success('已保存')
}

const uploadAvatar = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    if (url) {
      selectedAvatar.value = url
      try {
        await merchantApi.updateLogo({ merchantId: getMerchantId(), shopLogo: url })
      } catch (e) {
      }
      persistMerchantUser({ avatarUrl: url, shopLogo: url })
      avatarDialogVisible.value = false
      ElMessage.success('上传成功')
    } else {
      ElMessage.error('上传失败')
    }
    options?.onSuccess?.(res?.data, options.file)
  } catch (error) {
    options?.onError?.(error)
    ElMessage.error('上传失败')
  }
}

const loadHeaderCounters = async () => {
  try {
    const merchantId = getMerchantId()
    const res = await orderApi.list(merchantId, null)
    const orders = Array.isArray(res.data) ? res.data : []
    pendingOrders.value = orders.filter(o => o.status === 1).length
  } catch (error) {
    pendingOrders.value = 0
  }
}

const goHome = () => {
  router.push('/products')
}

const logoutMerchant = () => {
  localStorage.removeItem('merchantUser')
  localStorage.removeItem('shopping_user_token')
  window.dispatchEvent(new Event('shopping-user-token'))
  sessionStorage.removeItem('shopping_auth_role')
  router.push('/')
}

const doSearch = () => {
  const k = String(searchText.value || '').trim()
  if (!k) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  if (searchType.value === 'order') {
    router.push({ path: '/order', query: { keyword: k } })
    return
  }
  router.push({ path: '/goods', query: { keyword: k } })
}

const clearSearch = () => {
  searchText.value = ''
  if (route.path === '/goods' || route.path === '/order') {
    const q = { ...(route.query || {}) }
    if (q.keyword != null) {
      delete q.keyword
      router.replace({ path: route.path, query: q })
    }
  }
}

const goRoute = (path) => {
  if (path === '/chat') {
    openChatWindow()
    return
  }
  if (path === '/platform-support') {
    openPlatformSupportWindow()
    return
  }
  router.push(path)
}

const openChatWindow = () => {
  const href = router.resolve({ path: '/chat', query: { standalone: '1', shell: '1' } }).href
  const w = window.open(href, '_blank')
  if (!w) router.push('/chat')
}

const openPlatformSupportWindow = () => {
  const href = router.resolve({ path: '/platform-support', query: { standalone: '1', shell: '1' } }).href
  const w = window.open(href, '_blank')
  if (!w) router.push('/platform-support')
}

const onMenuSelect = (index) => {
  const path = String(index || '')
  if (path === '/chat') {
    openChatWindow()
    activeMenu.value = route.path
    menuKey.value += 1
    return
  }
  if (path === '/platform-support') {
    openPlatformSupportWindow()
    activeMenu.value = route.path
    menuKey.value += 1
    return
  }
  router.push(path)
}

watch(() => route.path, (newPath) => {
  activeMenu.value = newPath
  if (layout.value === 'merchant' && !isStandalonePage.value) {
    loadUser()
    loadHeaderCounters()
  }
}, { immediate: true })

onMounted(() => {
  if (layout.value === 'merchant' && !isStandalonePage.value) {
    loadUser()
    loadHeaderCounters()
  }
})
</script>

<style>
:root {
  --brand-red: #E60012;
  --brand-red-dark: #C4000F;
  --brand-red-light: #FFE8EA;

  --bg-page: #FFFFFF;
  --bg-section: #F7F7F7;
  --bg-soft: #FAFAFA;
  --bg-card: #FFFFFF;

  --text-main: #111111;
  --text-secondary: #555555;
  --text-muted: #999999;

  --border-light: #EEEEEE;
  --border-soft: #F3F3F3;

  --shadow-soft: 0 8px 24px rgba(0, 0, 0, 0.05);
  --shadow-card: 0 12px 32px rgba(0, 0, 0, 0.08);

  --radius-lg: 16px;
  --radius-md: 10px;
  --radius-sm: 6px;
  --radius-pill: 999px;

  --el-color-primary: var(--brand-red);
  --el-color-primary-dark-2: var(--brand-red-dark);
  --el-color-primary-light-3: #FFB9BF;
  --el-color-primary-light-5: #FFD6D9;
  --el-color-primary-light-7: #FFE9EA;
  --el-color-primary-light-8: #FFF0F0;
  --el-color-primary-light-9: #FFF7F7;
}

html,
body {
  background: var(--bg-page);
  color: var(--text-main);
}

.el-button--primary:not(.is-plain) {
  background: var(--brand-red);
  border-color: var(--brand-red);
  border-radius: var(--radius-pill);
}

.el-button--primary.is-plain {
  --el-button-text-color: var(--brand-red);
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--brand-red);
  --el-button-hover-text-color: var(--brand-red-dark);
  --el-button-hover-bg-color: var(--el-color-primary-light-9);
  --el-button-hover-border-color: var(--brand-red-dark);
  --el-button-active-text-color: var(--brand-red-dark);
  --el-button-active-bg-color: var(--el-color-primary-light-8);
  --el-button-active-border-color: var(--brand-red-dark);
  color: var(--brand-red) !important;
  background: transparent !important;
  border-color: var(--brand-red) !important;
}

.el-button--primary.is-plain:hover {
  --el-button-text-color: var(--brand-red-dark);
  --el-button-bg-color: var(--el-color-primary-light-9);
  --el-button-border-color: var(--brand-red-dark);
  color: var(--brand-red-dark) !important;
  background: var(--el-color-primary-light-9) !important;
  border-color: var(--brand-red-dark) !important;
}

.el-button--primary:not(.is-plain):hover {
  background: var(--brand-red-dark);
  border-color: var(--brand-red-dark);
}

.el-input .el-input__wrapper {
  border-radius: var(--radius-pill);
}

* {
  margin: 0;
  padding: 0;
}

.merchant-portal {
  min-height: 100vh;
  background: var(--bg-section);
}

.top-nav {
  background: #fefefe;
  height: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 50px;
  font-size: 12px;
  border-bottom: 1px solid #eee;
}

.top-nav a {
  color: #666;
  text-decoration: none;
  margin: 0 15px;
}

.top-nav a:hover {
  color: var(--brand-red);
}

.merchant-tag {
  font-weight: 600;
  color: var(--brand-red);
}

.main-header {
  background: #fff;
  height: 96px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 50px;
  gap: 24px;
  border-bottom: 2px solid var(--brand-red);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.logo-text {
  font-size: 40px;
  font-weight: bold;
  font-style: normal;
  line-height: 1;
  color: #e60012;
}

.brand-logo {
  color: #111827;
  letter-spacing: 0.5px;
}

.brand-all {
  color: #111827;
}

.brand-mart {
  color: #e60012;
}

.logo-sub {
  font-size: 12px;
  color: #999;
}

.brand-split {
  color: #ddd;
  font-size: 16px;
}

.brand-page {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.search-box {
  display: flex;
  gap: 0;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  height: 46px;
  flex: 1;
  max-width: 720px;
  border: 2px solid #e60012;
  padding: 2px;
}

.search-select {
  width: 110px;
}

.search-select :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 12px 0 0 12px;
  background: transparent;
  height: 42px;
}

.search-select {
  border-right: 1px solid #e5e7eb;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 0;
  height: 42px;
}

.search-box :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  padding-top: 0;
  padding-bottom: 0;
}

.search-box :deep(.el-input__inner) {
  height: 42px;
  line-height: 42px;
}

.search-box :deep(.el-input__inner:focus-visible) {
  outline: none;
}

.search-box :deep(.el-select .el-input__inner) {
  height: 42px;
  line-height: 42px;
}

.search-box :deep(.el-select .el-input__inner:focus-visible) {
  outline: none;
}

.search-box :deep(.el-select__wrapper) {
  height: 42px;
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  align-items: center;
}

.search-box :deep(.el-select__selected-item),
.search-box :deep(.el-select__placeholder),
.search-box :deep(.el-select__caret) {
  line-height: 42px;
}

.search-box :deep(.el-input__wrapper) {
  align-items: center;
}

.search-box :deep(.el-input) {
  --el-input-border-color: transparent;
  --el-input-hover-border-color: transparent;
  --el-input-focus-border-color: transparent;
  --el-input-outline-color: transparent;
}

.search-btn {
  background: #e60012;
  border: 1px solid #e60012;
  border-radius: 12px;
  padding: 0 26px;
  font-weight: 800;
  height: 42px;
}

.search-btn:hover {
  background: #ff2a3a;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-shrink: 0;
}

.shop-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff5f5;
  border-radius: 16px;
  padding: 10px 14px;
  color: #333;
  border: 1px solid #ffd6d9;
  cursor: pointer;
}

.shop-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #ffe9ea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #e60012;
  overflow: hidden;
}

.shop-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.shop-name {
  font-weight: 700;
  line-height: 1.2;
}

.shop-sub {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
}

.shop-split {
  opacity: 0.65;
}

.shop-badges {
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge-btn {
  border-radius: 16px;
  border: none;
  background: #fff;
  color: #e60012;
  padding: 0 14px;
  border: 1px solid #ffd6d9;
}

.badge-btn:hover {
  background: #fff5f5;
}

.main-nav {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 50px;
}

.merchant-top-menu {
  border-bottom: none;
}

.merchant-top-menu :deep(.el-menu-item) {
  height: 52px;
  line-height: 52px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.merchant-top-menu :deep(.el-menu-item.is-active) {
  color: #e60012;
  border-bottom-color: #e60012;
}

.main-content {
  padding: 20px 50px;
  max-width: 1400px;
  margin: 0 auto;
}

.content-wrapper {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.center-content {
  flex: 1;
  min-width: 0;
}

.right-sidebar {
  width: 160px;
  position: sticky;
  top: 20px;
}

.quick-toolbar {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.quick-tool {
  padding: 14px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.quick-tool:hover {
  background: #fff5f5;
  color: #e60012;
}

.quick-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #fff0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.quick-label {
  font-size: 13px;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 10px;
}

.avatar-item {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  background: #f8fafc;
}

.avatar-item.active {
  border-color: #e60012;
  box-shadow: 0 0 0 3px rgba(230, 0, 18, 0.15);
}

.avatar-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1200px) {
  .main-header {
    flex-wrap: wrap;
    height: auto;
    padding: 16px 20px;
  }

  .top-nav {
    padding: 0 20px;
  }

  .main-nav {
    padding: 0 20px;
  }

  .main-content {
    padding: 16px 20px;
  }

  .right-sidebar {
    display: none;
  }
}
</style>
