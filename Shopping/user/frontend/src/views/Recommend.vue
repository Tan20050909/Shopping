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
    <section class="page-hero">
      <div class="container">
        <span class="page-kicker">Recommend</span>
        <h1>精选推荐</h1>
        <p>根据最近浏览和收藏，先给你一版规则推荐。</p>
        <div class="row page-hero-actions">
          <el-button v-if="!isMerchantRole" @click="router.push('/cart')">去购物车</el-button>
          <el-button v-if="!isMerchantRole" @click="router.push('/profile')">个人中心</el-button>
          <el-button @click="router.push('/products')">返回商品分类</el-button>
        </div>
      </div>
    </section>

    <section class="page stack">
      <div v-if="items.length" class="grid">
        <ProductCard v-for="item in items" :key="item.goods_id || item.goodsId" :item="item" />
      </div>
      <section v-else class="band stack empty-state">
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
.empty-state {
  padding: 28px 20px;
}
</style>
