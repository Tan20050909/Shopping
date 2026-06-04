<template>
  <div class="admin-order-page">
    <!-- Hero Banner -->
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">ORDER SUPERVISION</span>
          <h1 class="admin-page-title">订单监督</h1>
          <p class="admin-page-desc">集中监督平台订单、履约状态与售后风险，及时跟进异常订单</p>
        </div>
      </div>
    </section>

    <!-- Content -->
    <div class="admin-page-container">
      <div class="admin-panel">
        <!-- 筛选栏 -->
        <div class="admin-filter-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索订单号"
            clearable
            class="admin-search-input"
            @clear="loadData"
            @keyup.enter="loadData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="statusFilter"
            placeholder="订单状态"
            clearable
            class="admin-status-select"
            @change="loadData"
          >
            <el-option v-for="(s,i) in ORDER_STATUS_TEXT" :key="i" :label="s" :value="i" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
        </div>

        <!-- 表格 -->
        <div class="admin-table-wrap">
          <el-table
            :data="tableData"
            stripe
            v-loading="loading"
            class="admin-table"
            style="width:100%"
          >
            <template #empty><el-empty description="暂无订单数据" /></template>
            <el-table-column prop="orderNo" label="订单号" width="200" />
            <el-table-column label="用户" width="120">
              <template #default="{ row }">{{ row.userId || '-' }}</template>
            </el-table-column>
            <el-table-column label="金额" width="130">
              <template #default="{ row }">
                <span class="admin-table-price">¥{{ row.payAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="'status-' + row.orderStatus">{{ ORDER_STATUS_TEXT[row.orderStatus] }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="190" />
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="showDetail(row)">详情</button>
                  <button
                    v-if="row.orderStatus===1 && hasPermission('ORDER_REMIND_SHIP')"
                    class="admin-action-btn"
                    @click="remindShip(row)"
                  >提醒发货</button>
                  <button
                    v-if="row.orderStatus===0 && hasPermission('ORDER_INTERVENE')"
                    class="admin-action-btn admin-action-danger"
                    @click="cancelOrder(row)"
                  >取消</button>
                  <button
                    v-if="[1,2].includes(row.orderStatus) && hasPermission('ORDER_REFUND')"
                    class="admin-action-btn admin-action-warning"
                    @click="refundOrder(row)"
                  >退款</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 分页 -->
        <div class="admin-pagination-bar">
          <el-pagination
            v-model:current-page="current"
            v-model:page-size="size"
            :page-sizes="[10,20,50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="admin-status-tag" :class="'status-' + detail.orderStatus">{{ ORDER_STATUS_TEXT[detail.orderStatus] }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="商品金额">¥{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="实付金额"><span style="color:#E60012;font-weight:600">¥{{ detail.payAmount }}</span></el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.receiveAddr || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail.items && detail.items.length" style="margin-top:16px">
        <h4 style="margin-bottom:12px;font-size:14px;font-weight:600">商品明细</h4>
        <el-table :data="detail.items" size="small" border>
          <el-table-column label="商品图片" width="80">
            <template #default="{ row }">
              <el-image
                v-if="row.goodsPic"
                :src="resolveImg(row.goodsPic)"
                style="width:50px;height:50px;border-radius:4px;object-fit:cover"
                fit="cover"
                :preview-src-list="[resolveImg(row.goodsPic)]"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="goodsName" label="商品名称" />
          <el-table-column prop="skuName" label="规格" width="120" />
          <el-table-column prop="price" label="单价" width="100" />
          <el-table-column prop="num" label="数量" width="80" />
          <el-table-column prop="totalPrice" label="小计" width="100" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, getOrderDetail, remindMerchantShip, refundOrder as refundOrderApi, cancelOrder as cancelOrderApi } from '../api/order'
import { hasPermission } from '../utils/permission'

const ORDER_STATUS_TEXT = ['待支付', '待发货', '待收货', '已完成', '已取消', '售后中']

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), statusFilter = ref(null)
const detailVisible = ref(false), detail = ref({})

const resolveImg = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return ''
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/uploads/')) return 'http://localhost:8081' + v
  if (v.startsWith('uploads/')) return 'http://localhost:8081/' + v
  return v
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value != null) params.orderStatus = statusFilter.value
    const res = await getOrderList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载订单数据失败，请重试')
  } finally { loading.value = false }
}

const showDetail = async (row) => {
  try {
    const res = await getOrderDetail(row.orderId)
    detail.value = res.data?.order ? { ...res.data.order, items: res.data.items || [] } : res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

const remindShip = async (row) => {
  try {
    await ElMessageBox.confirm('确认向该订单商家发送站内发货提醒？', '提醒确认', { type: 'warning' })
    await remindMerchantShip(row.orderId, { remark: '平台已提醒商家尽快发货' })
    ElMessage.success('已发送给商家，并记录到订单备注')
    loadData()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.warn(e)
  }
}

const cancelOrder = async (row) => {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '取消确认', { type: 'warning' })
    await cancelOrderApi(row.orderId)
    ElMessage.success('已取消')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('取消订单失败')
  }
}

const refundOrder = async (row) => {
  try {
    await ElMessageBox.confirm('确认退款？', '退款确认', { type: 'warning' })
    await refundOrderApi(row.orderId, { refundAmount: row.payAmount || 0, refundReason: '平台后台退款' })
    ElMessage.success('已退款')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('退款失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.admin-order-page {
  color: var(--text-main);
}
</style>
