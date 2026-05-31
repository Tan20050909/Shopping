<template>
  <div
    ref="pageRef"
    class="cs-page"
    :class="{ standalone: isStandalone, embedded: isEmbedded, shell: isShell }"
    :style="{ height: pageHeight ? `${pageHeight}px` : '', '--ww-scale': String(shellScale) }"
  >
    <el-card class="cs-card" shadow="never">
      <template #header v-if="!(isEmbedded || isShell)">
        <div class="card-header">
          <span>客服管理</span>
          <el-button plain @click="loadContacts">刷新</el-button>
        </div>
      </template>

      <div class="cs-layout">
        <div class="cs-left">
          <el-input v-model="keyword" placeholder="搜索昵称/用户ID" clearable />
          <div class="cs-list">
            <div
              v-for="c in filteredContacts"
              :key="c.key"
              class="cs-item"
              :class="{ active: c.key === activeKey }"
              @click="selectContact(c)"
            >
              <div class="cs-item-avatar">
                <img v-if="avatarUrl(c)" class="avatar-img" :src="avatarUrl(c)" alt="" @error="() => markAvatarBroken(c?.avatar)" />
                <span v-else>{{ avatarText(c) }}</span>
              </div>
              <div class="cs-item-main">
                <div class="cs-item-title">
                  <span class="nick">{{ c.nick }}</span>
                  <el-button text size="small" class="open-btn" @click.stop="openNewPage(c)">新页</el-button>
                </div>
                <div class="cs-item-sub">{{ c.sub }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="cs-center">
          <div v-if="activeContact" class="cs-chat">
            <div class="cs-chat-head">
              <div class="head-left">
                <div class="head-avatar">
                  <img
                    v-if="avatarUrl(activeContact)"
                    class="avatar-img"
                    :src="avatarUrl(activeContact)"
                    alt=""
                    @error="() => markAvatarBroken(activeContact?.avatar)"
                  />
                  <span v-else>{{ avatarText(activeContact) }}</span>
                </div>
                <div class="head-meta">
                  <div class="head-title">{{ activeContact.nick }}</div>
                  <div class="head-sub">{{ activeContact.sub }}</div>
                </div>
              </div>
              <div class="head-right">
                <el-button plain @click="openFaqDialog">快捷问答</el-button>
                <el-button plain @click="openNewPage(activeContact)">新页打开</el-button>
              </div>
            </div>

            <div ref="chatBodyRef" class="cs-chat-body" @click="onChatAreaClick">
              <div class="order-strip" v-if="unfinishedOrders.length" @click.stop>
                <div class="strip-title">最近未完成订单</div>
                <div class="strip-list">
                  <div v-for="o in unfinishedOrders" :key="o.id" class="strip-card" @click="openOrder(o)">
                    <div class="strip-no">{{ o.orderNo }}</div>
                    <div class="strip-meta">
                      <span class="strip-status">{{ statusText(o.status) }}</span>
                      <span class="strip-price">¥ {{ toMoney(o.payAmount || o.totalAmount) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-for="(m, idx) in messages" :key="idx" class="msg-row" :class="m.from === 'merchant' ? 'me' : m.from === 'user' ? 'user' : 'sys'">
                <div v-if="m.from !== 'merchant'" class="msg-avatar">
                  <img
                    v-if="avatarUrl(activeContact)"
                    class="avatar-img"
                    :src="avatarUrl(activeContact)"
                    alt=""
                    @error="() => markAvatarBroken(activeContact?.avatar)"
                  />
                  <span v-else>{{ avatarText(activeContact) }}</span>
                </div>
                <div class="msg-content">
                  <div v-if="m.type === 'addr_confirm'" class="card-msg">
                    <div class="card-top">
                      <div class="card-title">请确认收货地址</div>
                      <div class="card-sub">{{ m.orderNo || '-' }}</div>
                    </div>
                    <div class="card-goods" v-if="msgOrderPic(m)">
                      <img class="goods-pic" :src="resolveImg(msgOrderPic(m))" alt="" @error="onImgError" />
                      <div class="goods-meta">
                        <div class="goods-title">{{ m.goodsTitle || '下单商品' }}</div>
                        <div class="goods-sub">
                          <span class="goods-price">¥ {{ toMoney(msgOrderAmount(m)) }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="card-body">
                      <div class="card-line"><span class="k">收货人</span><span class="v">{{ m.consignee || '-' }}</span></div>
                      <div class="card-line"><span class="k">手机号</span><span class="v">{{ m.phone || '-' }}</span></div>
                      <div class="card-line"><span class="k">详细地址</span><span class="v">{{ m.addr || '-' }}</span></div>
                    </div>
                    <div class="card-actions">
                      <el-button size="small" disabled>修改地址</el-button>
                      <el-button size="small" type="primary" disabled>确认</el-button>
                    </div>
                    <div class="card-tip">此卡片为买家端操作，商家端仅展示</div>
                  </div>
                  <div v-else-if="m.type === 'goods_inquiry'" class="card-msg card-link" @click="openGoods(m)">
                    <div class="card-top">
                      <div class="card-title">正在咨询商品</div>
                      <div class="card-sub">点击查看</div>
                    </div>
                    <div class="card-goods">
                      <img class="goods-pic" :src="resolveImg(m.goodsPic || m.goods_pic || '')" alt="" @error="onImgError" />
                      <div class="goods-meta">
                        <div class="goods-title">{{ m.goodsName || m.goodsTitle || '商品' }}</div>
                      </div>
                    </div>
                  </div>
                  <div v-else-if="m.type === 'image'" class="msg-image-wrap" @click.stop="openMessageActions(m)">
                    <img class="msg-image" :src="resolveImg(m.url)" alt="" @error="onImgError" />
                  </div>
                  <div v-else-if="m.type === 'file'" class="msg-file" @click.stop="openMessageActions(m)">
                    <a class="file-link" :href="resolveFileUrl(m.url)" target="_blank" rel="noreferrer">{{ m.name || '文件' }}</a>
                    <div v-if="m.size" class="file-sub">{{ formatSize(m.size) }}</div>
                  </div>
                  <div v-else-if="m.type === 'coupon'" class="msg-coupon" @click.stop="openMessageActions(m)">
                    <div class="coupon-title">优惠券已发放</div>
                    <div class="coupon-sub">{{ m.couponName || '店铺优惠券' }}</div>
                  </div>
                  <div v-else class="msg-bubble" @click.stop="openMessageActions(m)">{{ m.text }}</div>
                  <div v-if="isActionOpen(m)" class="msg-actions" @click.stop>
                    <el-button v-if="m.type === 'text'" text size="small" @click="quoteMessage(m)">引用</el-button>
                    <el-button text size="small" @click="hideMessageLocal(m)">删除</el-button>
                  </div>
                  <div class="msg-meta">
                    <span v-if="m.autoReply" class="msg-auto">自动回复</span>
                    <span
                      v-if="m.from === 'merchant'"
                      class="msg-read"
                      :class="isMsgRead(m) ? 'read' : 'unread'"
                    >
                      {{ isMsgRead(m) ? '已读' : '未读' }}
                    </span>
                    <el-button v-if="canRevokeMessage(m)" text size="small" class="msg-revoke" @click="revokeMessage(m)">撤回</el-button>
                    <span class="msg-time">{{ formatTime(m.time) }}</span>
                  </div>
                </div>
                <div v-if="m.from === 'merchant'" class="msg-avatar me">
                  <img
                    v-if="merchantAvatarUrl"
                    class="avatar-img"
                    :src="merchantAvatarUrl"
                    alt=""
                    @error="() => markAvatarBroken(merchantAvatarRaw)"
                  />
                  <span v-else>{{ merchantAvatarText }}</span>
                </div>
              </div>
            </div>

            <div class="cs-chat-foot">
              <div class="tool-row">
                <el-popover placement="top-start" width="280" trigger="click">
                  <template #reference>
                    <el-button plain class="tool-btn">表情</el-button>
                  </template>
                  <div class="emoji-grid">
                    <span v-for="e in emojis" :key="e" class="emoji" @click="pickEmoji(e)">{{ e }}</span>
                  </div>
                </el-popover>
                <el-upload action="" :http-request="sendImage" :show-file-list="false" accept="image/*">
                  <el-button plain class="tool-btn">图片</el-button>
                </el-upload>
                <el-upload action="" :http-request="sendFile" :show-file-list="false">
                  <el-button plain class="tool-btn">文件</el-button>
                </el-upload>
                <el-button plain class="tool-btn" :disabled="!activeContact" @click="openCouponDialog">发券</el-button>
              </div>

              <div v-if="faqList.length && showFaqPanel" class="faq-row" @click.stop>
                <div class="faq-label">常见问题</div>
                <div class="faq-list">
                  <el-button v-for="(qa, idx) in faqList" :key="idx" plain size="small" class="faq-btn" @click="chooseFaq(qa)">
                    {{ qa.q }}
                  </el-button>
                </div>
              </div>

              <div v-if="quoted?.text" class="quote-bar" @click.stop>
                <span class="quote-title">引用：</span>
                <span class="quote-text">{{ quoted.text }}</span>
                <el-button text size="small" @click="clearQuote">取消</el-button>
              </div>

              <div class="input-row">
                <el-input
                  v-model="draft"
                  type="textarea"
                  :rows="2"
                  placeholder="输入消息，Enter发送"
                  @keydown.enter.exact.prevent="send"
                />
                <el-button type="primary" class="send-btn" @click="send">发送</el-button>
              </div>
            </div>
          </div>

          <el-empty v-else description="请选择一个用户开始聊天" />
        </div>

        <div class="cs-side" v-if="isStandalone">
          <div v-if="activeContact" class="side-card">
            <div class="side-head">
              <div class="side-title">{{ merchantName }}</div>
              <el-button size="small" plain @click="openShop">进入小店</el-button>
            </div>
            <div class="side-section">
              <div class="side-label">我的订单</div>
              <div v-if="latestOrder" class="side-order" @click="openOrder(latestOrder)">
                <div class="side-order-row">
                  <img class="side-order-pic" :src="resolveImg(orderThumb(latestOrder))" alt="" @error="onImgError" />
                  <div class="side-order-main">
                    <div class="side-order-no">{{ latestOrder.orderNo || '-' }}</div>
                    <div class="side-order-meta">
                      <span>实付款 ¥ {{ toMoney(latestOrder.payAmount || latestOrder.totalAmount) }}</span>
                    </div>
                    <div class="side-order-time">{{ formatTime(latestOrder.createTime) }}</div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无订单" :image-size="72" />
            </div>
          </div>
          <el-empty v-else description="请选择用户" />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="faqDialogVisible" title="快捷问答设置" width="720px">
      <div class="faq-editor">
        <div v-for="(it, idx) in faqDraft" :key="idx" class="faq-editor-row">
          <el-input v-model="it.q" placeholder="问题" />
          <el-input v-model="it.a" placeholder="回答" />
          <el-button plain type="danger" @click="removeFaq(idx)">删除</el-button>
        </div>
        <div class="faq-editor-actions">
          <el-button plain @click="addFaq">新增</el-button>
          <el-button type="primary" @click="saveFaq">保存</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="couponDialogVisible" title="发放店铺优惠券" width="640px">
      <el-skeleton v-if="couponLoading" :rows="4" animated />
      <div v-else class="coupon-panel">
        <el-empty v-if="!couponList.length" description="暂无可发放的优惠券" />
        <div v-else class="coupon-list">
          <div
            v-for="c in couponList"
            :key="c.id"
            class="coupon-item"
            :class="{ active: Number(selectedCouponId) === Number(c.id) }"
            @click="selectedCouponId = c.id"
          >
            <div class="coupon-name">{{ c.name }}</div>
            <div class="coupon-meta">
              <span>剩余 {{ c.surplusNum ?? 0 }}</span>
              <span v-if="couponPerLimitText(c)">每人限领 {{ couponPerLimitText(c) }}</span>
            </div>
          </div>
        </div>
        <div class="coupon-actions">
          <el-button plain @click="couponDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!selectedCouponId" @click="grantCoupon">确认发放</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatApi, couponApi, orderApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'
import { resolveProductImage } from '@/utils/image'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const contacts = ref([])
const activeKey = ref('')
const activeContact = ref(null)
const activeSession = ref(null)
const messages = ref([])
const draft = ref('')
const activeActionMessageId = ref(null)
const quoted = ref(null)
const localHidden = ref({})
const chatBodyRef = ref(null)
const orderSummary = ref([])
const selectSeq = ref(0)
const pageRef = ref(null)
const pageHeight = ref(0)
const isEmbedded = ref(false)

const isStandalone = computed(() => String(route.query?.standalone || '') === '1')
const isShell = computed(() => String(route.query?.shell || '') === '1')
const shellScale = ref(1)

const merchantName = computed(() => {
  try {
    const raw = localStorage.getItem('merchantUser')
    const u = raw ? JSON.parse(raw) : {}
    return String(u?.merchantName || u?.username || '商家店铺')
  } catch (e) {
    return '商家店铺'
  }
})

const chatCache = new Map()

const emojis = ['😀','😄','😁','😊','😍','😘','😎','🤔','😅','😭','😡','👍','👎','🙏','🎉','❤️','💯','🔥','✨','⭐']

const defaultImage = 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2280%22%20height%3D%2280%22%20viewBox%3D%220%200%2080%2080%22%3E%3Crect%20width%3D%2280%22%20height%3D%2280%22%20rx%3D%2214%22%20fill%3D%22%23f3f4f6%22/%3E%3Cpath%20d%3D%22M22%2028h36v24H22z%22%20fill%3D%22%23e5e7eb%22/%3E%3Cpath%20d%3D%22M26%2048l10-10%208%208%2010-12%22%20stroke%3D%22%23cbd5e1%22%20stroke-width%3D%223%22%20fill%3D%22none%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22/%3E%3Ccircle%20cx%3D%2232%22%20cy%3D%2236%22%20r%3D%223.5%22%20fill%3D%22%23cbd5e1%22/%3E%3C/svg%3E'

const resolveImg = (src) => resolveProductImage(src, defaultImage)

const onImgError = (e) => {
  if (e?.target) e.target.src = defaultImage
}

const faqDialogVisible = ref(false)
const faqList = ref([])
const faqDraft = ref([])
const showFaqPanel = ref(false)

const faqKey = () => `cs:faq:${getMerchantId()}`

const loadFaqList = () => {
  try {
    const raw = localStorage.getItem(faqKey()) || ''
    const parsed = raw ? JSON.parse(raw) : null
    const list = Array.isArray(parsed) ? parsed : []
    faqList.value = list
  } catch (e) {
    faqList.value = []
  }
  if (!faqList.value.length) {
    faqList.value = [
      { q: '发什么快递？', a: '默认发顺丰/圆通，具体以实际发货为准。' },
      { q: '多久发货？', a: '一般 24 小时内发货，特殊情况会延迟。' }
    ]
  }
}

const openFaqDialog = () => {
  faqDraft.value = (faqList.value || []).map(x => ({ q: String(x?.q || ''), a: String(x?.a || '') }))
  if (!faqDraft.value.length) faqDraft.value = [{ q: '', a: '' }]
  faqDialogVisible.value = true
}

const addFaq = () => {
  faqDraft.value.push({ q: '', a: '' })
}

const removeFaq = (idx) => {
  faqDraft.value.splice(idx, 1)
  if (!faqDraft.value.length) faqDraft.value = [{ q: '', a: '' }]
}

const saveFaq = () => {
  const cleaned = (faqDraft.value || [])
    .map(x => ({ q: String(x?.q || '').trim(), a: String(x?.a || '').trim() }))
    .filter(x => x.q && x.a)
  faqList.value = cleaned
  try {
    localStorage.setItem(faqKey(), JSON.stringify(cleaned))
  } catch (e) {
  }
  faqDialogVisible.value = false
  ElMessage.success('已保存')
}

const onChatAreaClick = () => {
  activeActionMessageId.value = null
  if (!faqList.value.length) return
  showFaqPanel.value = true
}

const hiddenStorageKey = () => `cs:hidden:${getMerchantId()}:${String(activeKey.value || '')}`

const loadLocalHidden = () => {
  try {
    const raw = localStorage.getItem(hiddenStorageKey()) || ''
    const parsed = raw ? JSON.parse(raw) : null
    localHidden.value = parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    localHidden.value = {}
  }
}

const persistLocalHidden = () => {
  try {
    localStorage.setItem(hiddenStorageKey(), JSON.stringify(localHidden.value || {}))
  } catch (e) {
  }
}

const isHidden = (m) => {
  const id = String(m?.messageId ?? '')
  if (!id) return false
  return Boolean(localHidden.value?.[id])
}

const openMessageActions = (m) => {
  const id = String(m?.messageId ?? '')
  if (!id) return
  activeActionMessageId.value = activeActionMessageId.value === id ? null : id
}

const isActionOpen = (m) => {
  const id = String(m?.messageId ?? '')
  if (!id) return false
  return String(activeActionMessageId.value || '') === id
}

const quotePreview = (m) => {
  if (!m) return '消息'
  if (m.type === 'text') {
    const t = String(m.text || '').trim()
    if (!t) return '消息'
    return t.length > 80 ? `${t.slice(0, 80)}…` : t
  }
  if (m.type === 'image') return '[图片]'
  if (m.type === 'file') return '[文件]'
  if (m.type === 'coupon') return '[优惠券]'
  if (m.type === 'addr_confirm') return '[地址确认卡片]'
  if (m.type === 'goods_inquiry') return '[商品咨询卡片]'
  return '[消息]'
}

const quoteMessage = (m) => {
  quoted.value = { messageId: m?.messageId ?? null, text: quotePreview(m) }
  activeActionMessageId.value = null
}

const clearQuote = () => {
  quoted.value = null
}

const hideMessageLocal = (m) => {
  const id = String(m?.messageId ?? '')
  if (!id) return
  localHidden.value = { ...(localHidden.value || {}), [id]: true }
  persistLocalHidden()
  activeActionMessageId.value = null
  const filtered = (messages.value || []).filter(x => String(x?.messageId ?? '') !== id)
  messages.value = filtered
  const key = activeKey.value
  if (key) {
    const cached = chatCache.get(key)
    chatCache.set(key, { session: cached?.session || activeSession.value, messages: filtered.slice(), time: Date.now() })
  }
}

const chooseFaq = async (qa) => {
  if (!activeContact.value) return
  const q = String(qa?.q || '').trim()
  const a = String(qa?.a || '').trim()
  if (!q || !a) return
  await sendText(a)
  showFaqPanel.value = false
}

const resolveFileUrl = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return ''
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  const idx = v.indexOf('/uploads/')
  if (idx > 0) return v.slice(idx)
  if (v.startsWith('/')) return v
  return `/${v}`
}

const formatSize = (bytes) => {
  const n = Number(bytes ?? 0)
  if (!Number.isFinite(n) || n <= 0) return ''
  const kb = 1024
  const mb = kb * 1024
  if (n < kb) return `${n} B`
  if (n < mb) return `${(n / kb).toFixed(1)} KB`
  return `${(n / mb).toFixed(1)} MB`
}

const sendImage = async (options) => {
  if (!activeContact.value) {
    ElMessage.warning('请先选择一个用户')
    return
  }
  try {
    const file = options?.file
    const res = await uploadApi.uploadImage(file)
    const url = res?.data?.url || ''
    if (!url) throw new Error('上传失败')
    await sendPayload({ type: 'image', url, name: file?.name, size: file?.size })
    options?.onSuccess?.(res?.data, file)
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.response?.data?.message || e?.message || '图片发送失败')
  }
}

const sendFile = async (options) => {
  if (!activeContact.value) {
    ElMessage.warning('请先选择一个用户')
    return
  }
  try {
    const file = options?.file
    const res = await uploadApi.uploadFile(file)
    const url = res?.data?.url || ''
    const name = res?.data?.name || file?.name || '文件'
    if (!url) throw new Error('上传失败')
    await sendPayload({ type: 'file', url, name, size: file?.size })
    options?.onSuccess?.(res?.data, file)
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error(e?.response?.data?.message || e?.message || '文件发送失败')
  }
}

const couponDialogVisible = ref(false)
const couponLoading = ref(false)
const couponList = ref([])
const selectedCouponId = ref(null)

const couponPerLimitText = (c) => {
  const limitEnabled = c?.limitEnabled == null ? null : Number(c.limitEnabled)
  const per = c?.perLimit == null ? null : Number(c.perLimit)
  if (limitEnabled === 0) return ''
  if (per != null && per <= 0) return ''
  const v = per == null ? 1 : per
  return v < 1 ? 1 : v
}

const openCouponDialog = async () => {
  if (!activeContact.value) return
  couponDialogVisible.value = true
  couponLoading.value = true
  selectedCouponId.value = null
  try {
    const res = await couponApi.list(getMerchantId())
    const list = Array.isArray(res?.data) ? res.data : []
    couponList.value = list
      .filter(c => Number(c?.status) === 1)
      .sort((a, b) => Number(b?.id || 0) - Number(a?.id || 0))
  } catch (e) {
    couponList.value = []
    ElMessage.error(e?.response?.data?.message || '加载优惠券失败')
  } finally {
    couponLoading.value = false
  }
}

const grantCoupon = async () => {
  if (!activeContact.value) return
  const userId = activeContact.value?.userId
  const couponId = selectedCouponId.value
  if (!userId || !couponId) return
  try {
    await couponApi.grant({ merchantId: getMerchantId(), couponId, userId })
    const picked = couponList.value.find(x => Number(x?.id) === Number(couponId))
    await sendPayload({ type: 'coupon', couponId, couponName: picked?.name || '店铺优惠券' })
    couponDialogVisible.value = false
    ElMessage.success('已发放')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '发放失败')
  }
}

const merchantAvatarText = computed(() => {
  try {
    const raw = localStorage.getItem('merchantUser')
    const u = raw ? JSON.parse(raw) : {}
    const name = String(u?.merchantName || u?.username || '商家').trim()
    return name ? name[0] : '商'
  } catch (e) {
    return '商'
  }
})

const formatTime = (t) => {
  const d = new Date(t || Date.now())
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}`
}

const filteredContacts = computed(() => {
  const k = String(keyword.value || '').trim()
  if (!k) return contacts.value
  return contacts.value.filter(c => `${c.nick || ''} ${c.sub || ''} ${c.key || ''}`.includes(k))
})

const toMoney = (value) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const statusText = (status) => {
  const s = Number(status)
  const texts = ['待付款', '待发货', '已发货', '已完成']
  return texts[s] || '未知'
}

const loadContacts = async () => {
  try {
    const merchantId = getMerchantId()
    const res = await chatApi.listSessions(merchantId)
    const sessions = Array.isArray(res?.data) ? res.data : []
    const map = new Map()
    for (const s of sessions) {
      const userIdRaw = s?.userId != null ? String(s.userId) : ''
      const key = userIdRaw ? `u:${userIdRaw}` : ''
      if (!key) continue
      if (map.has(key)) continue
      const nick = String(s?.userNickname || '').trim() || `用户${userIdRaw}`
      const summary = String(s?.lastMessageContent || '').trim()
      const sub = userIdRaw ? `ID：${userIdRaw}${summary ? ` · ${summary}` : ''}` : (summary || '')
      const avatar = String(s?.userAvatar || '').trim()
      map.set(key, { key, nick, sub, avatar, userId: Number(userIdRaw) })
    }
    contacts.value = Array.from(map.values())
    const wanted = String(route.query?.key || '').trim()
    const found = wanted ? contacts.value.find(x => x.key === wanted) : null
    if (found) {
      selectContact(found)
      return
    }
    if (!activeKey.value && contacts.value.length) {
      selectContact(contacts.value[0])
    }
  } catch (error) {
    contacts.value = []
  }
}

const parseUserIdFromKey = (key) => {
  const s = String(key || '')
  if (!s.startsWith('u:')) return null
  const id = Number(s.slice(2))
  return Number.isFinite(id) ? id : null
}

const loadOrderSummary = async (contactKey) => {
  orderSummary.value = []
  const userId = parseUserIdFromKey(contactKey)
  if (!userId) return
  try {
    const res = await orderApi.list(getMerchantId(), null, userId)
    const orders = Array.isArray(res.data) ? res.data : []
    orderSummary.value = orders
      .slice()
      .sort((a, b) => {
        const ta = new Date(String(a.createTime || '').replace(' ', 'T')).getTime() || 0
        const tb = new Date(String(b.createTime || '').replace(' ', 'T')).getTime() || 0
        return tb - ta
      })
      .slice(0, 10)
  } catch (error) {
    orderSummary.value = []
  }
}

const parseTimeValue = (v) => {
  if (v == null) return Date.now()
  if (typeof v === 'number') return v
  const s = String(v).trim()
  if (!s) return Date.now()
  const t = new Date(s.includes(' ') ? s.replace(' ', 'T') : s).getTime()
  return Number.isFinite(t) && t > 0 ? t : Date.now()
}

const canRevokeMessage = (m) => {
  if (!m || m.from !== 'merchant' || m.autoReply) return false
  const id = Number(m.messageId || 0)
  if (!Number.isFinite(id) || id <= 0) return false
  const ts = parseTimeValue(m.time)
  return Date.now() - ts <= 2 * 60 * 1000
}

const revokeMessage = async (m) => {
  if (!canRevokeMessage(m)) return
  try {
    await chatApi.revokeMessage({ messageId: m.messageId, senderType: 2, senderId: getMerchantId() })
    const filtered = (messages.value || []).filter(x => String(x?.messageId || '') !== String(m.messageId))
    messages.value = filtered
    const key = activeKey.value
    if (key) {
      const cached = chatCache.get(key)
      chatCache.set(key, { session: cached?.session || activeSession.value, messages: filtered.slice(), time: Date.now() })
    }
    ElMessage.success('已撤回')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '撤回失败')
  }
}

const safeJsonParse = (s) => {
  try {
    return JSON.parse(String(s || ''))
  } catch (e) {
    return null
  }
}

const mapApiMessage = (m) => {
  const senderType = Number(m?.senderType ?? m?.sender_type)
  const receiverType = Number(m?.receiverType ?? m?.receiver_type)
  const messageType = Number(m?.messageType ?? m?.message_type)
  const relatedType = Number(m?.relatedType ?? m?.related_type)
  const time = parseTimeValue(m?.createTime ?? m?.create_time)
  const isRead = Number(m?.isRead ?? m?.is_read) === 1
  const autoReply = senderType === 3
  let from =
    autoReply
      ? 'merchant'
      : senderType === 4 && messageType === 3 && relatedType === 1
      ? 'user'
      : senderType === 4 || messageType === 5
        ? 'sys'
        : senderType === 2
          ? 'merchant'
          : senderType === 1
            ? 'user'
            : receiverType === 2
              ? 'merchant'
              : receiverType === 1
                ? 'user'
                : 'sys'
  if (messageType === 2) {
    return { from, autoReply, type: 'image', url: String(m?.content || ''), time, read: from === 'merchant' ? isRead : true, messageId: m?.messageId ?? m?.message_id }
  }
  if (messageType === 1) {
    return { from, autoReply, type: 'text', text: String(m?.content || ''), time, read: from === 'merchant' ? isRead : true, messageId: m?.messageId ?? m?.message_id }
  }
  const payload = safeJsonParse(m?.content)
  const t = String(payload?.type || 'text')
  if (from === 'sys' && payload && typeof payload === 'object') {
    const richTypes = new Set(['addr_confirm', 'goods_inquiry', 'coupon'])
    if (richTypes.has(String(payload.type || ''))) {
      if (receiverType === 1) from = 'merchant'
      else if (receiverType === 2) from = 'user'
    }
  }
  const ui = { from, autoReply, type: t, time, read: from === 'merchant' ? isRead : true, messageId: m?.messageId ?? m?.message_id }
  for (const k of Object.keys(payload || {})) {
    if (k === 'type') continue
    if (k === 'from' || k === 'time' || k === 'read' || k === 'messageId') continue
    ui[k] = payload[k]
  }
  if (ui.type === 'text' && ui.text == null) ui.text = String(m?.content || '')
  return ui
}

const ensureSession = async (userId, contactKey, seq) => {
  if (!userId) return null
  try {
    const res = await chatApi.getSession(getMerchantId(), userId)
    if (seq != null && seq !== selectSeq.value) return null
    const s = res?.data || null
    activeSession.value = s
    if (contactKey) {
      const cached = chatCache.get(contactKey)
      chatCache.set(contactKey, { session: s, messages: cached?.messages || [], time: Date.now() })
    }
    return s
  } catch (e) {
    if (seq == null || seq === selectSeq.value) {
      activeSession.value = null
    }
    return null
  }
}

const loadMessages = async (sessionId, contactKey, seq) => {
  if (!sessionId) {
    if (seq == null || seq === selectSeq.value) messages.value = []
    return
  }
  try {
    const res = await chatApi.listMessages(sessionId, null, 200)
    const list = Array.isArray(res?.data) ? res.data : []
    if (seq == null || seq === selectSeq.value) {
      const ui = list.map(mapApiMessage).filter(x => !isHidden(x))
      const seenAddr = new Set()
      const deduped = []
      for (let i = ui.length - 1; i >= 0; i -= 1) {
        const m = ui[i]
        if (m && m.type === 'addr_confirm') {
          const k = `o:${Number(m.orderId) || 0}`
          if (seenAddr.has(k)) continue
          seenAddr.add(k)
        }
        deduped.push(m)
      }
      deduped.reverse()
      messages.value = deduped
      if (contactKey) {
        const cached = chatCache.get(contactKey)
        chatCache.set(contactKey, { session: cached?.session || activeSession.value, messages: deduped, time: Date.now() })
      }
    }
  } catch (e) {
    if (seq == null || seq === selectSeq.value) messages.value = []
  }
  if (seq == null || seq === selectSeq.value) {
    await nextTick()
    if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

const pruneAddrConfirmByPaid = (contactKey) => {
  const paid = new Set((orderSummary.value || []).filter(o => Number(o?.payStatus) === 1 || !!o?.payTime).map(o => Number(o?.id)))
  const filtered = (messages.value || []).filter(m => {
    if (!m || m.type !== 'addr_confirm') return true
    const oid = Number(m.orderId)
    if (!Number.isFinite(oid) || oid <= 0) return false
    return paid.has(oid)
  })
  messages.value = filtered
  if (contactKey) {
    const cached = chatCache.get(contactKey)
    chatCache.set(contactKey, { session: cached?.session || activeSession.value, messages: filtered.slice(), time: Date.now() })
  }
}

const selectContact = async (c) => {
  const seq = (selectSeq.value += 1)
  activeKey.value = c.key
  activeContact.value = c
  showFaqPanel.value = false
  activeActionMessageId.value = null
  quoted.value = null
  loadLocalHidden()

  const cached = chatCache.get(c.key)
  if (cached?.session) activeSession.value = cached.session
  if (Array.isArray(cached?.messages) && cached.messages.length) {
    messages.value = cached.messages.filter(x => !isHidden(x)).slice()
  } else {
    messages.value = []
  }
  orderSummary.value = []

  const pOrder = loadOrderSummary(c.key)

  const session = await ensureSession(c.userId, c.key, seq)
  if (seq !== selectSeq.value) return
  const sessionId = session?.sessionId
  if (sessionId) {
    chatApi.markRead(sessionId, 2, getMerchantId()).catch(() => {})
  }
  const pMsg = loadMessages(sessionId, c.key, seq)
  await Promise.all([pOrder, pMsg])
  if (seq !== selectSeq.value) return
  pruneAddrConfirmByPaid(c.key)
  ensureAddressCards()
}

const send = async () => {
  const raw = String(draft.value || '').trim()
  if (!raw) return
  if (!activeContact.value) {
    ElMessage.warning('请先选择一个用户')
    return
  }
  const text = quoted.value?.text ? `引用：${quoted.value.text}\n${raw}` : raw
  draft.value = ''
  quoted.value = null
  await sendText(text)
}

const openNewPage = (c) => {
  const href = router.resolve({ path: '/chat', query: { standalone: '1', shell: '1', key: c.key } }).href
  window.open(href, '_blank')
}

const brokenAvatars = ref(new Set())

const markAvatarBroken = (raw) => {
  const v = String(raw || '').trim()
  if (!v) return
  if (brokenAvatars.value.has(v)) return
  brokenAvatars.value = new Set([...brokenAvatars.value, v])
}

const avatarText = (c) => {
  const name = String(c?.nick || '').trim()
  if (name) return name[0]
  const k = String(c?.key || '')
  if (k.startsWith('u:')) return 'U'
  return '客'
}

const avatarUrl = (c) => {
  const raw = String(c?.avatar || '').trim()
  if (!raw) return ''
  if (brokenAvatars.value.has(raw)) return ''
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  if (raw.startsWith('/')) return raw
  return `/${raw}`
}

const resolveAvatar = (src) => {
  const raw = String(src || '').trim()
  if (!raw) return ''
  const v = raw.replace(/\\/g, '/')
  if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  if (v.startsWith('/shop/')) return `/uploads${v}`
  if (v.startsWith('shop/')) return `/uploads/${v}`
  if (v.startsWith('/')) return v
  return `/${v}`
}

const merchantAvatarRaw = computed(() => {
  try {
    const raw = localStorage.getItem('merchantUser')
    const u = raw ? JSON.parse(raw) : {}
    return String(u?.avatarUrl || u?.shopLogo || '').trim()
  } catch (e) {
    return ''
  }
})

const merchantAvatarUrl = computed(() => {
  const raw = merchantAvatarRaw.value
  if (!raw || brokenAvatars.value.has(raw)) return ''
  return resolveAvatar(raw)
})

const orderThumb = (o) => {
  const pics = o?.itemPics
  if (Array.isArray(pics) && pics.length) return pics[0]
  return ''
}

const findOrderForMsg = (m) => {
  const list = Array.isArray(orderSummary.value) ? orderSummary.value : []
  if (!list.length) return null
  const id = Number(m?.orderId)
  if (Number.isFinite(id)) {
    const hit = list.find(o => Number(o?.id) === id)
    if (hit) return hit
  }
  const no = String(m?.orderNo || '').trim()
  if (no) {
    const hit = list.find(o => String(o?.orderNo || '').trim() === no)
    if (hit) return hit
  }
  return null
}

const msgOrderPic = (m) => {
  const direct = String(m?.orderPic || '').trim()
  if (direct) return direct
  const o = findOrderForMsg(m)
  return o ? orderThumb(o) : ''
}

const msgOrderAmount = (m) => {
  const pay = m?.payAmount
  const total = m?.totalAmount
  if (pay != null) return pay
  if (total != null) return total
  const o = findOrderForMsg(m)
  if (!o) return 0
  return o?.payAmount ?? o?.totalAmount ?? 0
}

const normalizeAddrConfirmMessages = () => {
  if (!activeContact.value) return
  if (!messages.value?.length) return
  let updated = false
  for (const m of messages.value) {
    if (!m || m.type !== 'addr_confirm') continue
    const o = findOrderForMsg(m)
    if (!o) continue
    if (!m.orderPic) {
      const pic = orderThumb(o)
      if (pic) {
        m.orderPic = pic
        updated = true
      }
    }
    if (m.payAmount == null && o?.payAmount != null) {
      m.payAmount = o.payAmount
      updated = true
    }
    if (m.totalAmount == null && o?.totalAmount != null) {
      m.totalAmount = o.totalAmount
      updated = true
    }
  }
  if (updated) {}
}

const unfinishedOrders = computed(() => {
  return (orderSummary.value || [])
    .filter(o => Number(o.status) !== 3)
    .slice(0, 5)
})

const latestOrder = computed(() => {
  const list = Array.isArray(orderSummary.value) ? orderSummary.value : []
  return list.length ? list[0] : null
})

const openOrder = (o) => {
  if (!o?.id) return
  const href = router.resolve({ path: `/order/${o.id}` }).href
  window.open(href, '_blank')
}

const openGoods = (m) => {
  const id = Number(m?.goodsId ?? m?.goods_id ?? m?.relatedId ?? m?.related_id ?? 0)
  if (!Number.isFinite(id) || id <= 0) return
  const href = router.resolve({ path: `/goods/${id}` }).href
  window.open(href, '_blank')
}

const ensureAddressCards = () => {
  if (!activeContact.value) return
  const list = Array.isArray(orderSummary.value) ? orderSummary.value : []
  if (!list.length) return
  normalizeAddrConfirmMessages()

  for (const o of list) {
    if (!o?.id) continue
    const paid = Number(o?.payStatus) === 1 || !!o?.payTime
    if (!paid) continue
    const existed = (messages.value || []).some(m => m && m.type === 'addr_confirm' && Number(m.orderId) === Number(o.id))
    if (existed) continue
    const msg = {
      from: 'merchant',
      type: 'addr_confirm',
      orderId: o.id,
      orderNo: o.orderNo,
      orderPic: orderThumb(o),
      payAmount: o.payAmount,
      totalAmount: o.totalAmount,
      consignee: o.consignee,
      phone: o.consigneePhone,
      addr: o.receiveAddr,
      time: Date.now()
    }
    sendPayload(msg).catch(() => {})
  }
}

const pickEmoji = async (e) => {
  draft.value = `${draft.value || ''}${e}`
  await nextTick()
}

const isMsgRead = (m) => {
  if (!m || m.from !== 'merchant') return true
  if (typeof m.read === 'boolean') return m.read
  return true
}

onMounted(loadContacts)
onMounted(loadFaqList)

const sendPayload = async (payload) => {
  if (!activeContact.value) return
  const session = activeSession.value || (await ensureSession(activeContact.value.userId, activeKey.value, null))
  const sessionId = session?.sessionId
  if (!sessionId) return
  const merchantId = getMerchantId()
  const userId = activeContact.value.userId

  let messageType = 5
  let content = ''
  let relatedType = null
  let relatedId = null

  if (payload?.type === 'text') {
    messageType = 1
    content = String(payload?.text || '')
  } else if (payload?.type === 'image') {
    messageType = 2
    content = String(payload?.url || '')
  } else if (payload?.type === 'addr_confirm') {
    messageType = 4
    relatedType = 2
    relatedId = payload?.orderId ?? null
    content = JSON.stringify(payload || {})
  } else if (payload?.type === 'coupon') {
    messageType = 3
    relatedType = 4
    relatedId = payload?.couponId ?? null
    content = JSON.stringify(payload || {})
  } else {
    messageType = 5
    content = JSON.stringify(payload || {})
  }

  const res = await chatApi.sendMessage({
    sessionId,
    userId,
    merchantId,
    senderType: 2,
    senderId: merchantId,
    receiverType: 1,
    receiverId: userId,
    messageType,
    content,
    relatedType,
    relatedId
  })

  const created = res?.data
  if (created) {
    const mapped = mapApiMessage(created)
    const mid = mapped?.messageId
    if (!mid || !(messages.value || []).some(x => x && x.messageId != null && String(x.messageId) === String(mid))) {
      messages.value.push(mapped)
    }
    const key = activeKey.value
    if (key) {
      const cached = chatCache.get(key)
      chatCache.set(key, { session: cached?.session || session, messages: messages.value.slice(), time: Date.now() })
    }
    await nextTick()
    if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

const sendText = async (text) => {
  await sendPayload({ type: 'text', text })
}

const calcPageHeight = () => {
  if (isShell.value) {
    pageHeight.value = 0
    return
  }
  if (isStandalone.value && isEmbedded.value) {
    pageHeight.value = 676
    return
  }
  const el = pageRef.value
  if (!el) return
  const top = el.getBoundingClientRect().top
  const h = window.innerHeight - top - 12
  const min = isStandalone.value ? 680 : 520
  pageHeight.value = Math.max(min, Math.floor(h))
}

const calcShellScale = () => {
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

onMounted(() => {
  try {
    isEmbedded.value = isShell.value || !!window.frameElement
  } catch (e) {
    isEmbedded.value = false
  }
  calcPageHeight()
  calcShellScale()
  window.addEventListener('resize', calcPageHeight)
  window.addEventListener('resize', calcShellScale)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', calcPageHeight)
  window.removeEventListener('resize', calcShellScale)
})

const openShop = () => {
  window.open('http://localhost:3001/', '_blank')
}
</script>

<style scoped>
.cs-page {
  overflow: hidden;
}

.cs-page.standalone {
  height: 100vh;
  padding: 18px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.cs-page.standalone .cs-card {
  width: 1140px;
  max-width: calc(100vw - 36px);
  height: 660px;
  max-height: calc(100vh - 36px);
  border-radius: 14px;
  overflow: hidden;
}

.cs-page.standalone.shell {
  position: fixed;
  inset: 0;
  height: 100vh;
  padding: 0;
  overflow: hidden;
  background: radial-gradient(1200px 700px at 20% 10%, rgba(0, 0, 0, 0.06), transparent 60%),
    radial-gradient(900px 600px at 80% 40%, rgba(0, 0, 0, 0.05), transparent 60%),
    #f3f4f6;
}

.cs-page.standalone.shell .cs-card {
  width: 1528px;
  height: 676px;
  max-width: none;
  max-height: none;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.14);
  transform: scale(var(--ww-scale, 1));
  transform-origin: center center;
}

.cs-page.standalone.shell :deep(.el-card__body) {
  padding: 14px 16px;
}

.cs-page.standalone.embedded {
  height: 676px;
  padding: 0;
  background: transparent;
  align-items: stretch;
  justify-content: stretch;
  font-size: 12px;
}

.cs-page.standalone.embedded .cs-card {
  width: 100%;
  height: 100%;
  max-width: none;
  max-height: none;
  border-radius: 12px;
}

.cs-page.standalone.embedded :deep(.el-card__body) {
  padding: 14px 16px;
}

.cs-page.standalone :deep(.el-card__header) {
  padding: 10px 14px;
}

.cs-page.standalone :deep(.el-card__body) {
  padding: 12px 14px;
}

.cs-page.standalone :deep(.el-button) {
  --el-font-size-base: 12px;
}

.cs-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.cs-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
}

.cs-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 14px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.cs-page.standalone .cs-layout {
  grid-template-columns: 260px 1fr 300px;
  gap: 12px;
}

.cs-page.standalone.embedded .cs-layout {
  grid-template-columns: 280px 1fr 360px;
  gap: 16px;
}

.cs-left {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-right: 1px solid #eef2f7;
  padding-right: 14px;
  min-height: 0;
}

.cs-center {
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.cs-center > .cs-chat {
  flex: 1;
  min-height: 0;
}

.cs-page.standalone .cs-left {
  gap: 8px;
  padding-right: 12px;
}

.cs-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  flex: 1;
  min-height: 0;
}

.cs-page.standalone .cs-list {
  gap: 8px;
}

.cs-item {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 10px 12px;
  cursor: pointer;
  background: #fff;
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 10px;
  align-items: center;
}

.cs-page.standalone .cs-item {
  border-radius: 10px;
  padding: 8px 10px;
  grid-template-columns: 34px 1fr;
  gap: 8px;
}

.cs-item.active {
  border-color: #409eff;
  background: #f0f7ff;
}

.cs-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #111827;
  font-weight: 900;
  overflow: hidden;
}

.cs-page.standalone .cs-item-avatar {
  width: 34px;
  height: 34px;
  font-size: 12px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cs-item-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.nick {
  font-weight: 900;
  color: #111827;
}

.cs-item-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.cs-page.standalone .cs-item-sub {
  margin-top: 4px;
  font-size: 11px;
}

.cs-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.cs-chat-head {
  padding: 10px 0 12px;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head-left {
  display: flex;
  gap: 10px;
  align-items: center;
}

.head-avatar {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: #111827;
  overflow: hidden;
}

.cs-page.standalone .head-avatar {
  width: 36px;
  height: 36px;
  font-size: 12px;
}

.head-title {
  font-weight: 900;
  color: #111827;
}

.head-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.cs-page.standalone .head-sub {
  font-size: 11px;
}

.order-strip {
  z-index: 2;
  background: #fff;
  border-bottom: 1px solid #eef2f7;
  padding: 10px 0 12px;
  margin-bottom: 8px;
}

.cs-page.standalone .order-strip {
  padding: 8px 0 10px;
  margin-bottom: 6px;
}

.strip-title {
  font-size: 12px;
  font-weight: 900;
  color: #111827;
}

.strip-list {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  overflow: auto;
  padding-bottom: 4px;
}

.cs-page.standalone .strip-list {
  margin-top: 6px;
  gap: 6px;
}

.strip-card {
  min-width: 210px;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
}

.cs-page.standalone .strip-card {
  min-width: 190px;
  padding: 8px 10px;
}

.strip-no {
  font-size: 12px;
  font-weight: 900;
  color: #111827;
}

.strip-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.cs-chat-body {
  flex: 1;
  overflow: auto;
  padding: 14px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.cs-page.standalone .cs-chat-body {
  padding: 10px 0;
  gap: 8px;
}

.cs-side {
  border-left: 1px solid #eef2f7;
  padding-left: 14px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cs-page.standalone .cs-side {
  padding-left: 12px;
}

.side-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.side-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 0 12px;
  border-bottom: 1px solid #eef2f7;
}

.cs-page.standalone .side-head {
  padding: 8px 0 10px;
}

.side-title {
  font-weight: 900;
  color: #111827;
}

.side-section {
  padding-top: 12px;
}

.side-label {
  font-size: 12px;
  font-weight: 900;
  color: #111827;
}

.side-order {
  margin-top: 10px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
}

.side-order-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.side-order-pic {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #eef2f7;
  background: #f3f4f6;
  flex: none;
}

.side-order-main {
  flex: 1;
  min-width: 0;
}

.cs-page.standalone .side-order {
  margin-top: 8px;
  border-radius: 10px;
  padding: 10px;
}

.side-order-no {
  font-weight: 900;
  color: #111827;
}

.side-order-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

.cs-page.standalone .side-order-meta {
  margin-top: 6px;
  font-size: 11px;
}

.side-order-time {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.cs-page.standalone .side-order-time {
  font-size: 11px;
}

.msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.cs-page.standalone .msg-row {
  gap: 8px;
}

.msg-row.me {
  justify-content: flex-end;
}

.msg-row.user {
  justify-content: flex-start;
}

.msg-row.sys {
  justify-content: center;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: #111827;
  flex: none;
  overflow: hidden;
}

.cs-page.standalone .msg-avatar {
  width: 30px;
  height: 30px;
  font-size: 12px;
}

.msg-avatar.me {
  background: #fee2e2;
  color: #b91c1c;
}

.msg-content {
  display: flex;
  flex-direction: column;
  max-width: 68%;
}

.msg-bubble {
  border-radius: 12px;
  padding: 10px 12px;
  line-height: 1.6;
  background: #f3f4f6;
  color: #111827;
  word-break: break-word;
}

.cs-page.standalone .msg-bubble {
  border-radius: 10px;
  padding: 8px 10px;
  line-height: 1.5;
  font-size: 13px;
}

.msg-row.me .msg-bubble {
  background: #409eff;
  color: #fff;
}

.msg-image-wrap {
  width: 220px;
  max-width: 100%;
}

.msg-image {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  display: block;
  background: #f3f4f6;
}

.msg-file,
.msg-coupon {
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #eef2f7;
}

.file-link {
  color: #111827;
  font-weight: 900;
  text-decoration: none;
}

.file-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.coupon-title {
  font-weight: 900;
  color: #111827;
}

.coupon-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
}

.msg-time {
  font-size: inherit;
  color: inherit;
}

.cs-page.standalone .msg-time {
  font-size: inherit;
}

.msg-meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #6b7280;
}

.msg-auto {
  font-weight: 800;
  color: #9ca3af;
  flex: none;
}

.msg-revoke {
  padding: 0;
  min-height: auto;
  font-size: 12px;
}

.msg-actions {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.quote-bar {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
}

.quote-title {
  color: #6b7280;
  font-weight: 800;
  flex: none;
}

.quote-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cs-page.standalone .msg-meta {
  font-size: 11px;
}

.msg-read {
  font-weight: 800;
  flex: none;
}

.msg-time {
  margin-left: auto;
  flex: none;
}

.msg-read.unread {
  color: #2563eb;
}

.msg-read.read {
  color: #6b7280;
}

.cs-chat-foot {
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cs-page.standalone .cs-chat-foot {
  padding-top: 10px;
  gap: 8px;
}

.tool-row {
  display: flex;
  gap: 10px;
}

.faq-row {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
}

.faq-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.faq-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.faq-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
}

.faq-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.faq-editor-row {
  display: grid;
  grid-template-columns: 1fr 1fr 80px;
  gap: 10px;
  align-items: center;
}

.faq-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.coupon-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coupon-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
}

.coupon-item {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  background: #fff;
}

.coupon-item.active {
  border-color: #409eff;
  background: #f0f7ff;
}

.coupon-name {
  font-weight: 900;
  color: #111827;
}

.coupon-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.coupon-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 8px;
}

.emoji {
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  user-select: none;
}

.cs-page.standalone .emoji {
  font-size: 16px;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.cs-page.standalone .input-row {
  gap: 8px;
}

.send-btn {
  height: 40px;
}

.cs-page.standalone .send-btn {
  height: 36px;
}

.cs-page.standalone :deep(.el-textarea__inner) {
  font-size: 13px;
  line-height: 1.5;
}

.foot-actions {
  display: flex;
  justify-content: flex-end;
}

.card-msg {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eef2f7;
}

.card-goods {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.goods-pic {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #eef2f7;
  background: #f3f4f6;
  flex: none;
}

.goods-meta {
  flex: 1;
  min-width: 0;
}

.goods-title {
  font-size: 12px;
  font-weight: 900;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.goods-price {
  font-weight: 900;
  color: #111827;
}

.card-title {
  font-weight: 900;
  color: #111827;
}

.card-sub {
  font-size: 12px;
  color: #6b7280;
}

.card-body {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-line {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #6b7280;
}

.card-line .k {
  width: 60px;
  flex: none;
}

.card-line .v {
  color: #111827;
  font-weight: 800;
  flex: 1;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.card-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #6b7280;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 980px) {
  .cs-layout {
    grid-template-columns: 1fr;
  }
  .cs-left {
    border-right: none;
    padding-right: 0;
  }
}
</style>
