<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, imageOf } from '../api/client'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const paying = ref(false)
const acting = ref(false)
const updatingAddress = ref(false)

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

const timeline = computed(() => detail.value?.statusLogs || [])

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function itemAfterSaleStatus(item) {
  const afterSaleId = field(item, 'afterSaleId', 'after_sale_id')
  if (!afterSaleId) return ''
  const handleStatus = Number(field(item, 'afterSaleHandleStatus', 'after_sale_handle_status', -1))
  const refundStatus = Number(field(item, 'refundStatus', 'refund_status', -1))
  if (handleStatus === 4 || refundStatus === 2) return '退款成功'
  if (handleStatus === 2) return '售后驳回'
  if (refundStatus === 1) return '退款处理中'
  if (handleStatus === 1) return '查看售后'
  return '售后处理中'
}

function canApplyItem(item) {
  return detail.value?.actions?.canApplyAfterSale && !field(item, 'afterSaleId', 'after_sale_id')
}

function applyItem(item) {
  router.push(`/after-sales/apply?orderId=${field(item, 'orderId', 'order_id')}&orderItemId=${field(item, 'orderItemId', 'order_item_id')}`)
}

function viewAfterSale(item) {
  router.push(`/after-sales/${field(item, 'afterSaleId', 'after_sale_id')}`)
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

async function load() {
  try {
    detail.value = await api(`/api/user/orders/${route.params.id}`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function pay() {
  if (!detail.value?.groupId && !detail.value?.group_id) return
  paying.value = true
  try {
    await api(`/api/user/payments/${field(detail.value, 'groupId', 'group_id')}/pay`, { method: 'POST', body: { channel: 9 } })
    ElMessage.success('支付成功')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    paying.value = false
  }
}

async function cancel() {
  acting.value = true
  try {
    await api(`/api/user/orders/${field(detail.value, 'currentOrderId', 'order_id', route.params.id)}/cancel`, { method: 'PUT' })
    ElMessage.success('订单已取消')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    acting.value = false
  }
}

async function receive() {
  acting.value = true
  try {
    await api(`/api/user/orders/${field(detail.value, 'currentOrderId', 'order_id', route.params.id)}/receive`, { method: 'PUT' })
    ElMessage.success('已确认收货')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    acting.value = false
  }
}

async function rebuy() {
  acting.value = true
  try {
    const result = await api(`/api/user/orders/${field(detail.value, 'currentOrderId', 'order_id', route.params.id)}/rebuy`, { method: 'POST' })
    ElMessage.success(result.skipped?.length ? `已加入 ${result.addedCount} 件，部分商品当前不可再次购买` : '商品已重新加入购物车')
    router.push('/cart')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    acting.value = false
  }
}

function changeAddress() {
  const currentOrderId = field(detail.value, 'currentOrderId', 'order_id', route.params.id)
  router.push({
    path: '/profile/addresses',
    query: { orderId: String(currentOrderId), back: `/orders/${currentOrderId}` }
  })
}

onMounted(load)
</script>

<template>
  <main v-if="detail" class="page stack detail-page">
    <section class="band stack">
      <div class="detail-head">
        <div>
          <h1 class="section-title">订单详情</h1>
          <p class="muted">父订单 {{ field(detail, 'groupNo', 'group_no') }} · 当前包裹 {{ field(detail, 'currentOrderId', 'order_id') }}</p>
        </div>
        <div class="detail-actions">
          <el-button @click="router.push('/orders')">返回订单列表</el-button>
          <el-button v-if="detail.actions?.canPay" type="primary" :loading="paying" @click="pay">去支付</el-button>
          <el-button v-if="detail.actions?.canCancel" :loading="acting" @click="cancel">取消订单</el-button>
          <el-button v-if="detail.actions?.canUpdateAddress" :loading="updatingAddress" @click="changeAddress">修改地址</el-button>
          <el-button v-if="detail.actions?.canViewLogistics" @click="router.push(`/orders/${field(detail, 'currentOrderId', 'order_id', route.params.id)}/logistics`)">查看物流</el-button>
          <el-button v-if="detail.actions?.canReceive" type="success" :loading="acting" @click="receive">确认收货</el-button>
          <el-button v-if="detail.actions?.canRebuy" :loading="acting" @click="rebuy">再来一单</el-button>
        </div>
      </div>

      <div class="summary-grid">
        <div class="metric-block">
          <span class="muted">订单状态</span>
          <strong>{{ orderStatusText[field(detail, 'groupStatus', 'order_status')] || field(detail, 'groupStatus', 'order_status') }}</strong>
        </div>
        <div class="metric-block">
          <span class="muted">支付状态</span>
          <strong>{{ payStatusText[field(detail, 'payStatus', 'pay_status')] || field(detail, 'payStatus', 'pay_status') }}</strong>
        </div>
        <div class="metric-block">
          <span class="muted">实付金额</span>
          <strong class="price">¥{{ money(field(detail, 'payAmount', 'pay_amount')) }}</strong>
        </div>
        <div class="metric-block">
          <span class="muted">支付时间</span>
          <strong>{{ field(detail, 'payTime', 'pay_time') || '未支付' }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-two-col">
      <div class="band stack">
        <h2 class="section-title">收货信息</h2>
        <div class="info-lines">
          <strong>{{ detail.receiver?.consignee }} {{ detail.receiver?.phone }}</strong>
          <span class="muted">{{ detail.receiver?.address }}</span>
        </div>
      </div>

      <div class="band stack">
        <h2 class="section-title">支付信息</h2>
        <div v-if="detail.payment" class="info-lines">
          <span class="muted">支付单号：{{ detail.payment.payNo || detail.payment.pay_no }}</span>
          <span class="muted">支付渠道：{{ detail.payment.payChannel || detail.payment.pay_channel || 9 }}</span>
          <span class="muted">支付流水：{{ detail.payment.thirdTradeNo || detail.payment.third_trade_no || '未生成' }}</span>
          <span class="muted">过期时间：{{ detail.payment.expireTime || detail.payment.expire_time }}</span>
        </div>
        <el-empty v-else description="暂无支付信息" :image-size="72" />
      </div>
    </section>

    <section class="band stack">
      <h2 class="section-title">优惠信息</h2>
      <div v-if="detail.couponInfo?.coupons?.length" class="coupon-detail">
        <div v-for="coupon in detail.couponInfo.coupons" :key="field(coupon, 'orderCouponId', 'order_coupon_id')" class="row coupon-line">
          <span>{{ field(coupon, 'couponName', 'coupon_name') }}</span>
          <strong class="price">-¥{{ money(field(coupon, 'discountAmount', 'discount_amount')) }}</strong>
        </div>
      </div>
      <p v-else class="muted">未使用优惠券。</p>
    </section>

    <section class="band stack">
      <h2 class="section-title">包裹信息</h2>
      <div v-for="child in detail.childOrders || []" :key="field(child, 'orderId', 'order_id')" class="child-order-row">
        <div class="stack child-meta">
          <strong>{{ field(child, 'merchantName', 'merchant_name') || '店铺分单' }}</strong>
          <span class="muted">包裹编号 {{ field(child, 'orderNo', 'order_no') }}</span>
        </div>
        <div class="child-status">
          <span class="status-chip">{{ orderStatusText[field(child, 'orderStatus', 'order_status')] }}</span>
          <span class="muted">{{ payStatusText[field(child, 'payStatus', 'pay_status')] }}</span>
          <strong class="price">¥{{ money(field(child, 'payAmount', 'pay_amount')) }}</strong>
        </div>
      </div>
    </section>

    <section class="band stack">
      <h2 class="section-title">商品明细</h2>
      <div v-for="item in detail.items || []" :key="field(item, 'orderItemId', 'order_item_id')" class="list-item order-line">
        <img class="cover detail-cover" :src="imageOf(item)" :alt="field(item, 'goodsName', 'goods_name')" />
        <div class="stack line-body">
          <div>
            <strong>{{ field(item, 'goodsName', 'goods_name') }}</strong>
            <p class="muted">SKU：{{ field(item, 'skuName', 'sku_name') || '默认规格' }} · 数量 x {{ field(item, 'num', 'num') }}</p>
            <p class="muted">订单项 {{ field(item, 'orderItemId', 'order_item_id') }}</p>
          </div>
          <span class="price">¥{{ money(field(item, 'totalPrice', 'total_price')) }}</span>
          <div class="row">
            <el-button v-if="canApplyItem(item)" size="small" type="primary" @click="applyItem(item)">申请售后</el-button>
            <template v-else-if="field(item, 'afterSaleId', 'after_sale_id')">
              <span class="status-chip">{{ itemAfterSaleStatus(item) }}</span>
              <el-button size="small" @click="viewAfterSale(item)">查看售后</el-button>
            </template>
            <el-button v-if="field(item, 'canReview', 'canReview')" size="small" type="primary" @click="goReview(item)">去评价</el-button>
            <span v-else-if="reviewLabel(item)" class="status-chip soft">{{ reviewLabel(item) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="band stack">
      <h2 class="section-title">状态时间线</h2>
      <el-timeline v-if="timeline.length">
        <el-timeline-item
          v-for="log in timeline"
          :key="field(log, 'logId', 'log_id')"
          :timestamp="field(log, 'createTime', 'create_time')"
          placement="top"
        >
          <strong>{{ field(log, 'operationDesc', 'operation_desc') }}</strong>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无状态日志" :image-size="72" />
    </section>
  </main>
</template>

<style scoped>
.detail-head,
.detail-actions,
.child-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
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
  min-height: 88px;
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-soft);
}

.info-lines,
.child-meta,
.line-body {
  gap: 8px;
}

.child-order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.child-order-row:last-child {
  border-bottom: 0;
}

.coupon-detail {
  display: grid;
  gap: 10px;
}

.coupon-line {
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid #ffd9df;
  border-radius: 8px;
  background: #fff7f8;
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

.status-chip.soft {
  background: var(--bg-soft);
  color: var(--text-secondary);
}

.detail-cover {
  aspect-ratio: 1 / 1;
}

.order-line p {
  margin: 6px 0 0;
}

@media (max-width: 860px) {
  .summary-grid,
  .detail-two-col {
    grid-template-columns: 1fr;
  }

  .detail-head,
  .child-order-row {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-actions,
  .child-status {
    justify-content: flex-start;
  }
}
</style>
