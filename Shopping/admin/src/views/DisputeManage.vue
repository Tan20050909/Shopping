<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">DISPUTE ARBITRATION</span>
          <h1 class="admin-page-title">纠纷仲裁</h1>
          <p class="admin-page-desc">处理用户与商家之间的纠纷，进行判责和裁决</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel admin-panel-wide">
        <div class="admin-filter-bar">
          <el-select v-model="filterDisputeStatus" placeholder="纠纷状态" clearable class="admin-status-select" @change="loadData">
            <el-option label="待平台处理" :value="0" /><el-option label="举证中" :value="1" />
            <el-option label="平台处理中" :value="2" /><el-option label="已裁决" :value="3" />
            <el-option label="已关闭" :value="4" />
          </el-select>
          <el-select v-model="filterApplyType" placeholder="发起方" clearable class="admin-status-select" @change="loadData">
            <el-option label="用户" :value="1" /><el-option label="商家" :value="2" /><el-option label="平台" :value="3" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
        </div>
        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table admin-table-row-tall" style="width:100%">
            <template #empty><el-empty description="暂无纠纷数据" /></template>
            <el-table-column prop="disputeNo" label="纠纷单号" width="160" show-overflow-tooltip />
            <el-table-column prop="orderId" label="订单ID" width="100" show-overflow-tooltip />
            <el-table-column label="发起方" width="80">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="applyTypeClass(row.applyType)">{{ applyTypeMap[row.applyType] || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="纠纷状态" width="100">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="disputeStatusClass(row.disputeStatus)">{{ disputeStatusMap[row.disputeStatus] }}</span>
              </template>
            </el-table-column>
            <el-table-column label="判责结果" width="90">
              <template #default="{ row }">
                <span v-if="row.judgeResult" class="admin-td-nowrap">{{ judgeResultMap[row.judgeResult] }}</span>
                <span v-else style="color:var(--text-muted)">-</span>
              </template>
            </el-table-column>
            <el-table-column label="裁决金额" width="100">
              <template #default="{ row }">
                <span v-if="row.finalAmount" class="admin-table-price">¥{{ row.finalAmount }}</span>
                <span v-else style="color:var(--text-muted)">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="150" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="handleDetail(row)">详情</button>
                  <button v-if="row.disputeStatus !== 3 && row.disputeStatus !== 4" class="admin-action-btn" style="color:#1d4ed8;border-color:#bfdbfe" @click="handleJudge(row)">处理</button>
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

    <el-dialog v-model="judgeVisible" title="处理纠纷" width="560px">
      <el-form label-width="90px">
        <el-form-item label="纠纷单号"><span>{{ judgeForm.disputeNo }}</span></el-form-item>
        <el-form-item label="纠纷原因"><span>{{ judgeForm.disputeReason }}</span></el-form-item>
        <el-form-item label="纠纷说明" v-if="judgeForm.disputeDesc">
          <div style="background:#F7F7F7;border-radius:8px;padding:12px;font-size:13px;line-height:1.6;color:#666;white-space:pre-wrap">{{ judgeForm.disputeDesc }}</div>
        </el-form-item>
        <el-form-item label="判责结果">
          <el-select v-model="judgeForm.judgeResult" placeholder="选择判责结果" style="width:100%">
            <el-option v-for="(label, val) in judgeResultMap" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="裁决金额">
          <el-input v-model="judgeForm.finalAmount" :placeholder="judgeForm.judgeResult === 3 ? '部分支持必须填写裁决金额' : '请输入裁决金额（可选）'">
            <template #prepend>¥</template>
          </el-input>
          <div v-if="judgeForm.judgeResult === 3" style="color:#E6A23C;font-size:12px;margin-top:4px">部分支持必须填写裁决金额，且不能超过售后申请金额</div>
        </el-form-item>
        <el-form-item label="处理意见"><el-input v-model="judgeForm.platformOpinion" type="textarea" :rows="3" placeholder="请输入平台处理意见" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="judgeVisible=false">取消</el-button>
        <el-button type="primary" @click="submitJudge">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="纠纷详情" width="640px">
      <div v-if="detailRow" style="line-height:1.8">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="纠纷ID">{{ detailRow.disputeId }}</el-descriptions-item>
          <el-descriptions-item label="纠纷单号">{{ detailRow.disputeNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单ID">{{ detailRow.orderId }}</el-descriptions-item>
          <el-descriptions-item label="售后单ID">{{ detailRow.afterSaleId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailRow.userId }}</el-descriptions-item>
          <el-descriptions-item label="商户ID">{{ detailRow.merchantId }}</el-descriptions-item>
          <el-descriptions-item label="发起方">
            <span class="admin-status-tag" :class="applyTypeClass(detailRow.applyType)">{{ applyTypeMap[detailRow.applyType] || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="纠纷状态">
            <span class="admin-status-tag" :class="disputeStatusClass(detailRow.disputeStatus)">{{ disputeStatusMap[detailRow.disputeStatus] }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="判责结果">{{ judgeResultMap[detailRow.judgeResult] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="裁决金额"><span v-if="detailRow.finalAmount" class="admin-table-price">¥{{ detailRow.finalAmount }}</span><span v-else>-</span></el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailRow.createTime }}</el-descriptions-item>
          <el-descriptions-item label="裁决时间">{{ detailRow.judgeTime || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <div style="font-size:14px;font-weight:600;margin-bottom:8px">纠纷原因</div>
          <div style="background:#F7F7F7;border-radius:8px;padding:12px;font-size:13px;color:#666">{{ detailRow.disputeReason || '-' }}</div>
        </div>
        <div v-if="detailRow.disputeDesc" style="margin-top:12px">
          <div style="font-size:14px;font-weight:600;margin-bottom:8px">纠纷说明</div>
          <div style="background:#F7F7F7;border-radius:8px;padding:12px;font-size:13px;color:#666;white-space:pre-wrap">{{ detailRow.disputeDesc }}</div>
        </div>
        <div v-if="detailRow.platformOpinion" style="margin-top:12px">
          <div style="font-size:14px;font-weight:600;margin-bottom:8px">平台处理意见</div>
          <div style="background:#F5FFF5;border:1px solid #E8FFE8;border-radius:8px;padding:12px;font-size:13px;color:#666;white-space:pre-wrap">{{ detailRow.platformOpinion }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDisputeList, getDisputeDetail, judgeDispute } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const filterDisputeStatus = ref(null)
const filterApplyType = ref(null)

const judgeVisible = ref(false)
const judgeForm = reactive({ disputeId: null, disputeNo: '', disputeReason: '', disputeDesc: '', judgeResult: null, finalAmount: '', platformOpinion: '' })
const detailVisible = ref(false)
const detailRow = ref(null)

const applyTypeMap = { 1: '用户', 2: '商家', 3: '平台' }
const disputeStatusMap = { 0: '待平台处理', 1: '举证中', 2: '平台处理中', 3: '已裁决', 4: '已关闭' }
const judgeResultMap = { 1: '支持用户', 2: '支持商家', 3: '部分支持', 4: '协商关闭' }

const applyTypeClass = (t) => {
  const m = { 1: 'tag-primary', 2: 'tag-warning', 3: 'tag-info' }
  return m[t] || 'tag-info'
}

const disputeStatusClass = (s) => {
  const m = { 0: 'tag-warning', 1: 'tag-primary', 2: 'tag-primary', 3: 'tag-success', 4: 'tag-info' }
  return m[s] || 'tag-info'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (filterDisputeStatus.value != null) params.disputeStatus = filterDisputeStatus.value
    if (filterApplyType.value != null) params.applyType = filterApplyType.value
    const res = await getDisputeList(params)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const handleDetail = async (row) => {
  try {
    const res = await getDisputeDetail(row.disputeId)
    detailRow.value = res.data || row
    detailVisible.value = true
  } catch { detailRow.value = row; detailVisible.value = true }
}

const handleJudge = (row) => {
  judgeForm.disputeId = row.disputeId
  judgeForm.disputeNo = row.disputeNo || ''
  judgeForm.disputeReason = row.disputeReason || ''
  judgeForm.disputeDesc = row.disputeDesc || ''
  judgeForm.judgeResult = null
  judgeForm.finalAmount = ''
  judgeForm.platformOpinion = ''
  judgeVisible.value = true
}

const submitJudge = async () => {
  if (!judgeForm.judgeResult) { ElMessage.warning('请选择判责结果'); return }
  if (judgeForm.judgeResult === 3 && (!judgeForm.finalAmount || Number(judgeForm.finalAmount) <= 0)) {
    ElMessage.warning('部分支持必须填写裁决金额'); return
  }
  const payload = {
    disputeId: judgeForm.disputeId,
    judgeResult: judgeForm.judgeResult,
    finalAmount: judgeForm.finalAmount === '' || judgeForm.finalAmount == null ? null : Number(judgeForm.finalAmount),
    platformOpinion: judgeForm.platformOpinion || null
  }
  await judgeDispute(payload)
  ElMessage.success('处理成功')
  judgeVisible.value = false
  loadData()
}

onMounted(loadData)
</script>
<style scoped>
.admin-order-page { color: var(--text-main); }
</style>
