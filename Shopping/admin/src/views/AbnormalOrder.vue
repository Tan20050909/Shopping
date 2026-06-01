<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">异常订单</h2>
        <p class="page-subtitle">监控和处理异常交易订单</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-select v-model="statusFilter" placeholder="处理状态" clearable style="width:120px" @change="loadData">
          <el-option label="待处理" :value="0" />
          <el-option label="确认异常" :value="1" />
          <el-option label="误判解除" :value="2" />
        </el-select>
        <el-select v-model="typeFilter" placeholder="异常类型" clearable style="width:120px" @change="loadData">
          <el-option label="刷单" :value="1" />
          <el-option label="恶意退款" :value="2" />
          <el-option label="盗卡支付" :value="3" />
          <el-option label="虚假下单" :value="4" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无异常订单数据" /></template>
        <el-table-column prop="abnormalId" label="ID" width="70" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column label="异常类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.abnormalType]" size="small" effect="light" style="border-radius:999px">{{ typeTextMap[row.abnormalType] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="abnormalDesc" label="异常描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.handleStatus]" size="small" effect="light" style="border-radius:999px">{{ statusTextMap[row.handleStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="90">
          <template #default="{ row }">{{ row.handleAdminId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发现时间" width="170" />
        <el-table-column prop="handleTime" label="处理时间" width="170">
          <template #default="{ row }">{{ row.handleTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.handleStatus===0">
              <el-button type="success" link size="small" @click="handleItem(row, 1)">确认异常</el-button>
              <el-button type="warning" link size="small" @click="handleItem(row, 2)">误判解除</el-button>
            </template>
            <span v-else style="color:var(--text-muted);font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="异常订单详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="异常ID">{{ detail.abnormalId }}</el-descriptions-item>
        <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">{{ typeTextMap[detail.abnormalType] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="statusTagMap[detail.handleStatus]" size="small" style="border-radius:999px">{{ statusTextMap[detail.handleStatus] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="异常描述" :span="2">{{ detail.abnormalDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理人ID">{{ detail.handleAdminId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发现时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="处理时间" :span="2">{{ detail.handleTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAbnormalOrderList, getAbnormalDetail, handleAbnormal } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const statusFilter = ref(null), typeFilter = ref(null)
const detailVisible = ref(false), detail = ref({})

const typeTextMap = { 1: '刷单', 2: '恶意退款', 3: '盗卡支付', 4: '虚假下单' }
const typeTagMap = { 1: 'danger', 2: 'warning', 3: 'danger', 4: 'warning' }
const statusTextMap = { 0: '待处理', 1: '确认异常', 2: '误判解除' }
const statusTagMap = { 0: 'warning', 1: 'danger', 2: 'success' }

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (statusFilter.value != null) params.handleStatus = statusFilter.value
    if (typeFilter.value != null) params.abnormalType = typeFilter.value
    const res = await getAbnormalOrderList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const showDetail = async (row) => {
  try {
    const res = await getAbnormalDetail(row.abnormalId)
    detail.value = res.data || row
    detailVisible.value = true
  } catch (e) {
    detail.value = row
    detailVisible.value = true
  }
}

const handleItem = async (row, handleStatus) => {
  const actionText = handleStatus === 1 ? '确认异常' : '误判解除'
  try {
    await ElMessageBox.confirm(`确认${actionText}该异常订单？`, '处理确认', { type: 'warning' })
  } catch { return }

  let handleRemark = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入处理备注（可选）', actionText, {
      inputPlaceholder: '请输入处理备注',
      inputType: 'textarea'
    })
    handleRemark = value || ''
  } catch { return }

  await handleAbnormal({ abnormalId: row.abnormalId, handleStatus, handleRemark })
  ElMessage.success('处理成功')
  loadData()
}

onMounted(loadData)
</script>
