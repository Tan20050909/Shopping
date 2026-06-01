<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">团购活动</h2>
        <p class="page-subtitle">管理团购活动与拼团配置</p>
      </div>
      <el-button type="primary" @click="openForm()">新增团购</el-button>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="groupId" label="ID" width="70" />
        <el-table-column label="活动商品" min-width="160">
          <template #default="{ row }">商品ID：{{ row.goodsId }}</template>
        </el-table-column>
        <el-table-column label="团购价" width="110">
          <template #default="{ row }"><span style="color:#E60012;font-weight:600">¥{{ row.groupPrice }}</span></template>
        </el-table-column>
        <el-table-column prop="groupRequired" label="成团人数" width="100" />
        <el-table-column label="活动时间" min-width="260">
          <template #default="{ row }">{{ row.startTime }} 至 {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.groupStatus===1?'success':'info'" size="small" effect="light" style="border-radius:999px">{{ row.groupStatus===1?'进行中':'已结束' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.groupStatus===1" type="warning" link size="small" @click="endGroupBuy(row)">结束</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>
    <el-dialog v-model="formVisible" :title="form.groupId?'编辑团购':'新增团购'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品ID"><el-input-number v-model="form.goodsId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="团购价"><el-input-number v-model="form.groupPrice" :min="0.01" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="成团人数"><el-input-number v-model="form.groupRequired" :min="2" style="width:100%" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.groupStatus" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGroupBuyList, addGroupBuy, updateGroupBuy, updateGroupBuyStatus, deleteGroupBuy } from '../api/groupBuy'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const formVisible = ref(false)
const form = reactive({ groupId: null, goodsId: 1, groupPrice: 0, groupRequired: 2, groupStatus: 1, startTime: '', endTime: '' })

const formatDate = (d) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const defaultTimes = () => {
  const start = new Date()
  const end = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  return { startTime: formatDate(start), endTime: formatDate(end) }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getGroupBuyList({ current: current.value, size: size.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const openForm = (row) => {
  if (row) Object.assign(form, row)
  else Object.assign(form, { groupId: null, goodsId: 1, groupPrice: 0, groupRequired: 2, groupStatus: 1, ...defaultTimes() })
  formVisible.value = true
}

const submitForm = async () => {
  if (!form.goodsId || !form.startTime || !form.endTime) { ElMessage.warning('请填写完整团购信息'); return }
  if (form.groupId) await updateGroupBuy(form)
  else await addGroupBuy(form)
  ElMessage.success('操作成功'); formVisible.value = false; loadData()
}

const endGroupBuy = async (row) => {
  await ElMessageBox.confirm('确认结束该团购？', '结束确认', { type: 'warning' })
  await updateGroupBuyStatus(row.groupId, 0); ElMessage.success('已结束'); loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除？', '删除确认', { type: 'warning' })
  await deleteGroupBuy(row.groupId); ElMessage.success('已删除'); loadData()
}

onMounted(loadData)
</script>
