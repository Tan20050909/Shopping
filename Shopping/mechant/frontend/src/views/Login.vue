<template>
  <div class="login-page">
    <div class="login-top-tabs">
      <button class="top-tab" :class="{ active: activeRole === 'user' }" type="button" @click="activeRole = 'user'">个人用户登录</button>
      <button class="top-tab" :class="{ active: activeRole === 'merchant' }" type="button" @click="activeRole = 'merchant'">企业用户登录</button>
    </div>

    <div class="login-card">
      <section class="card-left" aria-hidden="true">
        <div class="brand-block">
          <div v-if="activeRole === 'merchant'" class="brand-title">
            <span class="brand-all">All</span><span class="brand-mart">Mart</span> 企业购
          </div>
          <div v-else class="brand-title">
            <span class="brand-all">All</span><span class="brand-mart">Mart</span>
          </div>
          <div v-if="activeRole === 'merchant'" class="brand-slogan">省钱 · 省心 · 省时间</div>
          <div v-else class="brand-slogan">精选好物 · 放心下单 · 送货更快</div>
        </div>

        <div class="feature-list">
          <template v-if="activeRole === 'merchant'">
            <div class="feature-item">
              <span class="feature-icon">采</span>
              <div class="feature-text">
                <div class="feature-title">优选集采</div>
                <div class="feature-sub">企业采购更省</div>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">票</span>
              <div class="feature-text">
                <div class="feature-title">开票无忧</div>
                <div class="feature-sub">发票管理清晰</div>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">服</span>
              <div class="feature-text">
                <div class="feature-title">专属服务</div>
                <div class="feature-sub">售前售后更快</div>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">护</span>
              <div class="feature-text">
                <div class="feature-title">平台保障</div>
                <div class="feature-sub">交易更安心</div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="feature-item">
              <span class="feature-icon">真</span>
              <div class="feature-text">
                <div class="feature-title">正品保障</div>
                <div class="feature-sub">品质严选更放心</div>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">快</span>
              <div class="feature-text">
                <div class="feature-title">快速发货</div>
                <div class="feature-sub">下单后尽快发出</div>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">惠</span>
              <div class="feature-text">
                <div class="feature-title">优惠活动</div>
                <div class="feature-sub">领券更划算</div>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-icon">售</span>
              <div class="feature-text">
                <div class="feature-title">售后无忧</div>
                <div class="feature-sub">问题处理更及时</div>
              </div>
            </div>
          </template>
        </div>
      </section>

      <section class="card-right">
        <div class="right-tabs">
          <button
            v-if="activeRole === 'user'"
            class="right-tab"
            :class="{ active: userTab === 'login' }"
            type="button"
            @click="userTab = 'login'"
          >密码登录</button>
          <button
            v-if="activeRole === 'user'"
            class="right-tab"
            :class="{ active: userTab === 'register' }"
            type="button"
            @click="userTab = 'register'"
          >注册</button>

          <button
            v-if="activeRole === 'merchant'"
            class="right-tab"
            :class="{ active: merchantTab === 'login' }"
            type="button"
            @click="merchantTab = 'login'"
          >密码登录</button>
          <button
            v-if="activeRole === 'merchant'"
            class="right-tab"
            :class="{ active: merchantTab === 'register' }"
            type="button"
            @click="merchantTab = 'register'"
          >注册</button>
        </div>

        <div v-if="activeRole === 'user' && userTab === 'login'" class="form-wrap">
          <el-form :model="userLoginForm" label-position="top">
            <el-form-item label="手机号">
              <el-input v-model="userLoginForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="userLoginForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item class="btn-row">
              <el-button type="primary" class="primary-btn" @click="handleUserLogin">登录</el-button>
            </el-form-item>
            <div class="link-row">
              <button class="link" type="button" @click="userTab = 'register'">立即注册</button>
              <button class="link" type="button" @click="router.push('/')">先逛逛</button>
            </div>
          </el-form>
        </div>

        <div v-else-if="activeRole === 'user' && userTab === 'register'" class="form-wrap">
          <el-form :model="userRegisterForm" label-position="top">
            <el-form-item label="手机号">
              <el-input v-model="userRegisterForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="userRegisterForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item class="btn-row">
              <el-button type="primary" class="primary-btn" @click="handleUserRegister">注册并登录</el-button>
            </el-form-item>
            <div class="link-row">
              <button class="link" type="button" @click="userTab = 'login'">返回登录</button>
              <span class="muted">注册后可在个人中心完善资料</span>
            </div>
          </el-form>
        </div>

        <div v-else-if="activeRole === 'merchant' && merchantTab === 'login'" class="form-wrap">
          <el-form :model="merchantLoginForm" label-position="top">
            <el-form-item label="手机号">
              <el-input v-model="merchantLoginForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="merchantLoginForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item class="btn-row">
              <el-button type="primary" class="primary-btn" @click="handleMerchantLogin">登录</el-button>
            </el-form-item>
            <div class="link-row">
              <button class="link" type="button" @click="merchantTab = 'register'">立即注册</button>
              <span class="muted">新商家可先注册再入驻</span>
            </div>
          </el-form>
        </div>

        <div v-else class="form-wrap">
          <el-form :model="registerForm" label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item class="btn-row">
              <el-button type="primary" class="primary-btn" @click="handleRegister">注册</el-button>
            </el-form-item>
            <div class="link-row">
              <button class="link" type="button" @click="merchantTab = 'login'">返回登录</button>
              <span class="muted">注册后可提交入驻材料</span>
            </div>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { merchantUserApi } from '../api'
import { ElMessage } from 'element-plus'

const AUTH_ROLE_KEY = 'shopping_auth_role'
const USER_TOKEN_KEY = 'shopping_user_token'

const route = useRoute()
const router = useRouter()

const activeRole = ref(route.query?.tab === 'merchant' ? 'merchant' : 'user')
const userTab = ref('login')
const merchantTab = ref('login')

const redirectTarget = computed(() => {
  const v = String(route.query?.redirect || '').trim()
  return v || ''
})

const backUrl = computed(() => {
  const v = String(route.query?.back || '').trim()
  return v || ''
})

const userLoginForm = ref({
  phone: '',
  password: ''
})

const userRegisterForm = ref({
  phone: '',
  password: ''
})

const merchantLoginForm = ref({
  phone: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  phone: ''
})

const setAuthRole = (role) => {
  sessionStorage.setItem(AUTH_ROLE_KEY, role)
}

const resolveRedirect = (role, fallback) => {
  const raw = redirectTarget.value
  if (!raw) return fallback
  let resolved
  try {
    resolved = router.resolve(raw)
  } catch (e) {
    return fallback
  }
  const layout = String(resolved?.matched?.[resolved.matched.length - 1]?.meta?.layout || '')
  if (role === 'merchant' && layout !== 'merchant') return fallback
  if (role === 'user' && layout !== 'user') return fallback
  return raw
}

const handleUserLogin = async () => {
  if (!userLoginForm.value.phone || !userLoginForm.value.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }

  let payload
  try {
    const resp = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userLoginForm.value)
    })
    payload = await resp.json()
    if (!resp.ok || payload?.code !== 'SUCCESS') {
      throw new Error(payload?.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error(error?.message || '登录失败')
    return
  }

  const data = payload?.data || {}
  if (data.token) {
    localStorage.setItem(USER_TOKEN_KEY, String(data.token))
    window.dispatchEvent(new Event('shopping-user-token'))
  }
  setAuthRole('user')
  ElMessage.success('登录成功')
  const back = backUrl.value
  if (back.startsWith('http://localhost:5173') || back.startsWith('http://127.0.0.1:5173')) {
    try {
      const u = new URL(back)
      if (data.token) u.searchParams.set('token', String(data.token))
      u.searchParams.set('role', 'user')
      window.location.href = u.toString()
    } catch (e) {
      window.location.href = back
    }
    return
  }
  router.push(resolveRedirect('user', '/'))
}

const handleUserRegister = async () => {
  if (!userRegisterForm.value.phone || !userRegisterForm.value.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }

  let payload
  try {
    const resp = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userRegisterForm.value)
    })
    payload = await resp.json()
    if (!resp.ok || payload?.code !== 'SUCCESS') {
      throw new Error(payload?.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error(error?.message || '注册失败')
    return
  }

  const data = payload?.data || {}
  if (data.token) {
    localStorage.setItem(USER_TOKEN_KEY, String(data.token))
    window.dispatchEvent(new Event('shopping-user-token'))
  }
  setAuthRole('user')
  ElMessage.success('注册成功')
  const back = backUrl.value
  if (back.startsWith('http://localhost:5173') || back.startsWith('http://127.0.0.1:5173')) {
    try {
      const u = new URL(back)
      if (data.token) u.searchParams.set('token', String(data.token))
      u.searchParams.set('role', 'user')
      window.location.href = u.toString()
    } catch (e) {
      window.location.href = back
    }
    return
  }
  router.push(resolveRedirect('user', '/'))
}

const handleMerchantLogin = async () => {
  if (!merchantLoginForm.value.phone || !merchantLoginForm.value.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }

  let res
  try {
    res = await merchantUserApi.login(merchantLoginForm.value)
  } catch (error) {
    ElMessage.error(error?.message || '登录失败')
    return
  }

  if (res?.data?.success) {
    const data = res.data.data || {}
    localStorage.setItem('merchantUser', JSON.stringify({
      ...data,
      merchantId: data.merchantId || data.id,
      merchantName: data.merchantName || data.shopName || data.username,
      source: 'login'
    }))
    setAuthRole('merchant')
    ElMessage.success('登录成功')
    router.push(resolveRedirect('merchant', '/'))
    return
  }

  ElMessage.error(res?.data?.message || '登录失败')
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  if (!registerForm.value.phone) {
    ElMessage.warning('请输入手机号')
    return
  }

  let res
  try {
    res = await merchantUserApi.register(registerForm.value)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '注册失败')
    return
  }
  if (res?.data?.success) {
    ElMessage.success('注册成功，请登录')
    merchantTab.value = 'login'
    return
  }
  ElMessage.error(res?.data?.message || '注册失败')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 26px 16px 34px;
}

.login-top-tabs {
  display: flex;
  gap: 42px;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.top-tab {
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 16px;
  font-weight: 900;
  color: rgba(100, 116, 139, 0.95);
  cursor: pointer;
  position: relative;
}

.top-tab.active {
  color: rgba(15, 23, 42, 0.95);
}

.top-tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 64px;
  height: 3px;
  border-radius: 999px;
  background: #E60012;
  transform: translateX(-50%);
}

.login-card {
  width: min(980px, 100%);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.08);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  overflow: hidden;
}

.card-left {
  padding: 44px 46px;
  border-right: 1px solid rgba(226, 232, 240, 0.9);
}

.brand-title {
  font-size: 34px;
  font-weight: 1000;
  letter-spacing: 0.2px;
  color: rgba(15, 23, 42, 0.96);
}

.brand-all {
  color: rgba(15, 23, 42, 0.92);
}

.brand-mart {
  color: #E60012;
}

.brand-slogan {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 800;
  color: rgba(100, 116, 139, 0.95);
}

.feature-list {
  margin-top: 26px;
  display: grid;
  gap: 16px;
}

.feature-item {
  display: grid;
  grid-template-columns: 30px 1fr;
  gap: 12px;
  align-items: center;
}

.feature-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(230, 0, 18, 0.08);
  color: #E60012;
  border: 1px solid rgba(230, 0, 18, 0.18);
  display: grid;
  place-items: center;
  font-weight: 1000;
  font-size: 12px;
}

.feature-title {
  font-size: 13px;
  font-weight: 900;
  color: rgba(15, 23, 42, 0.92);
}

.feature-sub {
  margin-top: 3px;
  font-size: 12px;
  font-weight: 700;
  color: rgba(100, 116, 139, 0.9);
}

.card-right {
  padding: 38px 44px;
}

.right-tabs {
  display: flex;
  gap: 18px;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.right-tab {
  border: none;
  background: transparent;
  padding: 8px 2px;
  font-size: 14px;
  font-weight: 900;
  color: rgba(100, 116, 139, 0.95);
  cursor: pointer;
  position: relative;
}

.right-tab.active {
  color: #E60012;
}

.right-tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 56px;
  height: 2px;
  border-radius: 999px;
  background: #E60012;
  transform: translateX(-50%);
}

.form-wrap {
  margin-top: 8px;
}

.btn-row :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.primary-btn {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  font-weight: 900;
  background: #E60012;
  border-color: #E60012;
  color: #fff;
}

.primary-btn:hover {
  background: #C4000F;
  border-color: #C4000F;
}

.link-row {
  margin-top: -2px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}

.link {
  border: none;
  background: transparent;
  color: rgba(100, 116, 139, 0.95);
  font-weight: 800;
  cursor: pointer;
  padding: 6px 0;
}

.link:hover {
  color: #E60012;
}

.muted {
  color: rgba(148, 163, 184, 0.98);
  font-weight: 700;
}

:deep(.el-form-item) {
  margin-bottom: 14px;
}

:deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-weight: 900;
  color: rgba(15, 23, 42, 0.9);
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  background: rgba(241, 245, 249, 0.75);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: none;
}

@media (max-width: 980px) {
  .login-card {
    grid-template-columns: 1fr;
  }
  .card-left {
    display: none;
  }
  .card-right {
    padding: 34px 26px;
  }
}
</style>
