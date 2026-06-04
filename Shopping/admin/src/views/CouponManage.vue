<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">COUPON MANAGEMENT</span>
          <h1 class="admin-page-title">优惠券管理</h1>
          <p class="admin-page-desc">创建和管理平台优惠券，支持满减、折扣、无门槛等多种类型</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel">
        <div class="admin-filter-bar">
          <el-input v-model="keyword" placeholder="搜索优惠券名称" clearable class="admin-search-input" @clear="loadData" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="typeFilter" placeholder="类型" clearable class="admin-status-select" @change="loadData">
            <el-option label="满减" :value="1" /><el-option label="折扣" :value="2" /><el-option label="无门槛" :value="3" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openForm()">新增优惠券</el-button>
        </div>
        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table" style="width:100%">
            <template #empty><el-empty description="暂无优惠券数据" /></template>
            <el-table-column prop="couponId" label="ID" width="70" />
            <el-table-column prop="couponName" label="名称" min-width="160" />
            <el-table-column label="类型" width="80">
              <template #default="{ row }">
                <span class="admin-status-tag tag-info">{{ ['','满减','折扣','无门槛'][row.couponType] }}</span>
              </template>
            </el-table-column>
            <el-table-column label="优惠" width="120">
              <template #default="{ row }">
                <span class="admin-table-price">{{ row.couponType===2 ? row.discountValue+'折' : '¥'+row.discountValue }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已领/总量" width="110">
              <template #default="{ row }">{{ row.receivedCount || 0 }}/{{ row.totalCount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="row.status===1?'tag-success':'tag-info'">{{ row.status===1?'启用':'禁用' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="有效期" width="180">
              <template #default="{ row }"><span style="font-size:12px">{{ row.startTime?.slice(0,10) }} ~ {{ row.endTime?.slice(0,10) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="openForm(row)">编辑</button>
                  <button v-if="row.status===1" class="admin-action-btn admin-action-warning" @click="toggleStatus(row)">禁用</button>
                  <button v-else class="admin-action-btn" style="color:#047857;border-color:#a7f3d0" @click="toggleStatus(row)">启用</button>
                  <button class="admin-action-btn admin-action-danger" @click="handleDelete(row)">删除</button>
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
    <el-dialog v-model="formVisible" :title="form.couponId?'编辑优惠券':'新增优惠券'" width="580px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required><el-input v-model="form.couponName" placeholder="优惠券名称" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.couponType" style="width:100%">
            <el-option label="满减券" :value="1" /><el-option label="折扣券" :value="2" /><el-option label="无门槛券" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠值" required><el-input-number v-model="form.discountValue" :min="0.1" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="满减门槛" v-if="form.couponType===1"><el-input-number v-model="form.minAmount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="发放总量" required><el-input-number v-model="form.totalCount" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="每人限领"><el-input-number v-model="form.perLimit" :min="0" style="width:100%" /><div style="font-size:12px;color:var(--text-muted)">0表示不限</div></el-form-item>
        <el-form-item label="适用范围">
          <el-select v-model="form.scopeType" style="width:100%">
            <el-option label="全场通用" :value="1" /><el-option label="指定分类" :value="2" /><el-option label="指定商品" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker v-model="form.dateRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="dateShortcuts" :default-time="[new Date(0,0,0,0,0,0), new Date(0,0,0,23,59,59)]" unlink-panels popper-class="range-date-popper"
          />
        </el-form-item>
        <el-form-item label="使用说明"><el-input v-model="form.usageDesc" type="textarea" :rows="2" placeholder="使用说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCouponList, addCoupon, updateCoupon, deleteCoupon, updateCouponStatus } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), typeFilter = ref(null)
const formVisible = ref(false), submitting = ref(false)
const form = reactive({ couponId: null, couponName: '', couponType: 1, discountValue: 10, minAmount: 0, totalCount: 100, perLimit: 0, scopeType: 1, dateRange: null, usageDesc: '', status: 1 })

const dateShortcuts = [
  { text: '7天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 7*24*3600*1000)] }},
  { text: '30天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 30*24*3600*1000)] }},
  { text: '90天', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 90*24*3600*1000)] }},
  { text: '半年', value: () => { const e = new Date(); const s = new Date(); return [s, new Date(e.getTime() + 180*24*3600*1000)] }},
]

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (typeFilter.value != null) params.couponType = typeFilter.value
    const res = await getCouponList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载优惠券数据失败，请重试')
  } finally { loading.value = false }
}

const openForm = (row) => {
  if (row) {
    Object.assign(form, { ...row, dateRange: [row.startTime, row.endTime] })
  } else {
    Object.assign(form, { couponId: null, couponName: '', couponType: 1, discountValue: 10, minAmount: 0, totalCount: 100, perLimit: 0, scopeType: 1, dateRange: null, usageDesc: '', status: 1 })
  }
  formVisible.value = true
}

const submitForm = async () => {
  if (!form.couponName) { ElMessage.warning('请输入名称'); return }
  submitting.value = true
  try {
    const data = { ...form, startTime: form.dateRange?.[0], endTime: form.dateRange?.[1] }
    delete data.dateRange
    if (form.couponId) await updateCoupon(data)
    else await addCoupon(data)
    ElMessage.success(form.couponId ? '更新成功' : '添加成功')
    formVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally { submitting.value = false }
}

const toggleStatus = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    const action = newStatus === 1 ? '启用' : '禁用'
    await ElMessageBox.confirm(`确定要${action}优惠券"${row.couponName}"吗？`, '操作确认', { type: 'warning' })
    await updateCouponStatus(row.couponId, newStatus)
    ElMessage.success('操作成功'); loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败，请重试')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该优惠券？', '删除确认', { type: 'warning' })
    await deleteCoupon(row.couponId)
    ElMessage.success('已删除'); loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败，请重试')
  }
}

onMounted(loadData)
</script>
<style scoped>
.admin-order-page { color: var(--text-main); }
</style>
<style>
.range-date-popper { z-index: 3000 !important; }
</style>
