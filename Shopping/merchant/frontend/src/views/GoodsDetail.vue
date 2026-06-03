<template>
  <div class="goods-detail">
    <div class="page-head">
      <el-button text @click="goBack">返回</el-button>
      <div class="head-title">{{ isCreate ? '发布商品' : '商品详情' }}</div>
      <div class="head-actions">
        <el-date-picker
          v-if="form.status !== 1"
          v-model="planOnTime"
          type="datetime"
          placeholder="定时上架（可选）"
          value-format="YYYY-MM-DDTHH:mm:ss"
          class="plan-picker"
        />
        <el-button v-if="!isCreate && form.status !== 3" plain @click="toggleShelf">{{ form.status === 1 ? '下架' : '上架' }}</el-button>
        <el-button v-if="!isCreate && form.status === 3" type="warning" plain @click="submitReview">提交复审</el-button>
        <el-button type="primary" @click="save">{{ isCreate ? '发布' : '保存' }}</el-button>
      </div>
    </div>

    <div class="detail-grid">
      <el-card shadow="never" class="media-card">
        <div class="media-wrap">
          <div class="thumbs">
            <div
              class="thumb thumb-video"
              :class="{ active: mediaMode === 'video' }"
              @click="setMedia('video')"
            >
              <div class="thumb-video-badge">视频</div>
            </div>
            <div
              v-for="(u, idx) in galleryUrls"
              :key="u + ':' + idx"
              class="thumb"
              :class="{ active: mediaMode === 'gallery' && idx === activeIndex }"
              @click="selectImage(idx)"
            >
              <img :src="resolveImg(u)" alt="" @error="onThumbError" />
            </div>
          </div>

          <div class="media-main">
            <video
              v-if="mediaMode === 'video'"
              class="main-video"
              controls
              :src="resolveMedia(form.goodsVideo)"
            ></video>
            <img
              v-else-if="mediaMode === 'gallery'"
              class="main-img"
              :src="resolveImg(currentImage)"
              alt=""
              @error="onImgError"
            />
            <div v-else class="main-param">
              <el-descriptions :column="1" border>
                <el-descriptions-item v-for="(d, idx) in detailList" :key="idx" :label="d.key || '-'">
                  {{ d.value || '-' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="media-tabs">
              <div class="tab-btn" :class="{ active: mediaMode === 'video' }" @click="setMedia('video')">视频</div>
              <div class="tab-btn" :class="{ active: mediaMode === 'gallery' }" @click="setMedia('gallery')">图集</div>
              <div class="tab-btn" :class="{ active: mediaMode === 'param' }" @click="setMedia('param')">参数</div>
            </div>
          </div>
        </div>

        <div class="media-actions">
          <div v-if="mediaMode === 'gallery'" class="media-action-row">
            <el-upload action="" :http-request="uploadGalleryImage" :show-file-list="false" accept="image/*" multiple>
              <el-button>添加图片</el-button>
            </el-upload>
            <el-button plain :disabled="!galleryUrls.length" @click="setAsMain">设为主图</el-button>
            <el-button type="danger" plain :disabled="!galleryUrls.length" @click="removeCurrentImage">删除当前</el-button>
          </div>

          <div v-else-if="mediaMode === 'video'" class="media-action-row">
            <el-upload action="" :http-request="uploadVideo" :show-file-list="false" accept="video/*">
              <el-button>上传视频</el-button>
            </el-upload>
            <el-input v-model="form.goodsVideo" placeholder="也可以粘贴视频 URL" />
          </div>

          <div v-else class="media-action-row">
            <span class="tip">参数在右侧“详细信息”里编辑</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="info-card">
        <el-form :model="form" label-position="top">
          <el-form-item label="商品名称">
            <el-input v-model="form.name" maxlength="128" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" class="full-width" placeholder="请选择分类">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="发货地">
            <el-input v-model="form.shipFrom" maxlength="128" placeholder="例如：广东省深圳市" />
          </el-form-item>
          <el-form-item label="商品描述">
            <el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>

        <div class="block-title">库存与规格</div>
        <div class="sku-actions">
          <el-button size="small" @click="addSku">添加规格</el-button>
          <el-button size="small" plain @click="resetDefaultSku">恢复单规格</el-button>
        </div>
        <el-table :data="skuList" size="small" class="sku-table">
          <el-table-column prop="spec" label="规格/型号/尺码" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.spec" />
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" />
            </template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ $index }">
              <el-button size="small" type="danger" text @click="removeSku($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="block-title">详细信息（可选）</div>
        <div class="kv-actions">
          <el-button size="small" @click="addDetail">添加一行</el-button>
        </div>
        <div v-for="(d, idx) in detailList" :key="idx" class="kv-row">
          <el-input v-model="d.key" placeholder="字段名，例如：材质/颜色/适用人群" class="kv-key" />
          <el-input v-model="d.value" placeholder="字段值" class="kv-val" />
          <el-button type="danger" text @click="removeDetail(idx)">删除</el-button>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="tab-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="商品详情" name="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="分类">{{ categoryName(form.categoryId) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <span :style="{ color: form.status === 3 ? '#f56c6c' : '' }">{{ form.status === 1 ? '上架中' : form.status === 3 ? '平台下架' : '未上架' }}</span>
              <div v-if="form.status === 3 && form.auditRemark" style="color:#f56c6c;font-size:12px;margin-top:4px">下架原因：{{ form.auditRemark }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="审核">
              <span :style="{ color: form.auditStatus === 2 ? '#f56c6c' : '' }">{{ form.auditStatus === 1 ? '已通过' : form.auditStatus === 2 ? '已拒绝' : '待审核' }}</span>
              <div v-if="form.auditStatus === 2 && form.auditRemark" style="color:#f56c6c;font-size:12px;margin-top:4px">拒绝原因：{{ form.auditRemark }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="用户评价" name="comment">
          <div class="comment-toolbar">
            <el-button type="warning" plain @click="goAppealCenter">评价申诉中心</el-button>
          </div>

          <el-empty v-if="!commentList.length" description="暂无评价" />

          <div v-else class="comment-list">
            <div
              v-for="c in commentList"
              :key="c.id"
              class="comment-card"
              :class="{ top: Number(c.isTop) === 1 }"
            >
              <div class="c-head">
                <div class="c-user">
                  <div class="c-avatar">
                    <img v-if="commentAvatar(c)" class="c-avatar-img" :src="commentAvatar(c)" alt="" @error="onThumbError" />
                    <span v-else>{{ avatarText(displayName(c)) }}</span>
                  </div>
                  <div class="c-meta">
                    <div class="c-name">
                      <span class="name">{{ displayName(c) }}</span>
                      <span v-if="Number(c.isTop) === 1" class="tag">置顶</span>
                    </div>
                    <div class="c-time">{{ formatDateTime(c.commentTime) }}</div>
                  </div>
                </div>
                <div class="c-actions" @click.stop>
                  <el-button size="small" @click="replyToComment(c)">回复</el-button>
                  <el-button size="small" plain :disabled="!canTop(c)" @click="toggleTop(c)">{{ Number(c.isTop) === 1 ? '取消置顶' : '置顶' }}</el-button>
                </div>
              </div>

              <div class="c-score">
                <div class="score-main">
                  <span class="score-label">商品</span>
                  <el-rate :model-value="Number(c.goodsScore ?? c.rating ?? 0)" disabled />
                  <span class="score-num">{{ Number(c.goodsScore ?? c.rating ?? 0) }}</span>
                </div>
                <div class="score-sub">
                  <span>服务 {{ Number.isFinite(Number(c.serviceScore)) ? Number(c.serviceScore) : '-' }}</span>
                  <span>物流 {{ Number.isFinite(Number(c.logisticsScore)) ? Number(c.logisticsScore) : '-' }}</span>
                </div>
              </div>

              <div v-if="String(c.content || '').trim()" class="c-content">{{ c.content }}</div>

              <div v-if="commentPicsResolved(c).length" class="c-pics">
                <el-image
                  v-for="(u, idx) in commentPicsResolved(c)"
                  :key="`${c.id}-${idx}`"
                  class="c-pic"
                  :src="u"
                  :preview-src-list="commentPicsResolved(c)"
                  :initial-index="idx"
                  fit="cover"
                  preview-teleported
                />
              </div>

              <div v-if="String(c.merchantReply || '').trim()" class="c-reply">
                <div class="reply-title">商家回复</div>
                <div class="reply-list">
                  <div v-for="(r, idx) in merchantReplies(c)" :key="`${c.id}-r-${idx}`" class="reply-item">
                    <div class="reply-head">
                      <span class="reply-time">{{ r.time }}</span>
                      <el-button text size="small" type="danger" @click.stop="deleteReplyItem(c, idx)">删除</el-button>
                    </div>
                    <div class="reply-text">{{ r.text }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-dialog v-model="replyVisible" title="回复评价" width="520px" destroy-on-close>
            <el-form :model="replyForm" label-width="80px">
              <el-form-item label="回复内容">
                <el-input v-model="replyForm.reply" type="textarea" :rows="4" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="replyVisible = false">取消</el-button>
              <el-button type="primary" @click="submitReply">提交</el-button>
            </template>
          </el-dialog>

          <el-dialog v-model="appealVisible" title="违规评价申诉" width="560px" destroy-on-close>
            <el-form :model="appealForm" label-position="top">
              <el-form-item label="申诉理由">
                <el-input v-model="appealForm.reason" type="textarea" :rows="4" placeholder="请说明评价为何不应判定为违规" />
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
              <el-button type="primary" @click="submitAppeal">提交申诉</el-button>
            </template>
          </el-dialog>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categoryApi, goodsApi, goodsCommentApi, goodsSkuApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'
import { DEFAULT_ANONYMOUS_AVATAR, DEFAULT_USER_AVATAR, resolveAvatar } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()

const defaultImage = 'https://via.placeholder.com/520x520?text=Goods'
const categories = ref([])
const skuList = ref([])
const detailList = ref([])
const activeTab = ref('detail')

const commentList = ref([])
const replyVisible = ref(false)
const currentComment = ref(null)
const replyForm = reactive({ reply: '' })

const appealVisible = ref(false)
const currentAppealComment = ref(null)
const appealForm = reactive({ reason: '', evidence: '' })

const isCreate = computed(() => route.name === 'GoodsCreate')
const goodsId = computed(() => (isCreate.value ? null : Number(route.params.id)))

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

const planOnTime = ref('')

const form = reactive({
  id: null,
  merchantId: getMerchantId(),
  categoryId: 1,
  name: '',
  description: '',
  shipFrom: '',
  goodsPic: '',
  goodsVideo: '',
  status: 0,
  auditStatus: 0,
  auditRemark: ''
})

const getErrorMessage = (error, fallback) => error?.response?.data?.message || fallback

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

const onThumbError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

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

const mediaMode = ref('gallery')
const galleryUrls = ref([])
const activeIndex = ref(0)

const currentImage = computed(() => {
  if (!galleryUrls.value.length) return ''
  const idx = Math.min(Math.max(activeIndex.value, 0), galleryUrls.value.length - 1)
  return galleryUrls.value[idx] || ''
})

const setMedia = (mode) => {
  if (mode === 'video') {
    mediaMode.value = 'video'
    return
  }
  if (mode === 'param') {
    mediaMode.value = 'param'
    return
  }
  mediaMode.value = 'gallery'
}

const normalizeUrls = (urls) => {
  const seen = new Set()
  const res = []
  ;(urls || []).forEach(u => {
    const v = String(u || '').trim()
    if (!v) return
    if (seen.has(v)) return
    seen.add(v)
    res.push(v)
  })
  return res
}

const selectImage = (idx) => {
  mediaMode.value = 'gallery'
  activeIndex.value = idx
}

const setAsMain = () => {
  if (!galleryUrls.value.length) return
  const idx = Math.min(Math.max(activeIndex.value, 0), galleryUrls.value.length - 1)
  const picked = galleryUrls.value[idx]
  galleryUrls.value = normalizeUrls([picked, ...galleryUrls.value.filter((_, i) => i !== idx)])
  activeIndex.value = 0
}

const removeCurrentImage = () => {
  if (!galleryUrls.value.length) return
  const idx = Math.min(Math.max(activeIndex.value, 0), galleryUrls.value.length - 1)
  galleryUrls.value.splice(idx, 1)
  if (activeIndex.value >= galleryUrls.value.length) activeIndex.value = Math.max(0, galleryUrls.value.length - 1)
}

const uploadGalleryImage = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    // 使用相对路径 path（/uploads/images/xxx.jpg）而非完整 url，保证跨端口可用
    const url = res?.data?.path || res?.data?.url || ''
    if (url) {
      galleryUrls.value = normalizeUrls([...galleryUrls.value, url])
      activeIndex.value = galleryUrls.value.length - 1
      mediaMode.value = 'gallery'
    }
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (error) {
    options?.onError?.(error)
    ElMessage.error(getErrorMessage(error, '上传失败'))
  }
}

const uploadVideo = async (options) => {
  try {
    const res = await uploadApi.uploadVideo(options.file)
    // 使用相对路径 path 而非完整 url
    form.goodsVideo = res?.data?.path || res?.data?.url || ''
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
    mediaMode.value = 'video'
  } catch (error) {
    options?.onError?.(error)
    ElMessage.error(getErrorMessage(error, '上传失败'))
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.list()
    categories.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    categories.value = []
  }
}

const categoryName = (categoryId) => {
  const id = Number(categoryId)
  const found = categories.value.find(c => Number(c.id) === id)
  return found ? found.name : String(categoryId ?? '-')
}

const resetDefaultSku = () => {
  skuList.value = [{ spec: '默认', price: 0, stock: 0 }]
}

const addSku = () => {
  skuList.value.push({ spec: '', price: 0, stock: 0 })
}

const removeSku = (index) => {
  skuList.value.splice(index, 1)
  if (!skuList.value.length) resetDefaultSku()
}

const detailsToObject = (list) => {
  const obj = {}
  ;(list || []).forEach(item => {
    const k = String(item.key || '').trim()
    if (!k) return
    obj[k] = String(item.value || '').trim()
  })
  return obj
}

const objectToDetails = (specParams) => {
  try {
    const raw = typeof specParams === 'string' ? specParams : ''
    if (!raw) return []
    const obj = JSON.parse(raw)
    if (!obj || typeof obj !== 'object') return []
    return Object.keys(obj).map(k => ({ key: k, value: String(obj[k] ?? '') }))
  } catch (e) {
    return []
  }
}

const addDetail = () => {
  detailList.value.push({ key: '', value: '' })
}

const removeDetail = (idx) => {
  detailList.value.splice(idx, 1)
}

const uploadGoodsPic = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    form.goodsPic = res?.data?.url || ''
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (error) {
    options?.onError?.(error)
    ElMessage.error(getErrorMessage(error, '上传失败'))
  }
}

const loadSkus = async (id) => {
  try {
    const res = await goodsSkuApi.list(id)
    const list = Array.isArray(res.data) ? res.data : []
    if (list.length) {
      skuList.value = list.map(s => ({
        id: s.id,
        spec: s.spec || '默认',
        price: Number(s.price || 0),
        stock: Number(s.stock || 0),
        specParams: s.specParams || ''
      }))
      detailList.value = objectToDetails(skuList.value[0]?.specParams)
    } else {
      resetDefaultSku()
      detailList.value = []
    }
  } catch (e) {
    resetDefaultSku()
    detailList.value = []
  }
}

const loadComments = async () => {
  if (!goodsId.value) {
    commentList.value = []
    return
  }
  try {
    const res = await goodsCommentApi.list(getMerchantId(), 0)
    const all = Array.isArray(res.data) ? res.data : []
    commentList.value = all.filter(c => Number(c.goodsId) === Number(goodsId.value) && Number(c.isValid) === 1)
  } catch (e) {
    commentList.value = []
    ElMessage.error(getErrorMessage(e, '评价加载失败'))
  }
}

const goAppealCenter = () => {
  if (!goodsId.value) {
    router.push('/comment-appeal')
    return
  }
  router.push({ path: '/comment-appeal', query: { goodsId: String(goodsId.value) } })
}

const appealTag = (c) => {
  const s = Number(c?.appealStatus)
  if (s === 0) return '申诉中'
  if (s === 1) return '申诉通过'
  if (s === 2) return '申诉驳回'
  return ''
}

const canTop = (c) => {
  if (!c) return false
  if (Number(c.isTop) === 1) return true
  if (Number(c.isValid) !== 1) return false
  const score = Number(c.goodsScore ?? c.rating ?? 0)
  return Number.isFinite(score) && score >= 4
}

const replyToComment = (row) => {
  currentComment.value = row
  replyForm.reply = ''
  replyVisible.value = true
}

const deleteReplyItem = async (row, index) => {
  try {
    await goodsCommentApi.deleteReply(row.id, index)
    ElMessage.success('已删除')
    await loadComments()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除失败'))
  }
}

const toggleTop = async (row) => {
  try {
    const next = Number(row.isTop) === 1 ? 0 : 1
    const res = await goodsCommentApi.setTop(row.id, next)
    const ok = Boolean(res?.data)
    if (!ok) {
      ElMessage.warning('仅支持有效且4星及以上评价置顶')
      return
    }
    row.isTop = next
    ElMessage.success(next === 1 ? '已置顶' : '已取消置顶')
    await loadComments()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

const openAppeal = (row) => {
  currentAppealComment.value = row
  appealForm.reason = ''
  appealForm.evidence = ''
  appealVisible.value = true
}

const uploadAppealImage = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    if (url) {
      appealForm.evidence = [appealForm.evidence, url].filter(Boolean).join(',')
    }
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.response?.data?.message || '上传失败')
  }
}

const submitAppeal = async () => {
  const row = currentAppealComment.value
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
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申诉提交失败'))
  }
}

const submitReply = async () => {
  try {
    await goodsCommentApi.reply(currentComment.value.id, replyForm.reply)
    ElMessage.success('回复成功')
    replyVisible.value = false
    await loadComments()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '回复失败'))
  }
}

const loadGoods = async () => {
  if (isCreate.value) {
    form.id = null
    form.merchantId = getMerchantId()
    form.categoryId = categories.value[0]?.id || 1
    form.name = ''
    form.description = ''
    form.shipFrom = ''
    form.goodsPic = ''
    form.goodsVideo = ''
    form.status = 0
    form.auditStatus = 0
    form.auditRemark = ''
    resetDefaultSku()
    detailList.value = []
    galleryUrls.value = []
    activeIndex.value = 0
    mediaMode.value = 'gallery'
    planOnTime.value = ''
    return
  }

  try {
    const res = await goodsApi.detail(goodsId.value)
    const g = res?.data
    if (!g) return
    form.id = g.id
    form.merchantId = g.merchantId
    form.categoryId = g.categoryId
    form.name = g.name || ''
    form.description = g.description || ''
    form.shipFrom = g.shipFrom || ''
    form.goodsPic = g.goodsPic || ''
    form.goodsVideo = g.goodsVideo || ''
    form.status = g.status ?? 0
    form.auditStatus = g.auditStatus ?? 0
    form.auditRemark = g.auditRemark || ''
    const picsRes = await goodsApi.listPics(form.id)
    const pics = Array.isArray(picsRes?.data) ? picsRes.data : []
    const urls = pics.map(p => p.url).filter(Boolean)
    galleryUrls.value = normalizeUrls([form.goodsPic, ...urls])
    activeIndex.value = 0
    await loadSkus(form.id)
    await loadComments()

    const map = loadScheduleMap(getMerchantId())
    planOnTime.value = String(map[String(form.id)] || '')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '商品加载失败'))
  }
}

const save = async () => {
  const name = String(form.name || '').trim()
  if (!name) {
    ElMessage.warning('请先填写商品名称')
    return
  }
  try {
    form.merchantId = getMerchantId()
    let id = form.id
    if (isCreate.value) {
      const created = await goodsApi.create({
        merchantId: form.merchantId,
        categoryId: form.categoryId,
        name: name,
        description: form.description,
        shipFrom: form.shipFrom,
        goodsPic: galleryUrls.value[0] || form.goodsPic,
        goodsVideo: form.goodsVideo
      })
      id = created?.data?.id
    } else {
      await goodsApi.update({
        id: form.id,
        merchantId: form.merchantId,
        categoryId: form.categoryId,
        name: name,
        description: form.description,
        shipFrom: form.shipFrom,
        goodsPic: galleryUrls.value[0] || form.goodsPic,
        goodsVideo: form.goodsVideo
      })
    }

    if (id) {
      const gallery = normalizeUrls(galleryUrls.value)
      await goodsApi.replacePics(
        id,
        gallery.map((u, idx) => ({ url: u, sort: idx }))
      )
      const detailObj = detailsToObject(detailList.value)
      const payload = (skuList.value.length ? skuList.value : []).map(s => ({
        spec: String(s.spec || '').trim() || '默认',
        price: s.price ?? 0,
        stock: s.stock ?? 0,
        specParams: JSON.stringify(detailObj)
      }))
      await goodsSkuApi.replaceByGoodsId(id, payload)
    }

    if (id) {
      const merchantId = getMerchantId()
      const map = loadScheduleMap(merchantId)
      const key = String(id)
      const planned = String(planOnTime.value || '').trim()
      if (planned) map[key] = planned
      else delete map[key]
      saveScheduleMap(merchantId, map)
    }

    ElMessage.success(isCreate.value ? '发布成功' : '保存成功')
    if (isCreate.value && id) {
      router.replace({ path: `/goods/${id}` })
    } else {
      await loadGoods()
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存失败'))
  }
}

const toggleShelf = async () => {
  if (!form.id) return
  try {
    const next = form.status === 1 ? 0 : 1
    await goodsApi.updateStatus(form.id, next)
    form.status = next
    const merchantId = getMerchantId()
    const map = loadScheduleMap(merchantId)
    delete map[String(form.id)]
    saveScheduleMap(merchantId, map)
    planOnTime.value = ''
    ElMessage.success(next === 1 ? '已上架' : '已下架')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

const submitReview = async () => {
  if (!form.id) return
  try {
    await ElMessageBox.confirm('提交后商品将重新进入平台审核，确认提交复审？', '提交复审', { type: 'warning' })
    await goodsApi.updateStatus(form.id, 1)
    form.status = 0
    form.auditStatus = 0
    form.auditRemark = ''
    ElMessage.success('已提交平台复审')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

const goBack = () => {
  router.push('/goods')
}

watch(
  () => route.query.tab,
  (v) => {
    if (v === 'comment') activeTab.value = 'comment'
    else activeTab.value = 'detail'
  },
  { immediate: true }
)

watch(
  () => activeTab.value,
  (v) => {
    if (v === 'comment') loadComments()
  }
)

onMounted(async () => {
  await loadCategories()
  await loadGoods()
})

const displayName = (c) => {
  if (Number(c?.isAnonymous) === 1) return '匿名用户'
  const nick = String(c?.userNickname || '').trim()
  if (nick) return nick
  const userId = c?.userId != null ? String(c.userId) : ''
  return userId ? `用户${userId}` : '用户'
}

const commentAvatar = (c) => {
  if (Number(c?.isAnonymous) === 1) return DEFAULT_ANONYMOUS_AVATAR
  return resolveAvatar(c?.userAvatar, DEFAULT_USER_AVATAR)
}

const avatarText = (name) => {
  const v = String(name || '').trim()
  return v ? v[0] : '匿'
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

const formatDateTime = (t) => {
  const d = t ? new Date(t) : new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}

const merchantReplies = (c) => {
  const raw = String(c?.merchantReply || '').trim()
  if (!raw) return []
  const lines = raw.split('\n').map(s => String(s || '').trim()).filter(Boolean)
  const res = []
  for (const line of lines) {
    if (line.startsWith('【') && line.includes('】')) {
      const end = line.indexOf('】')
      const time = line.slice(1, end).trim()
      const text = line.slice(end + 1).trim()
      if (time && text) {
        res.push({ time, text })
        continue
      }
    }
    const tab = line.indexOf('\t')
    if (tab > 0) {
      const time = line.slice(0, tab).trim()
      const text = line.slice(tab + 1).trim()
      if (time && text) {
        res.push({ time, text })
        continue
      }
    }
    res.push({ time: c?.replyTime ? formatDateTime(c.replyTime) : '-', text: line })
  }
  return res
}
</script>

<style scoped>
.goods-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.comment-card {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
}

.comment-card.top {
  border-color: rgba(239, 68, 68, 0.35);
  box-shadow: 0 8px 22px rgba(17, 24, 39, 0.06);
}

.c-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.c-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.c-avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  overflow: hidden;
  flex: 0 0 auto;
}

.c-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.c-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.c-name {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1;
}

.c-name .name {
  font-weight: 800;
  color: #111827;
}

.c-name .tag {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  font-size: 12px;
  font-weight: 700;
}

.c-name .tag.warn {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
}

.c-appeal-remark {
  margin-top: 10px;
  border-radius: 10px;
  padding: 10px;
  border: 1px solid rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.06);
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.upload-row {
  margin-top: 8px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.c-time {
  font-size: 12px;
  color: #6b7280;
}

.c-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.c-score {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.score-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-size: 12px;
  color: #6b7280;
}

.score-num {
  font-size: 12px;
  color: #111827;
  font-weight: 700;
}

.score-sub {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #6b7280;
}

.c-content {
  margin-top: 10px;
  color: #111827;
  line-height: 1.6;
  white-space: pre-wrap;
}

.c-pics {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.c-pic {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 10px;
  overflow: hidden;
}

.c-reply {
  margin-top: 12px;
  border-radius: 10px;
  background: #f9fafb;
  border: 1px solid #eef2f7;
  padding: 10px;
}

.reply-title {
  font-size: 12px;
  color: #6b7280;
  font-weight: 700;
}

.reply-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  background: #ffffff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 10px;
}

.reply-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.reply-time {
  font-size: 12px;
  color: #6b7280;
  font-weight: 700;
}

.reply-text {
  margin-top: 6px;
  color: #111827;
  line-height: 1.6;
  white-space: pre-wrap;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.head-title {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.detail-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 14px;
  align-items: start;
}

.media-wrap {
  display: grid;
  grid-template-columns: 74px 1fr;
  gap: 12px;
}

.thumbs {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  padding-right: 4px;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef2f7;
  background: #f8fafc;
  cursor: pointer;
  position: relative;
}

.thumb.active {
  border-color: var(--brand-red);
  box-shadow: 0 0 0 2px rgba(255, 122, 0, 0.15);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb-video {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
}

.thumb-video-badge {
  color: #fff;
  font-weight: 800;
  font-size: 12px;
}

.media-main {
  width: 100%;
  aspect-ratio: 1;
  background: #f8fafc;
  border-radius: 14px;
  overflow: hidden;
  position: relative;
}

.main-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.main-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  background: #000;
}

.main-param {
  width: 100%;
  height: 100%;
  overflow: auto;
  padding: 10px;
  background: #fff;
}

.media-tabs {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 10px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.tab-btn {
  min-width: 64px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.92);
  color: #111827;
  cursor: pointer;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
}

.tab-btn.active {
  border-color: var(--brand-red);
  color: var(--brand-red);
  background: #fff7ed;
}

.media-actions {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.media-action-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.tip {
  color: #6b7280;
  font-size: 13px;
}

.full-width {
  width: 100%;
}

.block-title {
  margin-top: 10px;
  font-weight: 800;
  color: #111827;
}

.sku-actions {
  margin: 10px 0;
  display: flex;
  gap: 10px;
}

.sku-table {
  width: 100%;
}

.kv-actions {
  margin: 10px 0;
}

.kv-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.kv-key {
  flex: 0 0 40%;
}

.kv-val {
  flex: 1;
}

@media (max-width: 980px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .media-wrap {
    grid-template-columns: 1fr;
  }

  .thumbs {
    flex-direction: row;
  }
}
</style>
