<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const items = ref([])
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

async function load() {
  loading.value = true
  try {
    const qs = merchantId.value ? `?merchantId=${encodeURIComponent(String(merchantId.value))}` : ''
    items.value = await api(`/api/user/recommendations${qs}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="recommend-page">
    <section class="allmart-page-hero">
      <div class="container">
        <div class="allmart-hero-inner">
          <span class="allmart-page-kicker">RECOMMEND</span>
          <h1 class="allmart-page-title">精选推荐</h1>
          <p class="allmart-page-subtitle">基于真实推荐接口展示更适合你的商品，保留收藏、加购和立即购买的完整链路。</p>
          <div class="allmart-chip-tabs allmart-hero-actions" aria-label="精选推荐入口">
            <button type="button" class="allmart-chip active" aria-selected="true">为你推荐</button>
            <button type="button" class="allmart-chip" @click="router.push('/rankings')">热门精选</button>
            <button type="button" class="allmart-chip" @click="router.push('/products')">新品好物</button>
            <button v-if="!isMerchantRole" type="button" class="allmart-chip" @click="router.push('/cart')">购物车</button>
          </div>
        </div>
      </div>
    </section>

    <section class="page recommend-content allmart-after-hero-page">
      <div v-if="items.length" class="allmart-product-grid recommend-grid" v-loading="loading">
        <ProductCard v-for="item in items" :key="item.goods_id || item.goodsId" :item="item" />
      </div>
      <section v-else class="allmart-empty-state" v-loading="loading">
        <strong>暂时还没有推荐结果</strong>
        <p class="muted">先去看看商品、点点详情、收藏几件商品，推荐页很快就会有内容。</p>
        <div class="row">
          <el-button type="primary" @click="router.push('/products')">去商品分类</el-button>
          <el-button v-if="!isMerchantRole" @click="router.push('/profile/history')">看浏览记录</el-button>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.recommend-page {
  background: #fff;
}

.recommend-content {
  display: grid;
  gap: 24px;
}

.recommend-grid {
  justify-content: start;
}

.recommend-grid > :deep(*) {
  min-width: 0;
}

@media (min-width: 1081px) {
  .recommend-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .recommend-grid:has(> :only-child) {
    grid-template-columns: minmax(0, 276px);
  }

  .recommend-grid:has(> :nth-child(2):last-child) {
    grid-template-columns: repeat(2, minmax(0, 276px));
  }

  .recommend-grid:has(> :nth-child(3):last-child) {
    grid-template-columns: repeat(3, minmax(0, 276px));
  }
}
</style>
