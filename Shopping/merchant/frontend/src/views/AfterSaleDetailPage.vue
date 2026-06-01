<template>
  <div class="page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="header">
          <div class="title">
            <el-button text @click="back">返回</el-button>
            <span class="title-text">售后详情</span>
          </div>
          <div class="actions">
            <el-button plain @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="8" animated />

      <div v-else-if="afterSale" class="content">
        <el-card shadow="never" class="block">
          <template #header>
            <div class="block-title">售后进度</div>
          </template>
          <el-steps :active="progressActive" align-center finish-status="success">
            <el-step v-for="(s, idx) in progressSteps" :key="idx" :title="s.title" :description="s.desc" />
          </el-steps>
        </el-card>

        <div class="top-grid">
          <el-card shadow="never" class="block">
            <template #header>
              <div class="block-title">申请信息</div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="售后单号">{{ afterSale.afterSaleNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusType(afterSale)">{{ statusText(afterSale) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="类型">{{ typeText(afterSale.type) }}</el-descriptions-item>
              <el-descriptions-item label="退款金额">¥ {{ toMoney(afterSale.refundAmount) }}</el-descriptions-item>
              <el-descriptions-item label="申请时间">{{ formatTime(afterSale.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="处理时间">{{ formatTime(afterSale.handleTime) }}</el-descriptions-item>
              <el-descriptions-item label="原因" :span="2">{{ afterSale.reason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="block">
            <template #header>
              <div class="block-title">关联订单</div>
            </template>
            <div class="order-box">
              <div class="order-row">
                <span class="k">订单ID</span>
                <span class="v">{{ orderView?.id || '-' }}</span>
              </div>
              <div class="order-row">
                <span class="k">订单号</span>
                <span class="v">{{ orderView?.orderNo || '-' }}</span>
              </div>
              <div class="order-row">
                <span class="k">购买商品</span>
                <span class="v">{{ orderGoodsSummary(orderView) }}</span>
              </div>
              <div class="order-row">
                <span class="k">实付款</span>
                <span class="v">¥ {{ toMoney(orderView?.payAmount ?? orderView?.totalAmount) }}</span>
              </div>
              <div class="order-row">
                <span class="k">状态</span>
                <span class="v">{{ orderStatusText(orderView?.status) }}</span>
              </div>
              <div class="order-actions">
                <el-button size="small" :disabled="!orderView?.id" @click="goOrderDetail">查看订单详情</el-button>
              </div>
            </div>
          </el-card>
        </div>

        <el-card v-if="disputeInfo" shadow="never" class="block">
          <template #header>
            <div class="block-title">平台介入信息</div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="纠纷单号">{{ disputeInfo.dispute_no || '-' }}</el-descriptions-item>
            <el-descriptions-item label="纠纷状态">
              <el-tag :type="disputeStatusType(disputeInfo.dispute_status)">{{ disputeStatusText(disputeInfo.dispute_status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="判责结果" v-if="disputeInfo.judge_result">{{ judgeResultText(disputeInfo.judge_result) }}</el-descriptions-item>
            <el-descriptions-item label="裁决金额" v-if="disputeInfo.final_amount">¥ {{ toMoney(disputeInfo.final_amount) }}</el-descriptions-item>
            <el-descriptions-item label="平台意见" :span="2" v-if="disputeInfo.platform_opinion">{{ disputeInfo.platform_opinion }}</el-descriptions-item>
            <el-descriptions-item label="裁决时间" v-if="disputeInfo.judge_time">{{ formatTime(disputeInfo.judge_time) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="block">
          <template #header>
            <div class="block-title">买家证据</div>
          </template>
          <el-empty v-if="!buyerEvidence.length" description="买家未提供证据" />
          <div v-else class="media-grid">
            <template v-for="(u, idx) in buyerEvidence" :key="idx">
              <img v-if="isImage(u)" class="media" :src="u" alt="" />
              <video v-else class="media" controls :src="u"></video>
            </template>
          </div>
        </el-card>

        <el-card v-if="showBuyerReturn" shadow="never" class="block">
          <template #header>
            <div class="block-title">买家物流（寄回）</div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="快递公司">{{ buyerLogistics?.company || '-' }}</el-descriptions-item>
            <el-descriptions-item label="快递单号">{{ buyerLogistics?.trackingNo || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">物流轨迹</el-divider>
          <el-empty v-if="buyerTraces.length === 0" description="暂无物流轨迹" :image-size="80" />
          <el-timeline v-else class="trace-timeline">
            <el-timeline-item
              v-for="t in buyerTraces"
              :key="t.id || `${t.traceTime}-${t.traceContent}`"
              :timestamp="formatTime(t.traceTime)"
              placement="top"
            >
              <div class="trace-content">{{ t.traceContent }}</div>
              <div v-if="t.traceLocation" class="trace-location">{{ t.traceLocation }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card v-if="showMerchantAfterSaleShip" shadow="never" class="block">
          <template #header>
            <div class="block-head">
              <div class="block-title">商家物流（售后发货）</div>
              <el-button v-if="canMerchantAfterSaleShip" size="small" type="primary" @click="openAfterSaleShip">售后发货</el-button>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="快递公司">{{ merchantLogistics?.company || '-' }}</el-descriptions-item>
            <el-descriptions-item label="快递单号">{{ merchantLogistics?.trackingNo || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">物流轨迹</el-divider>
          <el-empty v-if="merchantTraces.length === 0" description="暂无物流轨迹" :image-size="80" />
          <el-timeline v-else class="trace-timeline">
            <el-timeline-item
              v-for="t in merchantTraces"
              :key="t.id || `${t.traceTime}-${t.traceContent}`"
              :timestamp="formatTime(t.traceTime)"
              placement="top"
            >
              <div class="trace-content">{{ t.traceContent }}</div>
              <div v-if="t.traceLocation" class="trace-location">{{ t.traceLocation }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card shadow="never" class="block">
          <template #header>
            <div class="block-title">商家补充证据与处理</div>
          </template>
          <el-form label-position="top">
            <el-form-item label="商家备注">
              <el-input v-model="form.remark" type="textarea" :rows="4" placeholder="可填写处理说明/证据说明" />
            </el-form-item>
            <el-form-item label="商家证据（图片/视频URL或上传）">
              <el-input v-model="form.evidence" placeholder="多个用逗号分隔" />
              <div class="upload-row">
                <el-upload action="" :http-request="uploadImage" :show-file-list="false" accept="image/*">
                  <el-button>上传图片</el-button>
                </el-upload>
                <el-upload action="" :http-request="uploadVideo" :show-file-list="false" accept="video/*">
                  <el-button>上传视频</el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="success" :disabled="afterSale.status !== 0 || hasActiveDispute" @click="confirmHandle(1)">同意</el-button>
              <el-button type="danger" :disabled="afterSale.status !== 0 || hasActiveDispute" @click="confirmHandle(2)">拒绝</el-button>
              <el-button v-if="showCompleteBtn" plain :disabled="!canComplete || hasActiveDispute" @click="confirmComplete">标记完成</el-button>
              <span v-if="hasActiveDispute" style="color:#E6A23C;font-size:12px;margin-left:8px">平台介入中，暂不可操作</span>
            </el-form-item>
          </el-form>

          <el-divider content-position="left">当前商家证据</el-divider>
          <el-empty v-if="!merchantEvidenceList.length" description="暂无商家证据" />
          <div v-else class="media-grid">
            <template v-for="(u, idx) in merchantEvidenceList" :key="idx">
              <img v-if="isImage(u)" class="media" :src="u" alt="" />
              <video v-else class="media" controls :src="u"></video>
            </template>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无售后详情" />
    </el-card>
  </div>

  <el-dialog v-model="afterSaleShipVisible" title="售后发货" width="520px" destroy-on-close>
    <el-form :model="afterSaleShipForm" label-width="100px">
      <el-form-item label="快递公司">
        <el-input v-model="afterSaleShipForm.expressCompany" placeholder="例如：顺丰/中通/圆通" />
      </el-form-item>
      <el-form-item label="快递单号">
        <el-input v-model="afterSaleShipForm.expressNo" placeholder="请输入快递单号" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="afterSaleShipVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmAfterSaleShip">确认发货</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { afterSaleApi, orderApi, uploadApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const route = useRoute()
const router = useRouter()

const id = computed(() => Number(route.params.id))
const detail = ref(null)
const loading = ref(false)

const relatedOrder = ref(null)
const form = ref({ remark: '', evidence: '' })
const afterSaleShipVisible = ref(false)
const afterSaleShipForm = ref({ expressCompany: '', expressNo: '' })

const afterSale = computed(() => detail.value?.afterSale || null)
const orderView = computed(() => {
  const r = relatedOrder.value
  if (r && Array.isArray(r.itemNames) && r.itemNames.length) return r
  return detail.value?.order || r || null
})
const buyerLogistics = computed(() => detail.value?.buyerLogistics || null)
const merchantLogistics = computed(() => detail.value?.merchantLogistics || null)
const buyerTraces = computed(() => (Array.isArray(detail.value?.buyerLogisticsTraces) ? detail.value.buyerLogisticsTraces : []))
const merchantTraces = computed(() => (Array.isArray(detail.value?.merchantLogisticsTraces) ? detail.value.merchantLogisticsTraces : []))
const disputeInfo = computed(() => detail.value?.dispute || null)
const hasActiveDispute = computed(() => {
  const d = disputeInfo.value
  if (!d) return false
  const s = Number(d.dispute_status)
  return s === 0 || s === 1 || s === 2
})
const disputeStatusText = (s) => {
  const map = { 0: '待平台处理', 1: '举证中', 2: '平台处理中', 3: '已裁决', 4: '已关闭' }
  return map[Number(s)] || '-'
}
const disputeStatusType = (s) => {
  const map = { 0: 'warning', 1: '', 2: '', 3: 'success', 4: 'info' }
  return map[Number(s)] || 'info'
}
const judgeResultText = (r) => {
  const map = { 1: '支持用户', 2: '支持商家', 3: '部分支持', 4: '协商关闭' }
  return map[Number(r)] || '-'
}
const showBuyerReturn = computed(() => {
  const t = Number(afterSale.value?.type)
  return t === 2 || t === 4
})
const showMerchantAfterSaleShip = computed(() => Number(afterSale.value?.type) === 2)
const canMerchantAfterSaleShip = computed(() => Number(afterSale.value?.type) === 2 && Number(afterSale.value?.status) === 1)
const derivedStatus = computed(() => {
  const t = Number(afterSale.value?.type)
  const s = Number(afterSale.value?.status)
  if (t === 1 && s === 1) return 3
  return Number.isFinite(s) ? s : 0
})
const buyerHasWaybill = computed(() => Boolean(String(buyerLogistics.value?.trackingNo || '').trim()))
const merchantHasWaybill = computed(() => Boolean(String(merchantLogistics.value?.trackingNo || '').trim()))
const showCompleteBtn = computed(() => Number(afterSale.value?.type) === 2)
const canComplete = computed(() => showCompleteBtn.value && derivedStatus.value === 1 && merchantHasWaybill.value)

const parseEvidence = (val) => {
  const raw = String(val || '').trim()
  if (!raw) return []
  if (raw.startsWith('[') || raw.startsWith('{')) {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.map(v => String(v || '').trim()).filter(Boolean)
      if (parsed && typeof parsed === 'object') {
        const e = parsed.evidence
        const arr = Array.isArray(e) ? e : typeof e === 'string' ? e.split(',') : []
        return arr.map(v => String(v || '').trim()).filter(Boolean)
      }
    } catch (e) {
    }
  }
  return raw
    .split(',')
    .map(v => String(v || '').trim())
    .filter(Boolean)
}

const buyerEvidence = computed(() => parseEvidence(afterSale.value?.evidence))

const merchantParsed = computed(() => {
  const raw = String(afterSale.value?.merchantRemark || '').trim()
  if (!raw) return { remark: '', evidence: '' }
  if (raw.startsWith('{')) {
    try {
      const parsed = JSON.parse(raw)
      return {
        remark: String(parsed?.remark || '').trim(),
        evidence: String(parsed?.evidence || '').trim()
      }
    } catch (e) {
      return { remark: raw, evidence: '' }
    }
  }
  return { remark: raw, evidence: '' }
})

const merchantEvidenceList = computed(() => parseEvidence(merchantParsed.value.evidence))

const isImage = (url) => {
  const s = String(url || '').toLowerCase()
  return s.endsWith('.png') || s.endsWith('.jpg') || s.endsWith('.jpeg') || s.endsWith('.webp') || s.endsWith('.gif')
}

const toMoney = (value) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const formatTime = (t) => {
  const s = String(t || '').trim()
  if (!s) return '-'
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return s
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}

const statusText = (a) => {
  const s = a ? derivedStatus.value : 0
  return s === 1 ? '已同意' : s === 2 ? '已拒绝' : s === 3 ? '已完成' : '待处理'
}
const statusType = (a) => {
  const s = a ? derivedStatus.value : 0
  return s === 1 ? 'success' : s === 2 ? 'danger' : s === 3 ? 'success' : 'warning'
}
const typeText = (t) => {
  const v = Number(t)
  if (v === 1) return '退款'
  if (v === 2) return '换货'
  if (v === 3) return '补发'
  if (v === 4) return '退货退款'
  return '售后'
}
const orderStatusText = (s) => {
  const texts = ['待付款', '待发货', '已发货', '已完成']
  return texts[Number(s)] || '-'
}

const orderGoodsSummary = (o) => {
  const names = Array.isArray(o?.itemNames) ? o.itemNames.map(v => String(v || '').trim()).filter(Boolean) : []
  if (!names.length) return '-'
  if (names.length === 1) return names[0]
  return `${names[0]} 等${names.length}件`
}

const loadRelatedOrder = async (afterSale) => {
  relatedOrder.value = null
  if (!afterSale) return
  try {
    const res = await orderApi.list(getMerchantId(), null)
    const orders = Array.isArray(res.data) ? res.data : []
    const byId = orders.find(o => Number(o.id) === Number(afterSale.orderId))
    const byGroup = orders.find(o => Number(o.groupId) === Number(afterSale.groupId))
    relatedOrder.value = byId || byGroup || null
  } catch (e) {
    relatedOrder.value = null
  }
}

const progressSteps = computed(() => {
  const t = Number(afterSale.value?.type)
  if (t === 2) {
    return [
      { title: '提交申请', desc: '' },
      { title: '买家填写单号', desc: '' },
      { title: '商家同意换货', desc: '' },
      { title: '商家售后发货', desc: '' },
      { title: '买家收货确认', desc: '' },
      { title: '完成', desc: '' }
    ]
  }
  if (t === 4) {
    return [
      { title: '提交申请', desc: '' },
      { title: '买家填写单号', desc: '' },
      { title: '商家收到并退款', desc: '' },
      { title: '完成', desc: '' }
    ]
  }
  return [
    { title: '提交申请', desc: '' },
    { title: '商家同意退款', desc: '' },
    { title: '完成', desc: '' }
  ]
})

const progressActive = computed(() => {
  const s = derivedStatus.value
  if (s === 2) return 2
  if (s === 0) {
    const t = Number(afterSale.value?.type)
    if (t === 4) return buyerHasWaybill.value ? 3 : 2
    if (t === 2) return buyerHasWaybill.value ? 3 : 2
    return 2
  }
  const t = Number(afterSale.value?.type)
  if (t === 2) {
    if (s === 3) return 6
    if (merchantHasWaybill.value) return 4
    return 3
  }
  if (t === 4) {
    return s === 3 ? 4 : buyerHasWaybill.value ? 3 : 2
  }
  return s === 3 ? 3 : 2
})

const load = async () => {
  if (!Number.isFinite(id.value) || id.value <= 0) return
  loading.value = true
  try {
    const res = await afterSaleApi.detail(id.value)
    detail.value = res?.data || null
    form.value.remark = merchantParsed.value.remark
    form.value.evidence = merchantParsed.value.evidence
    await loadRelatedOrder(afterSale.value)
  } catch (error) {
    detail.value = null
    ElMessage.error(error?.response?.data?.message || '售后详情加载失败')
  } finally {
    loading.value = false
  }
}

const back = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/after-sale')
}

const goOrderDetail = () => {
  if (!orderView.value?.id) return
  router.push(`/order/${orderView.value.id}`)
}

const confirmHandle = async (status) => {
  try {
    await ElMessageBox.confirm(status === 1 ? '确认同意该售后？' : '确认拒绝该售后？', '提示', { type: 'warning' })
    await afterSaleApi.handle(id.value, status, form.value.remark, form.value.evidence)
    ElMessage.success('处理成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '处理失败')
    }
  }
}

const confirmComplete = async () => {
  try {
    await ElMessageBox.confirm('确认将该售后标记为已完成？', '提示', { type: 'warning' })
    await afterSaleApi.complete(id.value)
    ElMessage.success('已标记完成')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
    }
  }
}

const uploadImage = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    if (url) {
      form.value.evidence = [form.value.evidence, url].filter(Boolean).join(',')
    }
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.response?.data?.message || '上传失败')
  }
}

const uploadVideo = async (options) => {
  try {
    const res = await uploadApi.uploadVideo(options.file)
    const url = res?.data?.url || ''
    if (url) {
      form.value.evidence = [form.value.evidence, url].filter(Boolean).join(',')
    }
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.response?.data?.message || '上传失败')
  }
}

const openAfterSaleShip = () => {
  afterSaleShipForm.value = {
    expressCompany: String(merchantLogistics.value?.company || ''),
    expressNo: String(merchantLogistics.value?.trackingNo || '')
  }
  afterSaleShipVisible.value = true
}

const confirmAfterSaleShip = async () => {
  const expressCompany = String(afterSaleShipForm.value.expressCompany || '').trim()
  const expressNo = String(afterSaleShipForm.value.expressNo || '').trim()
  if (!expressCompany) {
    ElMessage.warning('请填写快递公司')
    return
  }
  if (!expressNo) {
    ElMessage.warning('请填写快递单号')
    return
  }
  try {
    await afterSaleApi.merchantShip(id.value, { expressCompany, expressNo })
    ElMessage.success('售后发货成功')
    afterSaleShipVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '售后发货失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page-card :deep(.el-card__header) {
  padding: 14px 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-text {
  font-weight: 800;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.top-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 14px;
}

.block-title {
  font-weight: 800;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.trace-timeline {
  margin-top: 6px;
}

.trace-content {
  font-weight: 700;
  color: #111827;
}

.trace-location {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.order-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}

.order-row .k {
  color: #6b7280;
  font-size: 12px;
}

.order-row .v {
  font-weight: 800;
  color: #111827;
  font-size: 12px;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
}

.upload-row {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.media {
  width: 100%;
  height: 110px;
  border-radius: 10px;
  object-fit: cover;
  background: #f5f5f5;
}

@media (max-width: 1100px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
}
</style>
