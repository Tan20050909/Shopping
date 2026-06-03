<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, fallbackImageOf, imageOf } from '../api/client'

const router = useRouter()
const afterSales = ref([])
const loading = ref(false)
const handlingId = ref(null)
const activeFilter = ref('all')

const filterTabs = [
  { key: 'apply', label: '从订单申请' },
  { key: 'all', label: '全部售后' },
  { key: 'pending', label: '待审核' },
  { key: 'processing', label: '售后中' },
  { key: 'done', label: '已完成' }
]

const filteredAfterSales = computed(() => {
  if (activeFilter.value === 'all' || activeFilter.value === 'apply') return afterSales.value
  return afterSales.value.filter((item) => matchFilter(item, activeFilter.value))
})
const hasRecords = computed(() => filteredAfterSales.value.length > 0)

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusText(item) {
  const handleStatus = Number(field(item, 'handleStatus', 'handle_status', -1))
  const refundStatus = Number(field(item, 'refundStatus', 'refund_status', -1))
  if (handleStatus === 4 || refundStatus === 2) return '退款成功'
  if (handleStatus === 2) return '商家已驳回'
  if (refundStatus === 1) return '退款处理中'
  if (handleStatus === 3) return '售后已完成'
  if (handleStatus === 1) return '商家已同意'
  if (handleStatus === 5) return '售后关闭'
  return '待审核'
}

function canCancel(item) {
  const handleStatus = Number(field(item, 'handleStatus', 'handle_status', -1))
  const refundStatus = Number(field(item, 'refundStatus', 'refund_status', -1))
  if (refundStatus === 1 || refundStatus === 2) return false
  return handleStatus === 0 || handleStatus === 3
}

function matchFilter(item, filterKey) {
  const handleStatus = Number(field(item, 'handleStatus', 'handle_status', -1))
  const refundStatus = Number(field(item, 'refundStatus', 'refund_status', -1))
  if (filterKey === 'pending') return handleStatus === 0
  if (filterKey === 'processing') return handleStatus === 1 || refundStatus === 1
  if (filterKey === 'done') return handleStatus === 3 || handleStatus === 4 || handleStatus === 5 || refundStatus === 2
  return true
}

function chooseFilter(filterKey) {
  if (filterKey === 'apply') {
    router.push('/orders')
    return
  }
  activeFilter.value = filterKey
}

async function load() {
  loading.value = true
  try {
    afterSales.value = await api('/api/user/after-sales')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function cancelAfterSale(item) {
  if (!item || handlingId.value) return
  if (!canCancel(item)) return
  const id = field(item, 'afterSaleId', 'after_sale_id')
  if (!id) return
  try {
    await ElMessageBox.confirm('确认取消该售后申请？', '提示', {
      confirmButtonText: '确认取消',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  handlingId.value = String(id)
  try {
    await api(`/api/user/after-sales/${id}/cancel`, { method: 'PUT' })
    ElMessage.success('已取消售后')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    handlingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <main class="after-sales-page">
    <section class="allmart-page-hero">
      <div class="container">
        <div class="allmart-hero-inner">
          <span class="allmart-page-kicker">AFTER SALES</span>
          <h1 class="allmart-page-title">我的售后</h1>
          <p class="allmart-page-subtitle">查看售后申请、退款金额和处理进度。</p>
          <div class="allmart-chip-tabs allmart-hero-actions" aria-label="售后筛选">
            <button
              v-for="tab in filterTabs"
              :key="tab.key"
              type="button"
              class="allmart-chip"
              :class="{ active: activeFilter === tab.key }"
              :aria-selected="activeFilter === tab.key"
              @click="chooseFilter(tab.key)"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="page stack after-sales-content allmart-after-hero-page">
      <section v-loading="loading" class="after-sale-list">
      <el-empty v-if="!loading && !hasRecords" description="目前还没有售后记录" />

      <article v-for="item in filteredAfterSales" :key="field(item, 'afterSaleId', 'after_sale_id')" class="after-sale-card">
        <img class="cover item-cover" :src="imageOf(item)" :alt="field(item, 'goodsName', 'goods_name')" @error="(e) => (e.target.src = fallbackImageOf(item))" />
        <div class="stack item-info">
          <div class="row title-row">
            <strong>{{ field(item, 'goodsName', 'goods_name') }}</strong>
            <span class="status-chip">{{ statusText(item) }}</span>
          </div>
          <span class="muted">SKU：{{ field(item, 'skuName', 'sku_name') || '默认规格' }}</span>
          <span class="muted">申请时间：{{ field(item, 'applyTime', 'apply_time') }}</span>
          <strong class="price">退款金额 ¥{{ money(field(item, 'applyAmount', 'apply_amount')) }}</strong>
          <div class="row">
            <el-button @click="router.push(`/after-sales/${field(item, 'afterSaleId', 'after_sale_id')}`)">查看详情</el-button>
            <el-button
              v-if="canCancel(item)"
              type="danger"
              plain
              :loading="handlingId === String(field(item, 'afterSaleId', 'after_sale_id'))"
              @click="cancelAfterSale(item)"
            >
              取消售后
            </el-button>
          </div>
        </div>
      </article>
      </section>
    </section>
  </main>
</template>

<style scoped>
.after-sales-page {
  background: #fff;
}

.after-sales-content {
  gap: 24px;
}

.after-sale-list {
  min-height: 260px;
  display: grid;
  gap: 16px;
}

.after-sale-card {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #fff;
}

.item-cover {
  border-radius: 8px;
}

.item-info {
  gap: 8px;
}

.title-row {
  justify-content: space-between;
  align-items: flex-start;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 700;
}

@media (max-width: 720px) {
  .title-row {
    align-items: stretch;
    flex-direction: column;
  }

  .after-sale-card {
    grid-template-columns: 88px minmax(0, 1fr);
  }
}
</style>
