<template>
  <div class="appeal-page">
    <section class="merchant-page-hero">
      <div class="merchant-page-container">
        <div class="merchant-page-hero-inner">
          <span class="merchant-page-kicker">REVIEW APPEAL</span>
          <h1 class="merchant-page-title">评价申诉</h1>
          <p class="merchant-page-desc">查看违规评价判定结果，提交证据并跟进申诉处理状态</p>
        </div>
      </div>
    </section>
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="head">
          <div>
            <div class="title">评价申诉中心</div>
            <div class="sub">仅展示被判定为违规（不展示）的评价，可发起申诉</div>
          </div>
          <div class="actions">
            <el-button plain @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="filters.goodsId" clearable placeholder="筛选商品" style="width: 260px">
          <el-option v-for="g in goodsOptions" :key="g.id" :label="String(g.name || '商品')" :value="g.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="申诉状态" style="width: 160px">
          <el-option label="全部" :value="-1" />
          <el-option label="未申诉" :value="9" />
          <el-option label="申诉中" :value="0" />
          <el-option label="申诉通过" :value="1" />
          <el-option label="申诉驳回" :value="2" />
        </el-select>
        <el-input v-model="filters.keyword" clearable placeholder="搜索评价内容/用户昵称" style="width: 260px" />
      </div>

      <el-empty v-if="!displayList.length" description="暂无可申诉的违规评价" />

      <el-table v-else :data="displayList" style="width: 100%" row-key="id">
        <el-table-column prop="commentTime" label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.commentTime) }}</template>
        </el-table-column>
        <el-table-column label="商品" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="goods-cell">
              <div class="g-name">{{ goodsName(row.goodsId) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ displayName(row) }}</template>
        </el-table-column>
        <el-table-column label="评分" width="110">
          <template #default="{ row }">{{ Number(row.goodsScore ?? 0) || 0 }}</template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content }}</template>
        </el-table-column>
        <el-table-column label="申诉状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusType(row)" effect="light">{{ statusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="warning" plain :disabled="Number(row.appealStatus) === 0" @click="openAppeal(row)">
              {{ Number(row.appealStatus) === 0 ? '申诉中' : '发起申诉' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="违规评价详情" width="760px" destroy-on-close>
      <div v-if="currentDetail" class="detail">
        <div class="detail-row">
          <span class="k">商品</span>
          <span class="v">{{ goodsName(currentDetail.goodsId) }}</span>
        </div>
        <div class="detail-row">
          <span class="k">用户</span>
          <span class="v">{{ displayName(currentDetail) }}</span>
        </div>
        <div class="detail-row">
          <span class="k">时间</span>
          <span class="v">{{ formatDateTime(currentDetail.commentTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="k">评分</span>
          <span class="v">{{ Number(currentDetail.goodsScore ?? 0) || 0 }}</span>
        </div>
        <div class="detail-row">
          <span class="k">内容</span>
          <span class="v pre">{{ String(currentDetail.content || '') }}</span>
        </div>
        <div v-if="commentPicsResolved(currentDetail).length" class="detail-row">
          <span class="k">图片</span>
          <div class="pics">
            <el-image
              v-for="(u, idx) in commentPicsResolved(currentDetail)"
              :key="`${currentDetail.id}-${idx}`"
              class="pic"
              :src="u"
              :preview-src-list="commentPicsResolved(currentDetail)"
              :initial-index="idx"
              fit="cover"
              preview-teleported
            />
          </div>
        </div>
        <div class="detail-row">
          <span class="k">申诉状态</span>
          <span class="v">
            <el-tag :type="statusType(currentDetail)" effect="light">{{ statusText(currentDetail) }}</el-tag>
          </span>
        </div>
        <div v-if="Number(currentDetail.appealStatus) === 2 && String(currentDetail.appealHandleRemark || '').trim()" class="detail-row">
          <span class="k">驳回原因</span>
          <span class="v pre">{{ currentDetail.appealHandleRemark }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealVisible" title="发起申诉" width="560px" destroy-on-close>
      <el-form :model="appealForm" label-position="top">
        <el-form-item label="申诉理由">
          <el-input v-model="appealForm.reason" type="textarea" :rows="4" placeholder="请说明该评价为何不应判定为违规" />
        </el-form-item>
        <el-form-item label="申诉证据（图片URL或上传，多个用逗号分隔）">
          <el-input v-model="appealForm.evidence" placeholder="例如：https://.../a.png,https://.../b.png" />
          <div class="upload-row">
            <el-upload action="" :http-request="uploadAppealImage" :show-file-list="false" accept="image/*">
              <el-button>上传图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { goodsApi, goodsCommentApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const route = useRoute()
const router = useRouter()

const defaultImage = 'https://via.placeholder.com/520x520?text=Image'
const commentList = ref([])
const goodsOptions = ref([])
const goodsNameMap = ref({})

const filters = reactive({
  goodsId: null,
  status: -1,
  keyword: ''
})

const detailVisible = ref(false)
const currentDetail = ref(null)

const appealVisible = ref(false)
const currentAppeal = ref(null)
const appealForm = reactive({ reason: '', evidence: '' })

const getErrorMessage = (error, fallback) => error?.response?.data?.message || fallback

const resolveMedia = (src) => {
  const v0 = String(src || '').trim()
  const v = v0.replaceAll('\\', '/')
  if (!v) return ''
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('/goods/')) return v
  return ''
}

const displayName = (c) => {
  if (Number(c?.isAnonymous) === 1) return '匿名用户'
  const nick = String(c?.userNickname || '').trim()
  if (nick) return nick
  const userId = c?.userId != null ? String(c.userId) : ''
  return userId ? `用户${userId}` : '用户'
}

const formatDateTime = (t) => {
  const s = String(t || '').trim()
  if (!s) return '-'
  const d = new Date(s.includes('T') ? s : s.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return s
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}

const commentPics = (c) => {
  const arr = Array.isArray(c?.commentPics) ? c.commentPics : []
  if (arr.length) return arr
  const raw = String(c?.commentPic || '').trim()
  if (!raw) return []
  return raw.split(',').map(s => String(s || '').trim()).filter(Boolean)
}

const commentPicsResolved = (c) => {
  return commentPics(c).map(u => resolveMedia(u)).filter(Boolean)
}

const goodsName = (goodsId) => {
  const id = Number(goodsId)
  if (!Number.isFinite(id)) return '-'
  return goodsNameMap.value[String(id)] || '商品'
}

const statusText = (row) => {
  const s = Number(row?.appealStatus)
  if (s === 0) return '申诉中'
  if (s === 1) return '申诉通过'
  if (s === 2) return '申诉驳回'
  return '未申诉'
}

const statusType = (row) => {
  const s = Number(row?.appealStatus)
  if (s === 0) return 'warning'
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'info'
}

const buildGoodsNameMap = (goods) => {
  const map = {}
  ;(goods || []).forEach(g => {
    const id = g?.id
    if (id == null) return
    map[String(id)] = String(g?.name || '').trim() || '商品'
  })
  goodsNameMap.value = map
}

const loadGoods = async () => {
  try {
    const res = await goodsApi.list(getMerchantId())
    const list = Array.isArray(res.data) ? res.data : []
    goodsOptions.value = list
    buildGoodsNameMap(list)
  } catch (e) {
    goodsOptions.value = []
    goodsNameMap.value = {}
  }
}

const loadComments = async () => {
  try {
    const res = await goodsCommentApi.list(getMerchantId(), 1)
    const all = Array.isArray(res.data) ? res.data : []
    commentList.value = all.filter(c => Number(c?.isValid) === 0)
  } catch (e) {
    commentList.value = []
    ElMessage.error(getErrorMessage(e, '违规评价加载失败'))
  }
}

const applyRoutePreset = () => {
  const qGoodsId = Number(route.query?.goodsId)
  if (Number.isFinite(qGoodsId) && qGoodsId > 0) {
    filters.goodsId = qGoodsId
  }
}

const load = async () => {
  await loadGoods()
  await loadComments()
  applyRoutePreset()
}

const displayList = computed(() => {
  const goodsId = filters.goodsId == null ? null : Number(filters.goodsId)
  const status = Number(filters.status)
  const kw = String(filters.keyword || '').trim()

  return (commentList.value || [])
    .filter(c => {
      if (goodsId != null && Number(c?.goodsId) !== goodsId) return false
      const s = Number(c?.appealStatus)
      if (status === 9) return !(s === 0 || s === 1 || s === 2)
      if (status === -1) return true
      return s === status
    })
    .filter(c => {
      if (!kw) return true
      const hay = [c?.content, c?.userNickname, c?.userId].filter(v => v != null).map(v => String(v))
      return hay.some(v => v.includes(kw))
    })
    .slice()
    .sort((a, b) => String(b?.commentTime || '').localeCompare(String(a?.commentTime || '')))
})

const openDetail = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const openAppeal = (row) => {
  currentAppeal.value = row
  appealForm.reason = ''
  appealForm.evidence = ''
  appealVisible.value = true
}

const uploadAppealImage = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    const resolved = resolveMedia(url) || url
    if (resolved) {
      appealForm.evidence = [appealForm.evidence, resolved].filter(Boolean).join(',')
    }
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(getErrorMessage(e, '上传失败'))
  }
}

const submitAppeal = async () => {
  const row = currentAppeal.value
  if (!row?.id) return
  const reason = String(appealForm.reason || '').trim()
  if (!reason) {
    ElMessage.warning('请填写申诉理由')
    return
  }
  try {
    await goodsCommentApi.appeal(row.id, getMerchantId(), {
      reason,
      evidence: String(appealForm.evidence || '').trim()
    })
    ElMessage.success('申诉已提交')
    appealVisible.value = false
    await loadComments()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '申诉提交失败'))
  }
}

onMounted(load)
</script>

<style scoped>
.appeal-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-card :deep(.el-card__header) {
  padding: 14px 16px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.goods-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.g-name {
  font-weight: 700;
  color: #111827;
}

.g-sub {
  font-size: 12px;
  color: #6b7280;
}

.detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
}

.k {
  color: #6b7280;
  font-size: 12px;
}

.v {
  color: #111827;
}

.pre {
  white-space: pre-wrap;
  line-height: 1.7;
}

.pics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.pic {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 10px;
  overflow: hidden;
}

.upload-row {
  margin-top: 8px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
