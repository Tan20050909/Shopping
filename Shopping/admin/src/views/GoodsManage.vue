<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">商品管理</h2>
        <p class="page-subtitle">管理平台商品，审核与上下架</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width:220px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="auditFilter" placeholder="审核状态" clearable style="width:120px" @change="loadData">
          <el-option label="待审核" :value="0" /><el-option label="已通过" :value="1" /><el-option label="已拒绝" :value="2" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="上架状态" clearable style="width:120px" @change="loadData">
          <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无商品数据" /></template>
        <el-table-column prop="goodsId" label="ID" width="70" />
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:12px">
              <el-image v-if="row.mainImage" :src="row.mainImage" style="width:56px;height:56px;border-radius:8px;object-fit:cover" fit="cover" :preview-src-list="[row.mainImage]" />
              <div v-else style="width:56px;height:56px;background:#FFF5F5;border-radius:8px;display:flex;align-items:center;justify-content:center"><el-icon :size="24" color="#FFB3B3"><Picture /></el-icon></div>
              <div>
                <div style="font-weight:500">{{ row.goodsName }}</div>
                <div style="font-size:12px;color:var(--text-muted)">{{ row.categoryName || '-'}} · {{ row.merchantName || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="110">
          <template #default="{ row }"><span style="color:#E60012;font-weight:600">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="审核" width="90">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus===1?'success':row.auditStatus===0?'warning':'danger'" size="small" effect="light" style="border-radius:999px">{{ ['待审核','已通过','已拒绝'][row.auditStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架" width="80">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':row.status===3?'danger':'info'" size="small" effect="light" style="border-radius:999px">{{ row.status===1?'上架':row.status===3?'平台下架':'下架' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.auditStatus===0" type="primary" link size="small" @click="handleAudit(row,1)">通过</el-button>
            <el-button v-if="row.auditStatus===0" type="danger" link size="small" @click="handleAudit(row,2)">拒绝</el-button>
            <el-button v-if="row.status===1" type="danger" link size="small" @click="handleViolation(row)">平台下架</el-button>
            <el-button v-if="row.status===0&&row.auditStatus===1" type="success" link size="small" @click="handleStatus(row,1)">上架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="商品详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品名">{{ detail.goodsName }}</el-descriptions-item>
        <el-descriptions-item label="价格"><span style="color:#E60012;font-weight:600">¥{{ detail.price }}</span></el-descriptions-item>
        <el-descriptions-item label="库存">{{ detail.stock }}</el-descriptions-item>
        <el-descriptions-item label="销量">{{ detail.sales }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="商户">{{ detail.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.goodsDesc || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail.images" style="margin-top:16px;display:flex;gap:8px;flex-wrap:wrap">
        <el-image v-for="(img,i) in (detail.images||'').split(',')" :key="i" :src="img" style="width:100px;height:100px;border-radius:8px" fit="cover" :preview-src-list="(detail.images||'').split(',')" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGoodsList, auditGoods, updateGoodsStatus, getGoodsDetail } from '../api/goods'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), auditFilter = ref(null), statusFilter = ref(null)
const detailVisible = ref(false), detail = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (auditFilter.value != null) params.auditStatus = auditFilter.value
    if (statusFilter.value != null) params.status = statusFilter.value
    const res = await getGoodsList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载商品数据失败，请重试')
  } finally { loading.value = false }
}

const showDetail = async (row) => {
  try {
    const res = await getGoodsDetail(row.goodsId)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取商品详情失败')
  }
}

const handleAudit = async (row, status) => {
  try {
    const msg = status === 1 ? '确认通过审核？' : '确认拒绝审核？'
    await ElMessageBox.confirm(msg, '审核确认', { type: 'warning' })
    await auditGoods(row.goodsId, status)
    ElMessage.success('操作成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('审核操作失败')
  }
}

const handleStatus = async (row, status) => {
  try {
    await ElMessageBox.confirm(`确定要${status === 1 ? '上架' : '下架'}该商品吗？`, '操作确认', { type: 'warning' })
    await updateGoodsStatus(row.goodsId, status)
    ElMessage.success(status === 1 ? '已上架' : '已下架')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleViolation = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入平台下架原因', '平台下架', {
      inputType: 'textarea',
      inputPlaceholder: '请输入下架原因（必填）',
      inputValidator: (v) => !!v?.trim() || '请输入下架原因',
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await updateGoodsStatus(row.goodsId, 3, value.trim())
    ElMessage.success('已平台下架')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(loadData)
</script>
