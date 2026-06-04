<template>
  <div class="finance-page">
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">FINANCE CENTER</span>
          <h1 class="merchant-page-title">财务中心</h1>
          <p class="merchant-page-desc">查看店铺资产、资金流水与提现记录，掌握经营资金动态</p>
        </div>
      </div>
    </section>
    <el-card shadow="never" class="merchant-content-card">
      <template #header><span>财务概览</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="可用余额">¥ {{ toMoney(finance?.balance) }}</el-descriptions-item>
        <el-descriptions-item label="未结算金额">¥ {{ toMoney(finance?.unsettledAmount) }}</el-descriptions-item>
        <el-descriptions-item label="冻结金额">¥ {{ toMoney(finance?.freezeAmount) }}</el-descriptions-item>
        <el-descriptions-item label="平台抽成比例">{{ toRate(finance?.commissionRate) }}</el-descriptions-item>
        <el-descriptions-item label="总资产(估算)" :span="2">¥ {{ totalAsset }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ finance?.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="merchant-content-card">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px">
          <span>金额来源/支出明细（资金流水）</span>
          <el-button plain @click="loadFlows">刷新</el-button>
        </div>
      </template>

      <div class="flow-tabs">
        <div class="tab" :class="{ active: flowTab === 'all' }" @click="flowTab = 'all'">全部</div>
        <div class="tab" :class="{ active: flowTab === 'in' }" @click="flowTab = 'in'">收入</div>
        <div class="tab" :class="{ active: flowTab === 'out' }" @click="flowTab = 'out'">支出</div>
      </div>

      <el-table :data="displayFlows" style="width: 100%" size="small">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="flowType" label="类型" width="140">
          <template #default="{ row }">{{ flowTypeText(row.flowType) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="140">
          <template #default="{ row }">
            <span :style="{ color: Number(row.amount) >= 0 ? '#16a34a' : '#dc2626', fontWeight: 700 }">
              {{ Number(row.amount) >= 0 ? '+' : '' }}{{ toMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="orderId" label="订单ID" width="120" />
        <el-table-column prop="withdrawId" label="提现ID" width="120" />
        <el-table-column prop="remark" label="说明" min-width="220" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="merchant-content-card">
      <template #header>
        <span>提现申请</span>
      </template>
      <el-form :inline="true" :model="withdrawForm">
        <el-form-item label="可提现">
          <strong style="color:#111827">¥ {{ toMoney(withdrawableAmount) }}</strong>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number
            v-model="withdrawForm.amount"
            :min="0"
            :max="withdrawableAmount"
            :precision="2"
            :disabled="withdrawableAmount <= 0"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="withdrawForm.remark" placeholder="可选" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="withdrawing" :disabled="withdrawableAmount <= 0" @click="applyWithdraw">申请提现</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="merchant-content-card">
      <template #header><span>提现记录</span></template>
      <el-table :data="withdrawList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="amount" label="金额">
          <template #default="{ row }">¥ {{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'warning' : 'info'">
              {{ row.status === 2 ? '已完成' : row.status === 1 ? '审核中' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createTime" label="申请时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { financeApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const finance = ref(null)
const withdrawList = ref([])
const flows = ref([])
const flowTab = ref('all')
const withdrawing = ref(false)
const withdrawForm = reactive({
  merchantId: getMerchantId(),
  amount: 0,
  remark: ''
})

const toMoney = (value) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const toRate = (value) => {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '0%'
  return `${(num * 100).toFixed(2)}%`
}

const totalAsset = computed(() => {
  const b = Number(finance.value?.balance ?? 0) || 0
  const u = Number(finance.value?.unsettledAmount ?? 0) || 0
  const f = Number(finance.value?.freezeAmount ?? 0) || 0
  return (b + u + f).toFixed(2)
})

const withdrawableAmount = computed(() => {
  const b = Number(finance.value?.balance ?? 0)
  return Number.isFinite(b) ? Math.max(0, b) : 0
})

const flowTypeText = (t) => {
  const v = Number(t)
  const map = {
    1: '订单入账',
    2: '平台佣金',
    3: '退款扣减',
    4: '提现冻结',
    5: '提现成功',
    6: '提现退回',
    7: '人工调整'
  }
  return map[v] || `类型${v}`
}

const displayFlows = computed(() => {
  const list = Array.isArray(flows.value) ? flows.value : []
  if (flowTab.value === 'in') return list.filter(x => Number(x.amount) >= 0)
  if (flowTab.value === 'out') return list.filter(x => Number(x.amount) < 0)
  return list
})

const loadFinance = async () => {
  try {
    const res = await financeApi.get(getMerchantId())
    finance.value = res.data
    const max = withdrawableAmount.value
    if (max <= 0) {
      withdrawForm.amount = 0
    } else {
      const cur = Number(withdrawForm.amount ?? 0)
      withdrawForm.amount = Math.min(Math.max(0, Number.isFinite(cur) ? cur : 0), max)
    }
  } catch (error) {
    finance.value = null
    ElMessage.error(error?.response?.data?.message || '财务数据加载失败')
  }
}

const loadFlows = async () => {
  try {
    const res = await financeApi.listFlow(getMerchantId(), 100, null)
    flows.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    flows.value = []
    ElMessage.error(error?.response?.data?.message || '资金流水加载失败')
  }
}

const loadWithdraws = async () => {
  try {
    const res = await financeApi.listWithdraw(getMerchantId())
    withdrawList.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    withdrawList.value = []
    ElMessage.error(error?.response?.data?.message || '提现记录加载失败')
  }
}

const applyWithdraw = async () => {
  if (withdrawing.value) return
  try {
    const max = withdrawableAmount.value
    const amount = Number(withdrawForm.amount ?? 0)
    if (!Number.isFinite(amount) || amount <= 0) {
      ElMessage.warning('请输入正确的提现金额')
      return
    }
    if (max <= 0) {
      ElMessage.warning('当前无可提现余额')
      return
    }
    if (amount > max) {
      ElMessage.warning(`余额不足，最多可提现 ¥${toMoney(max)}`)
      return
    }
    withdrawing.value = true
    withdrawForm.merchantId = getMerchantId()
    await financeApi.applyWithdraw(withdrawForm)
    ElMessage.success('申请成功')
    withdrawForm.amount = 0
    withdrawForm.remark = ''
    await loadWithdraws()
    await loadFinance()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '申请失败')
  } finally {
    withdrawing.value = false
  }
}

onMounted(() => {
  loadFinance()
  loadFlows()
  loadWithdraws()
})
</script>

<style scoped>
.flow-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tab {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  color: #374151;
  cursor: pointer;
  user-select: none;
}

.tab.active {
  border-color: rgba(239, 68, 68, 0.55);
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
  font-weight: 800;
}
</style>
