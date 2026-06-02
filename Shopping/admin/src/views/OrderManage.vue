<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">订单管理</h2>
        <p class="page-subtitle">查看和处理平台订单</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索订单号" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="订单状态" clearable style="width:120px" @change="loadData">
          <el-option v-for="(s,i) in ORDER_STATUS_TEXT" :key="i" :label="s" :value="i" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无订单数据" /></template>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="用户" width="100">
          <template #default="{ row }">{{ row.userId || '-' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }"><span style="color:#E60012;font-weight:600">¥{{ row.payAmount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.orderStatus]" size="small" effect="light" style="border-radius:999px">{{ ORDER_STATUS_TEXT[row.orderStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus===1 && hasPermission('ORDER_REMIND_SHIP')" type="success" link size="small" @click="remindShip(row)">提醒商家发货</el-button>
            <el-button v-if="row.orderStatus===0 && hasPermission('ORDER_INTERVENE')" type="danger" link size="small" @click="cancelOrder(row)">取消</el-button>
            <el-button v-if="[1,2].includes(row.orderStatus) && hasPermission('ORDER_REFUND')" type="warning" link size="small" @click="refundOrder(row)">退款</el-button>

          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusTypes[detail.orderStatus]" size="small" style="border-radius:999px">{{ ORDER_STATUS_TEXT[detail.orderStatus] }}</el-tag></el-descriptions-item>
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
              <el-image v-if="row.goodsPic" :src="row.goodsPic" style="width:50px;height:50px;border-radius:4px;object-fit:cover" fit="cover" :preview-src-list="[row.goodsPic]" />
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


const ORDER_STATUS_TEXT = ['待付款', '待发货', '待收货', '已完成', '已取消', '售后中']

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), statusFilter = ref(null)
const detailVisible = ref(false), detail = ref({})
const statusTypes = { 0: 'warning', 1: '', 2: '', 3: 'success', 4: 'info', 5: 'danger' }

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
