<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <template #header>
        <div class="card-header">
          <span>店铺活动（优惠券）</span>
          <el-button type="primary" @click="showCreateDialog">创建优惠券</el-button>
        </div>
      </template>
      <el-table :data="couponList" size="small" table-layout="fixed" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="优惠" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.discountType) === 2">{{ toDiscount(row.discountRate) }}</span>
            <span v-else>¥ {{ toMoney(row.discountAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minAmount" label="门槛" width="90">
          <template #default="{ row }">¥ {{ toMoney(row.minAmount) }}</template>
        </el-table-column>
        <el-table-column prop="perLimit" label="限领" width="80">
          <template #default="{ row }">
            <span v-if="Number(row.limitEnabled) === 0">不限</span>
            <span v-else>{{ row.perLimit || 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateShort(row.startTime) }} ~ {{ formatDateShort(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="上线" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button size="small" @click="openEditAny(row)">修改</el-button>
            <el-button v-if="Number(row.status) !== 1" type="primary" size="small" @click="setCouponStatus(row, 1)">上线</el-button>
            <el-button v-else size="small" plain @click="setCouponStatus(row, 2)">下线</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-bottom: 20px">
      <template #header><span>平台券</span></template>
      <el-table :data="platformList" size="small" table-layout="fixed" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="优惠" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.discountType) === 2">{{ toDiscount(row.discountRate) }}</span>
            <span v-else>¥ {{ toMoney(row.discountAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minAmount" label="门槛" width="90">
          <template #default="{ row }">¥ {{ toMoney(row.minAmount) }}</template>
        </el-table-column>
        <el-table-column label="库存" width="180">
          <template #default="{ row }">
            <span>总 {{ row.totalCount || 0 }}</span>
            <span class="split">/</span>
            <span>剩 {{ row.surplusNum || 0 }}</span>
            <span class="split">/</span>
            <span>已领 {{ row.usedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateShort(row.startTime) }} ~ {{ formatDateShort(row.endTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="couponForm" label-width="100px">
        <el-form-item label="优惠券名称">
          <el-input v-model="couponForm.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠类型">
          <el-select v-model="couponForm.discountType" style="width: 220px">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="无门槛券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="Number(couponForm.discountType) !== 2" label="面额(元)">
          <el-input-number v-model="couponForm.discountAmount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item v-else label="折扣率">
          <el-input-number v-model="couponForm.discountRate" :min="0.0001" :max="0.9999" :step="0.01" :precision="4" />
          <div class="tip">{{ toDiscount(couponForm.discountRate) }}</div>
        </el-form-item>
        <el-form-item label="使用门槛(元)">
          <el-input-number v-model="couponForm.minAmount" :min="0" :precision="2" :disabled="Number(couponForm.discountType) === 3" />
        </el-form-item>
        <el-form-item label="是否限领">
          <el-switch v-model="limitEnabledSwitch" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="couponForm.perLimit" :min="1" :disabled="Number(couponForm.limitEnabled) === 0" />
        </el-form-item>
        <el-form-item label="是否可叠加">
          <el-switch v-model="canStackSwitch" />
        </el-form-item>
        <el-form-item label="发放总数">
          <el-input-number v-model="couponForm.totalCount" :min="1" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCoupon">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { couponApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const platformList = ref([])
const couponList = ref([])
const createDialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const dateRange = ref([])
const couponForm = reactive({
  merchantId: getMerchantId(),
  name: '',
  discountType: 1,
  discountAmount: 10,
  discountRate: 0.9,
  minAmount: 0,
  perLimit: 1,
  limitEnabled: 1,
  canStack: 0,
  totalCount: 100,
  surplusNum: 100,
  scopeType: 1,
  targetIds: [],
  startTime: '',
  endTime: ''
})

const canStackSwitch = computed({
  get: () => Number(couponForm.canStack) === 1,
  set: (v) => {
    couponForm.canStack = v ? 1 : 0
  }
})

const limitEnabledSwitch = computed({
  get: () => Number(couponForm.limitEnabled) !== 0,
  set: (v) => {
    couponForm.limitEnabled = v ? 1 : 0
    if (Number(couponForm.limitEnabled) === 0) {
      couponForm.perLimit = 0
    } else if (!couponForm.perLimit || Number(couponForm.perLimit) < 1) {
      couponForm.perLimit = 1
    }
  }
})

const dialogTitle = computed(() => (isEdit.value ? '修改优惠券' : '创建优惠券'))

const normalizeDateTime = (value) => {
  if (!value) return ''
  if (value instanceof Date) {
    const y = value.getFullYear()
    const m = String(value.getMonth() + 1).padStart(2, '0')
    const d = String(value.getDate()).padStart(2, '0')
    const hh = String(value.getHours()).padStart(2, '0')
    const mm = String(value.getMinutes()).padStart(2, '0')
    const ss = String(value.getSeconds()).padStart(2, '0')
    return `${y}-${m}-${d}T${hh}:${mm}:${ss}`
  }
  const s = String(value).trim()
  if (!s) return ''
  return s.includes(' ') ? s.replace(' ', 'T') : s
}

const loadPlatformList = async () => {
  try {
    const res = await couponApi.listPlatform()
    platformList.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    platformList.value = []
  }
}

const loadCouponList = async () => {
  const res = await couponApi.list(getMerchantId())
  couponList.value = res.data || []
}

const showCreateDialog = () => {
  couponForm.merchantId = getMerchantId()
  couponForm.name = ''
  couponForm.discountType = 1
  couponForm.discountAmount = 10
  couponForm.discountRate = 0.9
  couponForm.minAmount = 0
  couponForm.perLimit = 1
  couponForm.limitEnabled = 1
  couponForm.canStack = 0
  couponForm.totalCount = 100
  couponForm.surplusNum = 100
  couponForm.scopeType = 1
  couponForm.targetIds = []
  couponForm.startTime = ''
  couponForm.endTime = ''
  dateRange.value = []
  isEdit.value = false
  editingId.value = null
  createDialogVisible.value = true
}

const openEditAny = (row) => {
  if (Number(row?.status) === 1) {
    ElMessage.warning('该券已上线，请先下线后再修改')
    return
  }
  couponForm.merchantId = getMerchantId()
  couponForm.name = row.name || ''
  couponForm.discountType = Number(row.discountType) || 1
  couponForm.discountAmount = row.discountAmount ?? 0
  couponForm.discountRate = row.discountRate ?? 0.9
  couponForm.minAmount = row.minAmount ?? 0
  couponForm.limitEnabled = row.limitEnabled == null ? 1 : Number(row.limitEnabled)
  couponForm.perLimit = row.perLimit ?? 1
  couponForm.canStack = Number(row.canStack) === 1 ? 1 : 0
  couponForm.totalCount = row.totalCount ?? 0
  couponForm.surplusNum = row.surplusNum ?? row.totalCount ?? 0
  couponForm.scopeType = row.scopeType ?? 1
  couponForm.targetIds = Array.isArray(row.targetIds) ? row.targetIds : []
  dateRange.value = [normalizeDateTime(row.startTime), normalizeDateTime(row.endTime)]
  isEdit.value = true
  editingId.value = row.id
  createDialogVisible.value = true
}

const submitCoupon = async () => {
  try {
    couponForm.merchantId = getMerchantId()
    if (!dateRange.value || dateRange.value.length !== 2) {
      ElMessage.warning('请选择有效期')
      return
    }
    couponForm.startTime = normalizeDateTime(dateRange.value[0])
    couponForm.endTime = normalizeDateTime(dateRange.value[1])
    if (Number(couponForm.discountType) === 3) {
      couponForm.minAmount = 0
    }
    if (Number(couponForm.limitEnabled) === 0) {
      couponForm.perLimit = 0
    }
    if (!isEdit.value) {
      couponForm.surplusNum = couponForm.totalCount
      await couponApi.create(couponForm)
      ElMessage.success('创建成功')
    } else {
      await couponApi.update(editingId.value, couponForm)
      ElMessage.success('保存成功')
    }
    createDialogVisible.value = false
    isEdit.value = false
    editingId.value = null
    loadCouponList()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

const createCoupon = async () => {
  await submitCoupon()
}

const setCouponStatus = async (row, status) => {
  try {
    await couponApi.updateStatus(row.id, status)
    ElMessage.success('操作成功')
    loadCouponList()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

const formatDateTime = (v) => {
  const s = String(v || '').trim()
  if (!s) return '-'
  return s.replace('T', ' ')
}

const formatDateShort = (v) => {
  const s = formatDateTime(v)
  if (s === '-') return '-'
  return s.length >= 10 ? s.slice(0, 10) : s
}

const toMoney = (v) => {
  const n = Number(v ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const toDiscount = (rate) => {
  const n = Number(rate ?? 0)
  if (!Number.isFinite(n) || n <= 0) return '-'
  return `${(n * 10).toFixed(1)}折`
}

const statusText = (v) => {
  const s = Number(v)
  if (s === 1) return '已上线'
  if (s === 2) return '已下线'
  return '未上线'
}
const statusTagType = (v) => {
  const s = Number(v)
  if (s === 1) return 'success'
  if (s === 2) return 'info'
  return 'warning'
}

onMounted(() => {
  loadCouponList()
  loadPlatformList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tip {
  margin-left: 10px;
  color: #6b7280;
  font-size: 12px;
}

:deep(.el-table .cell) {
  padding-left: 6px;
  padding-right: 6px;
}

:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell) {
  padding-top: 8px;
  padding-bottom: 8px;
}
</style>
