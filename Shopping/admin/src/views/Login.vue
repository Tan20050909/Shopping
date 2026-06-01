<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-decor-circle c1"></div>
      <div class="login-decor-circle c2"></div>
      <div class="login-decor-circle c3"></div>
      <div class="login-brand">
        <div class="brand-logo">
          <svg viewBox="0 0 40 40" width="48" height="48">
            <rect width="40" height="40" rx="8" fill="white"/>
            <path d="M12 12 C12 6 16 3 20 3 C24 3 28 6 28 12" fill="none" stroke="#E60012" stroke-width="2.5" stroke-linecap="round"/>
            <text x="20" y="28" text-anchor="middle" fill="#E60012" font-size="16" font-weight="bold" font-family="Arial">A</text>
          </svg>
        </div>
        <div class="brand-name"><span style="color:#fff">All</span><span style="color:rgba(255,255,255,0.8)">Mart</span></div>
        <div class="brand-slogan">精选好物 开心生活</div>
      </div>
      <div class="login-features">
        <div class="feature-item">
          <div class="feature-dot"></div>
          <div>
            <div class="feature-title">精选好物</div>
            <div class="feature-desc">严选品质，发现生活之美</div>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-dot"></div>
          <div>
            <div class="feature-title">智能运营</div>
            <div class="feature-desc">数据驱动，AI助手赋能决策</div>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-dot"></div>
          <div>
            <div class="feature-title">安全可靠</div>
            <div class="feature-desc">全链路保障，放心管理</div>
          </div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form-wrap">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录管理后台</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" :prefix-icon="Lock" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button>
          </el-form-item>
        </el-form>
        <div class="form-footer">
          <span>AllMart 管理平台</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api/admin'
import { hasPermission } from '../utils/permission'


const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const resolveHomePath = () => {
  // 各角色最优先进入的页面
  const rolePaths = [
    ['ORDER_ABNORMAL', '/abnormal'],       // 风控→异常订单
    ['CHAT_MGMT', '/after-sale'],           // 客服→售后管理
    ['FINANCE_MGMT', '/order'],             // 财务→订单管理
    ['LOG_VIEW', '/report'],                // 审计→数据报表
    ['REVIEW_MGMT', '/goods'],              // 内容审核→商品管理
    ['AFTER_SALE_MGMT', '/after-sale'],     // 有售后权限→售后
    ['MERCHANT_MGMT', '/merchant'],         // 运营→商户管理
    ['GOODS_MGMT', '/goods'],              // 运营→商品管理
    ['ORDER_MGMT', '/order'],              // 订单相关→订单
    ['DASHBOARD_VIEW', '/dashboard'],       // 默认→首页
  ]
  return rolePaths.find(([permission]) => hasPermission(permission))?.[1] || '/dashboard'
}

const handleLogin = async () => {

  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    localStorage.setItem('admin_token', res.data.token)
    localStorage.setItem('admin_info', JSON.stringify(res.data))
    ElMessage.success('登录成功')
    router.push(resolveHomePath())

  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}
.login-left {
  flex: 1;
  background: #E60012;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.login-decor-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.06);
}
.login-decor-circle.c1 {
  top: -120px;
  right: -80px;
  width: 400px;
  height: 400px;
}
.login-decor-circle.c2 {
  bottom: -60px;
  left: -60px;
  width: 280px;
  height: 280px;
}
.login-decor-circle.c3 {
  top: 40%;
  right: 10%;
  width: 120px;
  height: 120px;
  background: rgba(255,255,255,0.04);
}
.brand-logo {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.brand-name {
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: 12px;
}
.brand-slogan {
  font-size: 18px;
  opacity: 0.75;
  margin-bottom: 64px;
  letter-spacing: 2px;
}
.login-features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
}
.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  flex-shrink: 0;
}
.feature-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 2px;
}
.feature-desc {
  font-size: 13px;
  opacity: 0.65;
}
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
}
.login-form-wrap {
  width: 100%;
  max-width: 360px;
}
.form-header {
  margin-bottom: 40px;
}
.form-header h2 {
  font-size: 28px;
  color: var(--text-main);
  font-weight: 700;
  margin-bottom: 8px;
}
.form-header p {
  color: var(--text-muted);
  font-size: 15px;
}
.login-btn {
  width: 100%;
  height: 48px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: var(--radius-pill) !important;
  background: #E60012 !important;
  border-color: #E60012 !important;
}
.login-btn:hover {
  background: #C4000F !important;
  border-color: #C4000F !important;
}
.form-footer {
  text-align: center;
  margin-top: 32px;
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 768px) {
  .login-left { display: none; }
  .login-right { width: 100%; }
}
</style>
