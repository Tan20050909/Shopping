<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const shellScale = ref(1)
const confirmedAddrCards = ref({})
const activeActionMessageId = ref(null)
const quoted = ref(null)
const localHidden = ref({})
const defaultUserAvatar = DEFAULT_USER_AVATAR
const defaultShopAvatar = resolveLogo('')
const emojis = [
  '😀', '😁', '😂', '🤣', '😊', '😍', '😘', '😎', '🤔', '😅',
  '😭', '😡', '👍', '👎', '🙏', '👏', '🎉', '💯', '❤️', '🔥',
  '🌹', '☕', '📦', '🚚', '✅'
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

function logoOf(item) {
  return resolveLogo(item?.shopLogo || item?.shop_logo || '')
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
    return logoOf(detail.value) || defaultShopAvatar
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
  const id = Number(route.params.id || 0)
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
  sending.value = true
  try {
    detail.value = await api(`/api/user/chat/sessions/${route.params.id}/messages`, {
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
  if (!detail.value?.merchantId) return
  router.push(`/shops/${detail.value.merchantId}`)
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

function openStandalone() {
  const target = router.resolve({
    path: `/chat/${route.params.id}`,
    query: { standalone: '1', shell: '1', role: 'user' }
  })
  window.open(target.href, '_blank', 'noopener,noreferrer')
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
    :class="{ standalone: isStandalone, shell: isShell }"
    :style="{ '--ww-scale': String(shellScale) }"
  >
    <section class="chat-shell">
      <aside class="session-panel">
        <div class="panel-title">
          <span>聊天列表</span>
          <small>{{ sessions.length }} 个会话</small>
        </div>
        <el-input
          v-model="sessionKeyword"
          class="session-search"
          placeholder="搜索店铺/会话ID"
          clearable
        />
        <div class="session-list">
          <button
            v-for="session in filteredSessions"
            :key="sessionIdOf(session)"
            type="button"
            class="session-item"
            :class="{ active: Number(sessionIdOf(session)) === Number(route.params.id) }"
            @click="openSession(session)"
          >
            <img :src="logoOf(session)" :alt="session.merchantName" />
            <span>
              <strong>{{ session.merchantName }}</strong>
              <small>{{ session.lastMessageContent || '暂无消息' }}</small>
              <small class="session-id">会话 {{ sessionIdOf(session) }}</small>
            </span>
            <span class="session-extra">
              <em v-if="unreadCount(session)">{{ unreadCount(session) }}</em>
            </span>
          </button>
        </div>
      </aside>

      <section v-if="detail" v-loading="loading" class="conversation">
        <header class="chat-head">
          <img class="chat-head-logo" :src="logoOf(detail)" :alt="detail.merchantName" />
          <div class="chat-title">
            <h1>{{ detail.merchantName }}</h1>
            <p>ID：{{ detail.merchantId }} · 店铺客服</p>
          </div>
          <div class="chat-actions">
            <el-button plain @click="useQuickText('您好，我想咨询一下这件商品。')">快捷咨询</el-button>
            <el-button plain @click="openStandalone">新页打开</el-button>
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
            <el-button plain @click="useQuickText('请问什么时候可以发货？')">发货</el-button>
            <el-button plain @click="useQuickText('请问这个商品还有库存吗？')">库存</el-button>
            <el-button plain @click="useQuickText('请问售后政策是什么？')">售后</el-button>
            <el-popover placement="top-start" width="280" trigger="click">
              <template #reference>
                <el-button plain>表情</el-button>
              </template>
              <div class="emoji-grid">
                <span v-for="e in emojis" :key="e" class="emoji" @click="pickEmoji(e)">{{ e }}</span>
              </div>
            </el-popover>
            <el-upload action="" :http-request="sendImage" :show-file-list="false" accept="image/*">
              <el-button plain>图片</el-button>
            </el-upload>
            <el-upload action="" :http-request="sendFile" :show-file-list="false">
              <el-button plain>文件</el-button>
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
          <el-button type="primary" :loading="sending" @click="send">发送</el-button>
        </footer>
      </section>

      <aside v-if="detail" class="shop-side">
        <div class="shop-card">
          <img :src="logoOf(detail)" :alt="detail.merchantName" />
          <strong>{{ detail.merchantName }}</strong>
          <p>{{ detail.shopIntro || '精选商品持续上新。' }}</p>
          <el-button plain @click="enterShop">进入店铺</el-button>
        </div>
        <div class="side-block">
          <div class="side-title">店铺信息</div>
          <dl>
            <dt>综合评分</dt>
            <dd>{{ detail.shopScore || '暂无' }}</dd>
            <dt>商家ID</dt>
            <dd>{{ detail.merchantId }}</dd>
          </dl>
        </div>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.chat-page {
  overflow: hidden;
  font-size: 12px;
}

.chat-page:not(.standalone) {
  min-height: 100vh;
  padding: 18px;
  background: #f3f4f6;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.chat-page:not(.standalone) .chat-shell {
  margin-top: 18px;
}

.chat-page.standalone {
  height: 100vh;
  padding: 18px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-page.standalone.shell {
  position: fixed;
  inset: 0;
  height: 100vh;
  padding: 0;
  overflow: hidden;
  background: radial-gradient(1200px 700px at 20% 10%, rgba(0, 0, 0, 0.06), transparent 60%),
    radial-gradient(900px 600px at 80% 40%, rgba(0, 0, 0, 0.05), transparent 60%),
    #f3f4f6;
}

.chat-shell {
  width: 1140px;
  max-width: calc(100vw - 36px);
  height: 660px;
  max-height: calc(100vh - 36px);
  border-radius: 14px;
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  gap: 12px;
  background: #ffffff;
  border: 1px solid var(--border-light);
  overflow: hidden;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.14);
}

.chat-page.standalone.shell .chat-shell {
  width: 1528px;
  height: 676px;
  max-width: none;
  max-height: none;
  transform: scale(var(--ww-scale, 1));
  transform-origin: center center;
}

.session-panel,
.shop-side {
  background: #ffffff;
}

.session-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 12px;
  padding: 18px 16px;
  border-right: 1px solid var(--border-light);
}

.panel-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--text-main);
  font-weight: 800;
}

.panel-title small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 400;
}

.session-search {
  width: 100%;
}

.session-list {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 0;
  overflow-y: auto;
}

.session-item {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 12px;
  text-align: left;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.session-item.active,
.session-item:hover {
  border-color: var(--brand-red);
  background: #fffafa;
}

.session-item img,
.chat-head-logo {
  width: 46px;
  height: 46px;
  object-fit: cover;
  border-radius: 50%;
  background: #ffffff;
  border: 1px solid var(--border-light);
}

.session-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item strong {
  display: block;
  color: var(--text-main);
  font-weight: 800;
  line-height: 1.35;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
  word-break: break-all;
}

.session-item small {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 12px;
}

.session-id {
  color: var(--text-secondary);
}

.session-extra {
  display: grid;
  justify-items: end;
  gap: 8px;
  min-width: 24px;
}

.session-item em {
  min-width: 20px;
  height: 20px;
  color: #ffffff;
  font-style: normal;
  line-height: 20px;
  text-align: center;
  border-radius: 999px;
  background: var(--brand-red);
}

.conversation {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-width: 0;
  min-height: 0;
  background: #ffffff;
}

.chat-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  min-height: 70px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
}

.chat-head h1 {
  margin: 0;
  overflow: hidden;
  font-size: 16px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-title p,
.shop-card p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.chat-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.messages {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 16px;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--bg-soft);
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg-avatar {
  width: 34px;
  height: 34px;
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
  max-width: min(560px, 76%);
}

.mine .message-stack {
  justify-items: end;
}

.system .message-stack {
  justify-items: center;
  max-width: 70%;
}

.bubble {
  padding: 10px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid var(--border-light);
  font-size: 12px;
}

.mine .bubble {
  color: #ffffff;
  background: #409eff;
  border-color: #409eff;
}

.system .bubble {
  max-width: 70%;
  color: var(--text-secondary);
  background: var(--bg-soft);
  text-align: center;
}

.bubble p {
  margin: 0;
  line-height: 1.55;
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
  gap: 10px;
  align-items: center;
  padding: 0 2px;
}

.message-foot time {
  color: var(--text-muted);
  font-size: 11px;
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
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 12px 16px 14px;
  border-top: 1px solid var(--border-light);
  background: #ffffff;
}

.composer-tools {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.composer-tools.tool-row {
  align-items: stretch;
}

.composer-tools > * {
  width: 100%;
}

.composer-tools :deep(.el-upload) {
  width: 100%;
}

.composer-tools :deep(.el-button) {
  width: 100%;
  height: 32px;
  padding: 0 12px;
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
  gap: 18px;
  padding: 18px;
  border-left: 1px solid var(--border-light);
}

.shop-card,
.side-block {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 18px;
  background: #ffffff;
}

.shop-card {
  display: grid;
  justify-items: start;
  gap: 10px;
}

.shop-card img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border-light);
}

.shop-card strong {
  font-size: 15px;
  line-height: 1.4;
}

.side-title {
  margin-bottom: 14px;
  font-weight: 800;
}

.side-block dl {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px 14px;
  margin: 0;
  font-size: 12px;
}

.side-block dt {
  color: var(--text-muted);
}

.side-block dd {
  margin: 0;
  overflow: hidden;
  color: var(--text-main);
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .chat-shell {
    grid-template-columns: 240px minmax(0, 1fr);
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
    height: 100vh;
    border-radius: 0;
    border: 0;
  }

  .session-panel {
    display: none;
  }

  .composer {
    grid-template-columns: 1fr;
  }

  .chat-head {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .chat-actions {
    grid-column: 1 / -1;
  }
}
</style>
