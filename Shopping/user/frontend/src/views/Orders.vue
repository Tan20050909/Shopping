<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, imageOf } from '../api/client'

const router = useRouter()
const orders = ref([])
const status = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const fallbackOrderCover = new URL('../public/brand-assets/digital-banner.png', import.meta.url).href

// #region debug-point A:report
const DEBUG_SERVER_URL = 'http://127.0.0.1:7777/event'
const DEBUG_SESSION_ID = 'order-items-missing'
function dbg(hypothesisId, msg, data = {}) {
  fetch(DEBUG_SERVER_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: DEBUG_SESSION_ID,
      runId: 'pre-fix',
      hypothesisId,
      location: 'Orders.vue',
      msg: `[DEBUG] ${msg}`,
      data,
      ts: Date.now()
    })
  }).catch(() => {})
}
// #endregion

const orderStatusText = {
  0: '待支付',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消',
  5: '售后中'
}

const payStatusText = {
  0: '待支付',
  1: '支付成功',
  2: '支付失败',
  3: '已关闭',
  4: '退款处理中',
  5: '已退款'
}

const statusTabs = [
  { label: '全部订单', value: '' },
  { label: '待支付', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

const hasOrders = computed(() => orders.value.length > 0)

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function orderIdOf(order) {
  const direct = field(order, 'firstOrderId', 'order_id')
  if (direct) return direct
  const child = (order?.childOrders || order?.child_orders || [])[0]
  return field(child, 'orderId', 'order_id')
}

function itemsOf(order) {
  return order.items || order.orderItems || order.order_items || order.goodsItems || []
}

function previewItems(order) {
  return itemsOf(order)
}

function itemCount(order) {
  return itemsOf(order).reduce((sum, item) => sum + Number(field(item, 'num', 'num', 0)), 0)
}

function firstAfterSale(order) {
  return itemsOf(order).find((item) => field(item, 'afterSaleId', 'after_sale_id'))
}

function firstAvailableItem(order) {
  return itemsOf(order).find((item) => !field(item, 'afterSaleId', 'after_sale_id'))
}

function hasRefundSuccess(order) {
  return itemsOf(order).some((item) =>
    Number(field(item, 'refundStatus', 'refund_status', -1)) === 2 ||
    Number(field(item, 'afterSaleHandleStatus', 'after_sale_handle_status', -1)) === 4
  )
}

function hasAfterSale(order) {
  return Boolean(firstAfterSale(order))
}

function displayOrderStatus(order) {
  if (hasRefundSuccess(order) || Number(field(order, 'payStatus', 'pay_status')) === 5) return '已退款'
  return orderStatusText[field(order, 'groupStatus', 'order_status')] || `状态 ${field(order, 'groupStatus', 'order_status')}`
}

function applyAfterSale(order) {
  const item = firstAvailableItem(order)
  if (!item) {
    viewAfterSale(order)
    return
  }
  router.push(`/after-sales/apply?orderId=${field(item, 'orderId', 'order_id')}&orderItemId=${field(item, 'orderItemId', 'order_item_id')}`)
}

function viewAfterSale(order) {
  const item = firstAfterSale(order)
  if (item) {
    router.push(`/after-sales/${field(item, 'afterSaleId', 'after_sale_id')}`)
  } else {
    router.push('/after-sales')
  }
}

function goReview(item) {
  router.push(`/reviews/create?orderId=${field(item, 'orderId', 'order_id')}&orderItemId=${field(item, 'orderItemId', 'order_item_id')}`)
}

function reviewLabel(item) {
  const afterSaleStatus = Number(field(item, 'afterSaleStatus', 'after_sale_status', 0))
  if (afterSaleStatus === 1) return '售后中'
  if (afterSaleStatus === 2) return '已退款'
  if (field(item, 'reviewed', 'reviewed') || Number(field(item, 'commentStatus', 'comment_status', 0)) === 1) return '已评价'
  return ''
}

function viewDetail(order) {
  const id = orderIdOf(order)
  // #region debug-point C:detail-click
  dbg('C', 'click detail', {
    id,
    groupNo: field(order, 'groupNo', 'group_no'),
    groupId: field(order, 'groupId', 'group_id'),
    firstOrderId: field(order, 'firstOrderId', 'firstOrderId'),
    childOrdersLen: Number(order?.childOrders?.length || 0),
    itemsLen: Number(itemsOf(order)?.length || 0)
  })
  // #endregion
  if (!id) {
    ElMessage.warning('订单数据异常，缺少子订单 ID，无法打开详情')
    return
  }
  router.push(`/orders/${id}`)
}

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (status.value !== undefined && status.value !== null && status.value !== '') params.set('status', status.value)
    params.set('pageNum', pageNum.value)
    params.set('pageSize', pageSize.value)
    const data = await api(`/api/user/orders?${params.toString()}`)
    const list = data.records || []
    const seen = new Set()
    orders.value = list.filter((order) => {
      const key = String(field(order, 'groupNo', 'group_no') || field(order, 'groupId', 'group_id') || '')
      if (!key) return true
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    total.value = data.total || 0
    // #region debug-point B:orders-response
    dbg('B', 'orders response summary', {
      total: Number(data.total || 0),
      recordCount: Number(list.length || 0),
      filteredCount: Number(orders.value.length || 0),
      sample: orders.value.slice(0, 3).map((o) => ({
        groupNo: field(o, 'groupNo', 'group_no'),
        groupId: field(o, 'groupId', 'group_id'),
        firstOrderId: field(o, 'firstOrderId', 'order_id'),
        orderId: field(o, 'orderId', 'order_id'),
        childOrdersLen: Number(o?.childOrders?.length || 0),
        itemsLen: Number(itemsOf(o)?.length || 0),
        keys: Object.keys(o || {}).slice(0, 40)
      }))
    })
    // #endregion
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function cancel(order) {
  try {
    await api(`/api/user/orders/${orderIdOf(order)}/cancel`, { method: 'PUT' })
    ElMessage.success('订单已取消')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function receive(order) {
  try {
    await api(`/api/user/orders/${orderIdOf(order)}/receive`, { method: 'PUT' })
    ElMessage.success('已确认收货')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function rebuy(order) {
  try {
    const result = await api(`/api/user/orders/${orderIdOf(order)}/rebuy`, { method: 'POST' })
    ElMessage.success(result.skipped?.length ? `已加入 ${result.addedCount} 件，部分商品当前不可再次购买` : '商品已重新加入购物车')
    router.push('/cart')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleStatusChange() {
  pageNum.value = 1
  await load()
}

async function handlePageChange(page) {
  pageNum.value = page
  await load()
}

onMounted(load)
</script>

<template>
  <main class="page stack order-page">
    <section class="band order-toolbar">
      <div>
        <h1 class="section-title">我的订单</h1>
        <p class="muted">按结算记录查看订单、商品和售后进度。</p>
      </div>
      <el-segmented v-model="status" :options="statusTabs" @change="handleStatusChange" />
    </section>

    <section v-loading="loading" class="orders-surface">
      <el-empty v-if="!loading && !hasOrders" description="当前没有匹配的订单" />

      <article v-for="order in orders" :key="field(order, 'groupId', 'group_id')" class="order-card">
        <div class="order-head">
          <div class="stack order-meta">
            <strong>父订单号 {{ field(order, 'groupNo', 'group_no') || field(order, 'orderNo', 'order_no') }}</strong>
            <span class="muted">下单时间 {{ field(order, 'createTime', 'create_time') }}</span>
          </div>
          <div class="order-status-box">
            <span class="status-chip">{{ displayOrderStatus(order) }}</span>
            <span class="muted">{{ payStatusText[field(order, 'payStatus', 'pay_status')] || `支付 ${field(order, 'payStatus', 'pay_status')}` }}</span>
            <strong class="price">实付 ¥{{ money(field(order, 'payAmount', 'pay_amount')) }}</strong>
          </div>
        </div>

        <div v-if="order.childOrders?.length" class="package-summary">
          <span v-for="child in order.childOrders" :key="field(child, 'orderId', 'order_id')" class="package-chip">
            {{ field(child, 'merchantName', 'merchant_name') || '店铺分单' }}
            · {{ orderStatusText[field(child, 'orderStatus', 'order_status')] }}
            · ¥{{ money(field(child, 'payAmount', 'pay_amount')) }}
          </span>
        </div>

        <div class="order-items-strip">
          <div v-for="item in previewItems(order)" :key="field(item, 'orderItemId', 'order_item_id')" class="order-item-mini">
            <img
              class="cover mini-cover"
              :src="imageOf(item)"
              :alt="field(item, 'goodsName', 'goods_name')"
              @error="(e) => (e.target.src = fallbackOrderCover)"
            />
            <div class="mini-info">
              <strong>{{ field(item, 'goodsName', 'goods_name') }}</strong>
              <p class="muted">SKU：{{ field(item, 'skuName', 'sku_name') || '默认规格' }}</p>
              <p class="muted">数量 x {{ field(item, 'num', 'num') }}</p>
              <div class="mini-actions">
                <el-button v-if="field(item, 'canReview', 'canReview')" size="small" type="primary" @click="goReview(item)">去评价</el-button>
                <span v-else-if="reviewLabel(item)" class="mini-status">{{ reviewLabel(item) }}</span>
              </div>
            </div>
          </div>
          <span v-if="itemCount(order) > 6" class="muted">共 {{ itemCount(order) }} 件</span>
        </div>

        <div class="order-actions">
          <template v-if="Number(field(order, 'groupStatus', 'order_status')) === 0 && Number(field(order, 'payStatus', 'pay_status')) === 0">
            <el-button type="primary" @click="viewDetail(order)">去支付</el-button>
            <el-button @click="cancel(order)">取消订单</el-button>
          </template>
          <template v-else-if="hasRefundSuccess(order)">
            <el-button disabled>退款成功</el-button>
            <el-button @click="viewAfterSale(order)">查看售后</el-button>
          </template>
          <template v-else-if="hasAfterSale(order) || Number(field(order, 'groupStatus', 'order_status')) === 5">
            <el-button @click="viewAfterSale(order)">查看售后</el-button>
          </template>
          <template v-else-if="Number(field(order, 'groupStatus', 'order_status')) === 1">
            <el-button type="primary" @click="applyAfterSale(order)">申请退款</el-button>
          </template>
          <template v-else-if="Number(field(order, 'groupStatus', 'order_status')) === 2">
            <el-button @click="router.push(`/orders/${orderIdOf(order)}/logistics`)">查看物流</el-button>
            <el-button type="success" @click="receive(order)">确认收货</el-button>
            <el-button type="primary" @click="applyAfterSale(order)">申请售后</el-button>
          </template>
          <template v-else-if="Number(field(order, 'groupStatus', 'order_status')) === 3">
            <el-button type="primary" @click="applyAfterSale(order)">申请售后</el-button>
            <el-button @click="rebuy(order)">再来一单</el-button>
          </template>
          <el-button @click="viewDetail(order)">查看详情</el-button>
        </div>
      </article>
    </section>

    <el-pagination
      v-if="total > pageSize"
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :current-page="pageNum"
      :total="total"
      @current-change="handlePageChange"
    />
  </main>
</template>

<style scoped>
.order-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
}

.orders-surface {
  min-height: 280px;
  display: grid;
  gap: 16px;
}

.order-card {
  display: grid;
  gap: 16px;
  padding: 22px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #fff;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.order-meta {
  gap: 6px;
}

.order-status-box,
.order-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.package-summary,
.order-items-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.package-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
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

.order-item-mini {
  width: min(100%, 330px);
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.mini-info {
  min-width: 0;
}

.mini-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 26px;
  margin-top: 6px;
}

.mini-status {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.order-item-mini strong,
.mini-info p {
  display: block;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-cover {
  width: 64px;
  border-radius: 8px;
}

@media (max-width: 760px) {
  .order-toolbar,
  .order-head {
    align-items: stretch;
    flex-direction: column;
  }

  .order-status-box,
  .order-actions {
    justify-content: flex-start;
  }
}
</style>
