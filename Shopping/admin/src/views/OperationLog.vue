<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">操作日志</h2>
        <p class="page-subtitle">查看系统操作记录，追踪关键业务动作</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;justify-content:flex-end">
        <el-input v-model="keyword" placeholder="搜索管理员/模块/描述" clearable style="width:220px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width:110px" @change="loadData">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%" @row-click="showDetail">
        <el-table-column prop="logId" label="ID" width="70" />
        <el-table-column label="管理员" width="120">
          <template #default="{ row }">{{ row.adminName || row.operatorId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operationModule" label="模块" width="120" />
        <el-table-column prop="operationContent" label="操作内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.operationResult===0?'danger':'success'" size="small" effect="light" style="border-radius:999px">{{ row.operationResult===0?'失败':'成功' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="operationIp" label="IP" width="120" />
        <el-table-column prop="operationTime" label="时间" width="170" />
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="680px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="日志ID">{{ detail.logId }}</el-descriptions-item>
        <el-descriptions-item label="管理员">{{ detail.adminName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.operationModule }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.operationResult===0?'失败':'成功' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.operationIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.operationTime }}</el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2"><pre style="white-space:pre-wrap;margin:0">{{ detail.operationContent || '-' }}</pre></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOperationLogList } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), statusFilter = ref(null)
const detailVisible = ref(false), detail = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value != null) params.status = statusFilter.value
    const res = await getOperationLogList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const showDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>
