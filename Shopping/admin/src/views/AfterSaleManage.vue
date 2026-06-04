<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">AFTER-SALE SUPERVISION</span>
          <h1 class="admin-page-title">售后监管</h1>
          <p class="admin-page-desc">监督退款、退货等售后请求的处理进度，及时介入争议</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel admin-panel-wide">
        <div class="admin-filter-bar">
          <el-input v-model="keyword" placeholder="搜索售后单号" clearable class="admin-search-input" @clear="loadData" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="statusFilter" placeholder="处理状态" clearable class="admin-status-select" @change="loadData">
            <el-option v-for="(s, k) in statusTextMap" :key="k" :label="s" :value="Number(k)" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
        </div>
        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table admin-table-row-tall" style="width:100%">
            <template #empty><el-empty description="暂无售后数据" /></template>
            <el-table-column prop="afterSaleNo" label="售后单号" width="220" show-overflow-tooltip />
            <el-table-column prop="orderId" label="订单ID" width="90" show-overflow-tooltip />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="row.afterSaleType===1?'tag-info':'tag-warning'">{{ afterSaleTypeMap[row.afterSaleType] || '售后' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="90">
              <template #default="{ row }"><span class="admin-table-price">¥{{ row.applyAmount }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="statusClass(row.handleStatus)">{{ statusTextMap[row.handleStatus] }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="applyTime" label="申请时间" width="150" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="showDetail(row)">详情</button>
                  <template v-if="row.handleStatus===0">
                    <button class="admin-action-btn" style="color:#047857;border-color:#a7f3d0" @click="handleAfterSale(row,1)">商家同意</button>
                    <button class="admin-action-btn admin-action-danger" @click="handleAfterSale(row,2)">商家拒绝</button>
                  </template>
                  <button v-if="row.handleStatus===0" class="admin-action-btn admin-action-warning" @click="handleAfterSale(row,3)">平台介入</button>
                  <button v-if="row.handleStatus===1" class="admin-action-btn" style="color:#1d4ed8;border-color:#bfdbfe" @click="handleAfterSale(row,4)">确认退款</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="admin-pagination-bar">
          <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
        </div>
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

const statusClass = (s) => {
  const m = { 0: 'tag-warning', 1: 'tag-success', 2: 'tag-danger', 3: 'tag-info', 4: 'tag-success', 5: 'tag-info' }
  return m[s] || 'tag-info'
}

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
<style scoped>
.admin-order-page { color: var(--text-main); }
</style>
