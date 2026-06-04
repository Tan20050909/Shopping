<template>
  <div class="coupon-page">
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">COUPON CENTER</span>
          <h1 class="merchant-page-title">优惠券管理</h1>
          <p class="merchant-page-desc">创建和管理店铺优惠券，灵活配置发放方式、范围与有效期</p>
        </div>
      </div>
    </section>
    <el-card style="margin-bottom: 20px">
      <template #header>
        <div class="card-header">
          <span>优惠券管理</span>
          <el-button type="primary" @click="openCreate">创建优惠券</el-button>
        </div>
      </template>
      <el-table :data="list" size="small" table-layout="fixed" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="发放方式" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.grantType) === 2">自动弹出</span>
            <span v-else-if="Number(row.grantType) === 3">仅商家发放</span>
            <span v-else>用户自领</span>
          </template>
        </el-table-column>
        <el-table-column label="可用范围" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.couponType) === 1">全场通用</span>
            <span v-else>本店可用</span>
          </template>
        </el-table-column>
        <el-table-column label="优惠" width="110">
          <template #default="{ row }">
            <span v-if="Number(row.discountType) === 2">{{ toDiscount(row.discountRate) }}</span>
            <span v-else> {{ toMoney(row.discountAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="minAmount" label="门槛" width="90">
          <template #default="{ row }"> {{ toMoney(row.minAmount) }}</template>
        </el-table-column>
        <el-table-column prop="perLimit" label="限领" width="80">
          <template #default="{ row }">
            <span v-if="Number(row.limitEnabled) === 0">不限</span>
            <span v-else>{{ row.perLimit || 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="canStack" label="叠加" width="80">
          <template #default="{ row }">{{ Number(row.canStack) === 1 ? '可叠加' : '不可叠加' }}</template>
        </el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{ row }">
            <span>总 {{ row.totalCount || 0 }}</span>
            <span class="split">/</span>
            <span>剩 {{ row.surplusNum || 0 }}</span>
            <span class="split">/</span>
            <span>已领 {{ row.usedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="范围" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="Number(row.scopeType) === 4">指定商品({{ (row.targetIds || []).length }})</span>
            <span v-else>全场</span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateShort(row.startTime) }} ~ {{ formatDateShort(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核" width="90">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="上线" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" class="btn" @click="openEditAny(row)">修改</el-button>
            <el-button
              v-if="Number(row.status) !== 1"
              type="primary"
              size="small"
              class="btn"
              @click="setStatus(row, 1)"
            >
              上线
            </el-button>
            <el-button
              v-else
              plain
              size="small"
              class="btn"
              @click="setStatus(row, 2)"
            >
              下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="可用范围">
          <el-radio-group v-model="form.couponType">
            <el-radio :label="2">本店可用</el-radio>
            <el-radio :label="1">全场通用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发放方式">
          <el-radio-group v-model="form.grantType">
            <el-radio :label="1">用户自领</el-radio>
            <el-radio :label="2">自动弹出</el-radio>
            <el-radio :label="3">仅商家发放</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠券名称">
          <el-input v-model="form.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="优惠类型">
          <el-select v-model="form.discountType" style="width: 240px">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
            <el-option label="无门槛券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="Number(form.discountType) !== 2" label="面额(元)">
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item v-else label="折扣率">
          <el-input-number v-model="form.discountRate" :min="0.0001" :max="0.9999" :step="0.01" :precision="4" />
          <div class="tip">{{ toDiscount(form.discountRate) }}</div>
        </el-form-item>
        <el-form-item label="使用门槛(元)">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" :disabled="Number(form.discountType) === 3" />
        </el-form-item>
        <el-form-item label="是否限领">
          <el-switch v-model="limitEnabledSwitch" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perLimit" :min="1" :disabled="Number(form.limitEnabled) === 0" />
        </el-form-item>
        <el-form-item label="是否可叠加">
          <el-switch v-model="canStackSwitch" />
        </el-form-item>
        <el-form-item label="总发行量">
          <el-input-number v-model="form.totalCount" :min="1" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="剩余数量">
          <el-input-number v-model="form.surplusNum" :min="0" :max="form.totalCount" />
        </el-form-item>
        <el-form-item label="适用范围">
          <el-radio-group v-model="form.scopeType">
            <el-radio :label="1">全场</el-radio>
            <el-radio :label="4">指定商品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="Number(form.scopeType) === 4" label="适用商品">
          <el-select v-model="form.targetIds" multiple filterable collapse-tags collapse-tags-tooltip style="width: 520px">
            <el-option v-for="g in goodsList" :key="g.id" :label="g.name || String(g.id)" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { couponApi, goodsApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const list = ref([])
const goodsList = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const dateRange = ref([])
const form = reactive({
  merchantId: getMerchantId(),
  couponType: 2,
  grantType: 1,
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

const dialogTitle = computed(() => (isEdit.value ? '编辑优惠券' : '创建优惠券'))
const canStackSwitch = computed({
  get: () => Number(form.canStack) === 1,
  set: (v) => {
    form.canStack = v ? 1 : 0
  }
})

const limitEnabledSwitch = computed({
  get: () => Number(form.limitEnabled) !== 0,
  set: (v) => {
    form.limitEnabled = v ? 1 : 0
    if (Number(form.limitEnabled) === 0) {
      form.perLimit = 0
    }
  }
})

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

const loadList = async () => {
  try {
    const res = await couponApi.list(getMerchantId())
    list.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    list.value = []
    ElMessage.error(e?.response?.data?.message || '加载失败')
  }
}

const loadGoods = async () => {
  try {
    const res = await goodsApi.list(getMerchantId())
    goodsList.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    goodsList.value = []
  }
}

const resetForm = () => {
  form.merchantId = getMerchantId()
  form.couponType = 2
  form.grantType = 1
  form.name = ''
  form.discountType = 1
  form.discountAmount = 10
  form.discountRate = 0.9
  form.minAmount = 0
  form.perLimit = 1
  form.limitEnabled = 1
  form.canStack = 0
  form.totalCount = 100
  form.surplusNum = 100
  form.scopeType = 1
  form.targetIds = []
  form.startTime = ''
  form.endTime = ''
  dateRange.value = []
}

const openCreate = async () => {
  resetForm()
  isEdit.value = false
  editingId.value = null
  dialogVisible.value = true
  await loadGoods()
}

const openEdit = async (row) => {
  resetForm()
  isEdit.value = true
  editingId.value = row.id
  form.merchantId = getMerchantId()
  form.couponType = Number(row.couponType ?? 2)
  form.grantType = Number(row.grantType ?? 1)
  form.name = row.name || ''
  form.discountType = Number(row.discountType || 1)
  form.discountAmount = Number(row.discountAmount ?? 0)
  form.discountRate = Number(row.discountRate ?? 0.9)
  form.minAmount = Number(row.minAmount ?? 0)
  form.limitEnabled = row.limitEnabled == null ? 1 : Number(row.limitEnabled)
  form.perLimit = Number(row.perLimit ?? 1)
  form.canStack = Number(row.canStack ?? 0)
  form.totalCount = Number(row.totalCount ?? 1)
  form.surplusNum = Number(row.surplusNum ?? form.totalCount)
  form.scopeType = Number(row.scopeType ?? 1)
  form.targetIds = Array.isArray(row.targetIds) ? row.targetIds.slice() : []
  dateRange.value = [new Date(String(row.startTime).replace(' ', 'T')), new Date(String(row.endTime).replace(' ', 'T'))]
  dialogVisible.value = true
  await loadGoods()
}

const openEditAny = async (row) => {
  if (Number(row?.status) === 1) {
    ElMessage.warning('该券已上线，请先下线后再修改')
    return
  }
  await openEdit(row)
}

const submit = async () => {
  if (dateRange.value && dateRange.value.length === 2) {
    form.startTime = normalizeDateTime(dateRange.value[0])
    form.endTime = normalizeDateTime(dateRange.value[1])
  }
  form.merchantId = getMerchantId()
  try {
    if (Number(form.discountType) === 3) {
      form.minAmount = 0
    }
    if (!isEdit.value) {
      form.surplusNum = form.totalCount
      await couponApi.create({ ...form })
      ElMessage.success('创建成功')
    } else {
      await couponApi.update(editingId.value, { ...form })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  }
}

const setStatus = async (row, status) => {
  try {
    await couponApi.updateStatus(row.id, status)
    ElMessage.success('操作成功')
    await loadList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

const auditText = (v) => {
  const s = Number(v)
  if (s === 1) return '通过'
  if (s === 2) return '驳回'
  return '待审核'
}
const auditTagType = (v) => {
  const s = Number(v)
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'warning'
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

onMounted(loadList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.split {
  margin: 0 6px;
  color: #cbd5e1;
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
