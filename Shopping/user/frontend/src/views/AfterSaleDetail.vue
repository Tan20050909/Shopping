<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, imageOf } from '../api/client'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(false)
const canceling = ref(false)

const typeText = {
  1: '仅退款',
  4: '退货退款'
}

const timeline = computed(() => {
  const logs = detail.value?.logs || []
  if (logs.length) return logs
  return [{ operationDesc: '用户提交售后申请', createTime: detail.value?.applyTime || detail.value?.apply_time }]
})
const reasonParts = computed(() => {
  const reason = String(field(detail.value, 'applyReason', 'apply_reason', ''))
  const marker = '；补充说明：'
  if (!reason.includes(marker)) return { reason, note: '未填写' }
  const [main, note] = reason.split(marker)
  return { reason: main, note: note || '未填写' }
})

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
  if (handleStatus === 3) return '平台介入中'
  if (handleStatus === 1) return '商家已同意'
  if (handleStatus === 5) return '售后关闭'
  return '待审核'
}

function refundStatusText(item) {
  const refundStatus = Number(field(item, 'refundStatus', 'refund_status', -1))
  if (refundStatus === 2) return '退款成功'
  if (refundStatus === 1) return '退款处理中'
  if (refundStatus === 0) return '待退款'
  return '暂无退款单'
}

const canCancel = computed(() => {
  const handleStatus = Number(field(detail.value, 'handleStatus', 'handle_status', -1))
  const refundStatus = Number(field(detail.value, 'refundStatus', 'refund_status', -1))
  if (refundStatus === 1 || refundStatus === 2) return false
  return handleStatus === 0 || handleStatus === 3
})

const dispute = computed(() => detail.value?.dispute || null)
const disputeStatusMap = { 0: '待平台处理', 1: '举证中', 2: '平台处理中', 3: '已裁决', 4: '已关闭' }
const judgeResultMap = { 1: '支持用户', 2: '支持商家', 3: '部分支持', 4: '协商关闭' }

const canRaiseDispute = computed(() => {
  const handleStatus = Number(field(detail.value, 'handleStatus', 'handle_status', -1))
  // 已有任何纠纷（包括已裁决/已关闭）都不可重复创建
  if (dispute.value && dispute.value.disputeId) return false
  return handleStatus === 0 || handleStatus === 1 || handleStatus === 2
})

const raisingDispute = ref(false)
async function raiseDispute() {
  if (!canRaiseDispute.value || raisingDispute.value) return
  try {
    await ElMessageBox.confirm('确认申请平台介入？平台将介入处理该售后纠纷。', '申请平台介入', {
      confirmButtonText: '确认申请',
      cancelButtonText: '我再想想',
      type: 'warning'
    })
  } catch { return }
  raisingDispute.value = true
  try {
    await api('/api/user/disputes', {
      method: 'POST',
      body: JSON.stringify({ afterSaleId: Number(route.params.id), reason: '用户申请平台介入' }),
      headers: { 'Content-Type': 'application/json' }
    })
    ElMessage.success('已申请平台介入')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    raisingDispute.value = false
  }
}

async function load() {
  loading.value = true
  try {
    detail.value = await api(`/api/user/after-sales/${route.params.id}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function cancelAfterSale() {
  if (!canCancel.value || canceling.value) return
  try {
    await ElMessageBox.confirm('确认取消该售后申请？取消后可重新从订单发起售后。', '提示', {
      confirmButtonText: '确认取消',
      cancelButtonText: '我再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  canceling.value = true
  try {
    await api(`/api/user/after-sales/${route.params.id}/cancel`, { method: 'PUT' })
    ElMessage.success('已取消售后')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    canceling.value = false
  }
}

onMounted(load)
</script>

<template>
  <main v-loading="loading" class="page stack after-sale-detail-page">
    <template v-if="detail">
      <section class="band detail-head">
        <div>
          <h1 class="section-title">售后详情</h1>
          <p class="muted">售后单 {{ field(detail, 'afterSaleNo', 'after_sale_no') }}</p>
        </div>
        <div class="row">
          <span class="status-chip">{{ statusText(detail) }}</span>
          <el-button v-if="canCancel" type="danger" plain :loading="canceling" @click="cancelAfterSale">取消售后</el-button>
          <el-button v-if="canRaiseDispute" type="warning" plain :loading="raisingDispute" @click="raiseDispute">申请平台介入</el-button>
          <el-button @click="router.push('/after-sales')">返回我的售后</el-button>
        </div>
      </section>

      <section class="summary-grid">
        <div class="band metric-block">
          <span class="muted">售后状态</span>
          <strong>{{ statusText(detail) }}</strong>
        </div>
        <div class="band metric-block">
          <span class="muted">退款金额</span>
          <strong class="price">¥{{ money(field(detail, 'applyAmount', 'apply_amount')) }}</strong>
        </div>
        <div class="band metric-block">
          <span class="muted">售后类型</span>
          <strong>{{ typeText[field(detail, 'afterSaleType', 'after_sale_type')] || '售后申请' }}</strong>
        </div>
        <div class="band metric-block">
          <span class="muted">申请时间</span>
          <strong>{{ field(detail, 'applyTime', 'apply_time') }}</strong>
        </div>
      </section>

      <section class="detail-two-col">
        <div class="band stack">
          <h2 class="section-title">商品信息</h2>
          <div class="list-item product-line">
            <img class="cover" :src="imageOf(detail)" :alt="field(detail, 'goodsName', 'goods_name')" />
            <div class="stack">
              <strong>{{ field(detail, 'goodsName', 'goods_name') }}</strong>
              <span class="muted">SKU：{{ field(detail, 'skuName', 'sku_name') || '默认规格' }}</span>
              <span class="muted">数量 x {{ field(detail, 'num', 'num') }}</span>
              <span class="price">实付 ¥{{ money(field(detail, 'totalPrice', 'total_price')) }}</span>
            </div>
          </div>
        </div>

        <div class="band stack">
          <h2 class="section-title">订单信息</h2>
          <div class="info-lines">
            <span class="muted">父订单号：{{ field(detail, 'groupNo', 'group_no') }}</span>
            <span class="muted">包裹编号：{{ field(detail, 'orderNo', 'order_no') }}</span>
            <span class="muted">订单项：{{ field(detail, 'orderItemId', 'order_item_id') }}</span>
            <span class="muted">下单时间：{{ field(detail, 'orderCreateTime', 'order_create_time') }}</span>
          </div>
        </div>
      </section>

      <section class="detail-two-col">
        <div class="band stack">
          <h2 class="section-title">申请信息</h2>
          <div class="info-lines">
            <span class="muted">申请原因：{{ reasonParts.reason }}</span>
            <span class="muted">补充说明：{{ reasonParts.note }}</span>
            <span class="muted">凭证图片地址：{{ field(detail, 'applyEvidence', 'apply_evidence') || '未填写' }}</span>
          </div>
        </div>

        <div class="band stack">
          <h2 class="section-title">退款信息</h2>
          <div class="info-lines">
            <span class="muted">退款状态：{{ refundStatusText(detail) }}</span>
            <span class="muted">退款单号：{{ field(detail, 'refundNo', 'refund_no') || '暂未生成' }}</span>
            <span class="muted">退款渠道：{{ field(detail, 'refundChannel', 'refund_channel') || '模拟退款' }}</span>
            <span class="muted">到账时间：{{ field(detail, 'refundSuccessTime', 'refund_success_time') || '待处理' }}</span>
          </div>
        </div>
      </section>

      <section class="band stack">
        <h2 class="section-title">售后进度</h2>
        <el-timeline>
          <el-timeline-item
            v-for="log in timeline"
            :key="field(log, 'logId', 'log_id', field(log, 'operationDesc', 'operation_desc'))"
            :timestamp="field(log, 'createTime', 'create_time')"
            placement="top"
          >
            <strong>{{ field(log, 'operationDesc', 'operation_desc') }}</strong>
          </el-timeline-item>
        </el-timeline>
      </section>

      <section v-if="dispute" class="band stack">
        <h2 class="section-title">纠纷信息</h2>
        <div class="info-lines">
          <span class="muted">纠纷单号：{{ dispute.disputeNo || '-' }}</span>
          <span class="muted">纠纷状态：{{ disputeStatusMap[dispute.disputeStatus] || '-' }}</span>
          <span class="muted" v-if="dispute.judgeResult">判责结果：{{ judgeResultMap[dispute.judgeResult] || '-' }}</span>
          <span class="muted" v-if="dispute.finalAmount">裁决金额：¥{{ Number(dispute.finalAmount).toFixed(2) }}</span>
          <span class="muted" v-if="dispute.platformOpinion">平台意见：{{ dispute.platformOpinion }}</span>
          <span class="muted" v-if="dispute.judgeTime">裁决时间：{{ dispute.judgeTime }}</span>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
}

.summary-grid,
.detail-two-col {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-block {
  min-height: 104px;
  align-content: center;
  gap: 8px;
}

.info-lines {
  display: grid;
  gap: 10px;
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

.product-line {
  border-bottom: 0;
  padding-bottom: 0;
}

@media (max-width: 860px) {
  .detail-head,
  .detail-two-col {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .detail-head {
    display: grid;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
