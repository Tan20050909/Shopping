<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, imageOf } from '../api/client'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const previewUrl = ref('')
const detail = ref(null)
const loadError = ref('')
const orderId = computed(() => Number(route.query.orderId || 0))
const orderItemId = computed(() => Number(route.query.orderItemId || 0))
const form = ref({
  goodsScore: 5,
  serviceScore: 5,
  logisticsScore: 5,
  commentContent: '',
  commentPic: '',
  anonymous: false
})

const currentItem = computed(() =>
  detail.value?.items?.find((item) => Number(field(item, 'orderItemId', 'order_item_id')) === orderItemId.value) || null
)

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

async function load() {
  loadError.value = ''
  detail.value = null
  if (!orderId.value || !orderItemId.value) {
    loadError.value = '请从订单详情页选择具体商品后评价。'
    return
  }
  loading.value = true
  try {
    detail.value = await api(`/api/user/orders/${orderId.value}`)
    if (!currentItem.value) {
      loadError.value = '没有找到对应的订单商品，请回到订单详情页重新选择。'
    }
  } catch (error) {
    loadError.value = error.message
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.value.commentContent.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  submitting.value = true
  try {
    await api('/api/user/reviews', {
      method: 'POST',
      body: {
        orderId: orderId.value,
        orderItemId: orderItemId.value,
        goodsScore: form.value.goodsScore,
        serviceScore: form.value.serviceScore,
        logisticsScore: form.value.logisticsScore,
        commentContent: form.value.commentContent.trim(),
        commentPic: form.value.commentPic,
        anonymous: form.value.anonymous
      }
    })
    ElMessage.success('评价成功')
    router.push(`/orders/${orderId.value}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function uploadImage(option) {
  const file = option.file
  if (!file?.type?.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    option.onError?.(new Error('只能上传图片文件'))
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('评价图片不能超过 5MB')
    option.onError?.(new Error('评价图片不能超过 5MB'))
    return
  }
  uploading.value = true
  try {
    if (previewUrl.value?.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrl.value)
    }
    previewUrl.value = URL.createObjectURL(file)
    const formData = new FormData()
    formData.append('file', file)
    const result = await api('/api/user/reviews/upload-image', {
      method: 'POST',
      body: formData
    })
    form.value.commentPic = result.url
    ElMessage.success('图片已上传')
    option.onSuccess?.(result)
  } catch (error) {
    ElMessage.error(error.message)
    option.onError?.(error)
  } finally {
    uploading.value = false
  }
}

function clearImage() {
  form.value.commentPic = ''
  if (previewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
}

onMounted(load)
onUnmounted(() => {
  if (previewUrl.value?.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<template>
  <main v-loading="loading" class="page stack">
    <section class="band stack review-shell">
      <div class="row section-head">
        <div>
          <h1 class="section-title">发表评价</h1>
          <p class="muted">评价会展示在商品详情页，帮助其他用户判断是否适合购买。</p>
        </div>
        <el-button @click="router.back()">返回</el-button>
      </div>

      <div v-if="loadError" class="empty-state stack">
        <strong>{{ loadError }}</strong>
        <el-button type="primary" @click="router.push('/orders')">去我的订单</el-button>
      </div>

      <div v-else-if="detail && currentItem" class="review-layout">
        <aside class="product-panel">
          <img class="cover product-cover" :src="imageOf(currentItem)" :alt="field(currentItem, 'goodsName', 'goods_name')" />
          <div class="stack product-info">
            <strong>{{ field(currentItem, 'goodsName', 'goods_name') }}</strong>
            <span class="muted">SKU：{{ field(currentItem, 'skuName', 'sku_name') || '默认规格' }}</span>
            <span class="muted">购买数量：{{ field(currentItem, 'num', 'num') }}</span>
            <span class="muted">订单编号：{{ field(detail, 'groupNo', 'group_no') }}</span>
            <strong class="price">实付 ¥{{ money(field(currentItem, 'totalPrice', 'total_price')) }}</strong>
          </div>
        </aside>

        <div class="form-panel stack">
          <label class="rate-row">
            <span>商品评分</span>
            <el-rate v-model="form.goodsScore" />
          </label>
          <label class="rate-row">
            <span>服务评分</span>
            <el-rate v-model="form.serviceScore" />
          </label>
          <label class="rate-row">
            <span>物流评分</span>
            <el-rate v-model="form.logisticsScore" />
          </label>

          <label class="field-block">
            <span>评价内容</span>
            <el-input
              v-model="form.commentContent"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="说说商品质量、使用感受或尺码体验"
            />
          </label>

          <div class="field-block">
            <span>评价图片</span>
            <div class="upload-row">
              <el-upload
                :show-file-list="false"
                :http-request="uploadImage"
                accept="image/*"
              >
                <el-button :loading="uploading">上传本地图片</el-button>
              </el-upload>
              <el-button v-if="form.commentPic" @click="clearImage">移除图片</el-button>
            </div>
            <img v-if="previewUrl || form.commentPic" class="review-preview" :src="previewUrl || form.commentPic" alt="评价图片预览" />
          </div>

          <div class="field-block">
            <el-checkbox v-model="form.anonymous">匿名评价</el-checkbox>
            <span class="muted">匿名后商品详情页将显示“匿名用户”和默认头像。</span>
          </div>

          <div class="row">
            <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
            <el-button @click="router.push(`/orders/${orderId}`)">返回订单详情</el-button>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.section-head {
  justify-content: space-between;
}

.review-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.product-panel,
.form-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fff;
}

.product-cover {
  border-radius: 8px;
}

.product-info {
  gap: 8px;
}

.rate-row,
.field-block {
  display: grid;
  gap: 8px;
  color: var(--text-main);
  font-weight: 700;
}

.rate-row {
  grid-template-columns: 90px minmax(0, 1fr);
  align-items: center;
}

.field-block :deep(.el-textarea__inner),
.field-block :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.upload-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.review-preview {
  width: 168px;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-soft);
}

.empty-state {
  padding: 36px 20px;
  border: 1px dashed #ffb780;
  border-radius: 8px;
  background: var(--bg-soft);
}

@media (max-width: 860px) {
  .review-layout,
  .rate-row {
    grid-template-columns: 1fr;
  }
}
</style>
