<template>
  <div class="goods-page">
    <!-- 全宽 Hero — 参考用户端 /products -->
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">MERCHANT GOODS</span>
          <h1 class="merchant-page-title">商品管理</h1>
          <p class="merchant-page-desc">发布、编辑和管理店铺商品，实时掌握上架、审核与销售状态</p>
          <div v-if="queryKeyword" class="hero-search-tip">
            当前搜索：{{ queryKeyword }}
            <el-button text class="hero-clear" @click.stop="clearKeyword">清除</el-button>
          </div>
          <div class="merchant-page-actions">
            <div class="merchant-chip-tabs" aria-label="商品状态筛选">
              <button
                v-for="tab in statusTabs"
                :key="tab.key"
                type="button"
                class="merchant-chip"
                :class="{ active: goodsStatusFilter === tab.key }"
                @click="goodsStatusFilter = tab.key"
              >
                {{ tab.label }} <span class="chip-count">{{ tab.count }}</span>
              </button>
            </div>
            <el-button type="primary" size="large" class="merchant-primary-action" @click="goCreate">
              发布商品
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- 热销榜 TOP3 -->
    <section v-if="topSelling.length" class="goods-top-section">
      <div class="goods-container">
        <div class="section-header">
          <div>
            <div class="section-title">热销榜 TOP3</div>
            <div class="section-subtitle">按已付款订单购买量统计</div>
          </div>
        </div>
        <div class="top-grid">
          <div v-for="(g, idx) in topSelling" :key="g.id" class="top-card" @click="goDetail(g.id)">
            <div class="top-rank">{{ idx + 1 }}</div>
            <img class="top-img" :src="resolveImg(g.goodsPic)" alt="" @error="onImgError" />
            <div class="top-info">
              <div class="top-name">{{ g.name }}</div>
              <div class="top-sold">已售 {{ Number(g.buyCount ?? 0) }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 商品列表 -->
    <section class="goods-list-section">
      <div class="goods-container">
        <div class="section-header">
          <div>
            <div class="section-title">商品列表</div>
            <div class="section-subtitle">共 {{ goodsList.length }} 件商品</div>
          </div>
          <div class="header-actions">
            <div v-if="selectedCount > 0" class="bulk-bar">
              <span class="bulk-tip">已选 {{ selectedCount }}</span>
              <el-button size="small" type="primary" plain @click="bulkUpdateStatus(1)">批量上架</el-button>
              <el-button size="small" plain @click="bulkUpdateStatus(0)">批量下架</el-button>
              <el-button size="small" plain @click="bulkDelete">批量删除</el-button>
              <el-button size="small" plain @click="clearSelection">清空</el-button>
              <el-button size="small" plain @click="selectAllDisplayed">全选当前</el-button>
            </div>
            <el-button plain @click="loadGoods">刷新列表</el-button>
          </div>
        </div>

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
                <span class="tag" :class="g.status === 1 ? 't-on' : g.status === 3 ? 't-ban' : 't-off'">{{ g.status === 1 ? '上架中' : g.status === 3 ? '平台下架' : '未上架' }}</span>
                <span class="tag" :class="g.auditStatus === 1 ? 't-pass' : g.auditStatus === 2 ? 't-fail' : 't-wait'">{{ g.auditStatus === 1 ? '已通过' : g.auditStatus === 2 ? '已拒绝' : '待审核' }}</span>
                <span v-if="g.auditStatus === 2 && g.auditRemark" class="tag t-fail" :title="g.auditRemark">原因：{{ g.auditRemark }}</span>
                <span v-if="plannedTime(g.id)" class="tag t-plan">定时 {{ plannedTime(g.id) }}</span>
              </div>
            </div>
            <div class="goods-card-body">
              <div class="goods-card-name">{{ g.name }}</div>
              <div class="goods-card-desc">{{ g.description || '暂无商品描述' }}</div>
              <div class="goods-card-price">¥{{ formatPrice(goodsPrice(g)) }}</div>
              <div class="goods-card-meta">
                <span class="meta-item">分类：{{ g.categoryName || categoryName(g.categoryId) }}</span>
                <span class="meta-item">收藏：{{ Number(g.favoriteCount ?? 0) }}</span>
                <span class="meta-item">已售：{{ Number(g.buyCount ?? 0) }}</span>
              </div>
            </div>
            <div class="goods-card-actions" @click.stop>
              <el-button v-if="g.status !== 3" size="small" type="primary" plain @click="toggleShelf(g)">
                {{ g.status === 1 ? '下架' : '上架' }}
              </el-button>
              <el-button v-if="g.status === 3" size="small" type="warning" plain @click="submitReview(g)">提交复审</el-button>
              <span v-if="g.status === 3 && g.auditRemark" class="tag t-ban" :title="g.auditRemark" style="cursor:default;font-size:11px">原因：{{ g.auditRemark }}</span>
              <el-button size="small" plain @click="goDetail(g.id)">详情</el-button>
              <el-button size="small" plain @click="goDetail(g.id, 'comment')">评价</el-button>
              <el-button size="small" type="danger" plain @click="deleteGoods(g.id)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="goods-footer">
      <div class="goods-container">
        <div class="footer-inner">
          <div class="footer-brand">
            <img src="/brand-assets/allmart-logo-full.png" alt="AllMart" />
            <span>商家 ID：{{ merchantId }}</span>
          </div>
          <nav class="footer-nav">
            <a href="#" @click.prevent="goSupport">帮助中心</a>
            <a href="#" @click.prevent="goSupport">平台规则</a>
            <a href="#" @click.prevent="goSetting">隐私政策</a>
            <a href="#" @click.prevent="goSupport">联系我们</a>
          </nav>
          <p class="footer-copy">© 2025 AllMart，保留所有权利。</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categoryApi, goodsApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const route = useRoute()
const router = useRouter()

const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%22320%22%20height%3D%22320%22%20viewBox%3D%220%200%20320%20320%22%3E%3Crect%20width%3D%22320%22%20height%3D%22320%22%20rx%3D%2228%22%20fill%3D%22%23f7f7f7%22/%3E%3Cpath%20d%3D%22M86%20118h132v88H86z%22%20fill%3D%22%23ececec%22/%3E%3Cpath%20d%3D%22M104%20190l37-38%2029%2029%2028-35%22%20stroke%3D%22%23cfcfcf%22%20stroke-width%3D%2210%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22/%3E%3Ccircle%20cx%3D%22122%22%20cy%3D%22142%22%20r%3D%2213%22%20fill%3D%22%23d5d5d5%22/%3E%3C/svg%3E'
const goodsList = ref([])
const queryKeyword = ref('')
const merchantId = ref(getMerchantId())
const categories = ref([])
const scheduleMap = ref({})
const selectedIds = ref([])
const goodsStatusFilter = ref('all')

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
const offShelfCount = computed(() => goodsList.value.filter(item => item.status !== 1).length)

const statusTabs = computed(() => [
  { key: 'all', label: '全部', count: goodsList.value.length },
  { key: 'on', label: '已上架', count: publishedCount.value },
  { key: 'audit', label: '待审核', count: pendingAuditCount.value },
  { key: 'off', label: '已下架', count: offShelfCount.value }
])

const matchGoodsKeyword = (g) => {
  const k = String(queryKeyword.value || '').trim()
  if (!k) return true
  const fields = [g?.name, g?.description, g?.id, g?.categoryName].filter(v => v != null).map(String)
  return fields.some(v => v.includes(k))
}

const displayGoods = computed(() => {
  return (goodsList.value || [])
    .filter(matchGoodsKeyword)
    .filter(g => {
      if (goodsStatusFilter.value === 'on') return Number(g.status) === 1
      if (goodsStatusFilter.value === 'audit') return Number(g.auditStatus) !== 1
      if (goodsStatusFilter.value === 'off') return Number(g.status) !== 1
      return true
    })
})

const formatPrice = (value) => {
  const n = Number(value ?? 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const goodsPrice = (goods) => {
  return goods?.price ?? goods?.displayPrice ?? goods?.display_price ?? goods?.minPrice ?? goods?.min_price ?? 0
}

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
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  if (v.startsWith('/goods/') || v.startsWith('goods/')) return v.startsWith('/') ? `/uploads${v}` : `/uploads/${v}`
  if (v.startsWith('/images/') || v.startsWith('/videos/')) return v
  if (v.startsWith('images/') || v.startsWith('videos/')) return `/uploads/${v}`
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
    if (status === 1) {
      const banItems = goodsList.value.filter(g => selectedIds.value.includes(g.id) && g.status === 3)
      const normalItems = goodsList.value.filter(g => selectedIds.value.includes(g.id) && g.status !== 3)
      if (banItems.length > 0) {
        await goodsApi.batchUpdateStatus(banItems.map(g => g.id), 1)
      }
      if (normalItems.length > 0) {
        await goodsApi.batchUpdateStatus(normalItems.map(g => g.id), 1)
      }
      const msgs = []
      if (normalItems.length > 0) msgs.push(`${normalItems.length} 个商品已上架`)
      if (banItems.length > 0) msgs.push(`${banItems.length} 个平台下架商品已提交复审`)
      ElMessage.success(msgs.join('，'))
    } else {
      const banItems = goodsList.value.filter(g => selectedIds.value.includes(g.id) && g.status === 3)
      const normalItems = goodsList.value.filter(g => selectedIds.value.includes(g.id) && g.status !== 3)
      if (normalItems.length > 0) {
        await goodsApi.batchUpdateStatus(normalItems.map(g => g.id), 0)
      }
      if (banItems.length > 0) {
        ElMessage.warning(`${banItems.length} 个平台下架商品无法批量下架，请单独提交复审`)
      }
      if (normalItems.length > 0) {
        ElMessage.success(`${normalItems.length} 个商品已下架`)
      }
    }
    clearSelection()
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

const goSupport = () => {
  router.push('/platform-support')
}

const goSetting = () => {
  router.push('/merchant-setting')
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

const submitReview = async (row) => {
  try {
    await ElMessageBox.confirm('提交后商品将重新进入平台审核，确认提交复审？', '提交复审', { type: 'warning' })
    await goodsApi.updateStatus(row.id, 1)
    ElMessage.success('已提交平台复审')
    await loadGoods()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(getErrorMessage(error, '操作失败'))
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
/* ============ 基础 ============ */
.goods-page {
  color: var(--text-main);
  background: #fff;
}

.goods-page,
.goods-page * {
  box-sizing: border-box;
}

/* ============ 通用容器 ============ */
.goods-container {
  width: 100%;
  max-width: 1220px;
  margin: 0 auto;
  padding: 0 24px;
}

.hero-search-tip {
  color: var(--text-secondary);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hero-clear {
  color: var(--brand-red);
  padding: 0;
  height: 22px;
}

.chip-count {
  margin-left: 4px;
  color: inherit;
  font-size: 12px;
}

/* ============ 热销榜 TOP3 ============ */
.goods-top-section {
  padding: 24px 0 32px;
  background: #fff;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  color: var(--text-main);
  font-size: 18px;
  font-weight: 800;
}

.section-subtitle {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.top-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.top-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 1px solid var(--border-light);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.top-card:hover {
  border-color: #ffd6d9;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.top-rank {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 13px;
  flex: none;
}

.top-img {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  object-fit: cover;
  background: #f7f7f7;
  flex: none;
  border: 1px solid var(--border-light);
}

.top-info {
  min-width: 0;
  flex: 1;
}

.top-name {
  font-weight: 700;
  font-size: 14px;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-sold {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

/* ============ 商品列表区域 ============ */
.goods-list-section {
  padding: 36px 0 48px;
  background: var(--bg-section);
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
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: 999px;
  background: #fff;
}

.bulk-tip {
  font-size: 12px;
  color: var(--text-main);
  font-weight: 700;
}

/* ============ 商品网格 ============ */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.goods-card {
  border: 1px solid var(--border-light);
  border-radius: 16px;
  background: #fff;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}

.goods-card:hover {
  border-color: #f1d7da;
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.055);
  transform: translateY(-2px);
}

.goods-card-media {
  position: relative;
  height: 0;
  padding-top: 100%;
  background: #f7f7f7;
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
  border: 1px solid rgba(238, 238, 238, 0.95);
}

.goods-card-img {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
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
  flex-wrap: wrap;
  gap: 6px;
  max-width: calc(100% - 60px);
}

.tag {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: #f3f3f3;
  color: var(--text-secondary);
  line-height: 1.4;
}

.t-on { background: #e9f8ef; color: #2f9d62; }
.t-off { background: #f3f3f3; color: #777; }
.t-pass { background: #ecf8f0; color: #35a96d; }
.t-wait { background: #fff4df; color: #b57913; }
.t-fail { background: #fff0f1; color: var(--brand-red); }
.t-plan { background: #fff5f6; color: var(--brand-red); }
.t-ban { background: #fff0f1; color: var(--brand-red); }

.goods-card-body {
  padding: 14px 15px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.goods-card-name {
  font-weight: 800;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  min-height: 40px;
}

.goods-card-desc {
  color: var(--text-secondary);
  line-height: 1.4;
  font-size: 13px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  min-height: 36px;
}

.goods-card-price {
  color: var(--brand-red);
  font-size: 17px;
  font-weight: 900;
}

.goods-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  color: var(--text-muted);
  font-size: 12px;
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
  padding: 10px 14px 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  align-items: stretch;
}

.goods-card-actions :deep(.el-button) {
  width: 100%;
  border-radius: var(--radius-pill);
  height: 30px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 700;
}

.goods-card-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* ============ 按钮样式覆写 ============ */
.goods-page :deep(.el-button--primary.is-plain) {
  color: var(--brand-red);
  border-color: rgba(230, 0, 18, 0.45);
  background: #fff;
}

.goods-page :deep(.el-button--primary.is-plain:hover) {
  color: #fff;
  border-color: var(--brand-red);
  background: var(--brand-red);
}

.goods-page :deep(.el-button--success.is-plain),
.goods-page :deep(.el-button--warning.is-plain) {
  color: var(--text-secondary);
  border-color: var(--border-light);
  background: #fff;
}

.goods-page :deep(.el-button--success.is-plain:hover),
.goods-page :deep(.el-button--warning.is-plain:hover) {
  color: var(--brand-red);
  border-color: #ffd6d9;
  background: #fff7f8;
}

.goods-page :deep(.el-button--danger.is-plain) {
  color: var(--brand-red);
  border-color: #ffd6d9;
  background: #fff;
}

.goods-page :deep(.el-button--danger.is-plain:hover) {
  color: var(--brand-red);
  border-color: var(--brand-red);
  background: #fff5f6;
}

/* ============ Footer ============ */
.goods-footer {
  background: var(--bg-section);
  border-top: 1px solid var(--border-light);
  padding: 48px 0 40px;
}

.footer-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.footer-brand img {
  width: 120px;
  height: auto;
}

.footer-brand span {
  font-size: 13px;
  color: var(--text-muted);
  padding-left: 12px;
  border-left: 1px solid var(--border-light);
}

.footer-nav {
  display: flex;
  gap: 28px;
  flex-wrap: wrap;
  justify-content: center;
}

.footer-nav a {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  transition: color 0.2s;
}

.footer-nav a:hover {
  color: var(--brand-red);
}

.footer-copy {
  margin: 0;
  color: #bbb;
  font-size: 12px;
}

/* ============ 响应式 ============ */
@media (max-width: 1180px) {
  .goods-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .top-grid,
  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .top-grid,
  .goods-grid {
    grid-template-columns: 1fr;
  }

  .goods-list-section {
    padding: 24px 0 32px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .footer-nav {
    gap: 14px 22px;
  }
}
</style>
