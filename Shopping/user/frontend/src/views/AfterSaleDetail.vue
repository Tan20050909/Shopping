<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, fallbackImageOf, imageOf } from '../api/client'

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
  if (handleStatus === 3) return '售后已完成'
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
const detailEvidences = computed(() => {
  const raw = String(field(detail.value, 'applyEvidence', 'apply_evidence', ''))
  if (!raw) return []
  return raw.split(',').map(v => v.trim()).filter(Boolean)
})
const previewImageUrl = ref('')
const previewVisible = ref(false)
const returnLogistics = computed(() => detail.value?.returnLogistics || null)
const canSubmitReturnLogistics = computed(() => {
  const handleStatus = Number(field(detail.value, 'handleStatus', 'handle_status', -1))
  const afterSaleType = Number(field(detail.value, 'afterSaleType', 'after_sale_type', -1))
  return afterSaleType === 4 && handleStatus === 1 && !returnLogistics.value
})
const returnLogisticsForm = ref({ expressCompany: '', expressNo: '' })
const submittingLogistics = ref(false)
async function submitReturnLogistics() {
  if (!returnLogisticsForm.value.expressCompany.trim() || !returnLogisticsForm.value.expressNo.trim()) {
    ElMessage.warning('请填写快递公司和快递单号')
    return
  }
  submittingLogistics.value = true
  try {
    await api(`/api/user/after-sales/${route.params.id}/return-logistics`, {
      method: 'POST',
      body: {
        expressCompany: returnLogisticsForm.value.expressCompany.trim(),
        expressNo: returnLogisticsForm.value.expressNo.trim()
      }
    })
    ElMessage.success('退货物流信息已提交')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submittingLogistics.value = false
  }
}
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
          <el-button v-if="canRaiseDispute" class="dispute-action-btn" plain :loading="raisingDispute" @click="raiseDispute">申请平台介入</el-button>
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
            <img class="cover" :src="imageOf(detail)" :alt="field(detail, 'goodsName', 'goods_name')" @error="(e) => (e.target.src = fallbackImageOf(detail))" />
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
          </div>
          <div v-if="detailEvidences.length" style="margin-top:10px">
            <span class="muted" style="font-weight:700">凭证图片：</span>
            <div class="evidence-grid">
              <img
                v-for="(url, idx) in detailEvidences"
                :key="idx"
                :src="url"
                class="evidence-thumb"
                @click="previewImageUrl = url; previewVisible = true"
              />
            </div>
          </div>
          <span v-else class="muted">凭证图片：未提供</span>
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

      <section v-if="returnLogistics || canSubmitReturnLogistics" class="band stack">
        <h2 class="section-title">退货物流</h2>
        <div v-if="returnLogistics" class="info-lines">
          <span class="muted">快递公司：{{ returnLogistics.expressCompany || returnLogistics.express_company || '-' }}</span>
          <span class="muted">快递单号：{{ returnLogistics.expressNo || returnLogistics.express_no || '-' }}</span>
          <span class="muted">物流状态：{{ ['待发货', '待揽收', '运输中', '派送中', '已签收', '拒收'][returnLogistics.logisticsStatus || returnLogistics.logistics_status] || '未知' }}</span>
          <div v-if="returnLogistics.traces && returnLogistics.traces.length" class="stack" style="margin-top: 8px;">
            <span class="muted" style="font-weight: 700;">物流轨迹：</span>
            <span v-for="trace in returnLogistics.traces" :key="trace.traceId || trace.trace_id" class="muted">
              {{ trace.traceTime || trace.trace_time }} — {{ trace.traceContent || trace.trace_content }}
            </span>
          </div>
        </div>
        <div v-else-if="canSubmitReturnLogistics" class="info-lines">
          <span class="muted">商家已同意退货退款，请填写退货快递信息：</span>
          <div class="grid" style="grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px;">
            <label class="field-block">
              <span>快递公司</span>
              <el-input v-model="returnLogisticsForm.expressCompany" placeholder="如：顺丰速运、中通快递" />
            </label>
            <label class="field-block">
              <span>快递单号</span>
              <el-input v-model="returnLogisticsForm.expressNo" placeholder="填写快递单号" />
            </label>
          </div>
          <el-button type="primary" :loading="submittingLogistics" @click="submitReturnLogistics">提交退货物流</el-button>
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

  <div v-if="previewVisible" class="image-overlay" @click="previewVisible = false">
    <img :src="previewImageUrl" class="image-overlay-img" />
  </div>
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

.field-block {
  display: grid;
  gap: 8px;
  color: var(--text-main);
  font-weight: 700;
}

.field-block :deep(.el-input) {
  width: 100%;
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

.dispute-action-btn {
  min-width: 120px;
  color: var(--brand-red) !important;
  border-color: var(--brand-red) !important;
  background: #fff !important;
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

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(64px, 1fr));
  gap: 6px;
  margin-top: 6px;
}

.evidence-thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: opacity .2s;
}

.evidence-thumb:hover {
  opacity: .8;
}

.image-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  cursor: pointer;
}

.image-overlay-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  object-fit: contain;
}
</style>
