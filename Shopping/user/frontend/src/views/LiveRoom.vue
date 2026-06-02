<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, fallbackImageOf, imageOf } from '../api/client'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const live = ref(null)
const comments = ref([])
const content = ref('')
const loading = ref(false)
const sending = ref(false)
const redirecting = ref(false)
const roomStatusText = computed(() => ({
  0: '未开播',
  1: '直播中',
  2: '已结束',
  3: '已中断'
}))

const externalLiveUrl = computed(() => {
  const v = live.value
  const url = String(v?.live_url ?? v?.liveUrl ?? '').trim()
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return ''
})

function goExternal() {
  if (!externalLiveUrl.value) return
  window.location.href = externalLiveUrl.value
}

async function load() {
  loading.value = true
  try {
    live.value = await api(`/api/user/live-rooms/${route.params.id}`)
    if (externalLiveUrl.value) {
      redirecting.value = true
      goExternal()
      return
    }
    comments.value = await api(`/api/user/live-rooms/${route.params.id}/comments`)
  } catch (error) {
    ElMessage.error(error.message)
    live.value = null
  } finally {
    loading.value = false
  }
}

async function send() {
  if (!content.value.trim()) {
    ElMessage.warning('先输入评论内容')
    return
  }
  sending.value = true
  try {
    await api(`/api/user/live-rooms/${route.params.id}/comments`, {
      method: 'POST',
      body: { content: content.value.trim(), commentType: 1 }
    })
    content.value = ''
    comments.value = await api(`/api/user/live-rooms/${route.params.id}/comments`)
    ElMessage.success('评论已发送')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    sending.value = false
  }
}

function onLiveCoverError(event) {
  const el = event?.target
  if (!el || el.dataset.fallbackApplied === '1') return
  el.dataset.fallbackApplied = '1'
  el.src = fallbackImageOf(live.value || {})
}

onMounted(load)
</script>

<template>
  <main class="page stack">
    <section v-if="live" class="band live-block">
      <div v-if="redirecting" class="stack empty-state" style="grid-column: 1 / -1;">
        <strong>正在进入直播间…</strong>
        <p class="muted">如果没有自动跳转，请检查商家填写的直播链接是否以 http:// 或 https:// 开头。</p>
        <div class="row">
          <el-button type="primary" @click="goExternal">立即跳转</el-button>
          <el-button @click="router.push('/live')">返回直播广场</el-button>
        </div>
      </div>
      <img v-if="!redirecting" class="cover" :src="imageOf(live)" :alt="live.live_title" @error="onLiveCoverError" />
      <div v-if="!redirecting" class="stack">
        <div>
          <div class="row room-head">
            <h2>{{ live.live_title }}</h2>
            <el-button @click="router.push('/live')">返回直播广场</el-button>
          </div>
          <p class="muted">{{ live.merchant_name }} / {{ live.live_theme }} / 观看 {{ live.watch_num }}</p>
          <div class="row room-meta">
            <span class="status-chip">{{ roomStatusText[live.live_status] || `状态 ${live.live_status}` }}</span>
            <span class="muted">互动 {{ live.interact_num || 0 }}</span>
          </div>
        </div>
        <div class="stack">
          <div class="row room-head">
            <strong>直播商品</strong>
            <span class="muted">{{ live.products?.length || 0 }} 件在讲解</span>
          </div>
          <div class="grid">
          <ProductCard v-for="item in live.products" :key="item.lg_id" :item="item" />
          </div>
        </div>
        <div class="stack">
          <div class="row room-head">
            <strong>直播评论</strong>
            <el-button size="small" @click="load">刷新评论</el-button>
          </div>
          <div class="row">
            <el-input v-model="content" placeholder="发一条评论" maxlength="300" />
            <el-button type="primary" :loading="sending" @click="send">发送</el-button>
          </div>
          <div v-if="comments.length" class="stack comments-block">
            <p v-for="comment in comments" :key="comment.comment_id" class="muted">{{ comment.nickname || '用户' }}：{{ comment.content }}</p>
          </div>
          <p v-else class="muted">还没有评论，发第一条试试。</p>
        </div>
      </div>
    </section>
    <section v-else-if="!loading" class="band stack empty-state">
      <strong>直播间不存在或还没加载出来</strong>
      <div class="row">
        <el-button type="primary" @click="router.push('/live')">返回直播广场</el-button>
        <el-button @click="router.push('/products')">去商品列表</el-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.live-block {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 18px;
}

.room-head {
  justify-content: space-between;
}

.room-meta {
  gap: 10px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 600;
}

.comments-block {
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-soft);
}

.empty-state {
  padding: 28px 20px;
}

@media (max-width: 860px) {
  .live-block {
    grid-template-columns: 1fr;
  }
}
</style>
