<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, imageOf } from '../api/client'

const router = useRouter()
const lives = ref([])
const authRole = computed(() => String(sessionStorage.getItem('shopping_auth_role') || ''))
const isMerchantRole = computed(() => authRole.value === 'merchant')
const merchantId = computed(() => {
  if (!isMerchantRole.value) return null
  try {
    const raw = localStorage.getItem('merchantUser')
    const parsed = raw ? JSON.parse(raw) : null
    const id = parsed?.merchantId ?? parsed?.id
    const num = Number(id)
    return Number.isFinite(num) && num > 0 ? num : null
  } catch (e) {
    return null
  }
})
const statusText = {
  0: '未开播',
  1: '直播中',
  2: '已结束',
  3: '已中断'
}

const visibleLives = computed(() => {
  const list = Array.isArray(lives.value) ? lives.value : []
  return list.filter((item) => {
    const st = Number(item?.live_status)
    return st === 0 || st === 1
  })
})

async function load() {
  try {
    const qs = merchantId.value ? `?merchantId=${encodeURIComponent(String(merchantId.value))}` : ''
    lives.value = await api(`/api/user/live-rooms${qs}`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <main class="live-list-page">
    <section class="page-hero">
      <div class="container">
        <span class="page-kicker">Live</span>
        <h1>直播广场</h1>
        <p>当前阶段先做列表、商品和评论互动，不接真实视频流。</p>
        <div class="row page-hero-actions">
          <el-button @click="router.push('/products')">返回商品分类</el-button>
        </div>
      </div>
    </section>

    <section class="page stack">
      <div v-if="visibleLives.length" class="grid live-grid">
        <router-link v-for="live in visibleLives" :key="live.live_id" class="live-card" :to="`/live/${live.live_id}`">
          <img class="cover" :src="imageOf(live)" :alt="live.live_title" />
          <div class="stack">
            <strong>{{ live.live_title }}</strong>
            <span class="muted">{{ live.merchant_name }} / {{ live.live_theme }}</span>
            <div class="row">
              <span class="live-status">{{ statusText[live.live_status] || `状态 ${live.live_status}` }}</span>
              <span class="price">观看 {{ live.watch_num || 0 }}</span>
              <span class="muted">互动 {{ live.interact_num || 0 }}</span>
            </div>
          </div>
        </router-link>
      </div>
      <section v-else class="band stack empty-state">
        <strong>暂时还没有直播间</strong>
        <p class="muted">等直播测试数据补好后，这里就会展示直播列表。</p>
        <el-button type="primary" @click="router.push('/products')">先去商品分类</el-button>
      </section>
    </section>
  </main>
</template>

<style scoped>
.live-grid {
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}

.live-card {
  display: grid;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.live-status {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 600;
}

.empty-state {
  padding: 28px 20px;
}
</style>
