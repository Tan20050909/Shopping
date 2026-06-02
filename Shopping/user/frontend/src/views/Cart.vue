<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, fallbackImageOf, imageOf } from '../api/client'

const router = useRouter()
const cart = ref([])
const loading = ref(false)

function isAvailable(item) {
  return Number(item.goods_deleted || 0) === 0
    && Number(item.sku_deleted || 0) === 0
    && Number(item.goods_status || 0) === 1
    && Number(item.audit_status || 0) === 1
    && Number(item.sku_status || 0) === 1
    && Number(item.stock || 0) >= Number(item.buy_num || 0)
}

function itemStatusText(item) {
  if (Number(item.goods_deleted || 0) !== 0 || Number(item.sku_deleted || 0) !== 0) return '商品已删除'
  if (Number(item.goods_status || 0) !== 1 || Number(item.audit_status || 0) !== 1 || Number(item.sku_status || 0) !== 1) return '商品已下架'
  if (Number(item.stock || 0) < Number(item.buy_num || 0)) return '库存不足'
  return '可结算'
}

const selectableItems = computed(() => cart.value.filter(isAvailable))
const invalidItems = computed(() => cart.value.filter((item) => !isAvailable(item)))
const selectedCount = computed(() => cart.value.filter((item) => Number(item.is_selected) === 1 && isAvailable(item)).length)
const allSelected = computed(() => selectableItems.value.length > 0 && selectedCount.value === selectableItems.value.length)
const selectedAmount = computed(() => cart.value
  .filter((item) => Number(item.is_selected) === 1 && isAvailable(item))
  .reduce((sum, item) => sum + Number(item.price) * Number(item.buy_num), 0))
const selectedAvailableItems = computed(() => cart.value.filter((item) => Number(item.is_selected) === 1 && isAvailable(item)))
const selectedMerchantIds = computed(() => Array.from(new Set(selectedAvailableItems.value.map((item) => Number(item.merchant_id || 0)).filter(Boolean))))
const hasMultiMerchantSelected = computed(() => selectedMerchantIds.value.length > 1)
const merchantGroups = computed(() => {
  const groups = new Map()
  for (const item of cart.value) {
    const merchantId = Number(item.merchant_id || 0)
    if (!groups.has(merchantId)) {
      groups.set(merchantId, {
        merchantId,
        merchantName: item.merchant_name || `店铺 ${merchantId || ''}`.trim(),
        items: []
      })
    }
    groups.get(merchantId).items.push(item)
  }
  return Array.from(groups.values()).map((group) => {
    const selectable = group.items.filter(isAvailable)
    const selected = selectable.filter((item) => Number(item.is_selected) === 1)
    return {
      ...group,
      selectableCount: selectable.length,
      selectedCount: selected.length,
      selectedAmount: selected.reduce((sum, item) => sum + Number(item.price) * Number(item.buy_num), 0),
      allSelected: selectable.length > 0 && selected.length === selectable.length
    }
  })
})

async function load() {
  loading.value = true
  try {
    cart.value = await api('/api/user/cart')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function updateItem(item, patch) {
  try {
    const nextPatch = { ...patch }
    if (typeof nextPatch.num === 'number') {
      const max = Math.max(Number(item.stock || 0), 1)
      const requestedNum = Number(nextPatch.num)
      if (requestedNum > max) {
        nextPatch.num = max
        ElMessage.warning(`当前最多可购买 ${max} 件，已自动调整`)
      } else if (requestedNum < 1) {
        nextPatch.num = 1
      }
    }
    await api(`/api/user/cart/items/${item.cart_id}`, { method: 'PUT', body: nextPatch })
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function handleNumChange(item, value) {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) return
  updateItem(item, { num: numericValue })
}

async function removeItem(item) {
  try {
    await api(`/api/user/cart/items/${item.cart_id}`, { method: 'DELETE' })
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function clearChecked() {
  try {
    await api('/api/user/cart/checked', { method: 'DELETE' })
    ElMessage.success('已清空勾选商品')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function clearInvalidItems() {
  if (!invalidItems.value.length) return
  try {
    await Promise.all(invalidItems.value.map((item) => api(`/api/user/cart/items/${item.cart_id}`, { method: 'DELETE' })))
    ElMessage.success('已清理失效商品')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function toggleAllSelected(selected) {
  try {
    await api(`/api/user/cart/selected?selected=${selected}`, { method: 'PUT' })
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function toggleMerchantSelected(group, selected) {
  try {
    const tasks = group.items
      .filter((item) => isAvailable(item) && Number(item.is_selected) !== (selected ? 1 : 0))
      .map((item) => api(`/api/user/cart/items/${item.cart_id}`, { method: 'PUT', body: { selected } }))
    if (!tasks.length) return
    await Promise.all(tasks)
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function goConfirm() {
  if (!selectedCount.value) {
    ElMessage.warning('请先勾选要结算的商品')
    return
  }
  router.push('/order/confirm')
}

onMounted(load)
</script>

<template>
  <main class="page stack">
    <section class="band stack">
      <div class="row section-head">
        <div>
          <h1 class="section-title">购物车</h1>
          <p class="muted">先把商品挑好，下一步去确认订单。</p>
        </div>
        <div class="row">
          <el-button @click="router.push('/products')">继续逛逛</el-button>
          <el-button v-if="cart.length" @click="toggleAllSelected(!allSelected)">
            {{ allSelected ? '取消全选' : '全选商品' }}
          </el-button>
          <el-button v-if="selectedCount" @click="clearChecked">清空已勾选</el-button>
          <el-button v-if="invalidItems.length" @click="clearInvalidItems">清理失效商品</el-button>
        </div>
      </div>

      <div v-if="!cart.length && !loading" class="muted">购物车还是空的，先去挑点商品。</div>
      <div v-else-if="invalidItems.length" class="cart-alert">
        当前有 {{ invalidItems.length }} 件商品不可结算，建议先处理库存不足或已下架商品。
      </div>
      <div v-else-if="hasMultiMerchantSelected" class="cart-alert">
        当前勾选商品来自多个店铺，下单后会自动拆分成多个包裹，并统一支付。
      </div>

      <section v-for="group in merchantGroups" :key="group.merchantId" class="merchant-group">
        <div class="row merchant-head">
          <div class="stack merchant-meta">
            <strong>{{ group.merchantName }}</strong>
            <span class="muted">已选 {{ group.selectedCount }} / {{ group.items.length }} 件，店铺小计 ¥{{ group.selectedAmount.toFixed(2) }}</span>
          </div>
          <el-button
            v-if="group.selectableCount"
            text
            @click="toggleMerchantSelected(group, !group.allSelected)"
          >
            {{ group.allSelected ? '取消本店全选' : '全选本店商品' }}
          </el-button>
        </div>

        <div v-for="item in group.items" :key="item.cart_id" class="list-item">
          <img class="cover cart-cover" :src="imageOf(item)" :alt="item.goods_name" @error="(e) => (e.target.src = fallbackImageOf(item))" />
          <div class="stack">
            <div class="row">
              <el-checkbox
                :model-value="Number(item.is_selected) === 1"
                :disabled="!isAvailable(item)"
                @change="(checked) => updateItem(item, { selected: checked })"
              />
              <strong>{{ item.goods_name }}</strong>
              <span class="muted">{{ item.sku_name }}</span>
              <span class="status-chip" :class="{ invalid: !isAvailable(item) }">{{ itemStatusText(item) }}</span>
            </div>
            <div class="row">
              <span class="price">¥{{ item.price }}</span>
              <span class="muted">可售库存 {{ item.stock }}</span>
              <span class="muted">小计 ¥{{ (Number(item.price) * Number(item.buy_num)).toFixed(2) }}</span>
            </div>
            <div class="row">
              <el-input-number
                :model-value="item.buy_num"
                :min="1"
                :disabled="!isAvailable(item)"
                @change="(value) => handleNumChange(item, value)"
              />
              <el-button @click="removeItem(item)">删除</el-button>
            </div>
          </div>
        </div>
      </section>
    </section>

    <section class="band row settlement-bar">
      <strong>已选 {{ selectedCount }} 件 / {{ selectedMerchantIds.length || 0 }} 个店铺</strong>
      <span class="price">合计 ¥{{ selectedAmount.toFixed(2) }}</span>
      <el-button type="primary" :disabled="!selectedCount" @click="goConfirm">去结算</el-button>
    </section>
  </main>
</template>

<style scoped>
.section-head,
.settlement-bar {
  justify-content: space-between;
}

.cart-cover {
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-sm);
}

.merchant-group {
  padding: 20px 0 6px;
  border-top: 1px solid var(--border-light);
}

.merchant-head {
  justify-content: space-between;
  margin-bottom: 8px;
}

.merchant-meta {
  gap: 4px;
}

.cart-alert {
  padding: 12px 14px;
  border: 1px solid var(--brand-red-light);
  border-radius: var(--radius-md);
  background: var(--brand-red-light);
  color: var(--brand-red-dark);
}

.status-chip {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
}

.status-chip.invalid {
  background: var(--brand-red-light);
  color: var(--brand-red);
}

.settlement-bar {
  position: sticky;
  bottom: 18px;
  z-index: 8;
  box-shadow: var(--shadow-soft);
}
</style>
