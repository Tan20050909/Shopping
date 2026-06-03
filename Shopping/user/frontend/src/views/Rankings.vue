<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const type = ref('sales')
const items = ref([])
const loading = ref(false)
const rankTypes = [
  { label: '销量榜', value: 'sales' },
  { label: '热度榜', value: 'favorite' },
  { label: '好评榜', value: 'rating' }
]
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

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams({ type: String(type.value || 'sales') })
    if (merchantId.value) params.set('merchantId', String(merchantId.value))
    items.value = await api(`/api/user/rankings?${params.toString()}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function chooseType(nextType) {
  if (type.value === nextType) return
  type.value = nextType
  load()
}

onMounted(load)
</script>

<template>
  <main class="rankings-page">
    <section class="allmart-page-hero">
      <div class="container">
        <span class="allmart-page-kicker">RANKINGS</span>
        <h1 class="allmart-page-title">热门榜单</h1>
        <p class="allmart-page-subtitle">先按销量、收藏热度和评分做展示，用真实商品数据呈现当前值得关注的 AllMart 好物。</p>
        <div class="allmart-chip-tabs" aria-label="榜单筛选">
          <button
            v-for="rankType in rankTypes"
            :key="rankType.value"
            type="button"
            class="allmart-chip"
            :class="{ active: type === rankType.value }"
            :aria-selected="type === rankType.value"
            @click="chooseType(rankType.value)"
          >
            {{ rankType.label }}
          </button>
          <button v-if="!isMerchantRole" type="button" class="allmart-chip" @click="router.push('/cart')">购物车</button>
          <button type="button" class="allmart-chip" @click="router.push('/products')">返回商品分类</button>
        </div>
      </div>
    </section>

    <section class="page rankings-content">
      <div v-if="items.length" class="allmart-product-grid" v-loading="loading">
        <div v-for="(item, index) in items" :key="item.goods_id || item.goodsId" class="rank-card">
          <span class="rank-badge">TOP {{ index + 1 }}</span>
          <ProductCard :item="item" />
        </div>
      </div>
      <section v-else class="allmart-empty-state" v-loading="loading">
        <strong>榜单数据还没准备好</strong>
        <p class="muted">当前没有快照时，后端会尝试回退到销量或评分排序。要是还是空的，先去商品页补测试数据。</p>
        <el-button type="primary" @click="router.push('/products')">去商品分类</el-button>
      </section>
    </section>
  </main>
</template>

<style scoped>
.rankings-page {
  background: #fff;
}

.rankings-content {
  display: grid;
  gap: 22px;
  padding-top: 34px;
}

.rank-card {
  position: relative;
  display: grid;
  padding-top: 14px;
}

.rank-badge {
  position: absolute;
  z-index: 2;
  top: 1px;
  left: 18px;
  display: inline-flex;
  width: fit-content;
  min-height: 24px;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  background: #ffe8ea;
  color: var(--brand-red);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 6px 14px rgba(230, 0, 18, 0.06);
}
</style>
