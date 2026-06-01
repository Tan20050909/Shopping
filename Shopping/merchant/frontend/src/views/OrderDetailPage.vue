<template>
  <div class="page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="header">
          <div class="title">
            <el-button text @click="back">返回</el-button>
            <span class="title-text">订单详情</span>
          </div>
          <div class="actions">
            <el-button plain :disabled="!detail?.order?.userId" @click="contactBuyer">联系用户</el-button>
            <el-button plain :disabled="!detail?.order" @click="exportDetail">导出订单详情</el-button>
            <el-button plain @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="8" animated />

      <div v-else-if="detail?.order" class="content">
        <el-steps :active="stepActive" align-center finish-status="success">
          <el-step title="待付款" />
          <el-step title="待发货" />
          <el-step title="已发货" />
          <el-step title="已完成" />
        </el-steps>

        <div class="grid">
          <el-card shadow="never" class="block">
            <template #header>
              <div class="block-title">订单信息</div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(detail.order.status)">{{ getStatusText(detail.order.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(detail.order.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ formatTime(detail.order.payTime) }}</el-descriptions-item>
              <el-descriptions-item label="买家备注" :span="2">{{ detail.order.buyerRemark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="block">
            <template #header>
              <div class="block-title">支付明细</div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="商品总价">¥ {{ toMoney(detail.order.totalAmount) }}</el-descriptions-item>
              <el-descriptions-item label="运费">¥ {{ toMoney(detail.order.freight) }}</el-descriptions-item>
              <el-descriptions-item label="店铺优惠">- ¥ {{ toMoney(detail.order.discountAmount) }}</el-descriptions-item>
              <el-descriptions-item label="实付款">
                <span class="pay-amount">¥ {{ toMoney(detail.order.payAmount) }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="block span2">
            <template #header>
              <div class="block-title">收货信息</div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="收货人">{{ detail.order.consignee || '-' }}</el-descriptions-item>
              <el-descriptions-item label="电话">{{ detail.order.consigneePhone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地址" :span="2">{{ detail.order.receiveAddr || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="block span2">
            <template #header>
              <div class="block-title">商品明细</div>
            </template>
            <el-table :data="detail.items || []" style="width: 100%">
              <el-table-column label="商品" min-width="320">
                <template #default="{ row }">
                  <div class="item">
                    <img class="item-pic" :src="resolveImg(row.goodsPic)" alt="" @error="onImgError" />
                    <div class="item-meta">
                      <div class="item-name">{{ row.goodsName }}</div>
                      <div class="item-spec">{{ row.spec || '默认' }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="price" label="单价" width="120">
                <template #default="{ row }">¥ {{ toMoney(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="90" />
              <el-table-column prop="totalPrice" label="小计" width="120">
                <template #default="{ row }">¥ {{ toMoney(row.totalPrice) }}</template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="block span2">
            <template #header>
              <div class="block-head">
                <div class="block-title">物流信息</div>
                <el-button v-if="canEditWaybill" size="small" @click="openWaybillEdit">修改运单</el-button>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="快递公司">{{ detail.logistics?.company || '-' }}</el-descriptions-item>
              <el-descriptions-item label="快递单号">{{ detail.logistics?.trackingNo || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="trace-wrap">
              <el-divider content-position="left">物流轨迹</el-divider>
              <el-empty v-if="!isShippedOrDone" description="订单未发货" :image-size="80" />
              <el-empty v-else-if="logisticsTraces.length === 0" description="暂无物流轨迹" :image-size="80" />
              <el-timeline v-else class="trace-timeline">
                <el-timeline-item
                  v-for="t in logisticsTraces"
                  :key="t.id || `${t.traceTime}-${t.traceContent}`"
                  :timestamp="formatTime(t.traceTime)"
                  placement="top"
                >
                  <div class="trace-content">{{ t.traceContent }}</div>
                  <div v-if="t.traceLocation" class="trace-location">{{ t.traceLocation }}</div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>
        </div>
      </div>

      <el-empty v-else description="暂无订单详情" />
    </el-card>

    <el-dialog v-model="waybillVisible" title="修改运单" width="520px" destroy-on-close>
      <el-form :model="waybillForm" label-width="100px">
        <el-form-item label="快递公司">
          <el-input v-model="waybillForm.expressCompany" placeholder="例如：顺丰/中通/圆通" />
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="waybillForm.expressNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="waybillVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmWaybillEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => Number(route.params.id))
const detail = ref(null)
const loading = ref(false)
const waybillVisible = ref(false)
const waybillForm = ref({ expressCompany: '', expressNo: '' })

const defaultImage =
  'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2280%22%20height%3D%2280%22%20viewBox%3D%220%200%2080%2080%22%3E%3Crect%20width%3D%2280%22%20height%3D%2280%22%20rx%3D%2214%22%20fill%3D%22%23f3f4f6%22/%3E%3Cpath%20d%3D%22M22%2028h36v24H22z%22%20fill%3D%22%23e5e7eb%22/%3E%3Cpath%20d%3D%22M26%2048l10-10%208%208%2010-12%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22/%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2236%22%20r%3D%223.5%22%20fill%3D%22%23cbd5e1%22/%3E%3C/svg%3E'

const resolveImg = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return defaultImage
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  if (v.startsWith('/goods/')) return `/uploads${v}`
  if (v.startsWith('goods/')) return `/uploads/${v}`
  if (v.startsWith('/images/') || v.startsWith('/videos/')) return `/uploads${v}`
  if (v.startsWith('images/') || v.startsWith('videos/')) return `/uploads/${v}`
  const idx = v.indexOf('/uploads/')
  if (idx > 0) return v.slice(idx)
  if (v.startsWith('/')) return v
  return defaultImage
}

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
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

const getStatusType = (status) => {
  const types = ['info', 'warning', 'primary', 'success']
  return types[Number(status)] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待付款', '待发货', '已发货', '已完成']
  return texts[Number(status)] || '未知'
}

const stepActive = computed(() => Math.min(Math.max(Number(detail.value?.order?.status ?? 0), 0), 3) + 1)
const orderStatus = computed(() => Number(detail.value?.order?.status))
const isShippedOrDone = computed(() => Number.isFinite(orderStatus.value) && orderStatus.value >= 2)
const hasLogistics = computed(() => Boolean(detail.value?.logistics?.id))
const logisticsTraces = computed(() => {
  const arr = detail.value?.logisticsTraces
  return Array.isArray(arr) ? arr : []
})

const contactBuyer = () => {
  const userId = detail.value?.order?.userId
  if (!userId) return
  const key = `u:${userId}`
  const href = router.resolve({ path: '/chat', query: { standalone: '1', shell: '1', key } }).href
  const w = window.open(href, '_blank')
  if (!w) router.push({ path: '/chat', query: { key } })
}
const canEditWaybill = computed(() => isShippedOrDone.value)

const openWaybillEdit = () => {
  waybillForm.value = {
    expressCompany: String(detail.value?.logistics?.company || ''),
    expressNo: String(detail.value?.logistics?.trackingNo || '')
  }
  waybillVisible.value = true
}

const confirmWaybillEdit = async () => {
  const expressCompany = String(waybillForm.value.expressCompany || '').trim()
  const expressNo = String(waybillForm.value.expressNo || '').trim()
  if (!expressCompany) {
    ElMessage.warning('请填写快递公司')
    return
  }
  if (!expressNo) {
    ElMessage.warning('请填写快递单号')
    return
  }
  try {
    await orderApi.ship(orderId.value, { merchantId: getMerchantId(), expressCompany, expressNo })
    ElMessage.success('运单已更新')
    waybillVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '运单更新失败')
  }
}

const csvCell = (value) => {
  const raw = value == null ? '' : String(value)
  const normalized = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const escaped = normalized.replace(/"/g, '""')
  return `"${escaped}"`
}

const downloadTextFile = (filename, content, mime = 'text/plain;charset=utf-8') => {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const exportDetail = () => {
  const d = detail.value
  if (!d?.order) return
  const o = d.order
  const items = Array.isArray(d.items) ? d.items : []
  const lg = d.logistics || {}
  const traces = Array.isArray(d.logisticsTraces) ? d.logisticsTraces : []

  const lines = []
  lines.push(['订单号', '状态', '创建时间', '支付时间', '收货人', '电话', '地址', '商品总价', '运费', '店铺优惠', '实付款'].map(csvCell).join(','))
  lines.push(
    [
      o.orderNo,
      getStatusText(o.status),
      formatTime(o.createTime),
      formatTime(o.payTime),
      o.consignee || '',
      o.consigneePhone || '',
      o.receiveAddr || '',
      toMoney(o.totalAmount),
      toMoney(o.freight),
      toMoney(o.discountAmount),
      toMoney(o.payAmount)
    ].map(csvCell).join(',')
  )
  lines.push('')
  lines.push(['商品ID', '商品', '规格', '单价', '数量', '小计'].map(csvCell).join(','))
  for (const it of items) {
    lines.push(
      [
        it.goodsId ?? it.goods_id ?? '',
        it.goodsName ?? it.goods_name ?? '',
        it.spec || '默认',
        toMoney(it.price),
        it.quantity ?? it.num ?? '',
        toMoney(it.totalPrice ?? it.total_price)
      ].map(csvCell).join(',')
    )
  }
  lines.push('')
  lines.push(['快递公司', '快递单号'].map(csvCell).join(','))
  lines.push([lg.company || '', lg.trackingNo || ''].map(csvCell).join(','))
  lines.push('')
  lines.push(['轨迹时间', '轨迹内容', '轨迹地点'].map(csvCell).join(','))
  for (const t of traces) {
    lines.push([formatTime(t.traceTime), t.traceContent || '', t.traceLocation || ''].map(csvCell).join(','))
  }

  const filename = `订单详情_${o.orderNo || o.id || orderId.value}.csv`
  downloadTextFile(filename, `\ufeff${lines.join('\n')}`, 'text/csv;charset=utf-8')
}

const getErrorMessage = (error, fallback) => {
  const msg = error?.response?.data?.message || error?.message
  if (msg) return String(msg)
  const data = error?.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data && typeof data === 'object') {
    try {
      return JSON.stringify(data)
    } catch (e) {
    }
  }
  const status = error?.response?.status
  if (status) return `请求失败（${status}）`
  return fallback
}

const load = async () => {
  if (!Number.isFinite(orderId.value) || orderId.value <= 0) return
  loading.value = true
  try {
    const res = await orderApi.detail(orderId.value)
    detail.value = res?.data || null
  } catch (error) {
    detail.value = null
    ElMessage.error(getErrorMessage(error, '订单详情加载失败'))
  } finally {
    loading.value = false
  }
}

const back = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/order')
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
  gap: 18px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.block.span2 {
  grid-column: span 2;
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

.pay-amount {
  font-weight: 900;
  font-size: 16px;
  color: #e11d48;
}

.item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-pic {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  object-fit: cover;
  background: #f5f5f5;
}

.item-name {
  font-weight: 800;
}

.item-spec {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.trace-wrap {
  margin-top: 12px;
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
</style>
