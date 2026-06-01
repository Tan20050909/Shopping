<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">活动管理</h2>
        <p class="page-subtitle">创建和管理营销活动</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="filters.keyword" placeholder="搜索活动名称" clearable style="width:200px" @clear="loadList" @keyup.enter="loadList" />
        <el-select v-model="filters.activityType" placeholder="活动类型" clearable style="width:120px" @change="loadList">
          <el-option label="限时折扣" :value="1" /><el-option label="满减活动" :value="2" /><el-option label="拼团活动" :value="3" /><el-option label="秒杀活动" :value="4" /><el-option label="新人专享" :value="5" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width:110px" @change="loadList">
          <el-option label="草稿" :value="0" /><el-option label="进行中" :value="1" /><el-option label="已结束" :value="2" /><el-option label="已取消" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadList">搜索</el-button>
        <el-button type="primary" @click="showDialog()">创建活动</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="list" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="activityId" label="ID" width="70" />
        <el-table-column prop="activityName" label="活动名称" min-width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{row}"><el-tag size="small" effect="light" style="border-radius:999px">{{ typeMap[row.activityType] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="折扣/优惠" width="120">
          <template #default="{row}">
            <span style="color:#E60012;font-weight:600" v-if="row.activityType===1">{{ row.discountValue }}折</span>
            <span style="color:#E60012;font-weight:600" v-else-if="row.activityType===2">满{{ row.minAmount }}减{{ row.discountValue }}</span>
            <span style="color:#E60012;font-weight:600" v-else-if="row.activityType===3">{{ row.groupRequired }}人团 ¥{{ row.groupPrice }}</span>
            <span style="color:#E60012;font-weight:600" v-else-if="row.activityType===4">秒杀价 ¥{{ row.discountValue }}</span>
            <span style="color:#E60012;font-weight:600" v-else>专享价 ¥{{ row.discountValue }}</span>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="200">
          <template #default="{row}">{{ row.startTime?.substring(0,16) }} ~ {{ row.endTime?.substring(0,16) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="statusType[row.status]" size="small" style="border-radius:999px">{{ statusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="showDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="padding:16px;text-align:right">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="filters.size" v-model:current-page="filters.current" @current-change="loadList" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑活动' : '创建活动'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动名称"><el-input v-model="form.activityName" /></el-form-item>
        <el-form-item label="活动类型">
          <el-select v-model="form.activityType" style="width:100%">
            <el-option label="限时折扣" :value="1" /><el-option label="满减活动" :value="2" /><el-option label="拼团活动" :value="3" /><el-option label="秒杀活动" :value="4" /><el-option label="新人专享" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动描述"><el-input v-model="form.activityDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="适用范围">
          <el-select v-model="form.scopeType" style="width:100%">
            <el-option label="全场" :value="1" /><el-option label="指定分类" :value="2" /><el-option label="指定商品" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.activityType===1" label="折扣率"><el-input-number v-model="form.discountValue" :min="0.1" :max="9.9" :step="0.1" :precision="1" /></el-form-item>
        <el-form-item v-if="form.activityType===2" label="满减金额">
          <el-row :gutter="8"><el-col :span="12">满<el-input-number v-model="form.minAmount" :min="0" style="width:100%" /></el-col><el-col :span="12">减<el-input-number v-model="form.discountValue" :min="0" style="width:100%" /></el-col></el-row>
        </el-form-item>
        <el-form-item v-if="form.activityType===3" label="拼团配置">
          <el-row :gutter="8">
            <el-col :span="8">成团人数<el-input-number v-model="form.groupRequired" :min="2" style="width:100%" /></el-col>
            <el-col :span="8">拼团价<el-input-number v-model="form.groupPrice" :min="0" :precision="2" style="width:100%" /></el-col>
            <el-col :span="8">时效(h)<el-input-number v-model="form.groupTimeoutHours" :min="1" style="width:100%" /></el-col>
          </el-row>
        </el-form-item>
        <el-form-item v-if="form.activityType===4" label="秒杀库存"><el-input-number v-model="form.seckillStock" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="限购数量"><el-input-number v-model="form.perLimit" :min="0" /></el-form-item>
        <el-form-item label="活动时间">
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss"
            :shortcuts="dateShortcuts" :default-time="[new Date(0,0,0,0,0,0), new Date(0,0,0,23,59,59)]" unlink-panels popper-class="range-date-popper"
          />
        </el-form-item>
        <el-form-item label="活动预算"><el-input-number v-model="form.budgetAmount" :min="0" :precision="2" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getActivityList, addActivity, updateActivity, deleteActivity } from '../api/common'
import { ElMessage, ElMessageBox } from 'element-plus'

const typeMap = { 1:'限时折扣', 2:'满减活动', 3:'拼团活动', 4:'秒杀活动', 5:'新人专享' }
const statusMap = { 0:'草稿', 1:'进行中', 2:'已结束', 3:'已取消' }
const statusType = { 0:'info', 1:'success', 2:'', 3:'warning' }

const loading = ref(false)
const filters = reactive({ current:1, size:10, keyword:'', activityType:null, status:null })

const dateShortcuts = [
  { text: '3天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 3*24*3600*1000)] }},
  { text: '7天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 7*24*3600*1000)] }},
  { text: '15天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 15*24*3600*1000)] }},
  { text: '30天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 30*24*3600*1000)] }},
]
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref(null)
const timeRange = ref([])
const form = reactive({ activityName:'', activityType:1, activityDesc:'', scopeType:1, discountValue:null, minAmount:null, maxDiscount:null, groupRequired:2, groupPrice:null, groupTimeoutHours:24, seckillStock:null, perLimit:1, startTime:null, endTime:null, budgetAmount:null })

const loadList = async () => {
  loading.value = true
  try {
    const res = await getActivityList(filters)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const showDialog = (row) => {
  editingId.value = row?.activityId || null
  if (row) {
    Object.keys(form).forEach(k => form[k] = row[k])
    timeRange.value = [row.startTime, row.endTime]
  } else {
    Object.keys(form).forEach(k => form[k] = null)
    form.activityType = 1; form.scopeType = 1; form.perLimit = 1; form.groupRequired = 2; form.groupTimeoutHours = 24
    timeRange.value = []
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (timeRange.value?.length === 2) {
    form.startTime = timeRange.value[0]
    form.endTime = timeRange.value[1]
  }
  if (editingId.value) {
    form.activityId = editingId.value
    await updateActivity(form)
  } else {
    form.status = 0
    await addActivity(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadList()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该活动?', '提示', { type:'warning' })
  await deleteActivity(row.activityId)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style>
.range-date-popper { z-index: 3000 !important; }
</style>
