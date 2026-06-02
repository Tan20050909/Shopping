<template>
  <div class="order-page">
    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">订单管理</div>
        <div class="panel-tools">
          <el-input v-model="keyword" class="search" placeholder="搜索订单号/收货人/手机号" clearable />
          <el-button plain @click="loadOrders">刷新</el-button>
        </div>
      </div>

      <div class="tabs">
        <div class="tab" :class="{ active: tabKey === 'all' }" @click="setTab('all')">所有订单</div>
        <div class="tab" :class="{ active: tabKey === '0' }" @click="setTab('0')">待支付<span v-if="countPay" class="badge">{{ countPay }}</span></div>
        <div class="tab" :class="{ active: tabKey === '1' }" @click="setTab('1')">待发货<span v-if="countShip" class="badge">{{ countShip }}</span></div>
        <div class="tab" :class="{ active: tabKey === '2' }" @click="setTab('2')">待收货</div>
        <div class="tab" :class="{ active: tabKey === '3' }" @click="setTab('3')">已完成</div>
        <div class="tab" :class="{ active: tabKey === '4' }" @click="setTab('4')">已取消<span v-if="countCancel" class="badge">{{ countCancel }}</span></div>
        <div class="tab" :class="{ active: tabKey === '5' }" @click="setTab('5')">售后中<span v-if="countAfterSale" class="badge">{{ countAfterSale }}</span></div>
        <div class="tab" :class="{ active: tabKey === 'after' }" @click="setTab('after')">售后<span v-if="countAfter" class="badge">{{ countAfter }}</span></div>
      </div>

      <div v-if="displayOrders.length" class="card-list">
        <div v-for="o in displayOrders" :key="o.id" class="card">
          <div class="card-head">
            <div class="meta">
              <span class="date">{{ formatDate(o.payTime || o.createTime) }}</span>
              <span class="split">|</span>
              <span class="no">订单号：{{ o.orderNo || '-' }}</span>
            </div>
            <div class="status-wrap">
              <span v-if="hasAnyAfterSale(o)" class="status-tag after">
                {{ getAfterSaleTagText(o) }}
              </span>
              <span class="status-tag" :class="statusClass(o.status)">{{ getStatusText(o.status) }}</span>
            </div>
          </div>

          <div class="card-body">
            <div class="goods-block">
              <div class="thumbs">
                <div v-if="thumbs(o).length" v-for="(it, idx) in thumbs(o)" :key="idx" class="thumb">
                  <img class="thumb-img" :src="resolveImg(it.goodsPic)" alt="" @error="onImgError" />
                </div>
                <div v-else class="thumb" v-for="n in 2" :key="n"></div>
              </div>
              <div class="goods-summary">{{ orderGoodsSummary(o) }}</div>
            </div>

            <div class="amount">
              <div class="line">
                <span class="k">商品金额</span>
                <span class="v">¥ {{ toMoney(o.totalAmount) }}</span>
              </div>
              <div class="line">
                <span class="k">实付款</span>
                <span class="v strong">¥ {{ toMoney(o.payAmount ?? o.totalAmount) }}</span>
              </div>
            </div>

            <div class="actions">
              <el-button size="small" class="btn" @click="openDetail(o.id)">订单详情</el-button>
              <el-button
                v-if="hasAnyAfterSale(o)"
                size="small"
                class="btn"
                @click="openAfterSaleDetail(o)"
              >
                售后详情
              </el-button>
              <el-button v-if="o.status === 0" size="small" class="btn" @click="openPrice(o)">改价</el-button>
              <el-button v-if="o.status === 0" size="small" class="btn" @click="openFreight(o)">邮费/包邮</el-button>
              <el-button v-if="o.status === 1" size="small" type="primary" class="btn primary" @click="openShip(o.id)">发货</el-button>
            </div>
          </div>

          <div class="card-foot">
            <div class="receiver">
              <span class="name">{{ o.consignee || '-' }}</span>
              <span class="phone">{{ o.consigneePhone || '' }}</span>
              <span class="addr">{{ o.receiveAddr || '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty">
        <el-empty :description="tabKey === 'after' ? '暂无售后订单' : '暂无订单数据'" />
      </div>
    </div>

    <el-dialog v-model="shipVisible" title="发货" width="520px" destroy-on-close>
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="快递公司">
          <el-input v-model="shipForm.expressCompany" placeholder="例如：顺丰/中通/圆通" />
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.expressNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="priceVisible" title="修改应付金额" width="520px" destroy-on-close>
      <el-form :model="priceForm" label-width="100px">
        <el-form-item label="应付金额(元)">
          <el-input-number v-model="priceForm.payAmount" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPrice">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="freightVisible" title="设置邮费" width="520px" destroy-on-close>
      <el-form :model="freightForm" label-width="100px">
        <el-form-item label="是否包邮">
          <el-switch v-model="freightForm.freeShipping" />
        </el-form-item>
        <el-form-item label="邮费(元)">
          <el-input-number v-model="freightForm.freight" :min="0" :precision="2" :disabled="freightForm.freeShipping" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freightVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmFreight">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { afterSaleApi, orderApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const route = useRoute()
const router = useRouter()

const orderList = ref([])
const afterSaleList = ref([])
const tabKey = ref('all')
const keyword = ref('')
const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2280%22%20height%3D%2280%22%20viewBox%3D%220%200%2080%2080%22%3E%3Crect%20width%3D%2280%22%20height%3D%2280%22%20rx%3D%2214%22%20fill%3D%22%23f3f4f6%22/%3E%3Cpath%20d%3D%22M22%2028h36v24H22z%22%20fill%3D%22%23e5e7eb%22/%3E%3Cpath%20d%3D%22M26%2048l10-10%208%208%2010-12%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22/%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2236%22%20r%3D%223.5%22%20fill%3D%22%23cbd5e1%22/%3E%3C/svg%3E'

const shipVisible = ref(false)
const shipOrderId = ref(null)
const shipForm = ref({ expressCompany: '', expressNo: '' })

const priceVisible = ref(false)
const priceOrderId = ref(null)
const priceForm = ref({ payAmount: 0 })

const freightVisible = ref(false)
const freightOrderId = ref(null)
const freightForm = ref({ freeShipping: true, freight: 0 })

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

const thumbs = (o) => {
  const pics = o?.itemPics
  if (Array.isArray(pics) && pics.length) {
    return pics.slice(0, 2).map(p => ({ goodsPic: p }))
  }
  return []
}

const toMoney = (value) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const formatDate = (t) => {
  const s = String(t || '').trim()
  if (!s) return '-'
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return s
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const loadOrders = async () => {
  try {
    const merchantId = getMerchantId()
    const results = await Promise.allSettled([
      orderApi.list(merchantId, null),
      afterSaleApi.list(merchantId, null)
    ])
    const orderRes = results[0].status === 'fulfilled' ? results[0].value : null
    const asRes = results[1].status === 'fulfilled' ? results[1].value : null
    orderList.value = Array.isArray(orderRes?.data) ? orderRes.data : []
    afterSaleList.value = Array.isArray(asRes?.data) ? asRes.data : []
  } catch (error) {
    orderList.value = []
    ElMessage.error(error?.response?.data?.message || error?.message || '订单加载失败')
  }
}

const matchKeyword = (o) => {
  const k = String(keyword.value || '').trim()
  if (!k) return true
  const itemNames = Array.isArray(o?.itemNames) ? o.itemNames.filter(Boolean).map(String).join(' ') : ''
  const fields = [
    o.orderNo,
    o.id,
    o.groupId,
    o.userId,
    o.userNickname,
    o.consignee,
    o.consigneePhone,
    o.receiveAddr,
    itemNames
  ]
    .filter(v => v != null && String(v).trim() !== '')
    .map(String)
  return fields.some(v => v.includes(k))
}

const displayOrders = computed(() => {
  const list = orderList.value.filter(matchKeyword)
  if (tabKey.value === 'after') return list.filter(o => hasPendingAfterSale(o))
  if (tabKey.value === 'all') return list
  const s = Number(tabKey.value)
  if (!Number.isFinite(s)) return list
  return list.filter(o => Number(o?.status) === s)
})

const orderGoodsSummary = (o) => {
  const names = Array.isArray(o?.itemNames) ? o.itemNames.map(v => String(v || '').trim()).filter(Boolean) : []
  if (!names.length) return '商品：-'
  const first = names[0]
  if (names.length === 1) return `商品：${first}`
  return `商品：${first} 等${names.length}件`
}

const setTab = (k) => {
  tabKey.value = k
}

const openDetail = (id) => {
  router.push(`/order/${id}`)
}

const openAfterSaleDetail = async (o) => {
  try {
    let candidates = getAfterSales(o)
    if (!candidates.length) {
      const merchantId = getMerchantId()
      const res = await afterSaleApi.list(merchantId, null)
      afterSaleList.value = Array.isArray(res?.data) ? res.data : []
      candidates = getAfterSales(o)
    }
    if (!candidates.length) {
      ElMessage.warning('未找到对应的售后单')
      router.push('/after-sale')
      return
    }
    candidates.sort((x, y) => {
      const t1 = new Date(String(x?.createTime || '').includes('T') ? x.createTime : String(x?.createTime || '').replace(' ', 'T')).getTime()
      const t2 = new Date(String(y?.createTime || '').includes('T') ? y.createTime : String(y?.createTime || '').replace(' ', 'T')).getTime()
      if (Number.isFinite(t1) && Number.isFinite(t2) && t1 !== t2) return t2 - t1
      return Number(y?.id || 0) - Number(x?.id || 0)
    })
    router.push(`/after-sale/${candidates[0].id}`)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '售后详情加载失败')
  }
}

const openShip = (id) => {
  shipOrderId.value = id
  shipForm.value = { expressCompany: '', expressNo: '' }
  shipVisible.value = true
}

const openPrice = (row) => {
  priceOrderId.value = row.id
  const v = Number(row.payAmount ?? row.totalAmount ?? 0) || 0
  priceForm.value = { payAmount: v }
  priceVisible.value = true
}

const confirmPrice = async () => {
  try {
    const payAmount = Number(priceForm.value.payAmount || 0)
    const res = await orderApi.updatePayAmount(priceOrderId.value, payAmount)
    const updated = res?.data
    if (!updated) throw new Error('改价失败')
    ElMessage.success('已更新应付金额')
    priceVisible.value = false
    await loadOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '改价失败')
  }
}

const openFreight = (row) => {
  freightOrderId.value = row.id
  const f = Number(row.freight ?? 0) || 0
  freightForm.value = { freeShipping: f <= 0, freight: f }
  freightVisible.value = true
}

const confirmFreight = async () => {
  try {
    const freight = freightForm.value.freeShipping ? 0 : Number(freightForm.value.freight || 0)
    const res = await orderApi.updateFreight(freightOrderId.value, freight)
    const updated = res?.data
    if (!updated) throw new Error('邮费更新失败')
    ElMessage.success('邮费已更新')
    freightVisible.value = false
    await loadOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '邮费更新失败')
  }
}

const confirmShip = async () => {
  const expressCompany = String(shipForm.value.expressCompany || '').trim()
  const expressNo = String(shipForm.value.expressNo || '').trim()
  if (!expressCompany) {
    ElMessage.warning('请填写快递公司')
    return
  }
  if (!expressNo) {
    ElMessage.warning('请填写快递单号')
    return
  }
  try {
    await orderApi.ship(shipOrderId.value, { merchantId: getMerchantId(), expressCompany, expressNo })
    ElMessage.success('发货成功')
    shipVisible.value = false
    await loadOrders()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '发货失败')
  }
}

const ORDER_STATUS_TEXT = ['待支付', '待发货', '待收货', '已完成', '已取消', '售后中']
const ORDER_STATUS_TYPE = ['info', 'warning', 'primary', 'success', 'info', 'danger']

const getStatusType = (status) => {
  return ORDER_STATUS_TYPE[status] || 'info'
}

const getStatusText = (status) => {
  return ORDER_STATUS_TEXT[status] || '未知'
}

const statusClass = (status) => {
  const s = Number(status)
  if (s === 0) return 'pay'
  if (s === 1) return 'ship'
  if (s === 2) return 'deliver'
  if (s === 3) return 'done'
  if (s === 4) return 'cancel'
  if (s === 5) return 'aftersale'
  return 'other'
}

const countPay = computed(() => orderList.value.filter(o => Number(o.status) === 0).length)
const countShip = computed(() => orderList.value.filter(o => Number(o.status) === 1).length)
const countCancel = computed(() => orderList.value.filter(o => Number(o.status) === 4).length)
const countAfterSale = computed(() => orderList.value.filter(o => Number(o.status) === 5).length)
const countAfter = computed(() => afterSaleList.value.filter(a => Number(a?.status) === 0).length)

const afterSaleIndex = computed(() => {
  const byOrderId = new Map()
  const byGroupId = new Map()
  for (const a of (afterSaleList.value || [])) {
    if (!a) continue
    const oid = a.orderId != null ? Number(a.orderId) : null
    const gid = a.groupId != null ? Number(a.groupId) : null
    if (Number.isFinite(oid)) {
      const arr = byOrderId.get(oid) || []
      arr.push(a)
      byOrderId.set(oid, arr)
    }
    if (Number.isFinite(gid)) {
      const arr = byGroupId.get(gid) || []
      arr.push(a)
      byGroupId.set(gid, arr)
    }
  }
  return { byOrderId, byGroupId }
})

const getAfterSales = (o) => {
  if (!o) return []
  const orderId = o.id != null ? Number(o.id) : null
  const groupId = o.groupId != null ? Number(o.groupId) : null
  const { byOrderId, byGroupId } = afterSaleIndex.value
  const a1 = Number.isFinite(orderId) ? (byOrderId.get(orderId) || []) : []
  const a2 = Number.isFinite(groupId) ? (byGroupId.get(groupId) || []) : []
  if (!a1.length && !a2.length) return []
  const m = new Map()
  for (const a of [...a1, ...a2]) {
    if (!a) continue
    const id = a.id != null ? Number(a.id) : null
    if (Number.isFinite(id)) m.set(id, a)
  }
  return Array.from(m.values())
}

const isAfterSaleCompleted = (a) => {
  const t = Number(a?.type)
  const s = Number(a?.status)
  if (s === 2 || s === 3) return true
  if (t === 1 && s === 1) return true
  return false
}

const hasAnyAfterSale = (o) => getAfterSales(o).length > 0

const hasPendingAfterSale = (o) => getAfterSales(o).some(a => Number(a?.status) === 0)

const getAfterSaleTagText = (o) => {
  const list = getAfterSales(o)
  if (!list.length) return ''
  const pending = list.some(a => Number(a?.status) === 0)
  if (pending) return '售后'
  const completed = list.every(isAfterSaleCompleted)
  return completed ? '售后已完成' : '售后处理中'
}

onMounted(() => {
  loadOrders()
})

watch(
  () => route.query?.keyword,
  (v) => {
    keyword.value = String(v || '').trim()
    tabKey.value = 'all'
  },
  { immediate: true }
)

watch(
  () => route.query?.tab,
  (v) => {
    if (v != null && v !== '') {
      tabKey.value = String(v)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 900;
  color: #111827;
}

.panel-tools {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search {
  width: 280px;
}

.tabs {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 10px;
}

.tab {
  position: relative;
  font-weight: 800;
  font-size: 13px;
  color: #6b7280;
  padding: 8px 2px;
  cursor: pointer;
}

.tab.active {
  color: #e11d48;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -11px;
  height: 2px;
  background: #e11d48;
}

.badge {
  margin-left: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  border-radius: 999px;
  background: #e11d48;
  color: #fff;
}

.card-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 12px;
}

.no {
  color: #111827;
  font-weight: 800;
}

.status-wrap {
  display: flex;
  gap: 8px;
  align-items: center;
}

.status-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  background: #f3f4f6;
  color: #374151;
}

.status-tag.pay { background: #fff7ed; color: #c2410c; }
.status-tag.ship { background: #eff6ff; color: #1d4ed8; }
.status-tag.deliver { background: #eef2ff; color: #4338ca; }
.status-tag.done { background: #ecfdf5; color: #047857; }
.status-tag.after { background: #fee2e2; color: #b91c1c; }
.status-tag.cancel { background: #f5f5f5; color: #737373; }
.status-tag.aftersale { background: #fef3c7; color: #92400e; }

.card-body {
  display: grid;
  grid-template-columns: 180px 220px 1fr;
  gap: 12px;
  padding: 12px;
  align-items: center;
}

.goods-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thumbs {
  display: flex;
  gap: 10px;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  background: #f3f4f6;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-summary {
  font-size: 12px;
  color: #111827;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount .line {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: #6b7280;
}

.amount .line + .line {
  margin-top: 10px;
}

.amount .v {
  color: #111827;
  font-weight: 800;
}

.amount .v.strong {
  color: #111827;
  font-weight: 900;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.btn {
  border-radius: 10px;
}

.card-foot {
  padding: 10px 12px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.receiver {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
}

.receiver .name {
  font-weight: 900;
  color: #111827;
}

.receiver .addr {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty {
  padding: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.item-pic {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
}

.item-name {
  font-weight: 600;
}

.item-spec {
  font-size: 12px;
  color: #666;
}
</style>
