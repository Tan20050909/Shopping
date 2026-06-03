<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, fallbackImageOf, imageOf } from '../api/client'

const router = useRouter()

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  mode: {
    type: String,
    default: 'full'
  }
})

const skuDialogVisible = ref(false)
const skuDialogLoading = ref(false)
const productDetail = ref(null)
const selectedSkuId = ref()
const quantity = ref(1)
const pendingAction = ref('cart')

const productId = computed(() => props.item.goodsId || props.item.goods_id)
const productName = computed(() => props.item.goodsName || props.item.goods_name || '商品')
const dialogProduct = computed(() => productDetail.value || props.item)
const skus = computed(() => productDetail.value?.skus || props.item.skus || [])
const selectedSku = computed(() => skus.value.find((sku) => Number(skuKey(sku)) === Number(selectedSkuId.value)))
const selectedStock = computed(() => Number(selectedSku.value?.stock || 0))
const cardStock = computed(() => props.item.totalStock ?? props.item.total_stock ?? props.item.stock ?? 0)

function skuKey(sku) {
  return sku.skuId || sku.sku_id
}

function skuName(sku) {
  return sku.skuName || sku.sku_name || '默认规格'
}

function skuPrice(sku) {
  return sku.price || props.item.displayPrice || props.item.price || '0.00'
}

function formatPrice(value) {
  const numberValue = Number(value || 0)
  return Number.isFinite(numberValue) ? numberValue.toFixed(2) : '0.00'
}

function selectSku(sku) {
  if (Number(sku.stock || 0) <= 0) return
  selectedSkuId.value = skuKey(sku)
  quantity.value = 1
}

async function loadProductDetail() {
  if (!productId.value) {
    ElMessage.warning('这个商品需要先去详情页选择规格')
    skuDialogVisible.value = false
    return
  }
  skuDialogLoading.value = true
  try {
    productDetail.value = await api(`/api/user/products/${productId.value}`)
    const firstAvailableSku = (productDetail.value.skus || []).find((sku) => Number(sku.stock || 0) > 0)
    selectedSkuId.value = firstAvailableSku?.skuId || productDetail.value.skus?.[0]?.skuId
    quantity.value = 1
  } catch (error) {
    ElMessage.error(error.message)
    skuDialogVisible.value = false
  } finally {
    skuDialogLoading.value = false
  }
}

async function openSkuDialog(action) {
  if (skuDialogLoading.value) return
  pendingAction.value = action
  skuDialogVisible.value = true
  if (!productDetail.value || Number(productDetail.value.goodsId || productDetail.value.goods_id) !== Number(productId.value)) {
    await loadProductDetail()
  }
}

async function addToCart() {
  await openSkuDialog('cart')
}

async function buyNow() {
  await openSkuDialog('buy')
}

async function confirmSku() {
  if (!selectedSku.value) {
    ElMessage.warning('请先选择规格')
    return
  }
  if (selectedStock.value <= 0) {
    ElMessage.warning('该规格暂时无货')
    return
  }
  const skuId = skuKey(selectedSku.value)
  if (pendingAction.value === 'buy') {
    skuDialogVisible.value = false
    router.push(`/order/confirm?skuId=${skuId}&num=${quantity.value}`)
    return
  }
  try {
    await api('/api/user/cart/items', {
      method: 'POST',
      body: { skuId, num: quantity.value }
    })
    ElMessage.success('已加入购物车')
    skuDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function addFavorite() {
  try {
    await api(`/api/user/favorites/${props.item.goodsId || props.item.goods_id}`, {
      method: 'POST'
    })
    ElMessage.success('已加入收藏')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="product-card" :class="`mode-${props.mode}`">
    <router-link class="product-main" :to="`/products/${productId}`">
      <div class="product-image-wrap">
        <img class="cover" :src="imageOf(props.item)" :alt="productName" @error="(e) => (e.target.src = fallbackImageOf(props.item))" />
      </div>
      <div class="product-info">
        <strong>{{ productName }}</strong>
        <span class="muted">{{ props.item.categoryName || props.item.cate_name || props.item.merchant_name || '精选商品' }}</span>
        <div class="row">
          <span class="price">¥{{ props.item.displayPrice || props.item.price || props.item.live_price || '0.00' }}</span>
          <span class="muted">销量 {{ props.item.sellCount || props.item.sell_count || 0 }}</span>
        </div>
        <span v-if="props.mode !== 'compact'" class="muted">总可售库存 {{ cardStock }}</span>
      </div>
    </router-link>
    <div v-if="props.mode !== 'compact'" class="row actions">
      <el-button size="small" plain @click="addFavorite">收藏</el-button>
      <el-button size="small" plain @click="addToCart">加入购物车</el-button>
      <el-button size="small" type="primary" @click="buyNow">立即购买</el-button>
    </div>
  </div>

  <el-dialog
    v-model="skuDialogVisible"
    class="sku-dialog"
    width="520px"
    :title="pendingAction === 'buy' ? '选择规格并购买' : '选择规格加入购物车'"
  >
    <div v-if="skuDialogLoading" class="sku-loading">正在加载规格...</div>
    <div v-else class="sku-dialog-body">
      <div class="sku-head">
        <img class="cover sku-cover" :src="imageOf(dialogProduct)" :alt="productName" @error="(e) => (e.target.src = fallbackImageOf(dialogProduct))" />
        <div>
          <strong>{{ productName }}</strong>
          <p class="muted">总可售库存 {{ dialogProduct.totalStock ?? dialogProduct.total_stock ?? cardStock }}</p>
          <p class="price">¥{{ formatPrice(selectedSku ? skuPrice(selectedSku) : (dialogProduct.minPrice || dialogProduct.displayPrice || dialogProduct.price)) }}</p>
        </div>
      </div>

      <div class="sku-section">
        <span class="sku-label">规格</span>
        <div class="sku-options">
          <button
            v-for="sku in skus"
            :key="skuKey(sku)"
            type="button"
            class="sku-option"
            :class="{ active: Number(skuKey(sku)) === Number(selectedSkuId), disabled: Number(sku.stock || 0) <= 0 }"
            @click="selectSku(sku)"
          >
            <span>{{ skuName(sku) }}</span>
            <strong>¥{{ formatPrice(skuPrice(sku)) }}</strong>
            <small>可售 {{ sku.stock || 0 }}</small>
          </button>
        </div>
      </div>

      <div class="sku-section quantity-section">
        <span class="sku-label">数量</span>
        <el-input-number
          v-model="quantity"
          :min="1"
          :max="selectedStock || 1"
          :disabled="!selectedSku || selectedStock <= 0"
        />
        <span class="muted">当前规格可售 {{ selectedStock }}</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="skuDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :disabled="skuDialogLoading || !selectedSku || selectedStock <= 0"
        @click="confirmSku"
      >
        {{ pendingAction === 'buy' ? '立即购买' : '加入购物车' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.product-card {
  display: grid;
  gap: 8px;
  min-height: 0;
  padding: 0;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.035);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: rgba(230, 0, 18, 0.18);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.07);
}

.product-main {
  display: grid;
  gap: 0;
}

.product-image-wrap {
  padding: 12px 12px 6px;
  background: linear-gradient(180deg, #fbfbfb 0%, #f7f7f7 100%);
}

.product-image-wrap .cover {
  display: block;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-sm);
}

.product-info {
  display: grid;
  gap: 7px;
  padding: 10px 18px 0;
}

.product-info strong {
  display: -webkit-box;
  min-height: 40px;
  overflow: hidden;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-info .row {
  justify-content: space-between;
  gap: 12px;
  padding-right: 2px;
}

.product-info .row .price,
.product-info .row .muted {
  min-width: 0;
}

.product-info .row .muted:last-child {
  flex: 0 0 auto;
  padding-right: 1px;
  text-align: right;
}

.product-info .muted {
  font-size: 12px;
}

.actions {
  display: flex;
  flex-wrap: nowrap;
  margin-top: auto;
  padding: 6px 18px 14px;
  gap: 7px;
  align-items: center;
}

.actions :deep(.el-button) {
  flex: 0 0 auto;
  min-height: 30px;
  height: 30px;
  padding: 0 11px;
  margin-left: 0;
  border-radius: var(--radius-pill) !important;
  font-size: 12px;
  font-weight: 700;
}

.actions :deep(.el-button:nth-child(1)),
.actions :deep(.el-button:nth-child(2)) {
  background: #fff;
  border-color: #eeeeee;
  color: #555555;
}

.actions :deep(.el-button:nth-child(1):hover),
.actions :deep(.el-button:nth-child(2):hover) {
  background: #fff7f8;
  border-color: #ffd9df;
  color: var(--brand-red);
}

.actions :deep(.el-button:nth-child(1) span),
.actions :deep(.el-button:nth-child(2) span) {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

.actions :deep(.el-button:nth-child(1) span::before),
.actions :deep(.el-button:nth-child(2) span::before) {
  content: "";
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
  background: currentColor;
  opacity: 0.92;
}

.actions :deep(.el-button:nth-child(1) span::before) {
  mask: url("data:image/svg+xml,%3Csvg viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M12 20.2s-6.7-4.1-9.3-8.1C.8 9.1 2.3 5.5 5.7 5.1c1.9-.2 3.3.8 4.3 2.1 1-1.3 2.4-2.3 4.3-2.1 3.4.4 4.9 4 3 7.1-2.6 4-9.3 8-9.3 8Z' fill='none' stroke='black' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") center / contain no-repeat;
}

.actions :deep(.el-button:nth-child(2) span::before) {
  mask: url("data:image/svg+xml,%3Csvg viewBox='0 0 24 24' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M5 6h2l1.5 9h8.2l1.4-6.5H8.2M10 19.5h.1M17 19.5h.1' fill='none' stroke='black' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") center / contain no-repeat;
}

.actions :deep(.el-button--primary) {
  margin-left: auto;
  min-width: max-content;
  padding: 0 1px 0 8px;
  background: transparent;
  border-color: transparent;
  color: var(--brand-red) !important;
  box-shadow: none;
  font-size: 13px;
  font-weight: 800;
  transition: color 0.2s ease, transform 0.2s ease;
}

.actions :deep(.el-button--primary span::after) {
  content: ">";
  margin-left: 5px;
  font-weight: 900;
}

.actions :deep(.el-button--primary:hover) {
  transform: translateX(2px);
  background: transparent;
  border-color: transparent;
  color: var(--brand-red-dark) !important;
}

.actions :deep(.el-button--primary:active),
.actions :deep(.el-button--primary:focus) {
  background: transparent;
  border-color: transparent;
}

.mode-compact {
  gap: 0;
  min-height: auto;
  border-radius: var(--radius-md);
}

.mode-compact .product-main {
  gap: 0;
}

.mode-compact .product-info {
  gap: 7px;
  padding: 13px 18px 14px;
}

.mode-compact .product-info strong {
  font-size: 15px;
  line-height: 1.35;
}

.mode-compact .cover {
  border-radius: var(--radius-sm);
}

.mode-compact .product-image-wrap {
  padding: 10px;
}

.sku-loading {
  display: grid;
  min-height: 180px;
  place-items: center;
  color: var(--text-secondary);
}

.sku-dialog-body {
  display: grid;
  gap: 22px;
}

.sku-head {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 16px;
  align-items: center;
}

.sku-cover {
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-sm);
  background: var(--bg-soft);
}

.sku-head strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text-main);
  font-size: 17px;
  line-height: 1.4;
}

.sku-section {
  display: grid;
  gap: 10px;
}

.sku-label {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
}

.sku-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sku-option {
  display: grid;
  gap: 5px;
  min-height: 78px;
  padding: 11px 12px;
  text-align: left;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.sku-option span {
  color: var(--text-main);
  font-weight: 700;
}

.sku-option strong {
  color: var(--brand-red);
  font-size: 14px;
}

.sku-option small {
  color: var(--text-secondary);
}

.sku-option.active {
  background: var(--brand-red-light);
  border-color: var(--brand-red);
  box-shadow: 0 0 0 2px rgba(210, 46, 31, 0.1);
}

.sku-option.disabled {
  color: var(--text-muted);
  background: var(--bg-soft);
  cursor: not-allowed;
  opacity: 0.62;
}

.quantity-section {
  grid-template-columns: auto auto 1fr;
  align-items: center;
}

@media (max-width: 560px) {
  .sku-head {
    grid-template-columns: 78px 1fr;
  }

  .sku-options {
    grid-template-columns: 1fr;
  }

  .quantity-section {
    grid-template-columns: 1fr;
  }
}
</style>
