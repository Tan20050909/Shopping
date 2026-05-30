<template>
  <div class="goods-page">
    <div class="hero-card">
      <div>
        <p class="hero-badge">商品中心</p>
        <h2>发布和管理你的店铺商品</h2>
        <div v-if="queryKeyword" class="hero-search-tip">
          当前搜索：{{ queryKeyword }}
          <el-button text class="hero-clear" @click.stop="clearKeyword">清除</el-button>
        </div>
      </div>
      <el-button type="primary" size="large" class="hero-action" @click="goCreate">
        发布商品
      </el-button>
    </div>

    <div class="stats-grid">
      <div class="stats-card">
        <span class="stats-label">商品总数</span>
        <strong>{{ goodsList.length }}</strong>
      </div>
      <div class="stats-card">
        <span class="stats-label">已上架</span>
        <strong>{{ publishedCount }}</strong>
      </div>
      <div class="stats-card">
        <span class="stats-label">待审核</span>
        <strong>{{ pendingAuditCount }}</strong>
      </div>
    </div>

    <el-card v-if="topSelling.length" class="rank-panel" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">热销榜 TOP3</div>
            <div class="card-subtitle">按已付款订单购买量统计</div>
          </div>
        </div>
      </template>
      <div class="rank-grid">
        <div v-for="(g, idx) in topSelling" :key="g.id" class="rank-item" @click="goDetail(g.id)">
          <div class="rank-no">{{ idx + 1 }}</div>
          <img class="rank-img" :src="resolveImg(g.goodsPic)" alt="" @error="onImgError" />
          <div class="rank-meta">
            <div class="rank-name">{{ g.name }}</div>
            <div class="rank-sub">已售 {{ Number(g.buyCount ?? 0) }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">商品列表</div>
            <div class="card-subtitle">当前商家 ID：{{ merchantId }}</div>
          </div>
          <div class="header-actions">
            <div v-if="selectedCount > 0" class="bulk-bar">
              <span class="bulk-tip">已选 {{ selectedCount }}</span>
              <el-button size="small" type="success" plain @click="bulkUpdateStatus(1)">批量上架</el-button>
              <el-button size="small" type="warning" plain @click="bulkUpdateStatus(0)">批量下架</el-button>
              <el-button size="small" type="danger" plain @click="bulkDelete">批量删除</el-button>
              <el-button size="small" plain @click="clearSelection">清空</el-button>
              <el-button size="small" plain @click="selectAllDisplayed">全选当前</el-button>
            </div>
            <el-button plain @click="loadGoods">刷新列表</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!goodsList.length" description="还没有商品，先发布一个试试" />

      <el-empty v-else-if="!displayGoods.length" description="未找到匹配的商品" />

      <div v-else class="goods-grid">
        <div v-for="g in displayGoods" :key="g.id" class="goods-card" @click="goDetail(g.id)">
          <div class="goods-card-media">
            <img class="goods-card-img" :src="resolveImg(g.goodsPic)" alt="" @error="onImgError" />
            <div class="goods-card-select" @click.stop>
              <el-checkbox :model-value="isSelected(g.id)" @change="(v) => setSelected(g.id, v)" />
            </div>
            <div class="goods-card-tags">
              <span class="tag" :class="g.status === 1 ? 't-on' : 't-off'">{{ g.status === 1 ? '上架中' : '未上架' }}</span>
              <span class="tag" :class="g.auditStatus === 1 ? 't-pass' : 't-wait'">{{ g.auditStatus === 1 ? '已通过' : '待审核' }}</span>
              <span v-if="plannedTime(g.id)" class="tag t-plan">定时 {{ plannedTime(g.id) }}</span>
            </div>
          </div>
          <div class="goods-card-body">
            <div class="goods-card-name">{{ g.name }}</div>
            <div class="goods-card-desc">{{ g.description || '暂无商品描述' }}</div>
            <div class="goods-card-meta">
              <span class="meta-item">分类：{{ g.categoryName || categoryName(g.categoryId) }}</span>
              <span class="meta-item">收藏：{{ Number(g.favoriteCount ?? 0) }}</span>
              <span class="meta-item">已售：{{ Number(g.buyCount ?? 0) }}</span>
            </div>
          </div>
          <div class="goods-card-actions" @click.stop>
            <el-button size="small" type="primary" plain @click="toggleShelf(g)">
              {{ g.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" plain @click="goDetail(g.id)">详情</el-button>
            <el-button size="small" plain @click="goDetail(g.id, 'comment')">评价</el-button>
            <el-button size="small" type="danger" plain @click="deleteGoods(g.id)">删除</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categoryApi, goodsApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const route = useRoute()
const router = useRouter()

const defaultImage = 'https://via.placeholder.com/320x320?text=Goods'
const goodsList = ref([])
const queryKeyword = ref('')
const merchantId = ref(getMerchantId())
const categories = ref([])
const scheduleMap = ref({})
const selectedIds = ref([])

const scheduleKey = (merchantId) => `goodsSchedule:${merchantId}`

const loadScheduleMap = (merchantId) => {
  try {
    const raw = localStorage.getItem(scheduleKey(merchantId))
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

const saveScheduleMap = (merchantId, map) => {
  try {
    localStorage.setItem(scheduleKey(merchantId), JSON.stringify(map || {}))
  } catch (e) {
  }
}

const parsePlan = (value) => {
  if (!value) return null
  const s = String(value).trim()
  if (!s) return null
  const normalized = s.includes(' ') ? s.replace(' ', 'T') : s
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

const plannedTime = (id) => {
  const v = scheduleMap.value?.[String(id)]
  const s = String(v || '').trim()
  if (!s) return ''
  return (s.length > 16 ? s.slice(0, 16) : s).replace('T', ' ')
}

const publishedCount = computed(() => goodsList.value.filter(item => item.status === 1).length)
const pendingAuditCount = computed(() => goodsList.value.filter(item => item.auditStatus !== 1).length)

const matchGoodsKeyword = (g) => {
  const k = String(queryKeyword.value || '').trim()
  if (!k) return true
  const fields = [g?.name, g?.description, g?.id, g?.categoryName].filter(v => v != null).map(String)
  return fields.some(v => v.includes(k))
}

const displayGoods = computed(() => {
  return (goodsList.value || []).filter(matchGoodsKeyword)
})

const topSelling = computed(() => {
  return (goodsList.value || [])
    .slice()
    .sort((a, b) => Number(b?.buyCount ?? 0) - Number(a?.buyCount ?? 0))
    .slice(0, 3)
})

const getErrorMessage = (error, fallback) => {
  return error?.response?.data?.message || fallback
}

const resolveImg = (src) => {
  const v = String(src || '').trim()
  if (!v) return defaultImage
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  return defaultImage
}

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

const loadGoods = async () => {
  try {
    merchantId.value = getMerchantId()
    scheduleMap.value = loadScheduleMap(merchantId.value)
    const res = await goodsApi.list(merchantId.value)
    goodsList.value = (Array.isArray(res.data) ? res.data : []).map(g => ({
      ...g,
      buyCount: Number(g?.buyCount ?? 0) || 0
    }))
    await applyDueSchedules()
  } catch (error) {
    goodsList.value = []
    ElMessage.error(getErrorMessage(error, '商品加载失败，请检查后端接口和数据库字段映射'))
  }
}

const selectedCount = computed(() => selectedIds.value.length)

const isSelected = (id) => {
  const n = Number(id)
  return selectedIds.value.includes(n)
}

const setSelected = (id, v) => {
  const n = Number(id)
  if (!Number.isFinite(n)) return
  const on = Boolean(v)
  if (on) {
    if (!selectedIds.value.includes(n)) selectedIds.value.push(n)
    return
  }
  selectedIds.value = selectedIds.value.filter(x => x !== n)
}

const clearSelection = () => {
  selectedIds.value = []
}

const selectAllDisplayed = () => {
  const ids = (displayGoods.value || []).map(g => Number(g?.id)).filter(v => Number.isFinite(v))
  selectedIds.value = Array.from(new Set(ids))
}

const bulkUpdateStatus = async (status) => {
  if (!selectedIds.value.length) return
  try {
    await goodsApi.batchUpdateStatus(selectedIds.value, status)
    ElMessage.success(status === 1 ? '批量上架成功' : '批量下架成功')
    await loadGoods()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '批量操作失败'))
  }
}

const bulkDelete = async () => {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除已选 ${selectedIds.value.length} 个商品？删除后不可恢复。`, '批量删除', { type: 'warning' })
    await goodsApi.batchDelete(selectedIds.value)
    ElMessage.success('批量删除成功')
    clearSelection()
    await loadGoods()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '批量删除失败'))
    }
  }
}

const applyDueSchedules = async () => {
  const now = new Date()
  const due = goodsList.value
    .filter(g => g && g.id != null)
    .map(g => ({ goods: g, plan: scheduleMap.value[String(g.id)] }))
    .filter(x => {
      if (!x.plan) return false
      if (Number(x.goods.status) === 1) return false
      const d = parsePlan(x.plan)
      return d && d.getTime() <= now.getTime()
    })
    .slice(0, 20)

  if (!due.length) return

  const results = await Promise.allSettled(due.map(x => goodsApi.updateStatus(x.goods.id, 1)))
  let success = 0
  for (let i = 0; i < results.length; i++) {
    const r = results[i]
    const g = due[i].goods
    const key = String(g.id)
    if (r.status === 'fulfilled') {
      g.status = 1
      delete scheduleMap.value[key]
      success++
    }
  }

  saveScheduleMap(merchantId.value, scheduleMap.value)
  if (success > 0) {
    ElMessage.success(`已自动上架 ${success} 个商品`)
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.list()
    categories.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    categories.value = []
  }
}

const categoryName = (categoryId) => {
  const id = Number(categoryId)
  const found = categories.value.find(c => Number(c.id) === id)
  return found ? found.name : String(categoryId ?? '-')
}

const goCreate = () => {
  router.push('/goods/new')
}

const clearKeyword = () => {
  router.replace({ path: '/goods', query: {} })
}

const goDetail = (id, tab) => {
  if (!id) return
  router.push({ path: `/goods/${id}`, query: tab ? { tab } : {} })
}

const deleteGoods = async (id) => {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确认继续吗？', '删除商品', { type: 'warning' })
    await goodsApi.delete(id)
    ElMessage.success('商品删除成功')
    clearSelection()
    await loadGoods()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error, '商品删除失败'))
    }
  }
}

const toggleShelf = async (row) => {
  try {
    const next = row.status === 1 ? 0 : 1
    await goodsApi.updateStatus(row.id, next)
    ElMessage.success(next === 1 ? '已上架' : '已下架')
    await loadGoods()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

onMounted(() => {
  loadCategories()
  loadGoods()
})

watch(
  () => route.query?.keyword,
  (v) => {
    queryKeyword.value = String(v || '').trim()
  },
  { immediate: true }
)
</script>

<style scoped>
.goods-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 24px;
  background: linear-gradient(135deg, #182848 0%, #4b6cb7 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(24, 40, 72, 0.16);
}

.upload {
  margin-bottom: 10px;
}

.sku-block {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sku-actions {
  display: flex;
  gap: 10px;
}

.sku-table {
  width: 100%;
}

.kv-block {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kv-actions {
  display: flex;
  justify-content: flex-start;
}

.kv-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.kv-key {
  flex: 0 0 40%;
}

.kv-val {
  flex: 1;
}

.hero-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  letter-spacing: 1px;
}

.hero-card h2 {
  margin: 14px 0 10px;
  font-size: 30px;
}

.hero-desc {
  max-width: 680px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.7;
}

.hero-search-tip {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hero-clear {
  color: #fff;
  padding: 0;
  height: 22px;
}

.hero-action {
  border-radius: 14px;
  padding: 0 24px;
}

.rank-panel :deep(.el-card__header) {
  border-bottom: 1px solid #eef2f7;
}

.rank-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  background: #fff;
  cursor: pointer;
}

.rank-no {
  width: 26px;
  height: 26px;
  border-radius: 10px;
  background: #ffe8ea;
  color: #e60012;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  flex: none;
}

.rank-img {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid #eef2f7;
  background: #f8fafc;
  flex: none;
}

.rank-meta {
  min-width: 0;
  flex: 1;
}

.rank-name {
  font-weight: 900;
  font-size: 13px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 960px) {
  .rank-grid {
    grid-template-columns: 1fr;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stats-card {
  padding: 22px 24px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.stats-label {
  display: block;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 14px;
}

.stats-card strong {
  font-size: 30px;
  color: #0f172a;
}

.table-card {
  border: none;
  border-radius: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.bulk-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 10px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #ffffff;
}

.bulk-tip {
  font-size: 12px;
  color: #111827;
  font-weight: 700;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.card-subtitle {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.goods-card {
  border: 1px solid #eef2f7;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.goods-card:hover {
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.12);
  transform: translateY(-2px);
}

.goods-card-media {
  position: relative;
  height: 0;
  padding-top: 100%;
  background: #f8fafc;
  overflow: hidden;
}

.goods-card-select {
  position: absolute;
  right: 10px;
  top: 10px;
  z-index: 2;
  padding: 6px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(229, 231, 235, 0.9);
}

.goods-card-img {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
}

.goods-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.goods-card-tags {
  position: absolute;
  left: 10px;
  top: 10px;
  display: flex;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(17, 24, 39, 0.75);
  color: #fff;
}

.t-on {
  background: rgba(16, 185, 129, 0.9);
}

.t-off {
  background: rgba(107, 114, 128, 0.9);
}

.t-pass {
  background: rgba(34, 197, 94, 0.9);
}

.t-wait {
  background: rgba(245, 158, 11, 0.9);
}

.t-plan {
  background: rgba(230, 0, 18, 0.85);
}

.goods-card-body {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.goods-card-name {
  font-weight: 700;
  color: #111827;
  font-size: 16px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  height: 42px;
}

.goods-card-desc {
  color: #64748b;
  line-height: 1.4;
  font-size: 13px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  height: 40px;
}

.goods-card-meta {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  color: #94a3b8;
  font-size: 12px;
  height: 18px;
  min-height: 18px;
}

.goods-card-meta .meta-item {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-card-actions {
  margin-top: auto;
  padding: 12px 14px 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  align-items: stretch;
}

.goods-card-actions :deep(.el-button) {
  width: 100%;
  border-radius: 10px;
  height: 32px;
  padding: 0 10px;
  font-size: 12px;
}

.goods-card-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 900px) {
  .hero-card,
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>
