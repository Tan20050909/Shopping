<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, fallbackImageOf, imageOf } from '../api/client'

const router = useRouter()
const lives = ref([])
const loading = ref(false)
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
  loading.value = true
  try {
    const qs = merchantId.value ? `?merchantId=${encodeURIComponent(String(merchantId.value))}` : ''
    lives.value = await api(`/api/user/live-rooms${qs}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function onLiveCoverError(event, live) {
  const el = event?.target
  if (!el || el.dataset.fallbackApplied === '1') return
  el.dataset.fallbackApplied = '1'
  el.src = fallbackImageOf(live)
}

function liveStatusClass(live) {
  return Number(live?.live_status) === 1 ? 'is-live' : 'is-upcoming'
}

onMounted(load)
</script>

<template>
  <main class="live-list-page">
    <section class="allmart-page-hero">
      <div class="container">
        <div class="allmart-hero-inner">
          <span class="allmart-page-kicker">LIVE</span>
          <h1 class="allmart-page-title">直播广场</h1>
          <p class="allmart-page-subtitle">当前阶段先做列表、商品和评论互动，不接真实视频流。</p>
          <div class="allmart-chip-tabs allmart-hero-actions" aria-label="直播入口">
            <button type="button" class="allmart-chip active" aria-selected="true">直播列表</button>
            <button type="button" class="allmart-chip" @click="router.push('/products')">返回商品分类</button>
          </div>
        </div>
      </div>
    </section>

    <section class="page live-content allmart-after-hero-page">
      <div v-if="visibleLives.length" class="live-grid" v-loading="loading">
        <router-link v-for="live in visibleLives" :key="live.live_id" class="live-card allmart-card" :to="`/live/${live.live_id}`">
          <div class="live-card-cover">
            <img class="cover" :src="imageOf(live)" :alt="live.live_title" @error="(e) => onLiveCoverError(e, live)" />
            <span class="live-status" :class="liveStatusClass(live)">
              {{ statusText[live.live_status] || `状态 ${live.live_status}` }}
            </span>
          </div>
          <div class="live-card-body">
            <div class="live-card-copy">
              <strong>{{ live.live_title }}</strong>
              <span class="muted">{{ live.merchant_name }}<template v-if="live.live_theme"> / {{ live.live_theme }}</template></span>
            </div>
            <div class="live-card-meta">
              <span>观看 {{ live.watch_num || 0 }}</span>
              <span>互动 {{ live.interact_num || 0 }}</span>
            </div>
            <div class="live-card-footer">
              <span class="live-card-note">商品讲解与评论互动</span>
              <span class="live-enter-link">进入直播</span>
            </div>
          </div>
        </router-link>
      </div>
      <section v-else class="allmart-empty-state live-empty-state" v-loading="loading">
        <strong>暂无直播内容</strong>
        <p class="muted">等直播测试数据补好后，这里就会展示直播列表。</p>
        <el-button type="primary" @click="router.push('/products')">返回商品分类</el-button>
      </section>
    </section>
  </main>
</template>

<style scoped>
.live-list-page {
  background: #fff;
}

.live-content {
  display: grid;
  gap: 26px;
}

.live-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.live-card {
  display: grid;
  gap: 0;
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.04);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.live-card:hover {
  transform: translateY(-4px);
  border-color: rgba(230, 0, 18, 0.16);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.08);
}

.live-card-cover {
  position: relative;
  padding: 12px 12px 0;
  background: linear-gradient(180deg, #fffafa 0%, #fbfbfb 100%);
}

.live-card-cover .cover {
  aspect-ratio: 4 / 3;
  display: block;
  border-radius: 12px;
}

.live-card-body {
  display: grid;
  gap: 14px;
  padding: 16px 18px 18px;
}

.live-card-copy {
  display: grid;
  gap: 8px;
}

.live-card-copy strong {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
  line-height: 1.35;
}

.live-card-copy .muted {
  font-size: 13px;
  line-height: 1.6;
}

.live-status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #fff1f2;
  color: var(--brand-red);
  font-size: 12px;
  font-weight: 700;
  position: absolute;
  top: 22px;
  left: 22px;
}

.live-status.is-upcoming {
  background: #faf5f5;
  color: #9b6a70;
}

.live-card-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--text-muted);
  font-size: 12px;
}

.live-card-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-top: 2px;
}

.live-card-note {
  color: var(--text-secondary);
  font-size: 12px;
}

.live-enter-link {
  color: var(--brand-red);
  font-size: 13px;
  font-weight: 800;
}

.live-enter-link::after {
  content: " >";
}

.live-empty-state {
  min-height: 220px;
}

@media (max-width: 1080px) {
  .live-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .live-grid {
    grid-template-columns: 1fr;
  }

  .live-card-body {
    padding-inline: 16px;
  }
}
</style>
