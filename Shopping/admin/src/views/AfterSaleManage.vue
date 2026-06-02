<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">售后管理</h2>
        <p class="page-subtitle">处理退款退货申请</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索售后单号" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="处理状态" clearable style="width:120px" @change="loadData">
          <el-option v-for="(s, k) in statusTextMap" :key="k" :label="s" :value="Number(k)" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无售后数据" /></template>
        <el-table-column prop="afterSaleNo" label="售后单号" width="150" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column label="用户ID" width="90">
          <template #default="{ row }">{{ row.userId || '-' }}</template>
        </el-table-column>
        <el-table-column label="商家ID" width="90">
          <template #default="{ row }">{{ row.merchantId || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.afterSaleType===1?'':'warning'" size="small" effect="light" style="border-radius:999px">{{ afterSaleTypeMap[row.afterSaleType] || '售后' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }"><span style="color:#E60012;font-weight:600">¥{{ row.applyAmount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.handleStatus]" size="small" effect="light" style="border-radius:999px">{{ statusTextMap[row.handleStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyReason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="applyTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <template v-if="row.handleStatus===0">
              <el-button type="success" link size="small" @click="handleAfterSale(row,1)">商家同意</el-button>
              <el-button type="danger" link size="small" @click="handleAfterSale(row,2)">商家拒绝</el-button>
            </template>
            <el-button v-if="row.handleStatus===0" type="warning" link size="small" @click="handleAfterSale(row,3)">平台介入</el-button>
            <el-button v-if="row.handleStatus===1" type="primary" link size="small" @click="handleAfterSale(row,4)">确认退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAfterSaleList, handleAfterSale as handleAfterSaleApi } from '../api/common'

const router = useRouter()
const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), statusFilter = ref(null)

const afterSaleTypeMap = { 1: '仅退款', 2: '换货', 3: '补发', 4: '退货退款' }
const statusTextMap = { 0: '待商家处理', 1: '商家同意', 2: '商家拒绝', 3: '售后完成', 4: '退款成功', 5: '已撤销' }
const statusTagMap = { 0: 'warning', 1: 'success', 2: 'danger', 3: '', 4: 'success', 5: 'info' }

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value != null) params.handleStatus = statusFilter.value
    const res = await getAfterSaleList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const showDetail = (row) => {
  router.push(`/after-sale/${row.afterSaleId}`)
}

const handleAfterSale = async (row, status) => {
  const actionText = { 1: '同意', 2: '拒绝', 3: '平台介入处理', 4: '确认退款成功' }
  try {
    await ElMessageBox.confirm(`确认${actionText[status]}该售后申请？`, '提示', { type: 'warning' })
  } catch { return }

  let platformRemark = ''
  if (status === 3) {
    try {
      const { value } = await ElMessageBox.prompt('请输入平台介入意见', '平台介入', {
        inputPlaceholder: '请输入处理意见',
        inputType: 'textarea'
      })
      platformRemark = value || ''
    } catch { return }
  }

  await handleAfterSaleApi({ afterSaleId: row.afterSaleId, handleStatus: status, platformRemark })
  ElMessage.success('操作成功')
  loadData()
}

onMounted(loadData)
</script>
