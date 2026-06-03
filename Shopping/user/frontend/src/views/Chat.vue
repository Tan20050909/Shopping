<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  ChatDotRound,
  Document,
  FolderOpened,
  Goods,
  Picture,
  RefreshRight,
  Search,
  Service,
  ShoppingCart,
  Ticket,
  Van
} from '@element-plus/icons-vue'
import { api } from '../api/client'
import { shopLogo as resolveLogo } from '../utils'
import { useUserStore } from '../stores/user'
import { DEFAULT_USER_AVATAR } from '../avatar'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const sessions = ref([])
const detail = ref(null)
const input = ref('')
const sessionKeyword = ref('')
const loading = ref(false)
const sending = ref(false)
const messageList = ref(null)
const pageRef = ref(null)
const isStandalone = computed(() => String(route.query?.standalone || '') === '1')
const isShell = computed(() => String(route.query?.shell || '') === '1')
const isStandaloneShell = computed(() => isStandalone.value && isShell.value)
const shellScale = ref(1)
const confirmedAddrCards = ref({})
const activeActionMessageId = ref(null)
const quoted = ref(null)
const localHidden = ref({})
const relatedProductsOpen = ref(false)
const relatedProductsLoading = ref(false)
const relatedProductsLoaded = ref(false)
const relatedProducts = ref([])
const defaultUserAvatar = DEFAULT_USER_AVATAR
const defaultShopAvatar = resolveLogo('')
const brandLogo = '/brand-assets/allmart-logo-full.png'
const emojis = [
  '😀', '😁', '😂', '🤣', '😊', '😍', '😘', '😎', '🤔', '😅',
  '😭', '😡', '👍', '👎', '🙏', '👏', '🎉', '💯', '❤️', '🔥',
  '🌹', '☕', '📦', '🚚', '✅'
]
const quickQuestions = [
  { label: '发货时间', icon: Van },
  { label: '物流查询', icon: Search },
  { label: '退换政策', icon: RefreshRight },
  { label: '优惠券使用', icon: Ticket }
]
const serviceEntries = [
  { label: '联系客服', action: 'shop', icon: Service },
  { label: '售后帮助', action: 'afterSale', icon: Goods },
  { label: '平台介入', action: 'platform', icon: ChatDotRound },
  { label: '服务评价', action: 'review', icon: Ticket }
]
const reminderTips = [
  '商家在线时间：8:00 - 23:00',
  '平均响应时间：1 分钟内',
  '请文明沟通，避免发送违规内容'
]
const detailMessages = computed(() => {
  const list = Array.isArray(detail.value?.messages) ? detail.value.messages : []
  const hidden = localHidden.value || {}
  return list
    .filter((m) => {
      const id = String(m?.messageId ?? m?.message_id ?? '')
      return !id || !hidden[id]
    })
    .map((m) => {
    const content = String(m?.content || '').trim()
    let payload = null
    if (content.startsWith('{') && content.endsWith('}')) {
      try {
        const parsed = JSON.parse(content)
        if (parsed && typeof parsed === 'object') payload = parsed
      } catch (e) {
        payload = null
      }
    }
    return { ...m, _payload: payload }
  })
})

const filteredSessions = computed(() => {
  const keyword = sessionKeyword.value.trim().toLowerCase()
  if (!keyword) return sessions.value
  return sessions.value.filter((session) => {
    const name = String(session.merchantName || session.merchant_name || '').toLowerCase()
    const id = String(session.merchantId || session.merchant_id || session.sessionId || session.session_id || '')
    return name.includes(keyword) || id.includes(keyword)
  })
})

const activeSessionId = computed(() => {
  const routeId = Number(route.params.id || 0)
  if (Number.isFinite(routeId) && routeId > 0) return routeId
  const first = sessions.value[0]
  return first ? Number(sessionIdOf(first)) || null : null
})

const currentSession = computed(() => {
  if (!activeSessionId.value) return null
  return sessions.value.find((session) => Number(sessionIdOf(session)) === Number(activeSessionId.value)) || null
})

const orderPayload = computed(() => {
  const source = detailMessages.value.find((message) => {
    const type = String(message?._payload?.type || '')
    return type === 'addr_confirm' || type === 'goods_inquiry'
  })
  return source?._payload || null
})

const orderSummary = computed(() => {
  const payload = orderPayload.value || {}
  return {
    goodsName: payload.goodsTitle || payload.goodsName || '暂无关联商品',
    goodsPic: resolveMedia(payload.goodsPic || payload.goods_pic || ''),
    price: Number(payload.payAmount || payload.totalAmount || 0),
    quantity: Number(payload.num || payload.quantity || 1),
    orderNo: payload.orderNo || payload.order_no || payload.orderId || payload.order_id || '-',
    status: payload.orderStatusText || payload.orderStatus || '待确认',
    consignee: payload.consignee || '-',
    phone: payload.phone || '-',
    address: payload.addr || payload.address || '-'
  }
})

const sideOrderInfo = computed(() => ({
  status: orderSummary.value.status || '暂无',
  orderNo: orderSummary.value.orderNo || '-',
  createTime:
    orderPayload.value?.createTime ||
    orderPayload.value?.create_time ||
    orderPayload.value?.orderCreateTime ||
    orderPayload.value?.order_create_time ||
    detail.value?.lastMessageTime ||
    detail.value?.last_message_time ||
    '暂无',
  payAmount: orderSummary.value.price ? `¥${orderSummary.value.price.toFixed(2)}` : '暂无',
  payType:
    orderPayload.value?.payTypeText ||
    orderPayload.value?.payType ||
    orderPayload.value?.pay_type ||
    '在线支付',
  logistics:
    orderPayload.value?.logisticsText ||
    orderPayload.value?.logistics ||
    orderPayload.value?.deliveryTypeText ||
    orderPayload.value?.deliveryType ||
    orderPayload.value?.delivery_type ||
    '商家发货'
}))

function merchantIdFrom(source) {
  if (!source || typeof source !== 'object') return null
  const candidate = Number(
    source.merchantId ??
    source.merchant_id ??
    source.shopId ??
    source.shop_id ??
    source.storeId ??
    source.store_id ??
    source.sellerId ??
    source.seller_id ??
    0
  )
  return Number.isFinite(candidate) && candidate > 0 ? candidate : null
}

const currentMerchantId = computed(() => {
  const sources = [
    currentSession.value,
    detail.value,
    orderPayload.value,
    orderPayload.value?.merchant,
    orderPayload.value?.shop
  ]
  for (const source of sources) {
    const candidate = merchantIdFrom(source)
    if (candidate) return candidate
  }
  return null
})

function merchantAvatarRaw(...sources) {
  const fields = [
    'merchantAvatar',
    'shopAvatar',
    'storeAvatar',
    'avatar',
    'logo',
    'merchantLogo',
    'shopLogo',
    'merchant_avatar',
    'shop_avatar',
    'store_avatar',
    'merchant_logo',
    'shop_logo'
  ]
  for (const source of sources) {
    if (!source || typeof source !== 'object') continue
    for (const key of fields) {
      const value = String(source?.[key] || '').trim()
      if (value) return value
    }
  }
  return ''
}

function merchantAvatarUrl(...sources) {
  const raw = merchantAvatarRaw(...sources)
  if (!raw) return ''
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  if (raw.startsWith('/images/') || raw.startsWith('images/') || raw.startsWith('/videos/') || raw.startsWith('videos/')) {
    return resolveMedia(raw)
  }
  return resolveLogo({ shopLogo: raw })
}

const currentMerchantAvatar = computed(() => (
  merchantAvatarUrl(
    detail.value,
    currentSession.value,
    orderPayload.value,
    orderPayload.value?.merchant,
    orderPayload.value?.shop
  ) || defaultShopAvatar
))

function logoOf(item) {
  return merchantAvatarUrl(item) || defaultShopAvatar
}

function normalizeProduct(item = {}) {
  const goodsId = Number(item.goodsId ?? item.goods_id ?? item.id ?? item.productId ?? item.product_id ?? item.relatedId ?? item.related_id ?? 0)
  const merchantId = merchantIdFrom(item) ?? currentMerchantId.value
  return {
    goodsId: Number.isFinite(goodsId) && goodsId > 0 ? goodsId : 0,
    merchantId,
    merchantLogo: merchantAvatarRaw(item) || merchantAvatarRaw(detail.value, currentSession.value),
    goodsName: String(item.goodsName ?? item.goods_name ?? item.goodsTitle ?? item.title ?? item.name ?? '未命名商品').trim(),
    goodsPic: resolveMedia(item.goodsPic ?? item.goods_pic ?? item.mainImage ?? item.cover ?? item.image ?? item.pic ?? ''),
    price: Number(item.price ?? item.salePrice ?? item.sale_price ?? item.minPrice ?? item.min_price ?? item.goodsPrice ?? 0),
    stock: Number(item.stock ?? item.totalStock ?? item.total_stock ?? item.availableStock ?? item.available_stock ?? 0),
    sellCount: Number(item.sellCount ?? item.sell_count ?? item.sales ?? item.saleNum ?? item.sale_num ?? 0)
  }
}

function collectEmbeddedProducts() {
  const keys = ['relatedProducts', 'products', 'merchantProducts', 'shopProducts', 'goodsList', 'productList']
  const pools = [detail.value, currentSession.value, orderPayload.value, orderPayload.value?.merchant, orderPayload.value?.shop]
  for (const source of pools) {
    if (!source || typeof source !== 'object') continue
    for (const key of keys) {
      if (Array.isArray(source[key]) && source[key].length) {
        return source[key].map(normalizeProduct).filter((item) => item.goodsId || item.goodsName)
      }
    }
  }
  return []
}

async function ensureRelatedProducts() {
  if (relatedProductsLoaded.value) return
  const embedded = collectEmbeddedProducts()
  if (embedded.length) {
    relatedProducts.value = embedded
    relatedProductsLoaded.value = true
    return
  }
  if (!currentMerchantId.value) {
    console.warn('[Chat] 缺少 merchantId，暂时无法加载关联商品')
    relatedProducts.value = []
    relatedProductsLoaded.value = true
    return
  }
  relatedProductsLoading.value = true
  try {
    const params = new URLSearchParams({
      merchantId: String(currentMerchantId.value),
      pageNum: '1',
      pageSize: '8'
    })
    const data = await api(`/api/user/products?${params.toString()}`)
    const records = Array.isArray(data?.records) ? data.records : (Array.isArray(data) ? data : [])
    relatedProducts.value = records.map(normalizeProduct).filter((item) => item.goodsId || item.goodsName)
    relatedProductsLoaded.value = true
  } catch (error) {
    console.warn('[Chat] 加载关联商品失败', error)
    relatedProducts.value = []
    relatedProductsLoaded.value = true
  } finally {
    relatedProductsLoading.value = false
  }
}

async function toggleRelatedProducts() {
  relatedProductsOpen.value = !relatedProductsOpen.value
  if (relatedProductsOpen.value) {
    await ensureRelatedProducts()
  }
}

async function consultProduct(product) {
  if (!product) return
  await sendPayload({
    type: 'goods_inquiry',
    goodsId: product.goodsId,
    relatedId: product.goodsId,
    merchantId: product.merchantId || currentMerchantId.value || undefined,
    goodsName: product.goodsName,
    goodsTitle: product.goodsName,
    goodsPic: product.goodsPic,
    shopLogo: product.merchantLogo || merchantAvatarRaw(detail.value, currentSession.value)
  })
}

function sessionAvatar(session) {
  return merchantAvatarUrl(session) || defaultShopAvatar
}

function sessionInitial(session) {
  const text = String(session?.merchantName || session?.merchant_name || '店').trim()
  return (text || '店').slice(0, 1).toUpperCase()
}

function sessionPreview(session) {
  const raw = String(session?.lastMessageContent || session?.last_message_content || '').trim()
  return raw || '暂无消息'
}

function formatSessionTime(value) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const hit = raw.match(/(\d{1,2}):(\d{2})/)
  if (hit) return `${hit[1].padStart(2, '0')}:${hit[2]}`
  const parsed = parseTimeValue(raw)
  if (!Number.isFinite(parsed)) return raw
  const date = new Date(parsed)
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

function messageClass(message) {
  const type = Number(message.senderType || message.sender_type)
  const payloadType = String(message?._payload?.type || '')
  const receiverType = Number(message?.receiverType ?? message?.receiver_type)
  if (payloadType) {
    const richTypes = new Set(['addr_confirm', 'goods_inquiry', 'coupon'])
    if (richTypes.has(payloadType)) {
      if (receiverType === 1) return 'merchant'
      if (receiverType === 2) return 'mine'
    }
  }
  if (type === 1) return 'mine'
  if (type === 4) return 'system'
  if (type === 2 || type === 3) return 'merchant'
  if (Number(message.messageType || message.message_type) === 5) return 'system'
  return 'merchant'
}

function onAvatarImgError(who, e) {
  const el = e?.target
  if (!el) return
  el.onerror = null
  el.src = who === 'mine' ? defaultUserAvatar : defaultShopAvatar
}

function avatarTextFor(message) {
  const cls = messageClass(message)
  if (cls === 'mine') {
    const text = String(user.profile?.nickname || user.profile?.phone || '我').trim()
    return (text || '我').slice(0, 1).toUpperCase()
  }
  if (cls === 'merchant') {
    const text = String(detail.value?.merchantName || detail.value?.shopName || '店').trim()
    return (text || '店').slice(0, 1).toUpperCase()
  }
  return ''
}

function avatarUrlFor(message) {
  const cls = messageClass(message)
  if (cls === 'mine') {
    const url = resolveMedia(user.profile?.avatar || '')
    return url || defaultUserAvatar
  }
  if (cls === 'merchant') {
    return currentMerchantAvatar.value || defaultShopAvatar
  }
  return ''
}

function isMine(message) {
  return Number(message.senderType || message.sender_type) === 1
}

function messageRead(message) {
  const value = message.isRead ?? message.is_read
  return value === true || Number(value) === 1
}

function isAutoReply(message) {
  return Number(message?.senderType ?? message?.sender_type) === 3
}

function messageText(message) {
  if (message?._payload && typeof message._payload === 'object') {
    if (String(message._payload.type || '') === 'addr_confirm_ack') return '已确认收货地址'
    if (String(message._payload.type || '') === 'image') return '[图片]'
    if (String(message._payload.type || '') === 'file') return '[文件]'
    if (String(message._payload.type || '') === 'text' && message._payload.text != null) return String(message._payload.text)
  }
  return String(message?.content || '')
}

function quoteTextOf(message) {
  const raw = messageText(message).trim()
  if (!raw) return '消息'
  return raw.length > 80 ? `${raw.slice(0, 80)}…` : raw
}

function hiddenStorageKey() {
  return `chat:hidden:${String(route.params.id || '')}`
}

function loadLocalHidden() {
  try {
    const raw = localStorage.getItem(hiddenStorageKey()) || ''
    const parsed = raw ? JSON.parse(raw) : null
    localHidden.value = parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    localHidden.value = {}
  }
}

function persistLocalHidden() {
  try {
    localStorage.setItem(hiddenStorageKey(), JSON.stringify(localHidden.value || {}))
  } catch (e) {
  }
}

function openMessageActions(message) {
  const id = String(message?.messageId ?? message?.message_id ?? '')
  if (!id) return
  if (messageClass(message) === 'system') return
  activeActionMessageId.value = activeActionMessageId.value === id ? null : id
}

function hideMessageLocal(message) {
  const id = String(message?.messageId ?? message?.message_id ?? '')
  if (!id) return
  localHidden.value = { ...(localHidden.value || {}), [id]: true }
  persistLocalHidden()
  if (activeActionMessageId.value === id) activeActionMessageId.value = null
}

function quoteMessage(message) {
  const id = Number(message?.messageId ?? message?.message_id ?? 0)
  quoted.value = { id: Number.isFinite(id) ? id : null, text: quoteTextOf(message) }
  activeActionMessageId.value = null
}

function clearQuote() {
  quoted.value = null
}

function parseTimeValue(v) {
  if (v == null) return Date.now()
  if (typeof v === 'number') return v
  const s0 = String(v).trim()
  if (!s0) return Date.now()
  const s = s0.includes(' ') ? s0.replace(' ', 'T') : s0
  const t = new Date(s).getTime()
  return Number.isFinite(t) && t > 0 ? t : Date.now()
}

function canRevoke(message) {
  if (!isMine(message)) return false
  const id = Number(message.messageId || message.message_id || 0)
  if (!Number.isFinite(id) || id <= 0) return false
  if (messageClass(message) === 'system') return false
  const t = Number(message.messageType || message.message_type || 1)
  if (t === 5) return false
  const ts = parseTimeValue(message.createTime ?? message.create_time)
  return Date.now() - ts <= 2 * 60 * 1000
}

async function revokeMessage(message) {
  const id = Number(message?.messageId || message?.message_id || 0)
  if (!Number.isFinite(id) || id <= 0) return
  try {
    await api(`/api/user/chat/messages/${id}/revoke`, { method: 'PUT' })
    ElMessage.success('已撤回')
    await loadDetail()
    await loadSessions()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function sessionIdOf(session) {
  return session.sessionId || session.session_id
}

function sessionTime(session) {
  return session.lastMessageTime || session.last_message_time || ''
}

function unreadCount(session) {
  return Number(session.userUnreadCount || session.user_unread_count || 0)
}

async function loadSessions() {
  sessions.value = await api('/api/user/chat/sessions')
}

async function loadDetail() {
  const id = Number(activeSessionId.value || 0)
  if (!Number.isFinite(id) || id <= 0) {
    detail.value = null
    loading.value = false
    return
  }
  loadLocalHidden()
  loading.value = true
  try {
    detail.value = await api(`/api/user/chat/sessions/${id}`)
    await loadSessions()
    await nextTick()
    if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function send() {
  const raw = input.value.trim()
  let content = raw
  if (quoted.value?.text) {
    content = `引用：${quoted.value.text}\n${raw}`
  }
  await sendContent(content, true)
}

async function sendContent(content, clearInput = false) {
  const v = String(content || '').trim()
  if (!v || sending.value) return
  const id = Number(activeSessionId.value || 0)
  if (!Number.isFinite(id) || id <= 0) return
  sending.value = true
  try {
    detail.value = await api(`/api/user/chat/sessions/${id}/messages`, {
      method: 'POST',
      body: { content: v }
    })
    if (clearInput) {
      input.value = ''
      quoted.value = null
    }
    await loadSessions()
    await nextTick()
    if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    sending.value = false
  }
}

function openSession(session) {
  router.push({
    path: `/chat/${sessionIdOf(session)}`,
    query: route.query.standalone ? { standalone: '1', shell: route.query.shell || '1' } : {}
  })
}

function enterShop() {
  if (currentMerchantId.value) {
    router.push(`/shops/${currentMerchantId.value}`)
    return
  }
  // TODO: 后续统一会话详情里的商家标识字段，去掉当前店铺页兜底入口
  router.push('/products')
}

function resolveMedia(src) {
  const v0 = String(src || '').trim().replaceAll('\\', '/')
  if (!v0) return ''
  if (v0.startsWith('http://') || v0.startsWith('https://') || v0.startsWith('data:')) return v0
  if (v0.startsWith('/default/') || v0.startsWith('default/')) return ''
  if (v0.startsWith('/goods/')) return `/uploads${v0}`
  if (v0.startsWith('goods/')) return `/uploads/${v0}`
  if (v0.startsWith('/shop/')) return `/uploads${v0}`
  if (v0.startsWith('shop/')) return `/uploads/${v0}`
  if (v0.startsWith('/images/') || v0.startsWith('/videos/')) return `/uploads${v0}`
  if (v0.startsWith('images/') || v0.startsWith('videos/')) return `/uploads/${v0}`
  if (v0.startsWith('/')) return v0
  if (v0.startsWith('uploads/')) return `/${v0}`
  return `/${v0}`
}

function addrCardKey(payload) {
  if (!payload || typeof payload !== 'object') return ''
  const orderNo = String(payload.orderNo || payload.order_no || '').trim()
  if (orderNo) return `no:${orderNo}`
  const orderId = Number(payload.orderId || payload.order_id || payload.orderID || 0)
  if (Number.isFinite(orderId) && orderId > 0) return `id:${orderId}`
  return ''
}

function addressText(payload) {
  if (!payload || typeof payload !== 'object') return ''
  return String(payload.addr || payload.address || '').trim()
}

function goEditAddress(payload) {
  const orderId = Number(payload?.orderId || payload?.order_id || payload?.orderID || 0)
  router.push({
    path: '/profile/addresses',
    query: {
      ...(Number.isFinite(orderId) && orderId > 0 ? { orderId: String(orderId) } : {}),
      back: route.fullPath
    }
  })
}

async function confirmAddress(payload) {
  if (!payload || typeof payload !== 'object') return
  const key = addrCardKey(payload)
  if (key && confirmedAddrCards.value[key]) return
  if (key) confirmedAddrCards.value = { ...confirmedAddrCards.value, [key]: true }
  ElMessage.success('已确认')
}

function openCoupons() {
  router.push('/coupons')
}

function openOrderPanel() {
  if (currentMerchantId.value) {
    router.push({
      path: '/orders',
      query: { merchantId: String(currentMerchantId.value) }
    })
    return
  }
  console.warn('[Chat] 缺少 merchantId，查看订单已回退到全部订单页')
  // TODO: 订单页后续补充 merchantId 精准筛选；当前无商家标识时回退全部订单页
  router.push('/orders')
}

function openGoodsPanel() {
  if (currentMerchantId.value) {
    router.push(`/shops/${currentMerchantId.value}`)
    return
  }
  console.warn('[Chat] 找不到 merchantId，无法跳转店铺详情页')
}

function triggerService(action) {
  if (action === 'shop') {
    enterShop()
    return
  }
  if (action === 'afterSale') {
    router.push('/after-sales')
    return
  }
  if (action === 'review') {
    router.push('/profile/reviews')
    return
  }
  ElMessage.info('后续会接入平台服务入口')
}

function useQuickText(text) {
  input.value = input.value ? `${input.value}\n${text}` : text
}

function pickEmoji(e) {
  input.value = `${input.value || ''}${e || ''}`
}

function formatSize(bytes) {
  const n = Number(bytes ?? 0)
  if (!Number.isFinite(n) || n <= 0) return ''
  const kb = 1024
  const mb = kb * 1024
  if (n < kb) return `${n} B`
  if (n < mb) return `${(n / kb).toFixed(1)} KB`
  return `${(n / mb).toFixed(1)} MB`
}

function resolveFileUrl(src) {
  const raw = String(src || '').trim()
  if (!raw) return ''
  const v = raw.replaceAll('\\', '/')
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  const idx = v.indexOf('/uploads/')
  if (idx > 0) return v.slice(idx)
  if (v.startsWith('/')) return v
  return `/${v}`
}

async function sendPayload(payload) {
  const v = payload && typeof payload === 'object' ? JSON.stringify(payload) : ''
  await sendContent(v, false)
}

async function uploadChatImage(file) {
  const form = new FormData()
  form.append('file', file)
  return api('/api/user/chat/uploads/image', { method: 'POST', body: form })
}

async function uploadChatFile(file) {
  const form = new FormData()
  form.append('file', file)
  return api('/api/user/chat/uploads/file', { method: 'POST', body: form })
}

async function sendImage(options) {
  try {
    const file = options?.file
    const res = await uploadChatImage(file)
    const url = res?.url || res?.path || ''
    if (!url) throw new Error('上传失败')
    await sendPayload({ type: 'image', url, name: file?.name, size: file?.size })
    options?.onSuccess?.(res, file)
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.message || '图片发送失败')
  }
}

async function sendFile(options) {
  try {
    const file = options?.file
    const res = await uploadChatFile(file)
    const url = res?.url || res?.path || ''
    const name = res?.name || file?.name || '文件'
    if (!url) throw new Error('上传失败')
    await sendPayload({ type: 'file', url, name, size: file?.size })
    options?.onSuccess?.(res, file)
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.message || '文件发送失败')
  }
}

function openGoods(payload) {
  if (!payload || typeof payload !== 'object') return
  const id = Number(payload.goodsId || payload.goods_id || payload.relatedId || payload.related_id || 0)
  if (!Number.isFinite(id) || id <= 0) return
  router.push(`/products/${id}`)
}

function calcShellScale() {
  if (!isShell.value) {
    shellScale.value = 1
    return
  }
  const baseW = 1528
  const baseH = 676
  const vw = Math.max(0, window.innerWidth)
  const vh = Math.max(0, window.innerHeight)
  const marginW = 140
  const marginH = 84
  const s = Math.min(1, Math.max(0, (vw - marginW) / baseW), Math.max(0, (vh - marginH) / baseH))
  shellScale.value = s > 0 ? s : 1
}

watch(() => route.params.id, loadDetail)

onMounted(async () => {
  user.syncToken()
  if (user.isLoggedIn && !user.profile) {
    await user.loadMe().catch(() => {})
  }
  await loadSessions()
  await loadDetail()
  calcShellScale()
  window.addEventListener('resize', calcShellScale)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', calcShellScale)
})
</script>

<template>
  <main
    ref="pageRef"
    class="chat-page"
    :class="{ standalone: isStandalone, shell: isShell, 'standalone-shell-page': isStandaloneShell }"
    :style="{ '--ww-scale': String(shellScale) }"
  >
    <section v-if="isStandaloneShell" class="chat-shell-breadcrumb">
      <div class="chat-shell-breadcrumb-inner">
        <button type="button" class="breadcrumb-link" @click="router.push('/')">首页</button>
        <i>/</i>
        <span>消息中心</span>
      </div>
    </section>

    <section class="chat-shell">
      <aside class="session-panel allmart-chat-card">
        <div class="panel-title">
          <span>最近会话</span>
          <small>{{ sessions.length }} 个会话</small>
        </div>
        <el-input
          v-model="sessionKeyword"
          class="session-search"
          placeholder="搜索会话或商家"
          clearable
        />
        <div class="session-list">
          <div v-if="!filteredSessions.length" class="session-empty">
            <strong>暂无会话</strong>
            <span>新消息会显示在这里。</span>
          </div>
          <button
            v-for="session in filteredSessions"
            :key="sessionIdOf(session)"
            type="button"
            class="session-item"
            :class="{ active: Number(sessionIdOf(session)) === Number(activeSessionId) }"
            @click="openSession(session)"
          >
            <div class="session-avatar">
              <img v-if="sessionAvatar(session)" :src="sessionAvatar(session)" :alt="session.merchantName" />
              <span v-else>{{ sessionInitial(session) }}</span>
            </div>
            <span class="session-copy">
              <strong>{{ session.merchantName }}</strong>
              <small>{{ sessionPreview(session) }}</small>
            </span>
            <span class="session-extra">
              <small>{{ formatSessionTime(sessionTime(session)) || '' }}</small>
              <em v-if="unreadCount(session)">{{ unreadCount(session) }}</em>
            </span>
          </button>
        </div>
        <button type="button" class="session-footer-btn" @click="router.push('/messages')">查看全部会话</button>
      </aside>

      <section v-if="detail" v-loading="loading" class="conversation allmart-chat-card">
        <header class="chat-head allmart-chat-section">
          <div class="chat-head-main">
            <img class="chat-head-logo" :src="currentMerchantAvatar" :alt="detail.merchantName" />
            <div class="chat-title">
              <h1>{{ detail.merchantName }}</h1>
              <p><span class="online-dot"></span>在线</p>
            </div>
          </div>
          <div class="chat-actions">
            <el-button class="chat-action-btn" plain @click="openOrderPanel">
              <el-icon><Document /></el-icon>
              <span>查看订单</span>
            </el-button>
            <el-button class="chat-action-btn" plain @click="openGoodsPanel">
              <el-icon><Goods /></el-icon>
              <span>查看商品</span>
            </el-button>
          </div>
        </header>

        <div ref="messageList" class="messages">
          <div
            v-for="message in detailMessages"
            :key="message.messageId"
            class="message-row"
            :class="messageClass(message)"
          >
            <div v-if="messageClass(message) === 'merchant'" class="msg-avatar">
              <img v-if="avatarUrlFor(message)" :src="avatarUrlFor(message)" alt="" @error="(e) => onAvatarImgError('merchant', e)" />
              <span v-else>{{ avatarTextFor(message) }}</span>
            </div>
            <div class="message-stack">
              <div class="bubble">
                <template v-if="message._payload && message._payload.type === 'addr_confirm'">
                  <div class="card-msg">
                    <div class="card-top">
                      <div class="card-title">请确认收货地址</div>
                      <div class="card-sub">{{ message._payload.orderNo || message._payload.orderId || '-' }}</div>
                    </div>
                    <div v-if="message._payload.goodsPic || message._payload.goods_pic" class="card-goods">
                      <img class="goods-pic" :src="resolveMedia(message._payload.goodsPic || message._payload.goods_pic)" alt="" />
                      <div class="goods-meta">
                        <div class="goods-title">{{ message._payload.goodsTitle || message._payload.goodsName || '下单商品' }}</div>
                        <div class="goods-sub">
                          <span class="goods-price">¥ {{ Number(message._payload.payAmount || message._payload.totalAmount || 0).toFixed(2) }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="card-body">
                      <div class="card-line"><span class="k">收货人</span><span class="v">{{ message._payload.consignee || '-' }}</span></div>
                      <div class="card-line"><span class="k">手机号</span><span class="v">{{ message._payload.phone || '-' }}</span></div>
                      <div class="card-line"><span class="k">详细地址</span><span class="v">{{ message._payload.addr || message._payload.address || '-' }}</span></div>
                    </div>
                    <div class="card-actions">
                      <el-button size="small" @click="goEditAddress(message._payload)">修改地址</el-button>
                      <el-button
                        size="small"
                        type="primary"
                        :disabled="confirmedAddrCards[addrCardKey(message._payload)]"
                        @click="confirmAddress(message._payload)"
                      >
                        {{ confirmedAddrCards[addrCardKey(message._payload)] ? '已确认' : '确认' }}
                      </el-button>
                    </div>
                    <div class="card-tip">确认即可</div>
                  </div>
                </template>
                <template v-else-if="message._payload && message._payload.type === 'coupon'">
                  <div class="card-msg">
                    <div class="card-top">
                      <div class="card-title">优惠券已发放</div>
                      <div class="card-sub">店铺券</div>
                    </div>
                    <div class="card-body">
                      <div class="card-line"><span class="k">名称</span><span class="v">{{ message._payload.couponName || '优惠券' }}</span></div>
                    </div>
                    <div class="card-actions">
                      <el-button size="small" type="primary" @click="openCoupons">去使用</el-button>
                    </div>
                  </div>
                </template>
                <template v-else-if="message._payload && message._payload.type === 'goods_inquiry'">
                  <div class="card-msg clickable" role="button" tabindex="0" @click="openGoods(message._payload)">
                    <div class="card-top">
                      <div class="card-title">正在咨询商品</div>
                      <div class="card-sub">点击查看</div>
                    </div>
                    <div class="card-goods">
                      <img class="goods-pic" :src="resolveMedia(message._payload.goodsPic || message._payload.goods_pic)" alt="" />
                      <div class="goods-meta">
                        <div class="goods-title">{{ message._payload.goodsName || message._payload.goodsTitle || '商品' }}</div>
                      </div>
                    </div>
                  </div>
                </template>
                <template v-else-if="message._payload && message._payload.type === 'image'">
                  <img class="msg-image" :src="resolveMedia(message._payload.url)" alt="" />
                </template>
                <template v-else-if="message._payload && message._payload.type === 'file'">
                  <div class="file-msg">
                    <a
                      class="file-link"
                      :href="resolveFileUrl(message._payload.url)"
                      target="_blank"
                      rel="noreferrer"
                    >
                      {{ message._payload.name || '文件' }}
                    </a>
                    <span class="file-size">{{ formatSize(message._payload.size) }}</span>
                  </div>
                </template>
                <template v-else>
                  <p class="msg-text" @click.stop="openMessageActions(message)">{{ messageText(message) }}</p>
                  <div
                    v-if="String(message.messageId ?? message.message_id ?? '') && String(message.messageId ?? message.message_id ?? '') === String(activeActionMessageId)"
                    class="msg-actions"
                    @click.stop
                  >
                    <el-button text size="small" @click="quoteMessage(message)">引用</el-button>
                    <el-button text size="small" @click="hideMessageLocal(message)">删除</el-button>
                  </div>
                </template>
              </div>
              <div v-if="messageClass(message) !== 'system'" class="message-foot">
                <span v-if="isAutoReply(message)" class="auto-reply">自动回复</span>
                <time>{{ message.createTime }}</time>
                <span
                  v-if="isMine(message)"
                  class="read-state"
                  :class="{ unread: !messageRead(message) }"
                >
                  {{ messageRead(message) ? '已读' : '未读' }}
                </span>
                <el-button
                  v-if="canRevoke(message)"
                  text
                  size="small"
                  class="revoke-btn"
                  @click="revokeMessage(message)"
                >
                  撤回
                </el-button>
              </div>
            </div>
            <div v-if="messageClass(message) === 'mine'" class="msg-avatar">
              <img v-if="avatarUrlFor(message)" :src="avatarUrlFor(message)" alt="" @error="(e) => onAvatarImgError('mine', e)" />
              <span v-else>{{ avatarTextFor(message) }}</span>
            </div>
          </div>
        </div>

        <footer class="composer">
          <div class="composer-tools tool-row">
            <el-popover placement="top-start" width="280" trigger="click">
              <template #reference>
                <button type="button" class="composer-tool-icon" aria-label="表情">
                  <el-icon><ChatDotRound /></el-icon>
                </button>
              </template>
              <div class="emoji-grid">
                <span v-for="e in emojis" :key="e" class="emoji" @click="pickEmoji(e)">{{ e }}</span>
              </div>
            </el-popover>
            <el-upload action="" :http-request="sendImage" :show-file-list="false" accept="image/*">
              <button type="button" class="composer-tool-icon" aria-label="图片">
                <el-icon><Picture /></el-icon>
              </button>
            </el-upload>
            <el-upload action="" :http-request="sendFile" :show-file-list="false">
              <button type="button" class="composer-tool-icon" aria-label="文件">
                <el-icon><FolderOpened /></el-icon>
              </button>
            </el-upload>
          </div>
          <div v-if="quoted?.text" class="quote-bar">
            <span class="quote-title">引用：</span>
            <span class="quote-text">{{ quoted.text }}</span>
            <el-button text size="small" @click="clearQuote">取消</el-button>
          </div>
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="输入消息，按 Enter 发送"
            @keydown.enter.exact.prevent="send"
          />
          <el-button class="composer-send-btn" type="primary" :loading="sending" @click="send">发送</el-button>
        </footer>
      </section>

      <section v-else class="conversation empty-conversation allmart-chat-card">
        <div class="chat-empty">
          <strong>选择一个最近会话开始沟通</strong>
          <p>当前会保留真实会话列表、消息发送和上传入口，后续可继续接入用户端与商家端联动。</p>
        </div>
      </section>

      <aside class="shop-side">
        <div class="side-block side-order">
          <div class="side-head">
            <div class="side-title">订单信息</div>
            <button type="button" class="side-link-btn" @click="openOrderPanel">查看详情 &gt;</button>
          </div>
          <dl>
            <dt>订单状态</dt>
            <dd class="order-status-text">{{ sideOrderInfo.status }}</dd>
            <dt>订单编号</dt>
            <dd>{{ sideOrderInfo.orderNo }}</dd>
            <dt>下单时间</dt>
            <dd>{{ sideOrderInfo.createTime }}</dd>
            <dt>支付金额</dt>
            <dd>{{ sideOrderInfo.payAmount }}</dd>
            <dt>支付方式</dt>
            <dd>{{ sideOrderInfo.payType }}</dd>
            <dt>物流方式</dt>
            <dd>{{ sideOrderInfo.logistics }}</dd>
          </dl>
          <!-- TODO: 后续将右侧订单信息替换为真实订单详情 -->
        </div>

        <div class="side-block">
          <div class="side-title">推荐快捷问题</div>
          <div class="quick-grid">
            <button
              v-for="question in quickQuestions"
              :key="question.label"
              type="button"
              class="quick-btn"
              @click="useQuickText(question.label)"
            >
              <el-icon><component :is="question.icon" /></el-icon>
              <span>{{ question.label }}</span>
            </button>
          </div>
        </div>

        <div class="side-block">
          <div class="side-title">店铺服务</div>
          <div class="service-grid">
            <button
              v-for="service in serviceEntries"
              :key="service.label"
              type="button"
              class="service-btn"
              @click="triggerService(service.action)"
            >
              <el-icon><component :is="service.icon" /></el-icon>
              <span>{{ service.label }}</span>
            </button>
          </div>
          <!-- TODO: 后续将店铺服务入口接真实接口 -->
        </div>

        <div class="side-block">
          <div class="side-title side-title-with-icon">
            <el-icon><Bell /></el-icon>
            <span>温馨提示</span>
          </div>
          <ul class="tips-list">
            <li v-for="tip in reminderTips" :key="tip">{{ tip }}</li>
          </ul>
          <!-- TODO: 后续接入用户端与商家端统一会话接口 -->
        </div>
      </aside>
    </section>

    <footer v-if="isStandaloneShell" class="chat-shell-footer">
      <div class="chat-shell-footer-inner">
        <div class="shell-footer-brand">
          <img :src="brandLogo" alt="AllMart" />
        </div>
        <div class="shell-footer-links">
          <span>客服电话：400-888-9999</span>
          <button type="button">帮助中心</button>
          <button type="button">关于我们</button>
          <button type="button">隐私政策</button>
          <button type="button">服务条款</button>
        </div>
        <div class="shell-footer-copy">© 2025 AllMart. All Rights Reserved.</div>
      </div>
    </footer>
  </main>
</template>

<style scoped>
.chat-page {
  overflow: visible;
  font-size: 12px;
  background: #fff;
}

.chat-page :deep(.el-button--primary) {
  border-color: var(--brand-red);
  background: var(--brand-red);
}

.chat-page :deep(.el-button--primary:hover),
.chat-page :deep(.el-button--primary:focus) {
  border-color: var(--brand-red-dark);
  background: var(--brand-red-dark);
}

.chat-page :deep(.el-button.is-plain),
.chat-page :deep(.el-button--default) {
  border-color: var(--border-light);
  color: var(--text-main);
  background: #fff;
}

.chat-page :deep(.el-button.is-plain:hover),
.chat-page :deep(.el-button--default:hover) {
  border-color: rgba(230, 0, 18, 0.14);
  color: var(--brand-red);
  background: #fff5f6;
}

.chat-page :deep(.el-textarea__wrapper),
.chat-page :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--border-light) inset;
  border-radius: 14px;
}

.chat-page :deep(.el-textarea__wrapper:hover),
.chat-page :deep(.el-input__wrapper:hover),
.chat-page :deep(.el-textarea__wrapper.is-focus),
.chat-page :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(230, 0, 18, 0.22) inset;
}

.chat-page:not(.standalone) {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at 88% 10%, rgba(230, 0, 18, 0.06), transparent 22%),
    linear-gradient(180deg, #ffffff 0%, #fbfbfb 100%);
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.chat-page:not(.standalone) .chat-shell {
  margin-top: 18px;
}

.chat-page.standalone {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at 88% 10%, rgba(230, 0, 18, 0.06), transparent 22%),
    linear-gradient(180deg, #ffffff 0%, #fbfbfb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-page.standalone.shell {
  min-height: 100vh;
  padding: 0;
  overflow-x: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfb 100%);
  display: flex;
  flex-direction: column;
}

.standalone-shell-page {
  min-height: 100vh;
  overflow-x: hidden;
}

.chat-shell-breadcrumb-inner,
.chat-shell-footer-inner {
  width: min(1680px, calc(100vw - 64px));
  margin: 0 auto;
}
.shell-footer-brand img,
.shell-footer-brand img {
  height: 42px;
  display: block;
}

.chat-shell-breadcrumb {
  padding: 24px 0 16px;
  flex: 0 0 auto;
}

.chat-shell-breadcrumb-inner {
  display: flex;
  gap: 10px;
  align-items: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.breadcrumb-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--text-secondary);
  cursor: pointer;
}

.breadcrumb-link:hover {
  color: var(--brand-red);
}

.chat-shell {
  width: min(1680px, calc(100vw - 64px));
  margin: 0 auto;
  height: clamp(720px, calc(100vh - 142px), 860px);
  min-height: 720px;
  max-height: 860px;
  padding-bottom: 0;
  display: grid;
  grid-template-columns: 348px minmax(680px, 1fr) 332px;
  gap: 24px;
  align-items: stretch;
  background: transparent;
  border: 0;
  overflow: visible;
  box-shadow: none;
  flex: 1 1 auto;
}

.chat-page.standalone.shell .chat-shell {
  width: min(1680px, calc(100vw - 64px));
  height: clamp(720px, calc(100vh - 150px), 860px);
  min-height: 720px;
  max-height: 860px;
  transform: none;
  transform-origin: center center;
}

.allmart-chat-card {
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: 20px;
  box-shadow: 0 14px 36px rgba(17, 17, 17, 0.05);
}

.session-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 16px;
  padding: 26px 22px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--text-main);
  font-weight: 800;
  font-size: 17px;
}

.panel-title small {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 400;
}

.session-search {
  width: 100%;
}

.session-search :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 999px;
}

.session-list {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.session-empty {
  display: grid;
  gap: 6px;
  padding: 20px 12px;
  justify-items: center;
  color: var(--text-muted);
  text-align: center;
}

.session-item {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 56px;
  gap: 14px;
  align-items: start;
  min-height: 86px;
  padding: 16px 16px;
  text-align: left;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.session-item.active,
.session-item:hover {
  border-color: rgba(230, 0, 18, 0.12);
  background: #fff3f4;
  box-shadow: 0 10px 24px rgba(230, 0, 18, 0.05);
}

.session-avatar,
.chat-head-logo {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #fff6f6;
  border: 1px solid var(--border-light);
  display: grid;
  place-items: center;
  color: var(--brand-red);
  font-weight: 800;
}

.session-item img,
.chat-head-logo {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 50%;
}

.session-copy {
  display: grid;
  gap: 0;
  min-width: 0;
}

.session-item strong {
  display: block;
  color: var(--text-main);
  font-weight: 800;
  font-size: 15px;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}

.session-item small {
  display: -webkit-box;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
}

.session-id {
  color: var(--text-secondary);
}

.session-extra {
  display: grid;
  justify-items: end;
  align-content: start;
  gap: 10px;
  width: 56px;
  min-width: 56px;
}

.session-extra small {
  margin-top: 0;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
}

.session-item em {
  min-width: 22px;
  height: 22px;
  color: #ffffff;
  font-style: normal;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
  border-radius: 999px;
  background: var(--brand-red);
}

.session-footer-btn {
  min-height: 44px;
  border-radius: 999px;
  border: 1px solid var(--border-light);
  background: #fff;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.conversation {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  min-height: 0;
  background: #ffffff;
  overflow: hidden;
}

.empty-conversation {
  display: grid;
  place-items: center;
  padding: 32px;
}

.chat-empty {
  display: grid;
  gap: 10px;
  max-width: 360px;
  color: var(--text-muted);
  text-align: center;
}

.allmart-chat-section {
  margin: 20px 20px 0;
  border: 1px solid var(--border-light);
  border-radius: 18px;
  background: #fff;
}

.chat-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  min-height: 94px;
  padding: 22px 26px;
  border-bottom: 0;
}

.chat-head-main {
  display: flex;
  gap: 20px;
  align-items: center;
  min-width: 0;
}

.chat-head-logo,
.chat-head-logo img {
  width: 60px;
  height: 60px;
}

.chat-head h1 {
  margin: 0;
  overflow: hidden;
  font-size: 20px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-title p,
.shop-card p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.online-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
  border-radius: 50%;
  background: #2db55d;
  vertical-align: middle;
}

.chat-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.chat-action-btn {
  min-height: 41px;
  padding: 0 20px;
  border-radius: 999px;
}

.chat-action-btn :deep(.el-icon) {
  margin-right: 8px;
  font-size: 16px;
}

.messages {
  display: grid;
  align-content: start;
  flex: 1 1 auto;
  gap: 18px;
  padding: 18px 26px 14px;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fdfdfd 100%);
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.msg-avatar {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  background: #f3f4f6;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  color: #111827;
  font-weight: 800;
  font-size: 12px;
}

.msg-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/*noinspection CssUnusedSymbol*/
.message-row.mine {
  justify-content: flex-end;
}

/*noinspection CssUnusedSymbol*/
.message-row.system {
  justify-content: center;
}

.message-stack {
  display: grid;
  justify-items: start;
  gap: 6px;
  max-width: min(700px, 82%);
}

.mine .message-stack {
  justify-items: end;
}

.system .message-stack {
  justify-items: center;
  max-width: 70%;
}

.bubble {
  padding: 14px 16px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid var(--border-light);
  font-size: 15px;
}

.mine .bubble {
  color: var(--text-main);
  background: #fff1f2;
  border-color: #ffd9de;
}

.system .bubble {
  max-width: 70%;
  color: var(--text-secondary);
  background: var(--bg-soft);
  text-align: center;
}

.bubble p {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.msg-actions {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.auto-reply {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 700;
}

.quote-bar {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: #ffffff;
}

.quote-title {
  color: var(--text-muted);
  font-weight: 700;
  flex: none;
}

.quote-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-msg {
  display: grid;
  gap: 10px;
  min-width: 240px;
}

.card-msg.clickable {
  cursor: pointer;
}

.card-msg.clickable:focus-visible {
  outline: 2px solid var(--brand-red);
  outline-offset: 2px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-weight: 800;
}

.card-sub {
  font-size: 12px;
  opacity: 0.85;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.card-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.card-goods {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--border-light);
  background: rgba(255, 255, 255, 0.65);
}

.mine .card-goods {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
}

.goods-pic {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  object-fit: cover;
  background: #ffffff;
}

.goods-title {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-sub {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.85;
}

.card-body {
  display: grid;
  gap: 8px;
}

.card-line {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 10px;
  font-size: 12px;
}

.card-line .k {
  opacity: 0.8;
}

.card-line .v {
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
  padding: 0 2px;
}

.message-foot time {
  color: var(--text-muted);
  font-size: 11px;
  opacity: 0.8;
}

.read-state {
  color: var(--text-muted);
  font-size: 11px;
  white-space: nowrap;
}

.read-state.unread {
  color: var(--brand-red);
  font-weight: 700;
}

.revoke-btn {
  padding: 0;
  min-height: auto;
  font-size: 11px;
}

.composer {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  padding: 16px 20px 18px;
  margin: 0 20px 20px;
  border: 1px solid var(--border-light);
  border-radius: 20px;
  background: #ffffff;
}

.composer-tools {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-start;
  gap: 24px;
  align-items: center;
}

.composer-tools.tool-row {
  align-items: stretch;
}

.composer-tools > * {
  display: inline-flex;
}

.composer-send-btn {
  min-width: 88px;
  min-height: 40px;
  border-radius: 999px;
  align-self: center;
}

.composer-tool-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: #2f2f2f;
  cursor: pointer;
  border-radius: 10px;
  transition: color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.composer-tool-icon:hover {
  color: var(--brand-red);
  transform: translateY(-1px);
}

.composer-tool-icon:focus,
.composer-tool-icon:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(230, 0, 18, 0.12);
  background: rgba(230, 0, 18, 0.04);
}

.composer-tool-icon :deep(.el-icon) {
  font-size: 22px;
}

.composer-tools :deep(.el-upload) {
  display: inline-flex;
  width: auto;
}

.composer-tools :deep(.el-upload:focus-within) .composer-tool-icon {
  box-shadow: 0 0 0 3px rgba(230, 0, 18, 0.12);
  background: rgba(230, 0, 18, 0.04);
}

.composer :deep(.el-textarea__inner) {
  min-height: 78px !important;
  font-size: 14px;
  line-height: 1.6;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
  padding: 6px;
}

.emoji {
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  text-align: center;
  padding: 6px 0;
  border-radius: 6px;
}

.emoji:hover {
  background: rgba(0, 0, 0, 0.06);
}

.msg-image {
  width: 220px;
  max-width: 52vw;
  border-radius: 10px;
  display: block;
}

.file-msg {
  display: grid;
  gap: 6px;
}

.file-link {
  color: var(--brand-red);
  text-decoration: none;
  font-weight: 700;
  word-break: break-all;
}

.file-size {
  color: var(--text-muted);
  font-size: 11px;
}

.shop-side {
  display: grid;
  align-content: start;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: visible;
  padding-right: 0;
}

.side-block {
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 18px;
  padding: 18px;
  background:
    radial-gradient(circle at 100% 100%, rgba(255, 77, 77, 0.08), transparent 45%),
    linear-gradient(180deg, #ffffff 0%, #fffafa 100%);
  box-shadow: 0 10px 22px rgba(17, 17, 17, 0.035);
}

.side-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.side-link-btn {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.side-link-btn:hover {
  color: var(--brand-red);
}

.side-title {
  font-weight: 800;
  font-size: 18px;
  margin-bottom: 8px;
}

.side-head .side-title {
  margin-bottom: 0;
}

.side-title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-title-with-icon :deep(.el-icon) {
  color: #f4b000;
  font-size: 18px;
}

.side-block dl {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 6px 14px;
  margin: 0;
  font-size: 13px;
  align-items: center;
}

.side-block dt {
  color: var(--text-muted);
  min-height: 24px;
  line-height: 24px;
}

.side-block dd {
  margin: 0;
  overflow: hidden;
  color: var(--text-main);
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-height: 24px;
  padding-right: 2px;
  line-height: 24px;
}

.order-status-text {
  color: var(--brand-red);
  font-weight: 700;
}

.side-order dd:nth-of-type(4) {
  font-weight: 800;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quick-btn,
.service-btn {
  border: 0;
  cursor: pointer;
}

.quick-btn {
  min-height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 10px;
  background: linear-gradient(135deg, #fff5f5 0%, #fff0f0 100%);
  min-width: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-main);
}

.quick-btn :deep(.el-icon) {
  color: var(--brand-red);
  font-size: 15px;
}

.quick-btn span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-btn:hover {
  background: #ffecec;
  color: var(--brand-red);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  align-items: start;
}

.service-btn {
  min-height: auto;
  border-radius: 0;
  background: transparent;
  display: grid;
  justify-items: center;
  align-content: start;
  gap: 5px;
  padding: 2px 0;
  min-width: 0;
}

.service-btn :deep(.el-icon) {
  font-size: 26px;
  color: #2f2f2f;
}

.service-btn span {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-main);
  white-space: nowrap;
  text-align: center;
}

.service-btn:hover span,
.service-btn:hover :deep(.el-icon) {
  color: var(--brand-red);
}

.tips-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 0;
  list-style: none;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.tips-list li {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  color: #777;
}

.tips-list li::before {
  content: '';
  width: 6px;
  height: 6px;
  margin-top: 9px;
  border-radius: 50%;
  background: var(--brand-red);
  flex: 0 0 auto;
}

.chat-shell-footer {
  border-top: 1px solid rgba(17, 17, 17, 0.05);
  background: #fff;
  margin-top: 32px;
  flex: 0 0 auto;
}

.chat-shell-footer-inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 20px;
  align-items: center;
  min-height: 98px;
}

.shell-footer-links {
  display: flex;
  gap: 18px;
  align-items: center;
  color: var(--text-secondary);
  font-size: 14px;
  flex-wrap: wrap;
}

.shell-footer-links button {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.shell-footer-copy {
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 1100px) {
  .chat-shell-breadcrumb-inner,
  .chat-shell-footer-inner,
  .chat-shell {
    width: min(100vw - 24px, 1680px);
  }

  .chat-shell {
    grid-template-columns: 300px minmax(0, 1fr);
    height: clamp(700px, calc(100vh - 150px), 820px);
    min-height: 700px;
    max-height: 820px;
  }

  .shop-side {
    display: none;
  }
}

@media (max-width: 720px) {
  .chat-page {
    padding: 0;
  }

  .chat-shell {
    grid-template-columns: 1fr;
    width: calc(100vw - 16px);
    height: auto;
    min-height: auto;
    max-height: none;
    max-width: 100vw;
    border-radius: 0;
    padding: 12px 0 20px;
  }

  .session-panel {
    display: none;
  }

  .shop-side,
  .conversation {
    height: auto;
    overflow: visible;
  }

  .messages {
    height: auto;
    max-height: none;
  }

  .allmart-chat-section,
  .composer {
    margin-inline: 0;
  }

  .composer {
    grid-template-columns: 1fr;
  }

  .chat-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .chat-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .chat-shell-footer-inner {
    grid-template-columns: 1fr;
    justify-items: start;
    padding: 18px 0;
  }
}
</style>
