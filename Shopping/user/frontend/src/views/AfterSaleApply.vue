<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, imageOf } from '../api/client'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const loadError = ref('')
const form = ref({
  orderId: Number(route.query.orderId || 0),
  orderItemId: Number(route.query.orderItemId || 0),
  afterSaleType: 1,
  applyReason: '',
  applyNote: '',
  applyEvidence: '',
  applyAmount: null,
  returnExpressCompany: '',
  returnExpressNo: ''
})

const currentItem = computed(() =>
  detail.value?.items?.find((item) => Number(field(item, 'orderItemId', 'order_item_id')) === Number(form.value.orderItemId)) || null
)

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

async function loadOrder() {
  loadError.value = ''
  detail.value = null
  if (!form.value.orderId || !form.value.orderItemId) {
    loadError.value = '请从订单详情页选择具体商品后发起售后。'
    return
  }
  try {
    detail.value = await api(`/api/user/orders/${form.value.orderId}`)
    if (!currentItem.value) {
      loadError.value = '没有找到对应的订单商品，请回到订单详情页重新选择。'
      return
    }
    form.value.applyAmount = Number(field(currentItem.value, 'totalPrice', 'total_price', 0))
  } catch (error) {
    loadError.value = error.message
    ElMessage.error(error.message)
  }
}

async function submit() {
  if (!form.value.applyReason.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }
  loading.value = true
  try {
    const reason = form.value.applyNote.trim()
      ? `${form.value.applyReason.trim()}；补充说明：${form.value.applyNote.trim()}`
      : form.value.applyReason.trim()
    const result = await api('/api/user/after-sales', {
      method: 'POST',
      body: {
        orderId: form.value.orderId,
        orderItemId: form.value.orderItemId,
        afterSaleType: form.value.afterSaleType,
        applyReason: reason,
        applyAmount: form.value.applyAmount,
        applyEvidence: form.value.applyEvidence
      }
    })
    // 退货退款：如填写了退货物流信息，一并提交
    if (form.value.afterSaleType === 4 && form.value.returnExpressCompany.trim() && form.value.returnExpressNo.trim()) {
      try {
        await api(`/api/user/after-sales/${result.afterSaleId}/return-logistics`, {
          method: 'POST',
          body: {
            expressCompany: form.value.returnExpressCompany.trim(),
            expressNo: form.value.returnExpressNo.trim()
          }
        })
      } catch (logisticsErr) {
        ElMessage.warning('售后申请已提交，但退货物流信息提交失败，请稍后在售后详情页补充：' + logisticsErr.message)
      }
    }
    ElMessage.success('售后申请已提交')
    router.push(`/after-sales/${result.afterSaleId}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadOrder)
</script>

<template>
  <main class="page stack">
    <section class="band stack after-sale-shell">
      <div class="row section-head">
        <div>
          <h1 class="section-title">申请售后</h1>
          <p class="muted">售后会绑定到当前订单商品。</p>
        </div>
        <el-button @click="router.back()">返回</el-button>
      </div>

      <div v-if="loadError" class="empty-state stack">
        <strong>暂时还不能申请售后</strong>
        <p class="muted">{{ loadError }}</p>
        <el-button type="primary" @click="router.push('/orders')">去我的订单</el-button>
      </div>

      <div v-else-if="detail && currentItem" class="apply-layout">
        <aside class="product-panel">
          <img class="cover product-cover" :src="imageOf(currentItem)" :alt="field(currentItem, 'goodsName', 'goods_name')" />
          <div class="stack">
            <strong>{{ field(currentItem, 'goodsName', 'goods_name') }}</strong>
            <span class="muted">SKU：{{ field(currentItem, 'skuName', 'sku_name') || '默认规格' }}</span>
            <span class="muted">购买数量：{{ field(currentItem, 'num', 'num') }}</span>
            <span class="muted">订单编号：{{ field(detail, 'groupNo', 'group_no') }}</span>
            <span class="muted">实付金额：¥{{ money(field(currentItem, 'totalPrice', 'total_price')) }}</span>
            <strong class="price">可退金额：¥{{ money(field(currentItem, 'totalPrice', 'total_price')) }}</strong>
          </div>
        </aside>

        <div class="form-panel stack">
          <label class="field-block">
            <span>售后类型</span>
            <el-select v-model="form.afterSaleType" placeholder="请选择售后类型">
              <el-option label="仅退款" :value="1" />
              <el-option label="退货退款" :value="4" />
            </el-select>
          </label>

          <label class="field-block">
            <span>申请原因</span>
            <el-input v-model="form.applyReason" type="textarea" :rows="3" placeholder="请描述需要售后的原因" />
          </label>

          <label class="field-block">
            <span>补充说明</span>
            <el-input v-model="form.applyNote" type="textarea" :rows="3" placeholder="可补充商品情况、期望处理方式等" />
          </label>

          <div class="grid form-grid">
            <label class="field-block">
              <span>退款金额</span>
              <el-input-number
                v-model="form.applyAmount"
                :min="0.01"
                :max="Number(field(currentItem, 'totalPrice', 'total_price', 0))"
                :step="1"
                :precision="2"
              />
            </label>
            <label class="field-block">
              <span>凭证图片地址</span>
              <el-input v-model="form.applyEvidence" placeholder="选填，填写图片 URL" />
            </label>
          </div>

          <div v-if="form.afterSaleType === 4" class="grid form-grid">
            <label class="field-block">
              <span>退货快递公司</span>
              <el-input v-model="form.returnExpressCompany" placeholder="如：顺丰速运、中通快递" />
            </label>
            <label class="field-block">
              <span>退货快递单号</span>
              <el-input v-model="form.returnExpressNo" placeholder="填写快递单号" />
            </label>
          </div>

          <div class="row">
            <el-button type="primary" :loading="loading" @click="submit">提交申请</el-button>
            <el-button @click="router.push(`/orders/${form.orderId}`)">返回订单详情</el-button>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.section-head {
  justify-content: space-between;
}

.after-sale-shell {
  border-color: var(--border-light);
}

.apply-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.product-panel,
.form-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fff;
}

.product-cover {
  border-radius: 8px;
}

.field-block {
  display: grid;
  gap: 8px;
  color: var(--text-main);
  font-weight: 700;
}

.field-block :deep(.el-input),
.field-block :deep(.el-select),
.field-block :deep(.el-input-number) {
  width: 100%;
}

.field-block :deep(.el-textarea__inner),
.field-block :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.empty-state {
  padding: 36px 20px;
  border: 1px dashed #ffb780;
  border-radius: 8px;
  background: var(--bg-soft);
}

@media (max-width: 860px) {
  .apply-layout,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
