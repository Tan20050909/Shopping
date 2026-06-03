<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div>
        <h2 class="section-title">售后监管详情</h2>
        <p class="page-subtitle" v-if="afterSale">售后单号：{{ afterSale.afterSaleNo }}</p>
      </div>
      <div style="display:flex;gap:10px">
        <el-button @click="$router.push('/after-sale')">返回售后列表</el-button>
      </div>
    </div>

    <template v-if="afterSale">
      <div class="detail-grid">
        <!-- 基础信息 -->
        <el-card shadow="never" class="detail-card">
          <template #header><span class="card-title">售后基础信息</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="售后单号">{{ afterSale.afterSaleNo }}</el-descriptions-item>
            <el-descriptions-item label="售后类型">{{ typeMap[afterSale.afterSaleType] || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单ID">{{ afterSale.orderId }}</el-descriptions-item>
            <el-descriptions-item label="订单项ID">{{ afterSale.orderItemId }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ afterSale.userId }}</el-descriptions-item>
            <el-descriptions-item label="商家ID">{{ afterSale.merchantId }}</el-descriptions-item>
            <el-descriptions-item label="申请金额"><span style="color:#E60012;font-weight:600">¥{{ afterSale.applyAmount }}</span></el-descriptions-item>
            <el-descriptions-item label="处理状态">
              <el-tag :type="statusTagMap[afterSale.handleStatus]" size="small" style="border-radius:999px">{{ statusTextMap[afterSale.handleStatus] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请原因" :span="2">{{ afterSale.applyReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ afterSale.applyTime }}</el-descriptions-item>
            <el-descriptions-item label="处理时间">{{ afterSale.handleTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 关联订单 -->
        <el-card v-if="order" shadow="never" class="detail-card">
          <template #header><span class="card-title">关联订单</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ order.order_no || '-' }}</el-descriptions-item>
            <el-descriptions-item label="父订单号">{{ order.group_no || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥{{ order.pay_amount || '0' }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">{{ orderStatusText(order.order_status) }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ order.create_time || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="orderItems && orderItems.length" class="order-item-section">
            <div class="section-subtitle">商品明细</div>
            <div class="order-item-list">
              <div v-for="item in orderItems" :key="item.order_item_id" class="order-item-row">
                <img v-if="item.goods_pic" class="item-pic" :src="resolveImg(item.goods_pic)" alt="" @error="onImgError" />
                <div class="item-info">
                  <div class="item-name">{{ item.goods_name || '-' }}</div>
                  <div class="item-spec">{{ item.sku_name || '默认规格' }} x {{ item.num || 1 }}</div>
                  <div class="item-price">¥{{ item.total_price || '0' }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 买家证据 -->
        <el-card shadow="never" class="detail-card">
          <template #header><span class="card-title">买家证据</span></template>
          <div v-if="evidenceList.length" class="evidence-grid">
            <img v-for="(url, i) in evidenceList" :key="i" class="evidence-thumb" :src="resolveImg(url)" alt="" @error="onImgError" @click="previewUrl = url; previewVisible = true" />
          </div>
          <el-empty v-else description="买家未提供证据" :image-size="60" />
        </el-card>

        <!-- 商家备注与证据 -->
        <el-card shadow="never" class="detail-card">
          <template #header><span class="card-title">商家处理备注</span></template>
          <p v-if="afterSale.merchantRemark" style="white-space:pre-wrap;color:#333">{{ afterSale.merchantRemark }}</p>
          <el-empty v-else description="商家未填写备注" :image-size="60" />
          <div v-if="merchantEvidenceList.length" class="section-subtitle" style="margin-top:12px">商家证据</div>
          <div v-if="merchantEvidenceList.length" class="evidence-grid">
            <img v-for="(url, i) in merchantEvidenceList" :key="i" class="evidence-thumb" :src="resolveImg(url)" alt="" @error="onImgError" @click="previewUrl = url; previewVisible = true" />
          </div>
        </el-card>

        <!-- 退货物流 -->
        <el-card v-if="returnLogistics" shadow="never" class="detail-card">
          <template #header><span class="card-title">买家退货物流</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="快递公司">{{ returnLogistics.express_company || '-' }}</el-descriptions-item>
            <el-descriptions-item label="快递单号">{{ returnLogistics.express_no || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="returnLogistics.traces && returnLogistics.traces.length" style="margin-top:12px">
            <div class="section-subtitle">物流轨迹</div>
            <el-timeline>
              <el-timeline-item v-for="t in returnLogistics.traces" :key="t.trace_id" :timestamp="t.trace_time" placement="top">
                {{ t.trace_content }}
                <div v-if="t.trace_location" style="font-size:12px;color:#6b7280">{{ t.trace_location }}</div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>

        <!-- 平台备注 -->
        <el-card shadow="never" class="detail-card">
          <template #header><span class="card-title">平台备注</span></template>
          <p v-if="afterSale.platformRemark" style="white-space:pre-wrap;color:#333">{{ afterSale.platformRemark }}</p>
          <el-empty v-else description="平台未填写备注" :image-size="60" />
        </el-card>

        <!-- 退款信息 -->
        <el-card v-if="refund" shadow="never" class="detail-card">
          <template #header><span class="card-title">退款信息</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="退款单号">{{ refund.refund_no || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退款状态">{{ refundStatusMap[refund.refund_status] || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退款金额">¥{{ refund.refund_amount || '0' }}</el-descriptions-item>
            <el-descriptions-item label="退款渠道">{{ refund.refund_channel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="到账时间">{{ refund.success_time || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 售后处理记录 -->
        <el-card v-if="logs && logs.length" shadow="never" class="detail-card">
          <template #header><span class="card-title">售后处理记录</span></template>
          <el-timeline>
            <el-timeline-item v-for="(log, i) in logs" :key="log.log_id || i" :timestamp="log.create_time" placement="top">
              <div style="font-weight:700">{{ log.operation_desc }}</div>
              <div v-if="log.operator_type != null" style="font-size:12px;color:#6b7280;margin-top:2px">
                操作类型：{{ operatorTypeMap[log.operator_type] || log.operator_type }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 纠纷信息（已裁决时只读展示结果） -->
        <el-card v-if="dispute && !canJudge" shadow="never" class="detail-card">
          <template #header><span class="card-title">平台介入裁决结果</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="纠纷单号">{{ dispute.dispute_no || '-' }}</el-descriptions-item>
            <el-descriptions-item label="纠纷状态">{{ disputeStatusMap[dispute.dispute_status] || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="dispute.judge_result" label="判责结果">{{ judgeResultMap[dispute.judge_result] || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="dispute.final_amount" label="裁决金额">¥{{ dispute.final_amount }}</el-descriptions-item>
            <el-descriptions-item v-if="dispute.platform_opinion" label="平台意见" :span="2">{{ dispute.platform_opinion }}</el-descriptions-item>
            <el-descriptions-item v-if="dispute.judge_time" label="裁决时间">{{ dispute.judge_time }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 待裁决区域（dispute存在且未裁决时显示） -->
        <el-card v-if="dispute && canJudge" shadow="never" class="detail-card">
          <template #header><span class="card-title">平台裁决</span></template>
          <el-form label-position="top">
            <el-descriptions :column="2" border style="margin-bottom:16px">
              <el-descriptions-item label="纠纷单号">{{ dispute.dispute_no || '-' }}</el-descriptions-item>
              <el-descriptions-item label="纠纷状态">{{ disputeStatusMap[dispute.dispute_status] || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-form-item label="判责结果">
              <el-radio-group v-model="judgeForm.judgeResult">
                <el-radio :value="1">支持用户</el-radio>
                <el-radio :value="2">支持商家</el-radio>
                <el-radio :value="3">部分支持</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="judgeForm.judgeResult === 3" label="裁决金额">
              <el-input-number v-model="judgeForm.finalAmount" :min="0.01" :precision="2" placeholder="请输入裁决金额" />
              <span style="margin-left:8px;color:#999;font-size:12px">不超过售后申请金额 ¥{{ afterSale?.applyAmount || '0' }}</span>
            </el-form-item>
            <el-form-item label="处理意见">
              <el-input v-model="judgeForm.platformOpinion" type="textarea" :rows="3" placeholder="填写平台处理意见（将展示给用户和商家）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="judging" @click="submitJudge">提交裁决</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="售后单不存在" />

    <!-- 图片预览 -->
    <div v-if="previewVisible" class="image-overlay" @click="previewVisible = false">
      <img :src="previewUrl" class="image-overlay-img" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAfterSaleDetailFull, judgeDispute } from '../api/common'

const route = useRoute()
const loading = ref(false)
const judging = ref(false)
const detailData = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')
const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2280%22%20height%3D%2280%22%20viewBox%3D%220%200%2080%2080%22%3E%3Crect%20width%3D%2280%22%20height%3D%2280%22%20rx%3D%2214%22%20fill%3D%22%23f3f4f6%22/%3E%3Cpath%20d%3D%22M22%2028h36v24H22z%22%20fill%3D%22%23e5e7eb%22/%3E%3Cpath%20d%3D%22M26%2048l10-10%208%208%2010-12%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22/%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2236%22%20r%3D%223.5%22%20fill%3D%22%23cbd5e1%22/%3E%3C/svg%3E'

const judgeForm = ref({
  judgeResult: null,
  finalAmount: null,
  platformOpinion: ''
})

const typeMap = { 1: '仅退款', 2: '换货', 3: '补发', 4: '退货退款' }
const statusTextMap = { 0: '待商家处理', 1: '商家同意', 2: '商家拒绝', 3: '售后完成', 4: '退款成功', 5: '已撤销' }
const statusTagMap = { 0: 'warning', 1: 'success', 2: 'danger', 3: '', 4: 'success', 5: 'info' }
const refundStatusMap = { 0: '待退款', 1: '退款处理中', 2: '退款成功' }
const disputeStatusMap = { 0: '待平台处理', 1: '举证中', 2: '平台处理中', 3: '已裁决', 4: '已关闭' }
const judgeResultMap = { 1: '支持用户', 2: '支持商家', 3: '部分支持', 4: '协商关闭' }
const operatorTypeMap = { 1: '用户', 2: '商家', 3: '系统', 4: '平台' }

const afterSale = computed(() => detailData.value?.afterSale || null)
const order = computed(() => detailData.value?.order || null)
const orderItems = computed(() => detailData.value?.orderItems || null)
const logs = computed(() => detailData.value?.logs || null)
const returnLogistics = computed(() => detailData.value?.returnLogistics || null)
const dispute = computed(() => detailData.value?.dispute || null)
const refund = computed(() => detailData.value?.refund || null)

/** 是否存在待裁决的 dispute（状态为 0/1/2 时显示裁决按钮） */
const canJudge = computed(() => {
  if (!dispute.value) return false
  const s = Number(dispute.value.dispute_status)
  return s === 0 || s === 1 || s === 2
})

const evidenceList = computed(() => {
  const raw = String(afterSale.value?.applyEvidence || '').trim()
  return raw ? raw.split(',').map(v => v.trim()).filter(Boolean) : []
})

const merchantEvidenceList = computed(() => {
  const raw = String(afterSale.value?.merchantRemark || '').trim()
  if (!raw) return []
  if (raw.startsWith('{')) {
    try {
      const parsed = JSON.parse(raw)
      const e = parsed.evidence
      if (!e) return []
      if (Array.isArray(e)) return e.map(v => String(v).trim()).filter(Boolean)
      return String(e).split(',').map(v => v.trim()).filter(Boolean)
    } catch (e) { return [] }
  }
  return []
})

/** 提交裁决 */
const submitJudge = async () => {
  if (!judgeForm.value.judgeResult) {
    ElMessage.warning('请选择判责结果')
    return
  }
  if (judgeForm.value.judgeResult === 3 && (!judgeForm.value.finalAmount || judgeForm.value.finalAmount <= 0)) {
    ElMessage.warning('部分支持时请填写裁决金额')
    return
  }
  if (!judgeForm.value.platformOpinion.trim()) {
    ElMessage.warning('请填写处理意见')
    return
  }
  try {
    await ElMessageBox.confirm('确认提交裁决结果？提交后不可撤销。', '提示', { type: 'warning' })
  } catch { return }
  judging.value = true
  try {
    await judgeDispute({
      disputeId: dispute.value.dispute_id,
      judgeResult: judgeForm.value.judgeResult,
      finalAmount: judgeForm.value.judgeResult === 3 ? judgeForm.value.finalAmount : null,
      platformOpinion: judgeForm.value.platformOpinion.trim()
    })
    ElMessage.success('裁决已提交')
    judgeForm.value = { judgeResult: null, finalAmount: null, platformOpinion: '' }
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '裁决提交失败')
  } finally {
    judging.value = false
  }
}

const resolveImg = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return defaultImage
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  // 商品图片统一从商家端静态资源获取
  if (v.startsWith('/uploads/')) return 'http://localhost:8081' + v
  if (v.startsWith('uploads/')) return 'http://localhost:8081/' + v
  const idx = v.indexOf('/uploads/')
  if (idx > 0) return 'http://localhost:8081' + v.slice(idx)
  if (v.startsWith('/')) return v
  return defaultImage
}

const orderStatusText = (s) => {
  const map = ['待支付', '待发货', '待收货', '已完成', '已取消', '售后中']
  return map[Number(s)] || '-'
}

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

const load = async () => {
  const id = Number(route.params.id)
  if (!id || id <= 0) return
  loading.value = true
  try {
    const res = await getAfterSaleDetailFull(id)
    detailData.value = res.data || null
  } catch (e) {
    detailData.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card {
  border-radius: 8px;
}

.card-title {
  font-weight: 700;
  font-size: 15px;
}

.section-subtitle {
  font-weight: 700;
  font-size: 13px;
  color: #333;
  margin-bottom: 8px;
}

.order-item-section {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.order-item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-item-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.item-pic {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.item-name {
  font-weight: 700;
  font-size: 13px;
  color: #111827;
}

.item-spec {
  font-size: 12px;
  color: #6b7280;
}

.item-price {
  font-size: 13px;
  font-weight: 700;
  color: #E60012;
}

.evidence-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.evidence-thumb {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #eef2f7;
  cursor: pointer;
  transition: opacity .2s;
}

.evidence-thumb:hover {
  opacity: .8;
}

.image-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.75);
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
