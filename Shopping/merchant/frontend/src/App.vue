<template>
  <div v-if="isStandalonePage">
    <router-view></router-view>
  </div>

  <div v-else-if="layout === 'merchant'" class="merchant-portal" :class="{ 'dashboard-shell': route.path === '/dashboard', 'goods-inner-page': route.path === '/goods' }">
    <!-- 单层顶部栏 — 参考用户端 site-header 结构 -->
    <header class="merchant-site-header">
      <div class="merchant-header-inner">
        <!-- 左侧：Logo + 商家中心 -->
        <router-link class="merchant-brand-lockup" to="/dashboard">
          <img class="merchant-logo-img" :src="brandLogo" alt="AllMart" />
          <span class="merchant-brand-name">商家中心</span>
        </router-link>

        <!-- 中间：自定义导航（无 el-menu） -->
        <nav class="merchant-site-nav">
          <template v-for="item in navItems" :key="item.label">
            <!-- 有子项的导航 — 悬浮下拉 -->
            <div
              v-if="item.children"
              class="merchant-nav-hover"
              @mouseenter="openDropdown(item.label)"
              @mouseleave="closeDropdown"
            >
              <button
                class="merchant-nav-link"
                :class="{ active: isNavActive(item) }"
                type="button"
              >
                {{ item.label }}
                <span class="nav-arrow">▾</span>
              </button>
              <div v-if="activeDropdown === item.label" class="merchant-dropdown-menu">
                <template v-for="child in item.children" :key="child.label">
                  <button
                    v-if="child.openChat"
                    class="merchant-dropdown-item"
                    type="button"
                    @click="handleNavClick(child)"
                  >
                    {{ child.label }}
                  </button>
                  <router-link
                    v-else
                    class="merchant-dropdown-item"
                    :to="child.path"
                    @click="closeDropdown"
                  >
                    {{ child.label }}
                  </router-link>
                </template>
              </div>
            </div>
            <!-- 无子项的导航 — 直接跳转 -->
            <router-link
              v-else
              class="merchant-nav-link"
              :class="{ active: route.path === item.path }"
              :to="item.path"
            >
              {{ item.label }}
            </router-link>
          </template>
        </nav>

        <!-- 右侧：搜索框 + 店铺胶囊 + 退出 -->
        <div class="merchant-site-actions">
          <div class="merchant-site-search">
            <el-input
              v-model="searchText"
              placeholder="搜索商品/订单"
              clearable
              @keyup.enter="doSearch"
              @clear="clearSearch"
            />
            <el-button type="primary" @click="doSearch">搜</el-button>
          </div>

          <el-dropdown trigger="click" @command="handleUserMenuCommand">
            <button class="merchant-user-chip" type="button">
              <template v-if="currentAvatar">
                <img class="merchant-user-avatar" :src="currentAvatar" alt="" />
              </template>
              <span v-else class="merchant-user-avatar merchant-avatar-fallback">{{ shopAvatar }}</span>
              <span class="merchant-user-name">{{ currentUser?.merchantName || currentUser?.username || '商家店铺' }}</span>
              <span class="user-caret">▾</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="merchant-user-menu">
                <el-dropdown-item command="setting">店铺设置</el-dropdown-item>
                <el-dropdown-item command="audit">入驻审核</el-dropdown-item>
                <el-dropdown-item command="profile">账号资料</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button class="merchant-logout-link" type="button" @click="logoutMerchant">退出</button>
        </div>
      </div>
    </header>

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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { merchantApi, orderApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { ensureMerchantUser, getMerchantId } from '@/utils/merchant'
import { DEFAULT_AVATARS, DEFAULT_USER_AVATAR, resolveAvatar } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const brandLogo = '/brand-assets/allmart-logo-full.png'

const layout = computed(() => String(route.meta?.layout || 'user'))
const isStandalonePage = computed(() => layout.value === 'merchant' && String(route.query?.standalone || '') === '1')

const currentUser = ref(null)
const searchText = ref('')
const pendingOrders = ref(0)
const unreadMessages = ref(0)
const activeDropdown = ref(null)

// 导航数据结构
const navItems = [
  { label: '首页', path: '/dashboard' },
  { label: '商品管理', children: [
    { label: '商品列表', path: '/goods' },
    { label: '库存管理', path: '/stock' },
    { label: '评价申诉', path: '/comment-appeal' },
  ]},
  { label: '订单管理', children: [
    { label: '订单管理', path: '/order' },
    { label: '售后管理', path: '/after-sale' },
  ]},
  { label: '营销中心', children: [
    { label: '营销活动', path: '/coupon' },
    { label: '直播管理', path: '/merchant-live' },
  ]},
  { label: '数据中心', path: '/data-center' },
  { label: '财务中心', path: '/finance' },
  { label: '服务市场', children: [
    { label: '客服管理', path: '/chat', openChat: true },
    { label: '平台客服', path: '/platform-support', openChat: true },
    { label: '店铺设置', path: '/merchant-setting' },
  ]},
]

const isNavActive = (item) => {
  if (item.children) {
    return item.children.some(child => route.path.startsWith(child.path === '/chat' ? '/chat' : child.path))
  }
  return route.path === item.path
}

const openDropdown = (label) => { activeDropdown.value = label }
const closeDropdown = () => { activeDropdown.value = null }

const handleNavClick = (child) => {
  activeDropdown.value = null
  if (child.openChat) {
    if (child.path === '/chat') openChatWindow()
    else openPlatformSupportWindow()
  } else {
    router.push(child.path)
  }
}

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
      ...(shopLogo ? { shopLogo } : {}),
      ...(Number.isFinite(shopScore) ? { shopScore } : {}),
      ...(Number.isFinite(followerCount) ? { followerCount } : {})
    }
    localStorage.setItem('merchantUser', JSON.stringify(merged))
    currentUser.value = merged
  } catch (e) {
  }
}

const shopScore = computed(() => {
  const v = Number(currentUser.value?.shopScore ?? currentUser.value?.shop_score ?? currentUser.value?.rating ?? 0)
  return Number.isFinite(v) ? v : 0
})
const shopAvatar = computed(() => (currentUser.value?.merchantName || currentUser.value?.username || '商')[0])
const currentAvatar = computed(() => {
  return resolveAvatar(currentUser.value?.avatarUrl, DEFAULT_USER_AVATAR)
})

const avatarDialogVisible = ref(false)
const avatarCandidates = ref(DEFAULT_AVATARS)
const selectedAvatar = ref('')

const openAvatarDialog = () => {
  selectedAvatar.value = currentAvatar.value || avatarCandidates.value[0] || ''
  avatarDialogVisible.value = true
}

const selectAvatar = (url) => { selectedAvatar.value = url }

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
  persistMerchantUser({ avatarUrl: url })
  avatarDialogVisible.value = false
  ElMessage.success('已保存')
}

const uploadAvatar = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    if (url) {
      selectedAvatar.value = url
      persistMerchantUser({ avatarUrl: url })
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
    if (!merchantId) { pendingOrders.value = 0; return }
    const res = await orderApi.list(merchantId, null)
    const orders = Array.isArray(res.data) ? res.data : []
    pendingOrders.value = orders.filter(o => o.status === 1).length
  } catch (error) {
    pendingOrders.value = 0
  }
}

const logoutMerchant = () => {
  localStorage.removeItem('merchantUser')
  sessionStorage.removeItem('shopping_auth_role')
  router.push({ path: '/login', query: { tab: 'merchant', forceLogin: '1' } })
}

const doSearch = () => {
  const k = String(searchText.value || '').trim()
  if (!k) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  router.push({ path: '/goods', query: { keyword: k } })
}

const clearSearch = () => {
  searchText.value = ''
  if (route.path === '/goods') {
    const q = { ...(route.query || {}) }
    if (q.keyword != null) {
      delete q.keyword
      router.replace({ path: route.path, query: q })
    }
  }
}

const goRoute = (path) => {
  if (path === '/chat') { openChatWindow(); return }
  if (path === '/platform-support') { openPlatformSupportWindow(); return }
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

const handleUserMenuCommand = (command) => {
  if (command === 'setting') { router.push('/merchant-setting'); return }
  if (command === 'audit') { router.push('/merchant'); return }
  if (command === 'profile') { openAvatarDialog() }
}

watch(() => route.path, () => {
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

html, body {
  background: var(--bg-page);
  color: var(--text-main);
}

.el-button--primary:not(.is-plain) {
  background: var(--brand-red);
  border-color: var(--brand-red);
  border-radius: var(--radius-pill);
}

.el-button--primary:not(.is-plain):hover {
  background: var(--brand-red-dark);
  border-color: var(--brand-red-dark);
}

.el-input .el-input__wrapper {
  border-radius: var(--radius-pill);
}

* { margin: 0; padding: 0; }

/* ============ 商家端单层顶部栏 ============ */
.merchant-site-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.97);
  border-bottom: 1px solid var(--border-light);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.035);
  backdrop-filter: blur(12px);
  min-height: 76px;
  display: flex;
  align-items: center;
}

.merchant-header-inner {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  min-height: 76px;
}

/* Logo + 商家中心 */
.merchant-brand-lockup {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  text-decoration: none;
}

.merchant-logo-img {
  display: block;
  width: 140px;
  height: auto;
  object-fit: contain;
}

.merchant-brand-name {
  position: relative;
  padding-left: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
}

.merchant-brand-name::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 14px;
  background: var(--border-light);
}

/* 中间导航 */
.merchant-site-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(8px, 1vw, 16px);
  min-width: 0;
  overflow: hidden;
}

.merchant-nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 28px 0 24px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  text-decoration: none;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s;
}

.merchant-nav-link::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: transparent;
  transition: background 0.2s;
}

.merchant-nav-link:hover,
.merchant-nav-link.active {
  color: var(--brand-red);
}

.merchant-nav-link.active::after {
  background: var(--brand-red);
}

.nav-arrow {
  font-size: 10px;
  color: var(--text-muted);
  line-height: 1;
}

/* 导航下拉菜单 */
.merchant-nav-hover {
  position: relative;
}

.merchant-dropdown-menu {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  min-width: 140px;
  padding: 8px;
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.1);
  z-index: 60;
}

.merchant-dropdown-item {
  display: block;
  width: 100%;
  padding: 8px 16px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
  font-family: inherit;
  transition: color 0.15s, background 0.15s;
}

.merchant-dropdown-item:hover {
  color: var(--brand-red);
  background: var(--brand-red-light);
}

/* 右侧操作区 */
.merchant-site-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* 搜索框 — 用户端胶囊风格 */
.merchant-site-search {
  display: flex;
  align-items: center;
  gap: 6px;
}

.merchant-site-search :deep(.el-input) {
  width: clamp(140px, 12vw, 220px);
}

.merchant-site-search :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: var(--radius-pill);
  background: var(--bg-soft);
  box-shadow: 0 0 0 1px var(--border-light) inset;
}

.merchant-site-search :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #ddd inset;
}

.merchant-site-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand-red) inset;
}

.merchant-site-search :deep(.el-input__inner) {
  height: 42px;
  font-size: 12px;
}

.merchant-site-search :deep(.el-input__inner::placeholder) {
  font-size: 12px;
  color: var(--text-muted);
}

.merchant-site-search .el-button {
  flex-shrink: 0;
  min-height: 34px;
  height: 34px;
  border-radius: var(--radius-pill);
  padding: 0 14px;
  font-size: 12px;
  font-weight: 700;
}

/* 店铺头像胶囊 — 用户端 user-chip 风格 */
.merchant-user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-pill);
  background: #fff;
  cursor: pointer;
  font-family: inherit;
  white-space: nowrap;
  transition: border-color 0.2s;
}

.merchant-user-chip:hover {
  border-color: #ffd6d9;
}

.merchant-user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.merchant-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 700;
  font-size: 13px;
}

.merchant-user-name {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.user-caret {
  color: var(--text-muted);
  font-size: 10px;
  line-height: 1;
}

/* 退出按钮 */
.merchant-logout-link {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  padding: 0 2px;
  white-space: nowrap;
  transition: color 0.2s;
}

.merchant-logout-link:hover {
  color: var(--brand-red);
}

/* ============ 旧样式保留（不影响其他页面） ============ */
.merchant-portal {
  min-height: 100vh;
  background: #fbfbfb;
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

.dashboard-shell .main-content {
  width: 100%;
  max-width: 1280px;
  padding: 24px 0 0;
  box-sizing: border-box;
}

.dashboard-shell .content-wrapper {
  display: block;
}

.dashboard-shell .right-sidebar {
  display: none;
}

.goods-inner-page .content-wrapper {
  display: block;
}

.goods-inner-page .right-sidebar {
  display: none;
}

.goods-inner-page .main-content {
  width: 100%;
  max-width: 100%;
  padding: 0;
}

/* 用户菜单下拉 */
.merchant-user-menu .el-dropdown-menu__item {
  color: var(--text-secondary);
  font-weight: 700;
}

.merchant-user-menu .el-dropdown-menu__item:hover {
  color: var(--brand-red);
  background: #fff7f8;
}

/* 头像弹窗 */
.avatar-panel { display: flex; flex-direction: column; gap: 14px; }
.avatar-grid { display: grid; grid-template-columns: repeat(10, 1fr); gap: 10px; }
.avatar-item { width: 56px; height: 56px; border-radius: 50%; overflow: hidden; border: 2px solid transparent; cursor: pointer; background: #f8fafc; }
.avatar-item.active { border-color: #e60012; box-shadow: 0 0 0 3px rgba(230, 0, 18, 0.15); }
.avatar-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.avatar-actions { display: flex; justify-content: flex-end; gap: 10px; }

/* 响应式 */
@media (max-width: 1200px) {
  .merchant-header-inner {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 14px 20px;
    min-height: auto;
  }

  .merchant-site-header {
    min-height: auto;
  }

  .merchant-site-nav {
    justify-content: flex-start;
    gap: 14px;
    overflow-x: auto;
  }

  .merchant-nav-link {
    padding: 6px 0 10px;
  }

  .merchant-site-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .merchant-site-search :deep(.el-input) {
    width: min(220px, 50vw);
  }

  .main-content {
    padding: 16px 20px;
  }

  .dashboard-shell .main-content {
    padding: 18px 16px 0;
  }

  .right-sidebar {
    display: none;
  }
}
</style>
