<template>
  <div class="stock-page">
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">STOCK MANAGEMENT</span>
          <h1 class="merchant-page-title">库存管理</h1>
          <p class="merchant-page-desc">集中维护商品规格库存、价格与预警阈值，及时掌握库存变化</p>
        </div>
      </div>
    </section>
    <el-card shadow="never" class="merchant-content-card">
      <template #header>
        <div class="head">
          <span>库存管理</span>
          <div class="tools">
            <el-select v-model="goodsId" placeholder="选择商品" style="width: 240px" @change="loadSkus">
              <el-option v-for="g in goodsOptions" :key="g.id" :label="String(g.name || '商品')" :value="g.id" />
            </el-select>
            <el-input-number v-model="warningStock" :min="0" controls-position="right" style="width: 180px" />
            <el-button-group class="tool-btns">
              <el-button plain @click="saveWarning">保存预警</el-button>
              <el-button plain @click="openLog()">库存记录</el-button>
              <el-button plain @click="loadAll">刷新</el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <el-empty v-if="!goodsOptions.length" description="暂无商品" />

      <el-table v-else :data="skuList" style="width: 100%">
        <el-table-column prop="id" label="SKU ID" width="110" />
        <el-table-column prop="spec" label="规格" min-width="140" />
        <el-table-column prop="price" label="价格" width="140">
          <template #default="{ row }">¥ {{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="220">
          <template #default="{ row }">
            <div class="stock-cell">
              <el-input-number v-model="row.stock" :min="0" size="small" @change="updateStock(row)" />
              <el-tag v-if="isLowStock(row)" type="danger">低库存</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" plain @click="openLog(row)">库存记录</el-button>
              <el-button size="small" @click="showPriceDialog(row)">价格管理</el-button>
              <el-button size="small" plain @click="loadPriceLog(row)">价格历史</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="priceDialogVisible" title="修改价格" width="400px">
      <el-form :model="priceForm" label-width="80px">
        <el-form-item label="新价格">
          <el-input-number v-model="priceForm.price" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updatePrice">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="priceLogVisible" title="价格历史" width="600px">
      <el-table :data="priceLogList" style="width: 100%">
        <el-table-column prop="oldPrice" label="原价格">
          <template #default="{ row }">¥ {{ row.oldPrice }}</template>
        </el-table-column>
        <el-table-column prop="newPrice" label="新价格">
          <template #default="{ row }">¥ {{ row.newPrice }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="修改时间" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="stockLogVisible" title="库存变动记录" width="720px" destroy-on-close>
      <el-table :data="stockLogList" style="width: 100%">
        <el-table-column prop="skuId" label="SKU" width="120" />
        <el-table-column prop="oldStock" label="原库存" width="110" />
        <el-table-column prop="newStock" label="新库存" width="110" />
        <el-table-column prop="changeStock" label="变化" width="90" />
        <el-table-column prop="createTime" label="时间" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { goodsApi, goodsSkuApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const skuList = ref([])
const goodsOptions = ref([])
const goodsId = ref(null)
const warningStock = ref(0)

const priceDialogVisible = ref(false)
const priceLogVisible = ref(false)
const currentSku = ref(null)
const priceForm = reactive({ price: 0 })
const priceLogList = ref([])

const stockLogVisible = ref(false)
const stockLogList = ref([])
const currentLogSkuId = ref(null)

const loadGoods = async () => {
  try {
    const res = await goodsApi.list(getMerchantId())
    goodsOptions.value = Array.isArray(res.data) ? res.data : []
    if (!goodsId.value && goodsOptions.value.length) {
      goodsId.value = goodsOptions.value[0].id
    }
  } catch (e) {
    goodsOptions.value = []
    ElMessage.error(e?.response?.data?.message || '商品列表加载失败')
  }
}

const loadWarning = async () => {
  try {
    const res = await goodsSkuApi.getStockWarning(getMerchantId())
    const v = Number(res?.data ?? 0)
    warningStock.value = Number.isFinite(v) && v >= 0 ? v : 0
  } catch (e) {
    warningStock.value = 0
  }
}

const saveWarning = async () => {
  try {
    await goodsSkuApi.setStockWarning(getMerchantId(), Number(warningStock.value ?? 0))
    ElMessage.success('预警库存已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

const loadSkus = async () => {
  if (!goodsId.value) {
    skuList.value = []
    return
  }
  try {
    const res = await goodsSkuApi.list(goodsId.value)
    skuList.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    skuList.value = []
    ElMessage.error(e?.response?.data?.message || 'SKU 列表加载失败')
  }
}

const updateStock = async (row) => {
  try {
    await goodsSkuApi.updateStock(row.id, row.stock)
    ElMessage.success('库存更新成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '库存更新失败')
  }
}

const showPriceDialog = (row) => {
  currentSku.value = row
  priceForm.price = row.price
  priceDialogVisible.value = true
}

const updatePrice = async () => {
  try {
    await goodsSkuApi.updatePrice(currentSku.value.id, priceForm.price)
    ElMessage.success('价格更新成功')
    priceDialogVisible.value = false
    loadSkus()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '价格更新失败')
  }
}

const loadPriceLog = async (row) => {
  try {
    const res = await goodsSkuApi.listPriceLog(row.id)
    priceLogList.value = res.data || []
    priceLogVisible.value = true
  } catch (e) {
    priceLogList.value = []
    ElMessage.error(e?.response?.data?.message || '价格历史加载失败')
  }
}

const isLowStock = (row) => {
  const w = Number(warningStock.value ?? 0)
  const s = Number(row?.stock ?? 0)
  if (!Number.isFinite(w) || w <= 0) return false
  return Number.isFinite(s) && s <= w
}

const openLog = async (row) => {
  currentLogSkuId.value = row?.id || null
  try {
    const res = await goodsSkuApi.listStockLog(getMerchantId(), goodsId.value, currentLogSkuId.value, 200)
    stockLogList.value = Array.isArray(res.data) ? res.data : []
    stockLogVisible.value = true
  } catch (e) {
    stockLogList.value = []
    ElMessage.error(e?.response?.data?.message || '库存记录加载失败')
  }
}

const loadAll = async () => {
  await loadGoods()
  await loadWarning()
  await loadSkus()
}

onMounted(loadAll)
</script>

<style scoped>
.stock-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tool-btns :deep(.el-button) {
  min-width: 86px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.stock-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
