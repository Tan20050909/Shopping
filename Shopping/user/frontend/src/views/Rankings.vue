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

onMounted(load)
</script>

<template>
  <main class="rankings-page">
    <section class="page-hero">
      <div class="container">
        <span class="page-kicker">Rankings</span>
        <h1>热门榜单</h1>
        <p>先按销量、收藏热度和评分做展示。</p>
        <div class="row page-hero-actions">
          <el-radio-group v-model="type" @change="load">
            <el-radio-button label="sales">销量榜</el-radio-button>
            <el-radio-button label="favorite">热度榜</el-radio-button>
            <el-radio-button label="rating">好评榜</el-radio-button>
          </el-radio-group>
          <el-button v-if="!isMerchantRole" @click="router.push('/cart')">购物车</el-button>
          <el-button @click="router.push('/products')">返回商品分类</el-button>
        </div>
      </div>
    </section>

    <section class="page stack">
      <div v-if="items.length" class="grid">
        <div v-for="(item, index) in items" :key="item.goods_id || item.goodsId" class="rank-card stack">
          <span class="rank-badge">TOP {{ index + 1 }}</span>
          <ProductCard :item="item" />
        </div>
      </div>
      <section v-else class="band stack empty-state">
        <strong>榜单数据还没准备好</strong>
        <p class="muted">当前没有快照时，后端会尝试回退到销量或评分排序。要是还是空的，先去商品页补测试数据。</p>
        <el-button type="primary" @click="router.push('/products')">去商品分类</el-button>
      </section>
    </section>
  </main>
</template>

<style scoped>
.rank-card {
  gap: 10px;
}

.rank-badge {
  display: inline-flex;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 700;
}

.empty-state {
  padding: 28px 20px;
}
</style>
