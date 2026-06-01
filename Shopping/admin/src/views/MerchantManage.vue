<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">商户管理</h2>
        <p class="page-subtitle">管理平台入驻商户</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索商户名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="auditFilter" placeholder="审核状态" clearable style="width:120px" @change="loadData">
          <el-option label="待审核" :value="0" /><el-option label="已通过" :value="1" /><el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无商户数据" /></template>
        <el-table-column prop="merchantId" label="ID" width="70" />
        <el-table-column label="商户" min-width="200">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:12px">
              <el-avatar :size="40" :src="row.shopLogo" style="border-radius:8px;flex-shrink:0">{{ (row.merchantName||'')[0] }}</el-avatar>
              <div>
                <div style="font-weight:500">{{ row.merchantName }}</div>
                <div style="font-size:12px;color:var(--text-muted)">{{ row.phone || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="100">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus===1?'success':row.auditStatus===0?'warning':'danger'" size="small" effect="light" style="border-radius:999px">{{ ['待审核','已通过','已拒绝'][row.auditStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':row.status===3?'danger':'info'" size="small" effect="light" style="border-radius:999px">{{ row.status===1?'营业':row.status===3?'平台冻结':'停业' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="registerTime" label="入驻时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.auditStatus===0" type="success" link size="small" @click="handleAudit(row,1)">通过</el-button>
            <el-button v-if="row.auditStatus===0" type="danger" link size="small" @click="handleReject(row)">拒绝</el-button>
            <el-button v-if="row.status===1" type="warning" link size="small" @click="handleFreeze(row)">冻结</el-button>
            <el-button v-if="row.status===3" type="success" link size="small" @click="handleUnfreeze(row)">解冻</el-button>
            <el-button v-if="row.status===0&&row.auditStatus===1" type="success" link size="small" @click="toggleStatus(row,1)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="商户详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商户名">{{ detail.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="法人">{{ detail.legalPerson }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ ['待审核','已通过','已拒绝'][detail.auditStatus] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="营业状态">{{ detail.status===1?'营业':detail.status===3?'平台冻结':'停业' }}</el-descriptions-item>
        <el-descriptions-item label="拒绝/冻结原因" :span="2" v-if="detail.auditRemark">{{ detail.auditRemark }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail.shopLogo" style="margin-top:16px"><span style="color:var(--text-muted);font-size:13px">Logo：</span><el-image :src="detail.shopLogo" style="width:80px;height:80px;border-radius:8px" fit="cover" /></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantList, auditMerchant, updateMerchantStatus, getMerchantDetail, freezeMerchant, unfreezeMerchant } from '../api/merchant'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), auditFilter = ref(null)
const detailVisible = ref(false), detail = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (auditFilter.value != null) params.auditStatus = auditFilter.value
    const res = await getMerchantList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载商户数据失败，请重试')
  } finally { loading.value = false }
}

const showDetail = async (row) => {
  try {
    const res = await getMerchantDetail(row.merchantId)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取商户详情失败')
  }
}

const handleAudit = async (row, status) => {
  try {
    await ElMessageBox.confirm('确认通过审核？', '审核确认', { type: 'warning' })
    await auditMerchant(row.merchantId, 1, '')
    ElMessage.success('已通过')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('审核操作失败')
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
      inputType: 'textarea',
      inputPlaceholder: '请输入拒绝原因（必填）',
      inputValidator: (v) => !!v?.trim() || '请输入拒绝原因',
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await auditMerchant(row.merchantId, 2, value.trim())
    ElMessage.success('已拒绝')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('审核操作失败')
  }
}

const handleFreeze = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入冻结原因', '冻结商户', {
      inputType: 'textarea',
      inputPlaceholder: '请输入冻结原因（必填）',
      inputValidator: (v) => !!v?.trim() || '请输入冻结原因',
      confirmButtonText: '确认冻结',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await freezeMerchant(row.merchantId, value.trim())
    ElMessage.success('已冻结')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleUnfreeze = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要解冻商户"${row.merchantName}"吗？`, '解冻确认', { type: 'warning' })
    await unfreezeMerchant(row.merchantId)
    ElMessage.success('已解冻')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const toggleStatus = async (row, status) => {
  try {
    const action = status===1?'恢复营业':'停业'
    await ElMessageBox.confirm(`确定要${action}商户"${row.merchantName}"吗？`, '操作确认', { type: 'warning' })
    await updateMerchantStatus(row.merchantId, status)
    ElMessage.success(status===1?'已恢复':'已停业')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(loadData)
</script>
