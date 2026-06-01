<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">异常订单</h2>
        <p class="page-subtitle">监控和处理异常交易订单</p>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="abnormalId" label="ID" width="70" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }"><el-tag size="small" effect="light" style="border-radius:999px">{{ row.abnormalType || '异常' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.handleStatus===0?'warning':'success'" size="small" effect="light" style="border-radius:999px">{{ row.handleStatus===0?'待处理':'已处理' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="发现时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handleStatus===0" type="primary" link size="small" @click="handleItem(row)">处理</el-button>
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
import { ElMessage } from 'element-plus'
import { getAbnormalOrderList, handleAbnormal } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAbnormalOrderList({ current: current.value, size: size.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const handleItem = async (row) => {
  await handleAbnormal({ abnormalId: row.abnormalId, handleStatus: 1 })
  ElMessage.success('已标记处理')
  loadData()
}

onMounted(loadData)
</script>
