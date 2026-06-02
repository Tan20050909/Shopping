<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, imageOf } from '../api/client'
import { useUserStore } from '../stores/user'
import { DEFAULT_AVATARS, DEFAULT_USER_AVATAR, resolveAvatar } from '../avatar'

const user = useUserStore()
const route = useRoute()
const router = useRouter()
const tab = ref('addresses')
const addresses = ref([])
const coupons = ref([])
const couponFilter = ref('unused')
const favorites = ref([])
const history = ref([])
const reviews = ref([])
const addressForm = ref({
  consignee: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detailAddr: '',
  defaultAddress: false
})
const editingAddressId = ref(null)
const showAddressDialog = ref(false)
const profileForm = ref({
  nickname: '',
  avatar: '',
  realName: '',
  gender: 0,
  birthday: ''
})
const avatarUploading = ref(false)
const avatarFileInput = ref(null)
const defaultAvatars = DEFAULT_AVATARS
const currentAvatar = computed(() => resolveAvatar(profileForm.value.avatar || user.profile?.avatar, DEFAULT_USER_AVATAR))
const couponStatusText = computed(() => ({
  0: '未使用',
  1: '已使用',
  2: '已过期',
  3: '已锁定'
}))
const couponFilters = [
  { label: '全部', value: 'all' },
  { label: '未使用', value: 'unused' },
  { label: '快失效', value: 'expiring' },
  { label: '已锁定', value: 'locked' },
  { label: '已使用', value: 'used' },
  { label: '已过期', value: 'expired' }
]
const filteredCoupons = computed(() => coupons.value.filter((coupon) => {
  const status = couponUseStatus(coupon)
  const expireTime = new Date(coupon.expireTime || coupon.expire_time).getTime()
  const soon = status === 0 && Number.isFinite(expireTime) && expireTime - Date.now() <= 7 * 24 * 60 * 60 * 1000
  if (couponFilter.value === 'unused') return status === 0
  if (couponFilter.value === 'expiring') return soon
  if (couponFilter.value === 'locked') return status === 3
  if (couponFilter.value === 'used') return status === 1
  if (couponFilter.value === 'expired') return status === 2 || (status === 0 && Number.isFinite(expireTime) && expireTime < Date.now())
  return true
}))

function syncTabWithRoute() {
  if (route.path === '/profile/coupons') tab.value = 'coupons'
  else if (route.path === '/profile/favorites') tab.value = 'favorites'
  else if (route.path === '/profile/history') tab.value = 'history'
  else if (route.path === '/profile/reviews') tab.value = 'reviews'
  else tab.value = 'addresses'
}

function handleTabChange(name) {
  const pathMap = {
    addresses: '/profile',
    coupons: '/profile/coupons',
    favorites: '/profile/favorites',
    history: '/profile/history',
    reviews: '/profile/reviews'
  }
  router.push(pathMap[name] || '/profile')
}

async function load() {
  await user.loadMe()
  profileForm.value.nickname = user.profile?.nickname || ''
  profileForm.value.avatar = user.profile?.avatar || ''
  profileForm.value.realName = user.profile?.real_name || ''
  profileForm.value.gender = Number.isFinite(Number(user.profile?.gender)) ? Number(user.profile?.gender) : 0
  profileForm.value.birthday = user.profile?.birthday ? String(user.profile?.birthday).slice(0, 10) : ''
  addresses.value = await api('/api/user/addresses')
  coupons.value = await api('/api/user/coupons')
  favorites.value = await api('/api/user/favorites')
  history.value = await api('/api/user/browse-history')
  reviews.value = await api('/api/user/reviews')
}

async function saveAddress() {
  try {
    if (editingAddressId.value) {
      await api(`/api/user/addresses/${editingAddressId.value}`, { method: 'PUT', body: addressForm.value })
      ElMessage.success('地址已更新')
    } else {
      await api('/api/user/addresses', { method: 'POST', body: addressForm.value })
      ElMessage.success('地址已添加')
    }
    resetAddressForm()
    await load()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

function editAddress(addr) {
  editingAddressId.value = addr.addr_id
  addressForm.value = {
    consignee: addr.consignee,
    phone: addr.phone,
    province: addr.province || '',
    city: addr.city || '',
    district: addr.district || '',
    detailAddr: addr.detail_addr,
    defaultAddress: addr.is_default === 1
  }
  showAddressDialog.value = true
}

function resetAddressForm() {
  editingAddressId.value = null
  showAddressDialog.value = false
  addressForm.value = { consignee: '', phone: '', province: '', city: '', district: '', detailAddr: '', defaultAddress: false }
}

async function setDefaultAddress(addrId) {
  await api(`/api/user/addresses/${addrId}/default`, { method: 'PUT' })
  ElMessage.success('已设为默认地址')
  await load()
}

async function deleteAddress(addrId) {
  await api(`/api/user/addresses/${addrId}`, { method: 'DELETE' })
  ElMessage.success('地址已删除')
  await load()
}

async function saveProfile() {
  await api('/api/user/profile', { method: 'PUT', body: profileForm.value })
  ElMessage.success('资料已更新')
  await load()
}

function chooseAvatar(avatar) {
  profileForm.value.avatar = avatar
}

async function uploadAvatarFile(file) {
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return
  }
  avatarUploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await fetch('/api/upload/image', { method: 'POST', body: form })
    if (!res.ok) throw new Error('上传失败')
    const data = await res.json()
    profileForm.value.avatar = data?.url || ''
    try {
      await saveProfile()
    } catch (e) {
    }
    ElMessage.success('头像已保存')
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
    if (avatarFileInput.value) avatarFileInput.value.value = ''
  }
}

function triggerAvatarUpload() {
  if (avatarUploading.value) return
  avatarFileInput.value?.click?.()
}

async function removeFavorite(goodsId) {
  await api(`/api/user/favorites/${goodsId}`, { method: 'DELETE' })
  ElMessage.success('已取消收藏')
  await load()
}

async function addToCart(item) {
  const skuId = item.default_sku_id || item.defaultSkuId || item.sku_id || item.skuId
  if (!skuId) {
    ElMessage.warning('这个商品需要先去详情页选择规格')
    router.push(`/products/${item.goods_id || item.goodsId}`)
    return
  }
  await api('/api/user/cart/items', {
    method: 'POST',
    body: { skuId, num: 1 }
  })
  ElMessage.success('已加入购物车')
}

function buyNow(item) {
  const skuId = item.default_sku_id || item.defaultSkuId || item.sku_id || item.skuId
  if (!skuId) {
    router.push(`/products/${item.goods_id || item.goodsId}`)
    return
  }
  router.push(`/order/confirm?skuId=${skuId}&num=1`)
}

async function clearHistory() {
  await api('/api/user/browse-history', { method: 'DELETE' })
  ElMessage.success('浏览记录已清空')
  await load()
}

async function deleteHistory(historyId) {
  await api(`/api/user/browse-history/${historyId}`, { method: 'DELETE' })
  ElMessage.success('已删除浏览记录')
  await load()
}

function reviewOrderId(item) {
  const raw = item?.orderId ?? item?.order_id ?? item?.orderID
  const id = Number(raw)
  return Number.isFinite(id) && id > 0 ? String(Math.trunc(id)) : ''
}

function goReviewOrder(item) {
  const orderId = reviewOrderId(item)
  if (!orderId) {
    ElMessage.warning('这条评价缺少订单关联，不能跳转订单详情')
    return
  }
  router.push(`/orders/${orderId}`)
}

function reviewImage(src, item = null) {
  if (!src) return ''
  const v = String(src || '').trim().replaceAll('\\', '/')
  if (!v) return ''
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/uploads/')) return `http://localhost:8082${v}`
  if (v.startsWith('uploads/')) return `http://localhost:8082/${v}`
  if (v.startsWith('/goods/') || v.startsWith('goods/')) return imageOf(item || {})
  if (v.startsWith('/')) return v
  return `/${v}`
}

function onReviewImageError(event, item) {
  const el = event?.target
  if (!el || el.dataset.fallbackApplied === '1') return
  el.dataset.fallbackApplied = '1'
  el.src = imageOf(item || {})
}

function couponUseStatus(coupon) {
  const status = Number(coupon.useStatus ?? coupon.use_status)
  const expireTime = new Date(coupon.expireTime || coupon.expire_time).getTime()
  if (status === 0 && Number.isFinite(expireTime) && expireTime < Date.now()) return 2
  return status
}

async function deleteReview(commentId) {
  await ElMessageBox.confirm('删除后评价将不再公开展示，但该订单商品不能再次评价，确定删除吗？', '删除评价', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await api(`/api/user/reviews/${commentId}`, { method: 'DELETE' })
  ElMessage.success('评价已删除')
  await load()
}

watch(() => route.path, syncTabWithRoute, { immediate: true })
onMounted(load)
</script>

<template>
  <main class="page stack">
    <section class="band row profile-hero">
      <img class="avatar" :src="currentAvatar" alt="用户头像" />
      <div class="stack">
        <h1 class="section-title">{{ user.profile?.nickname || '个人中心' }}</h1>
        <p class="muted">{{ user.profile?.phone }} / 积分 {{ user.profile?.asset?.integral || 0 }}</p>
        <div class="row">
          <el-input v-model="profileForm.nickname" placeholder="昵称" />
          <el-input v-model="profileForm.realName" placeholder="姓名" />
          <el-select v-model="profileForm.gender" placeholder="性别" style="width: 120px">
            <el-option label="未知" :value="0" />
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
          <el-date-picker
            v-model="profileForm.birthday"
            type="date"
            placeholder="生日"
            value-format="YYYY-MM-DD"
            style="width: 160px"
          />
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </div>
        <div class="avatar-picker">
          <div class="avatar-picker-head">
            <strong>默认头像</strong>
            <span class="muted">点击选择，或上传自己的头像</span>
            <input ref="avatarFileInput" type="file" accept="image/*" style="display:none" @change="(e) => uploadAvatarFile(e.target.files?.[0])" />
            <el-button :loading="avatarUploading" plain @click="triggerAvatarUpload">上传头像</el-button>
          </div>
          <div class="avatar-groups">
            <div class="avatar-group">
              <div class="avatar-grid">
                <button
                  v-for="avatar in defaultAvatars"
                  :key="avatar"
                  class="avatar-option"
                  :class="{ active: profileForm.avatar === avatar || currentAvatar === avatar }"
                  type="button"
                  @click="chooseAvatar(avatar)"
                >
                  <img :src="avatar" alt="默认头像" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <el-tabs v-model="tab" class="band" @tab-change="handleTabChange">
      <el-tab-pane label="收货地址" name="addresses">
        <div class="stack">
          <el-button size="small" type="primary" @click="showAddressDialog = true">新增地址</el-button>
          <el-dialog v-model="showAddressDialog" :title="editingAddressId ? '编辑地址' : '新增地址'" width="500px" @closed="resetAddressForm">
            <div class="stack">
              <div class="row">
                <el-input v-model="addressForm.consignee" placeholder="收货人" />
                <el-input v-model="addressForm.phone" placeholder="手机号" />
              </div>
              <div class="row">
                <el-input v-model="addressForm.province" placeholder="省" />
                <el-input v-model="addressForm.city" placeholder="市" />
                <el-input v-model="addressForm.district" placeholder="区" />
              </div>
              <el-input v-model="addressForm.detailAddr" placeholder="详细地址" type="textarea" :rows="2" />
              <el-checkbox v-model="addressForm.defaultAddress">设为默认地址</el-checkbox>
            </div>
            <template #footer>
              <el-button @click="resetAddressForm">取消</el-button>
              <el-button type="primary" @click="saveAddress">保存</el-button>
            </template>
          </el-dialog>
          <div v-for="addr in addresses" :key="addr.addr_id" class="band address-card">
            <div class="stack address-info">
              <div class="row address-head">
                <strong>{{ addr.consignee }}</strong>
                <span>{{ addr.phone }}</span>
                <span v-if="addr.is_default === 1" class="coupon-status">默认</span>
              </div>
              <span class="muted">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail_addr }}</span>
            </div>
            <div class="row address-actions">
              <el-button size="small" @click="editAddress(addr)">编辑</el-button>
              <el-button v-if="addr.is_default !== 1" size="small" @click="setDefaultAddress(addr.addr_id)">设为默认</el-button>
              <el-button size="small" type="danger" plain @click="deleteAddress(addr.addr_id)">删除</el-button>
            </div>
          </div>
          <p v-if="!addresses.length" class="muted">还没有收货地址，点击上方按钮新增。</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="优惠券" name="coupons">
        <div class="stack">
          <div class="row coupon-toolbar">
            <h3>我的券包</h3>
            <el-segmented v-model="couponFilter" :options="couponFilters" />
          </div>
          <div v-if="filteredCoupons.length" v-for="coupon in filteredCoupons" :key="coupon.userCouponId || coupon.user_coupon_id" class="coupon-item wallet-coupon">
            <div class="coupon-amount">
              <strong>¥{{ coupon.denomination }}</strong>
              <span>满 {{ coupon.minAmount || coupon.min_amount }} 可用</span>
            </div>
            <div class="stack coupon-wallet-info">
              <strong>{{ coupon.couponName || coupon.coupon_name }}</strong>
              <span class="muted">{{ coupon.merchantName || coupon.merchant_name || '平台通用' }}</span>
              <span class="muted">到期时间 {{ coupon.expireTime || coupon.expire_time }}</span>
            </div>
            <div class="stack coupon-actions">
              <span class="coupon-status">{{ couponStatusText[couponUseStatus(coupon)] || `状态 ${couponUseStatus(coupon)}` }}</span>
              <el-button v-if="couponUseStatus(coupon) === 0" size="small" type="primary" @click="router.push('/products')">去使用</el-button>
              <el-button v-else size="small" disabled>不可使用</el-button>
            </div>
          </div>
          <p v-else class="muted">这个筛选下没有优惠券，可以去领券中心看看。</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="收藏" name="favorites">
        <div v-if="favorites.length" v-for="item in favorites" :key="item.collect_id" class="list-item">
          <router-link :to="`/products/${item.goods_id}`">
            <img class="cover" :src="imageOf(item)" :alt="item.goods_name" />
          </router-link>
          <div class="stack">
            <router-link :to="`/products/${item.goods_id}`">{{ item.goods_name }} ¥{{ item.price }}</router-link>
            <div class="row">
              <el-button size="small" @click="addToCart(item)">加入购物车</el-button>
              <el-button size="small" type="primary" @click="buyNow(item)">立即购买</el-button>
              <el-button size="small" @click="removeFavorite(item.goods_id)">取消收藏</el-button>
            </div>
          </div>
        </div>
        <p v-else class="muted">你还没有收藏商品，先去商品页挑几件喜欢的吧。</p>
      </el-tab-pane>

      <el-tab-pane label="浏览记录" name="history">
        <div class="row">
          <el-button size="small" @click="clearHistory">清空全部</el-button>
        </div>
        <div v-if="history.length" v-for="item in history" :key="item.history_id" class="list-item">
          <router-link :to="`/products/${item.goods_id}`">
            <img class="cover" :src="imageOf(item)" :alt="item.goods_name" />
          </router-link>
          <div class="stack">
            <router-link :to="`/products/${item.goods_id}`">{{ item.goods_name }} / 浏览 {{ item.browse_count }} 次</router-link>
            <div class="row">
              <el-button size="small" @click="addToCart(item)">加入购物车</el-button>
              <el-button size="small" type="primary" @click="buyNow(item)">立即购买</el-button>
              <el-button size="small" @click="deleteHistory(item.history_id)">删除记录</el-button>
            </div>
          </div>
        </div>
        <p v-else class="muted">还没有浏览记录，先去商品详情页逛一圈。</p>
      </el-tab-pane>

      <el-tab-pane label="我的评价" name="reviews">
        <div v-if="reviews.length" v-for="item in reviews" :key="item.commentId || item.comment_id" class="list-item review-row">
          <router-link :to="`/products/${item.goodsId || item.goods_id}`">
            <img class="cover" :src="imageOf(item)" :alt="item.goodsName || item.goods_name" />
          </router-link>
          <div class="stack review-info">
            <div class="row review-head">
              <router-link :to="`/products/${item.goodsId || item.goods_id}`">
                <strong>{{ item.goodsName || item.goods_name }}</strong>
              </router-link>
              <span class="coupon-status">{{ item.isAnonymous || item.is_anonymous ? '匿名评价' : '公开评价' }}</span>
            </div>
            <span class="muted">SKU：{{ item.skuName || item.sku_name || '默认规格' }} · {{ item.commentTime || item.comment_time }}</span>
            <p>{{ item.commentContent || item.comment_content }}</p>
            <span class="muted">商品 {{ item.goodsScore || item.goods_score }} 星 / 服务 {{ item.serviceScore || item.service_score }} 星 / 物流 {{ item.logisticsScore || item.logistics_score }} 星</span>
            <img
              v-if="item.commentPic || item.comment_pic"
              class="review-thumb"
              :src="reviewImage(item.commentPic || item.comment_pic, item)"
              alt="评价图片"
              @error="(e) => onReviewImageError(e, item)"
            />
            <div class="row">
              <el-button size="small" :disabled="!reviewOrderId(item)" @click="goReviewOrder(item)">查看订单</el-button>
              <el-button size="small" type="danger" plain @click="deleteReview(item.commentId || item.comment_id)">删除评价</el-button>
            </div>
          </div>
        </div>
        <p v-else class="muted">还没有发表评价，完成订单后可以给具体商品写评价。</p>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<style scoped>
.profile-hero {
  align-items: flex-start;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
}

.address-info {
  flex: 1;
  gap: 6px;
}

.address-head {
  gap: 12px;
  align-items: center;
}

.address-actions {
  flex-shrink: 0;
}

.avatar {
  width: 104px;
  height: 104px;
  object-fit: cover;
  border-radius: 50%;
  background: var(--bg-section);
  box-shadow: 0 0 0 4px #fff, var(--shadow-soft);
}

.avatar-picker {
  display: grid;
  gap: 14px;
  padding-top: 4px;
}

.avatar-picker-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(10, 48px);
  gap: 12px;
}

.avatar-groups,
.avatar-group {
  display: grid;
  gap: 10px;
}

.avatar-group + .avatar-group {
  margin-top: 4px;
}

.avatar-group-title {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.avatar-option {
  position: relative;
  width: 48px;
  height: 48px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.avatar-option img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-option:hover,
.avatar-option.active {
  border-color: var(--brand-red);
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(230, 0, 18, 0.14);
}

.avatar-option.active::after {
  content: "";
  position: absolute;
  right: 0;
  bottom: 2px;
  width: 12px;
  height: 12px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: var(--brand-red);
}

.coupon-toolbar {
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.coupon-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #f1f1f1;
}

.wallet-coupon {
  align-items: stretch;
  padding: 16px;
  border: 1px solid #ffd9df;
  border-radius: 14px;
  background: #fff;
}

.coupon-amount {
  display: grid;
  place-items: center;
  align-content: center;
  min-width: 130px;
  border-radius: 12px;
  background: var(--brand-red-light);
  color: var(--brand-red);
}

.coupon-amount strong {
  font-size: 30px;
}

.coupon-wallet-info {
  justify-content: center;
  flex: 1;
}

.coupon-actions {
  align-items: flex-end;
  justify-content: center;
}

.coupon-status {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 600;
}

.review-info {
  gap: 8px;
}

.review-info p {
  margin: 0;
  line-height: 1.7;
}

.review-head {
  justify-content: space-between;
}

.review-thumb {
  width: 120px;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-soft);
}

@media (max-width: 720px) {
  .profile-hero {
    grid-template-columns: 1fr;
  }

  .avatar-grid {
    grid-template-columns: repeat(5, 48px);
  }
}
</style>
