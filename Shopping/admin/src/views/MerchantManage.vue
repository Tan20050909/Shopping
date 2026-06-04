<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">MERCHANT MANAGEMENT</span>
          <h1 class="admin-page-title">商户管理</h1>
          <p class="admin-page-desc">审核与冻结平台入驻商户，确保合规运营</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel">
        <div class="admin-filter-bar">
          <el-input v-model="keyword" placeholder="搜索商户名称" clearable class="admin-search-input" @clear="loadData" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="auditFilter" placeholder="审核状态" clearable class="admin-status-select" @change="loadData">
            <el-option label="待审核" :value="0" /><el-option label="已通过" :value="1" /><el-option label="已拒绝" :value="2" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
        </div>
        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table" style="width:100%">
            <template #empty><el-empty description="暂无商户数据" /></template>
            <el-table-column prop="merchantId" label="ID" width="70" />
            <el-table-column label="商户" min-width="200">
              <template #default="{ row }">
                <div style="display:flex;align-items:center;gap:12px">
                  <el-avatar :size="40" :src="row.shopLogo" style="border-radius:8px;flex-shrink:0">{{ (row.merchantName||'')[0] }}</el-avatar>
                  <div>
                    <div style="font-weight:600">{{ row.merchantName }}</div>
                    <div style="font-size:12px;color:var(--text-muted)">{{ row.phone || '-' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="审核" width="100">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="row.auditStatus===1?'tag-success':row.auditStatus===0?'tag-warning':'tag-danger'">{{ ['待审核','已通过','已拒绝'][row.auditStatus] }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="row.status===1?'tag-success':row.status===3?'tag-danger':'tag-info'">{{ row.status===1?'营业':row.status===3?'平台冻结':'停业' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="registerTime" label="入驻时间" width="170" />
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="showDetail(row)">详情</button>
                  <button v-if="row.auditStatus===0" class="admin-action-btn" style="color:#047857;border-color:#a7f3d0" @click="handleAudit(row,1)">通过</button>
                  <button v-if="row.auditStatus===0" class="admin-action-btn admin-action-danger" @click="handleReject(row)">拒绝</button>
                  <button v-if="row.status===1" class="admin-action-btn admin-action-warning" @click="handleFreeze(row)">冻结</button>
                  <button v-if="row.status===3" class="admin-action-btn" style="color:#1d4ed8;border-color:#bfdbfe" @click="handleUnfreeze(row)">解冻</button>
                  <button v-if="row.status===0&&row.auditStatus===1" class="admin-action-btn" style="color:#047857;border-color:#a7f3d0" @click="toggleStatus(row,1)">恢复</button>
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
    <el-dialog v-model="detailVisible" title="商户详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商户名">{{ detail.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="法人">{{ detail.legalPerson }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <span class="admin-status-tag" :class="detail.auditStatus===1?'tag-success':detail.auditStatus===0?'tag-warning':'tag-danger'">{{ ['待审核','已通过','已拒绝'][detail.auditStatus] || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="营业状态">
          <span class="admin-status-tag" :class="detail.status===1?'tag-success':detail.status===3?'tag-danger':'tag-info'">{{ detail.status===1?'营业':detail.status===3?'平台冻结':'停业' }}</span>
        </el-descriptions-item>
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
<style scoped>
.admin-order-page { color: var(--text-main); }
</style>
