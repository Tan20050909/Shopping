<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { api, imageOf } from '../api/client'
import { shopLogo as resolveLogo, scoreOf } from '../utils'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const reviews = ref([])
const reviewTotal = ref(0)
const skuId = ref()
const favorite = ref(false)
const quantity = ref(1)
const contacting = ref(false)

const selectedSku = computed(() => (detail.value?.skus || []).find((sku) => sku.skuId === skuId.value))

async function load() {
  detail.value = await api(`/api/user/products/${route.params.id}`)
  await loadReviews()
  skuId.value = detail.value.skus?.[0]?.skuId
  quantity.value = 1
  favorite.value = Boolean(detail.value.collected)
}

async function loadReviews() {
  const data = await api(`/api/user/products/${route.params.id}/reviews?pageNum=1&pageSize=10`)
  reviews.value = data.records || []
  reviewTotal.value = data.total || 0
}

async function addToCart() {
  if (!skuId.value) {
    ElMessage.warning('请先选择规格')
    return
  }
  try {
    await api('/api/user/cart/items', {
      method: 'POST',
      body: { skuId: skuId.value, num: quantity.value }
    })
    ElMessage.success('已加入购物车')
    router.push('/cart')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function buyNow() {
  if (!skuId.value) {
    ElMessage.warning('请先选择规格')
    return
  }
  router.push(`/order/confirm?skuId=${skuId.value}&num=${quantity.value}`)
}

async function toggleFavorite() {
  try {
    if (favorite.value) {
      await api(`/api/user/favorites/${route.params.id}`, { method: 'DELETE' })
      favorite.value = false
      ElMessage.success('已取消收藏')
    } else {
      await api(`/api/user/favorites/${route.params.id}`, { method: 'POST' })
      favorite.value = true
      ElMessage.success('已加入收藏')
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function selectSku(sku) {
  if (Number(sku.stock || 0) <= 0) return
  skuId.value = sku.skuId
  quantity.value = 1
}

function avatarOf(comment) {
  const avatar = comment.avatar || ''
  if (avatar.startsWith('http') || avatar.startsWith('/')) return avatar
  return 'https://api.dicebear.com/9.x/notionists/svg?seed=allmart-comment'
}

function reviewImage(src) {
  if (!src) return ''
  const v = String(src || '').trim().replaceAll('\\', '/')
  if (!v) return ''
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  return `/${v}`
}

function pictureOf(pic) {
  const raw = pic?.picUrl || pic?.pic_url || ''
  const v = String(raw || '').trim().replaceAll('\\', '/')
  if (!v) {
    const base = detail.value ? { ...detail.value } : {}
    return imageOf({ ...base, goodsId: pic?.picId || pic?.pic_id })
  }
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  return `/${v}`
}

function shopLogo() {
  return resolveLogo(detail.value?.merchantLogo || '')
}

async function contactMerchant() {
  if (!detail.value?.merchantId || contacting.value) return
  contacting.value = true
  try {
    const session = await api('/api/user/chat/sessions', {
      method: 'POST',
      body: {
        merchantId: detail.value.merchantId,
        goodsId: detail.value.goodsId
      }
    })
    const target = router.resolve({
      path: `/chat/${session.sessionId || session.session_id}`,
      query: { standalone: '1', shell: '1', role: 'user' }
    })
    window.open(target.href, '_blank', 'noopener,noreferrer')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    contacting.value = false
  }
}

function visitShop() {
  router.push(`/shops/${detail.value.merchantId}`)
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <main v-if="detail" class="page stack detail-page">
    <section class="detail">
      <div class="detail-media">
        <img class="cover main-cover" :src="imageOf(detail)" :alt="detail.goodsName" />
      </div>
      <div class="detail-info stack">
        <div class="merchant-strip">
          <img class="merchant-logo" :src="shopLogo()" :alt="detail.merchantName" />
          <div class="merchant-meta">
            <el-popover placement="bottom-start" trigger="hover" width="360" popper-class="merchant-popover">
              <template #reference>
                <button class="merchant-name" type="button">
                  <span>{{ detail.merchantName }}</span>
                  <span aria-hidden="true">›</span>
                </button>
              </template>
              <div class="merchant-popover-body">
                <div class="merchant-popover-head">
                  <img class="merchant-logo large" :src="shopLogo()" :alt="detail.merchantName" />
                  <div>
                    <strong>{{ detail.merchantName }}</strong>
                    <p>{{ detail.merchantIntro || '精选商品持续上新，欢迎咨询商品、订单与售后问题。' }}</p>
                  </div>
                </div>
                <div class="merchant-score-grid">
                  <span>综合体验<strong>{{ scoreOf(detail.merchantScore) }}</strong></span>
                  <span>服务保障<strong>{{ scoreOf(detail.merchantServiceScore) }}</strong></span>
                  <span>物流体验<strong>{{ scoreOf(detail.merchantLogisticsScore) }}</strong></span>
                </div>
                <div class="row merchant-popover-actions">
                  <el-button plain :loading="contacting" @click="contactMerchant">和商家联系</el-button>
                  <el-button type="primary" @click="visitShop">进店逛逛</el-button>
                </div>
              </div>
            </el-popover>
            <p class="merchant-rating">店铺评分 {{ scoreOf(detail.merchantScore) }} · {{ detail.categoryName }}</p>
          </div>
        </div>
        <h1>{{ detail.goodsName }}</h1>
        <p>{{ detail.goodsIntro || '当前阶段先完成商品浏览闭环。' }}</p>
        <div class="price">¥{{ selectedSku?.price || detail.minPrice }}</div>
        <p class="muted">销量 {{ detail.sellCount }} / 评分 {{ detail.goodsScore }} / 总库存 {{ detail.totalStock }}</p>
        <p class="muted">价格区间 ¥{{ detail.minPrice }} - ¥{{ detail.maxPrice }}</p>
        <div class="sku-section">
          <span class="sku-label">规格</span>
          <div class="sku-options">
            <button
              v-for="sku in detail.skus"
              :key="sku.skuId"
              type="button"
              class="sku-option"
              :class="{ active: Number(sku.skuId) === Number(skuId), disabled: Number(sku.stock || 0) <= 0 }"
              @click="selectSku(sku)"
            >
              <span>{{ sku.skuName }}</span>
              <strong>¥{{ sku.price }}</strong>
              <small>可售 {{ sku.stock }}</small>
            </button>
          </div>
        </div>
        <div class="quantity-section">
          <span class="sku-label">数量</span>
          <el-input-number v-model="quantity" :min="1" :max="selectedSku?.stock || 1" :disabled="!selectedSku || Number(selectedSku.stock || 0) <= 0" />
          <span class="muted">当前规格可售 {{ selectedSku?.stock || 0 }}</span>
        </div>
        <div class="row purchase-actions">
          <el-button size="small" type="primary" @click="buyNow">立即购买</el-button>
          <el-button size="small" plain @click="addToCart">加入购物车</el-button>
          <el-button size="small" :type="favorite ? 'warning' : 'default'" @click="toggleFavorite">{{ favorite ? '已收藏' : '收藏商品' }}</el-button>
          <el-button size="small" plain :loading="contacting" @click="contactMerchant">联系商家</el-button>
          <router-link class="text-link" to="/cart">去购物车</router-link>
        </div>
      </div>
    </section>

    <section class="band stack">
      <h2 class="section-title">商品图片</h2>
      <div class="grid">
        <img v-for="pic in detail.pictures" :key="pic.picId" class="cover" :src="pictureOf(pic)" :alt="detail.goodsName" />
      </div>
    </section>

    <section class="band stack">
      <div class="review-head">
        <div>
          <h2 class="section-title">用户评价</h2>
          <p class="muted">共 {{ reviewTotal }} 条评价 · 商品综合评分 {{ detail.goodsScore || '5.0' }}</p>
        </div>
      </div>
      <div v-if="reviews.length" v-for="comment in reviews" :key="comment.commentId" class="list-item review-item">
        <img class="review-avatar" :src="avatarOf(comment)" alt="用户头像" />
        <div class="stack review-body">
          <div class="row review-meta">
            <strong>{{ comment.nickname || '用户' }}</strong>
            <span class="muted">{{ comment.commentTime }}</span>
          </div>
          <p>{{ comment.commentContent || '这位用户没有留下文字评价。' }}</p>
          <p class="muted">SKU：{{ comment.skuName || '默认规格' }}</p>
          <p class="muted">商品 {{ comment.goodsScore }} 星 / 服务 {{ comment.serviceScore }} 星 / 物流 {{ comment.logisticsScore }} 星</p>
          <img
            v-if="comment.commentPic"
            class="review-pic"
            :src="reviewImage(comment.commentPic)"
            alt="评价图片"
          />
          <div v-if="comment.replyContent" class="reply-box">
            <strong>商家回复</strong>
            <p>{{ comment.replyContent }}</p>
            <span class="muted">{{ comment.replyTime }}</span>
          </div>
        </div>
      </div>
      <p v-else class="muted">暂无评价，购买后可以发表第一条评价。</p>
    </section>
  </main>
</template>

<style scoped>
.detail-page {
  gap: 32px;
}

.detail {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 440px);
  gap: 48px;
  align-items: start;
}

.detail-media {
  padding: 28px;
  border-radius: var(--radius-lg);
  background: var(--bg-section);
}

.main-cover {
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-md);
}

.detail-info {
  padding: 34px 0;
}

.detail-info h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.18;
}

.detail-info p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.detail-info .price {
  font-size: 34px;
}

.purchase-actions {
  gap: 10px;
}

.merchant-strip {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-soft);
}

.merchant-logo {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid var(--border-light);
}

.merchant-logo.large {
  width: 64px;
  height: 64px;
}

.merchant-meta {
  min-width: 0;
}

.merchant-name {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 0;
  color: var(--text-main);
  background: transparent;
  border: 0;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.merchant-name:hover {
  color: var(--brand-red);
}

.merchant-rating {
  margin-top: 4px !important;
  color: var(--text-secondary);
  font-size: 13px;
}

.merchant-popover-body {
  display: grid;
  gap: 18px;
}

.merchant-popover-head {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.merchant-popover-head strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text-main);
  font-size: 18px;
}

.merchant-popover-head p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.merchant-score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--border-light);
}

.merchant-score-grid span {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  text-align: center;
}

.merchant-score-grid strong {
  color: var(--text-main);
  font-size: 20px;
}

.merchant-popover-actions {
  justify-content: flex-end;
}

.sku-section {
  display: grid;
  gap: 10px;
}

.sku-label {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
}

.sku-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sku-option {
  display: grid;
  gap: 5px;
  min-height: 78px;
  padding: 11px 12px;
  text-align: left;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.sku-option span {
  color: var(--text-main);
  font-weight: 700;
}

.sku-option strong {
  color: var(--brand-red);
  font-size: 14px;
}

.sku-option small {
  color: var(--text-secondary);
}

.sku-option.active {
  background: var(--brand-red-light);
  border-color: var(--brand-red);
  box-shadow: 0 0 0 2px rgba(210, 46, 31, 0.1);
}

.sku-option.disabled {
  color: var(--text-muted);
  background: var(--bg-soft);
  cursor: not-allowed;
  opacity: 0.62;
}

.quantity-section {
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: 10px;
  align-items: center;
}

.review-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.review-item {
  grid-template-columns: 56px minmax(0, 1fr);
}

.review-avatar {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 50%;
  background: var(--bg-section);
}

.review-body {
  gap: 8px;
}

.review-body p {
  margin: 0;
  line-height: 1.7;
}

.review-meta {
  justify-content: space-between;
}

.review-pic {
  width: min(180px, 100%);
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border: 1px solid var(--border-light);
  border-radius: 8px;
}

.reply-box {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-soft);
}

@media (max-width: 840px) {
  .detail {
    grid-template-columns: 1fr;
  }

  .quantity-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .sku-options {
    grid-template-columns: 1fr;
  }
}
</style>
