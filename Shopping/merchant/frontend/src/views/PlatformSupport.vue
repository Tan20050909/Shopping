<template>
  <main class="ps-page chat-page standalone shell standalone-shell-page">
    <section class="chat-shell-breadcrumb">
      <div class="chat-shell-breadcrumb-inner">
        <button type="button" class="breadcrumb-link" @click="router.push('/dashboard')">商家中心</button>
        <i>/</i>
        <span>平台客服</span>
      </div>
    </section>

    <section class="chat-shell platform-support-shell">
      <aside class="session-panel allmart-chat-card">
        <div class="panel-title">
          <span>最近会话</span>
          <small>1 个会话</small>
        </div>
        <el-input class="session-search" placeholder="搜索平台客服或工单" clearable />
        <div class="session-list">
          <div class="session-item active">
            <div class="session-avatar">{{ botAvatarText }}</div>
            <div class="session-copy">
              <div class="session-name">平台客服</div>
              <div class="session-sub">结算/故障/入驻/订单等问题咨询</div>
            </div>
            <div class="session-extra"><small>在线</small></div>
          </div>
        </div>
        <button type="button" class="session-footer-btn" @click="reloadAll">查看全部会话</button>
      </aside>

      <section class="conversation allmart-chat-card">
        <header class="chat-head allmart-chat-section">
          <div class="chat-head-main">
            <div class="chat-head-logo">{{ botAvatarText }}</div>
            <div class="head-meta">
              <div class="head-title">平台客服</div>
              <div class="head-sub"><span class="online-dot"></span>在线 · 平台智能客服</div>
            </div>
          </div>
          <div class="chat-actions">
            <el-button plain @click="openTicketCreate()">提交工单</el-button>
            <el-button plain @click="openTicketList">我的工单</el-button>
          </div>
        </header>

        <div ref="chatBodyRef" class="ps-body messages" @click="onBodyClick">
          <div class="ps-top-tip">到啦啦，您新产生的消息，平台客服最多为您保留60天以内的消息记录。</div>

          <div v-for="m in messages" :key="m.id" class="ps-row" :class="m.from === 'merchant' ? 'me' : m.from === 'bot' ? 'bot' : 'sys'">
            <div v-if="m.from !== 'merchant'" class="ps-avatar">{{ botAvatarText }}</div>
            <div class="ps-msg">
              <div v-if="m.type === 'ticket'" class="ps-card-msg">
                <div class="ps-card-title">已为您创建工单</div>
                <div class="ps-card-sub">工单号：{{ m.ticketId }}</div>
                <div class="ps-card-actions">
                  <el-button size="small" plain @click="openTicketList">查看工单</el-button>
                </div>
              </div>
              <div v-else class="ps-bubble">{{ m.text }}</div>
              <div class="ps-time">{{ formatTime(m.time) }}</div>
            </div>
            <div v-if="m.from === 'merchant'" class="ps-avatar me">{{ merchantAvatarText }}</div>
          </div>

          <div v-if="showQuickPanel" class="ps-quick" @click.stop>
            <div class="ps-quick-title">快捷入口</div>
            <div class="ps-quick-list">
              <div
                v-for="a in visibleQuickActions"
                :key="a.key"
                class="ps-quick-item"
                @click="onQuickAction(a)"
              >
                <span v-if="a.hot" class="ps-hot">热</span>
                <span class="ps-quick-text">{{ a.label }}</span>
                <span class="ps-quick-arrow">›</span>
              </div>
            </div>
            <div class="ps-quick-more">
              <el-button v-if="!quickExpanded" text @click="quickExpanded = true">查看更多</el-button>
              <el-button v-else text @click="quickExpanded = false">收起</el-button>
            </div>
          </div>
        </div>

        <div class="ps-foot composer">
          <div class="ps-tools">
            <el-button plain @click="toggleQuick">快捷入口</el-button>
            <el-button plain @click="openTicketCreate()">提交工单</el-button>
            <el-button plain @click="transferToHuman">转人工</el-button>
          </div>
          <div class="ps-input">
            <el-input
              v-model="draft"
              placeholder="请输入想咨询的问题"
              @keydown.enter.exact.prevent="sendText"
              clearable
            />
            <el-button type="primary" class="ps-send" @click="sendText">发送</el-button>
          </div>
        </div>
      </section>

      <aside class="shop-side assistant-side">
        <div class="side-card allmart-chat-card">
          <div class="side-title">平台联系方式</div>
          <div class="contact-summary">
            <div><span>客服电话</span><strong>{{ platformPhone }}</strong></div>
            <div><span>客服邮箱</span><strong>{{ platformEmail }}</strong></div>
            <div><span>服务时间</span><strong>{{ serviceHours }}</strong></div>
          </div>
          <el-button plain class="side-wide-btn" @click="contactsVisible = true">联系方式</el-button>
        </div>

        <div class="side-card allmart-chat-card">
          <div class="side-title">我的工单</div>
          <div v-if="tickets.length" class="recent-ticket">
            <strong>{{ tickets[0]?.subject || '工单' }}</strong>
            <span>{{ tickets[0]?.status === 'closed' ? '已关闭' : '处理中' }}</span>
          </div>
          <div v-else class="side-empty">暂无工单</div>
          <div class="side-actions">
            <el-button plain @click="openTicketList">查看工单</el-button>
            <el-button type="primary" @click="openTicketCreate()">提交工单</el-button>
          </div>
        </div>

        <div class="side-card allmart-chat-card">
          <div class="side-title">推荐快捷入口</div>
          <div class="quick-grid">
            <button v-for="a in quickActions.slice(0, 4)" :key="a.key" type="button" class="quick-btn" @click="onQuickAction(a)">
              {{ a.label }}
            </button>
          </div>
        </div>

        <div class="side-card allmart-chat-card">
          <div class="side-title">温馨提示</div>
          <ul class="tips-list">
            <li>平台客服会保留 60 天以内消息记录</li>
            <li>复杂问题建议提交工单并附截图</li>
            <li>涉及订单或金额请提供对应编号</li>
          </ul>
        </div>
      </aside>
    </section>

    <footer class="chat-shell-footer">
      <div class="chat-shell-footer-inner">
        <span>AllMart 商家服务中心</span>
        <span>客服电话：{{ platformPhone }}</span>
        <span>服务时间：{{ serviceHours }}</span>
      </div>
    </footer>

    <el-drawer v-model="contactsVisible" title="平台联系方式" size="420px">
      <div class="ps-contacts">
        <div class="ps-contact">
          <div class="k">客服电话</div>
          <div class="v">
            <span>{{ platformPhone }}</span>
            <el-button text size="small" @click="copyText(platformPhone)">复制</el-button>
          </div>
        </div>
        <div class="ps-contact">
          <div class="k">客服邮箱</div>
          <div class="v">
            <span>{{ platformEmail }}</span>
            <el-button text size="small" @click="copyText(platformEmail)">复制</el-button>
          </div>
        </div>
        <div class="ps-contact">
          <div class="k">服务时间</div>
          <div class="v">{{ serviceHours }}</div>
        </div>
        <div class="ps-contact">
          <div class="k">当前商家</div>
          <div class="v">{{ merchantName }}（ID：{{ merchantId }}）</div>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="ticketCreateVisible" title="提交工单" size="560px" destroy-on-close>
      <el-form :model="ticketForm" label-position="top" class="ps-ticket-form">
        <div class="ps-form-row">
          <el-form-item label="问题类型" class="w-180">
            <el-select v-model="ticketForm.type" placeholder="请选择" style="width: 100%">
              <el-option label="账户/入驻" value="account" />
              <el-option label="订单/退款" value="order" />
              <el-option label="商品/库存" value="goods" />
              <el-option label="结算/财务" value="finance" />
              <el-option label="活动/营销" value="marketing" />
              <el-option label="系统故障" value="bug" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="紧急程度" class="w-180">
            <el-select v-model="ticketForm.priority" placeholder="请选择" style="width: 100%">
              <el-option label="普通" value="normal" />
              <el-option label="紧急" value="urgent" />
              <el-option label="非常紧急" value="critical" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="标题">
          <el-input v-model="ticketForm.subject" maxlength="60" show-word-limit placeholder="例如：无法发布商品/结算金额异常" />
        </el-form-item>

        <el-form-item label="问题描述">
          <el-input
            v-model="ticketForm.content"
            type="textarea"
            :rows="6"
            maxlength="1200"
            show-word-limit
            placeholder="请尽量描述：发生时间、操作路径、报错信息、影响范围、可复现步骤"
          />
        </el-form-item>

        <div class="ps-form-row">
          <el-form-item label="联系电话（选填）" class="w-240">
            <el-input v-model="ticketForm.contactPhone" placeholder="便于客服回访" />
          </el-form-item>
          <el-form-item label="联系邮箱（选填）" class="w-240">
            <el-input v-model="ticketForm.contactEmail" placeholder="便于发送处理结果" />
          </el-form-item>
        </div>

        <el-form-item label="附件（可选）">
          <div class="ps-upload-row">
            <el-upload action="" :http-request="uploadAttachment" :show-file-list="false">
              <el-button plain>上传附件</el-button>
            </el-upload>
            <el-upload action="" :http-request="uploadImage" :show-file-list="false" accept="image/*">
              <el-button plain>上传截图</el-button>
            </el-upload>
          </div>

          <el-empty v-if="!ticketForm.attachments.length" description="暂无附件" :image-size="72" />
          <div v-else class="ps-attach-list">
            <div v-for="a in ticketForm.attachments" :key="a.key" class="ps-attach-item">
              <div class="ps-attach-main">
                <div class="ps-attach-name">{{ a.name }}</div>
                <div class="ps-attach-sub">
                  <span v-if="a.size">{{ formatSize(a.size) }}</span>
                  <span v-if="a.url" class="ps-attach-url">{{ a.url }}</span>
                </div>
              </div>
              <div class="ps-attach-actions">
                <el-button text size="small" :disabled="!a.url" @click="openUrl(a.url)">打开</el-button>
                <el-button text size="small" type="danger" @click="removeAttachment(a.key)">移除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>

        <div class="ps-form-actions">
          <el-button plain @click="resetForm">清空</el-button>
          <el-button type="primary" @click="submitTicket">提交工单</el-button>
        </div>
      </el-form>
    </el-drawer>

    <el-drawer v-model="ticketListVisible" title="我的工单" size="860px" destroy-on-close>
      <div class="ps-list-filters">
        <el-select v-model="statusFilter" style="width: 160px">
          <el-option label="全部" value="all" />
          <el-option label="处理中" value="open" />
          <el-option label="已关闭" value="closed" />
        </el-select>
        <el-input v-model="keyword" clearable placeholder="搜索标题/内容" style="width: 280px" />
        <el-button plain @click="loadTickets">刷新</el-button>
      </div>

      <el-empty v-if="!filteredTickets.length" description="暂无工单" />
      <el-table v-else :data="filteredTickets" style="width: 100%" row-key="id">
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'closed' ? 'info' : 'warning'" effect="light">
              {{ row.status === 'closed' ? '已关闭' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subject }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="danger" plain :disabled="row.status === 'closed'" @click="closeTicket(row)">
              关闭
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="detailVisible" title="工单详情" width="760px" destroy-on-close>
      <div v-if="currentTicket" class="ps-detail">
        <div class="ps-detail-row"><span class="k">工单号</span><span class="v">{{ currentTicket.id }}</span></div>
        <div class="ps-detail-row"><span class="k">状态</span><span class="v">{{ currentTicket.status === 'closed' ? '已关闭' : '处理中' }}</span></div>
        <div class="ps-detail-row"><span class="k">类型</span><span class="v">{{ typeText(currentTicket.type) }}</span></div>
        <div class="ps-detail-row"><span class="k">紧急程度</span><span class="v">{{ priorityText(currentTicket.priority) }}</span></div>
        <div class="ps-detail-row"><span class="k">标题</span><span class="v">{{ currentTicket.subject }}</span></div>
        <div class="ps-detail-row"><span class="k">描述</span><span class="v pre">{{ currentTicket.content }}</span></div>
        <div class="ps-detail-row"><span class="k">创建时间</span><span class="v">{{ formatDateTime(currentTicket.createTime) }}</span></div>
        <div v-if="currentTicket.closeTime" class="ps-detail-row">
          <span class="k">关闭时间</span><span class="v">{{ formatDateTime(currentTicket.closeTime) }}</span>
        </div>

        <div class="ps-detail-row">
          <span class="k">联系方式</span>
          <span class="v">{{ contactText(currentTicket) }}</span>
        </div>

        <div class="ps-detail-row" v-if="Array.isArray(currentTicket.attachments) && currentTicket.attachments.length">
          <span class="k">附件</span>
          <div class="v">
            <div class="ps-attach-list ps-detail-attach">
              <div v-for="a in currentTicket.attachments" :key="a.key" class="ps-attach-item">
                <div class="ps-attach-main">
                  <div class="ps-attach-name">{{ a.name }}</div>
                  <div class="ps-attach-sub">
                    <span v-if="a.size">{{ formatSize(a.size) }}</span>
                    <span v-if="a.url" class="ps-attach-url">{{ a.url }}</span>
                  </div>
                </div>
                <div class="ps-attach-actions">
                  <el-button text size="small" :disabled="!a.url" @click="openUrl(a.url)">打开</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button plain @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentTicket && currentTicket.status !== 'closed'"
          type="danger"
          @click="closeTicket(currentTicket, { fromDetail: true })"
        >
          关闭工单
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadApi } from '@/api'
import { ensureMerchantUser, getMerchantId } from '@/utils/merchant'

const platformPhone = '400-800-1234'
const platformEmail = 'support@allmart.example.com'
const serviceHours = '周一至周日 09:00-21:00'
const router = useRouter()

const merchantId = computed(() => getMerchantId())
const merchantName = computed(() => {
  const u = ensureMerchantUser()
  return u?.merchantName || u?.shopName || u?.username || '商家'
})

const storageKey = computed(() => `platformSupportTickets:${merchantId.value}`)
const chatStorageKey = computed(() => `platformSupportChat:${merchantId.value}`)

const contactsVisible = ref(false)
const ticketCreateVisible = ref(false)
const ticketListVisible = ref(false)

const draft = ref('')
const chatBodyRef = ref(null)
const showQuickPanel = ref(true)
const quickExpanded = ref(false)

const messages = ref([])
const botAvatarText = computed(() => '平')
const merchantAvatarText = computed(() => (String(merchantName.value || '商')[0] || '商'))

const quickActions = computed(() => [
  { key: 'finance', label: '结算/提现问题', hot: true },
  { key: 'bug', label: '系统故障/报错', hot: true },
  { key: 'order', label: '订单/退款咨询', hot: true },
  { key: 'account', label: '入驻/账号问题', hot: false },
  { key: 'marketing', label: '活动/营销咨询', hot: false },
  { key: 'appeal', label: '违规/申诉指引', hot: false },
  { key: 'ticket', label: '提交工单', hot: false },
  { key: 'human', label: '转人工客服', hot: false },
  { key: 'contact', label: '联系方式', hot: false }
])

const visibleQuickActions = computed(() => {
  const all = quickActions.value || []
  if (quickExpanded.value) return all
  return all.slice(0, 4)
})

const keyword = ref('')
const statusFilter = ref('all')
const tickets = ref([])

const ticketForm = reactive({
  type: 'other',
  priority: 'normal',
  subject: '',
  content: '',
  contactPhone: '',
  contactEmail: '',
  attachments: []
})

const detailVisible = ref(false)
const currentTicket = ref(null)

const safeParse = (raw) => {
  try {
    const v = raw ? JSON.parse(raw) : null
    return v
  } catch (e) {
    return null
  }
}

const loadTickets = () => {
  const raw = localStorage.getItem(storageKey.value)
  const parsed = safeParse(raw)
  tickets.value = Array.isArray(parsed) ? parsed : []
}

const saveTickets = () => {
  localStorage.setItem(storageKey.value, JSON.stringify(tickets.value || []))
}

const loadChat = () => {
  const raw = localStorage.getItem(chatStorageKey.value)
  const parsed = safeParse(raw)
  const list = Array.isArray(parsed?.messages) ? parsed.messages : []
  const qe = Boolean(parsed?.quickExpanded)
  messages.value = list
  quickExpanded.value = qe
}

const saveChat = () => {
  localStorage.setItem(
    chatStorageKey.value,
    JSON.stringify({
      messages: messages.value || [],
      quickExpanded: Boolean(quickExpanded.value)
    })
  )
}

const scrollToBottom = async () => {
  await nextTick()
  const el = chatBodyRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const pushMsg = async (from, text, extra = {}) => {
  const msg = {
    id: `${Date.now()}${Math.floor(Math.random() * 1000)}`,
    from,
    type: extra.type || 'text',
    text: String(text || ''),
    time: new Date().toISOString(),
    ...extra
  }
  messages.value = [...(messages.value || []), msg]
  saveChat()
  await scrollToBottom()
}

const ensureGreet = async () => {
  if ((messages.value || []).length) return
  await pushMsg('bot', '亲，我是平台智能客服，很高兴为您服务！')
  await pushMsg('bot', '亲，您有任何疑问都可以在这里咨询～')
  showQuickPanel.value = true
}

const openTicketCreate = (preset = {}) => {
  const type = preset.type != null ? String(preset.type) : ''
  const subject = preset.subject != null ? String(preset.subject) : ''
  if (type) ticketForm.type = type
  if (subject) ticketForm.subject = subject
  ticketCreateVisible.value = true
}

const openTicketList = () => {
  ticketListVisible.value = true
}

const toggleQuick = () => {
  showQuickPanel.value = !showQuickPanel.value
  scrollToBottom()
}

const onBodyClick = () => {
  showQuickPanel.value = false
}

const transferToHuman = async () => {
  await pushMsg('merchant', '转人工客服')
  await pushMsg('bot', '已为您转接人工客服（演示）。为提升处理效率，建议同时提交工单并附上截图/订单号。')
}

const onQuickAction = async (a) => {
  const key = String(a?.key || '')
  await pushMsg('merchant', String(a?.label || ''))

  if (key === 'finance') {
    await pushMsg('bot', '结算/提现问题建议提供：商家ID、发生时间、金额、截图。也可在【数据中心】核对流水与提现记录。')
    openTicketCreate({ type: 'finance', subject: '结算/提现问题' })
    return
  }
  if (key === 'bug') {
    await pushMsg('bot', '系统故障/报错请提供：操作路径、报错信息、截图、是否可复现。建议提交工单。')
    openTicketCreate({ type: 'bug', subject: '系统故障/报错' })
    return
  }
  if (key === 'order') {
    await pushMsg('bot', '订单/退款问题请提供：订单号、用户ID、当前状态。若涉及异常金额或无法操作，建议提交工单。')
    openTicketCreate({ type: 'order', subject: '订单/退款咨询' })
    return
  }
  if (key === 'account') {
    await pushMsg('bot', '入驻/账号问题可先检查：资料是否完整、审核状态、登录账号是否正确。复杂情况建议提交工单。')
    openTicketCreate({ type: 'account', subject: '入驻/账号问题' })
    return
  }
  if (key === 'marketing') {
    await pushMsg('bot', '活动/营销问题建议说明：活动名称、报名时间、页面截图、提示文案。可提交工单进一步处理。')
    openTicketCreate({ type: 'marketing', subject: '活动/营销咨询' })
    return
  }
  if (key === 'appeal') {
    await pushMsg('bot', '违规/申诉类问题建议提供：相关内容截图、规则提示、时间点。若是评价相关，可在【评价申诉】查看。')
    openTicketCreate({ type: 'other', subject: '违规/申诉指引' })
    return
  }
  if (key === 'ticket') {
    await pushMsg('bot', '请填写工单信息并提交，我们将尽快处理。')
    openTicketCreate()
    return
  }
  if (key === 'human') {
    await transferToHuman()
    return
  }
  if (key === 'contact') {
    contactsVisible.value = true
    await pushMsg('bot', `客服电话：${platformPhone}；邮箱：${platformEmail}。`)
    return
  }
  await pushMsg('bot', '已收到您的诉求。建议通过快捷入口选择对应场景，或直接提交工单。')
}

const sendText = async () => {
  const text = String(draft.value || '').trim()
  if (!text) return
  draft.value = ''
  await pushMsg('merchant', text)
  const lower = text.toLowerCase()
  if (text.includes('工单') || lower.includes('ticket')) {
    await pushMsg('bot', '好的，我这边为您打开工单提交入口。')
    openTicketCreate()
    return
  }
  if (text.includes('提现') || text.includes('结算') || text.includes('财务')) {
    await pushMsg('bot', '看起来是结算/提现相关问题。建议补充金额与截图，并提交工单。')
    openTicketCreate({ type: 'finance', subject: '结算/提现问题' })
    return
  }
  if (text.includes('报错') || text.includes('打不开') || text.includes('异常') || text.includes('故障')) {
    await pushMsg('bot', '看起来是系统故障/异常。建议补充复现步骤与截图，并提交工单。')
    openTicketCreate({ type: 'bug', subject: '系统故障/报错' })
    return
  }
  if (text.includes('订单') || text.includes('退款')) {
    await pushMsg('bot', '订单/退款相关建议提供订单号与当前状态，必要时提交工单。')
    openTicketCreate({ type: 'order', subject: '订单/退款咨询' })
    return
  }
  if (text.includes('入驻') || text.includes('审核') || text.includes('账号') || text.includes('登录')) {
    await pushMsg('bot', '入驻/账号相关建议说明当前状态与提示信息，必要时提交工单。')
    openTicketCreate({ type: 'account', subject: '入驻/账号问题' })
    return
  }
  await pushMsg('bot', '已收到。为提升处理效率：如涉及金额/订单/截图，建议直接提交工单。')
}

const filteredTickets = computed(() => {
  const kw = String(keyword.value || '').trim()
  const status = String(statusFilter.value || 'all')

  return (tickets.value || [])
    .filter(t => {
      if (status !== 'all' && String(t?.status) !== status) return false
      if (!kw) return true
      const hay = [t?.subject, t?.content, t?.id].filter(v => v != null).map(v => String(v))
      return hay.some(v => v.includes(kw))
    })
    .slice()
    .sort((a, b) => String(b?.createTime || '').localeCompare(String(a?.createTime || '')))
})

const resetForm = () => {
  ticketForm.type = 'other'
  ticketForm.priority = 'normal'
  ticketForm.subject = ''
  ticketForm.content = ''
  ticketForm.contactPhone = ''
  ticketForm.contactEmail = ''
  ticketForm.attachments = []
}

const submitTicket = () => {
  const subject = String(ticketForm.subject || '').trim()
  const content = String(ticketForm.content || '').trim()
  if (!subject) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!content) {
    ElMessage.warning('请填写问题描述')
    return
  }

  const id = `${Date.now()}${Math.floor(Math.random() * 1000)}`.padStart(16, '0')
  const item = {
    id,
    merchantId: merchantId.value,
    merchantName: merchantName.value,
    type: String(ticketForm.type || 'other'),
    priority: String(ticketForm.priority || 'normal'),
    subject,
    content,
    contactPhone: String(ticketForm.contactPhone || '').trim(),
    contactEmail: String(ticketForm.contactEmail || '').trim(),
    attachments: Array.isArray(ticketForm.attachments) ? ticketForm.attachments : [],
    status: 'open',
    createTime: new Date().toISOString()
  }

  tickets.value = [item, ...(tickets.value || [])]
  saveTickets()
  resetForm()
  ticketCreateVisible.value = false
  ElMessage.success('工单已提交')
  pushMsg('bot', '', { type: 'ticket', ticketId: id, text: '' })
}

const openDetail = (row) => {
  currentTicket.value = row
  detailVisible.value = true
}

const closeTicket = (row, options = {}) => {
  const id = row?.id
  if (!id) return

  const list = Array.isArray(tickets.value) ? tickets.value.slice() : []
  const idx = list.findIndex(t => String(t?.id) === String(id))
  if (idx < 0) return
  if (String(list[idx]?.status) === 'closed') return

  list[idx] = { ...list[idx], status: 'closed', closeTime: new Date().toISOString() }
  tickets.value = list
  saveTickets()

  if (options.fromDetail && currentTicket.value) {
    currentTicket.value = list[idx]
  }
  ElMessage.success('已关闭')
}

const uploadImage = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    if (!url) throw new Error('empty url')
    addAttachment({
      name: options.file?.name || 'image',
      url,
      size: options.file?.size || 0
    })
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error('上传失败')
  }
}

const uploadAttachment = async (options) => {
  try {
    const res = await uploadApi.uploadFile(options.file)
    const url = res?.data?.url || ''
    if (!url) throw new Error('empty url')
    addAttachment({
      name: options.file?.name || 'file',
      url,
      size: options.file?.size || 0
    })
    options?.onSuccess?.(res?.data, options.file)
    ElMessage.success('上传成功')
  } catch (e) {
    options?.onError?.(e)
    ElMessage.error('上传失败')
  }
}

const addAttachment = (payload) => {
  const key = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  const item = {
    key,
    name: String(payload?.name || '附件'),
    url: String(payload?.url || ''),
    size: Number(payload?.size || 0) || 0
  }
  ticketForm.attachments = [...(ticketForm.attachments || []), item]
}

const removeAttachment = (key) => {
  ticketForm.attachments = (ticketForm.attachments || []).filter(a => String(a?.key) !== String(key))
}

const openUrl = (url) => {
  const u = String(url || '').trim()
  if (!u) return
  window.open(u, '_blank', 'noopener,noreferrer')
}

const copyText = async (text) => {
  const v = String(text || '').trim()
  if (!v) return
  try {
    await navigator.clipboard.writeText(v)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
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

const formatSize = (bytes) => {
  const n = Number(bytes || 0) || 0
  if (n <= 0) return ''
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  if (n < 1024 * 1024 * 1024) return `${(n / (1024 * 1024)).toFixed(1)} MB`
  return `${(n / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

const typeText = (v) => {
  const k = String(v || 'other')
  if (k === 'account') return '账户/入驻'
  if (k === 'order') return '订单/退款'
  if (k === 'goods') return '商品/库存'
  if (k === 'finance') return '结算/财务'
  if (k === 'marketing') return '活动/营销'
  if (k === 'bug') return '系统故障'
  return '其他'
}

const priorityText = (v) => {
  const k = String(v || 'normal')
  if (k === 'urgent') return '紧急'
  if (k === 'critical') return '非常紧急'
  return '普通'
}

const contactText = (t) => {
  const phone = String(t?.contactPhone || '').trim()
  const email = String(t?.contactEmail || '').trim()
  if (phone && email) return `${phone} / ${email}`
  if (phone) return phone
  if (email) return email
  return '-'
}

const formatTime = (t) => {
  return formatDateTime(t)
}

const reloadAll = () => {
  loadTickets()
  loadChat()
  ensureGreet()
  scrollToBottom()
}

onMounted(() => {
  loadTickets()
  loadChat()
  ensureGreet()
  scrollToBottom()
})

watch(quickExpanded, () => {
  saveChat()
})
</script>

<style scoped>
.ps-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ps-card :deep(.el-card__header) {
  padding: 14px 16px;
}

.ps-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.ps-title {
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.ps-sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.ps-head-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ps-shell {
  height: calc(100vh - 260px);
  min-height: 520px;
  display: flex;
  flex-direction: column;
  border: 1px solid #eee;
  border-radius: 14px;
  overflow: hidden;
  background: #fafafa;
}

.ps-body {
  flex: 1;
  overflow: auto;
  padding: 14px;
}

.ps-top-tip {
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
  padding: 10px 0 18px;
}

.ps-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  align-items: flex-start;
}

.ps-row.me {
  justify-content: flex-end;
}

.ps-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #111827;
  font-weight: 700;
}

.ps-avatar.me {
  background: #fff7f7;
  border-color: #ffd6d9;
  color: #e60012;
}

.ps-msg {
  max-width: 720px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ps-bubble {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 10px 12px;
  color: #111827;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.ps-row.me .ps-bubble {
  background: #fff7f7;
  border-color: #ffd6d9;
}

.ps-time {
  font-size: 12px;
  color: #9ca3af;
}

.ps-card-msg {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
}

.ps-card-title {
  font-weight: 800;
  color: #111827;
}

.ps-card-sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.ps-card-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.ps-quick {
  margin-top: 10px;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.ps-quick-title {
  color: #6b7280;
  font-size: 12px;
}

.ps-quick-list {
  width: 520px;
  max-width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ps-quick-item {
  background: #fff;
  border: 1px solid #ffcfad;
  border-radius: 10px;
  padding: 12px 14px;
  color: #f97316;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.ps-quick-item:hover {
  border-color: #fb923c;
}

.ps-hot {
  width: 22px;
  height: 16px;
  border-radius: 4px;
  background: #ef4444;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.ps-quick-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ps-quick-arrow {
  color: #f97316;
  font-size: 18px;
  line-height: 1;
}

.ps-foot {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ps-tools {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ps-input {
  display: flex;
  gap: 10px;
}

.ps-send {
  border-radius: 10px;
}

.ps-contacts {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ps-contact {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 10px;
  align-items: start;
}

.ps-contact .k {
  color: #6b7280;
}

.ps-contact .v {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #111827;
  min-width: 0;
}

.ps-ticket-form {
  padding-right: 4px;
}

.ps-form-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.w-180 {
  width: 180px;
}

.w-240 {
  width: 240px;
}

.ps-upload-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.ps-attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ps-attach-item {
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.ps-attach-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ps-attach-name {
  font-weight: 700;
  color: #111827;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ps-attach-sub {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #6b7280;
  font-size: 12px;
}

.ps-attach-url {
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ps-attach-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.ps-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ps-list-filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.ps-detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ps-detail-row {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  align-items: start;
}

.ps-detail-row .k {
  color: #6b7280;
}

.ps-detail-row .v {
  color: #111827;
  min-width: 0;
}

.pre {
  white-space: pre-wrap;
  word-break: break-word;
}

.ps-detail-attach .ps-attach-name,
.ps-detail-attach .ps-attach-url {
  max-width: 480px;
}

/* standalone shell 与商家消息中心保持同一套三栏结构 */
.ps-page.standalone-shell-page {
  display: block;
  min-height: 100vh;
  background: #f7f8fa;
  color: #111827;
}

.chat-shell-breadcrumb {
  background: #fff;
  border-bottom: 1px solid #eee;
}

.chat-shell-breadcrumb-inner,
.chat-shell-footer-inner {
  width: min(1680px, calc(100vw - 64px));
  margin: 0 auto;
}

.chat-shell-breadcrumb-inner {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #98a2b3;
  font-size: 13px;
}

.breadcrumb-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #667085;
  cursor: pointer;
}

.breadcrumb-link:hover {
  color: #e60012;
}

.platform-support-shell {
  width: min(1680px, calc(100vw - 64px));
  height: clamp(720px, calc(100vh - 150px), 860px);
  min-height: 720px;
  max-height: 860px;
  margin: 24px auto;
  display: grid;
  grid-template-columns: 348px minmax(680px, 1fr) 332px;
  gap: 24px;
  align-items: stretch;
}

.allmart-chat-card {
  background: #fff;
  border: 1px solid #eaecf0;
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, .055);
}

.session-panel {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  overflow: hidden;
  padding: 18px 16px 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 17px;
  font-weight: 800;
}

.panel-title small {
  color: #98a2b3;
  font-size: 12px;
  font-weight: 500;
}

.session-search {
  margin-bottom: 14px;
}

.session-search :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f8f9fb;
  box-shadow: 0 0 0 1px #edf0f3 inset;
}

.session-list {
  min-height: 0;
  overflow-y: auto;
}

.session-item {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  gap: 11px;
  align-items: center;
  padding: 13px 11px;
  border: 1px solid #ffe0e2;
  border-radius: 14px;
  background: #fff7f7;
}

.session-avatar,
.chat-head-logo {
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #e60012, #ff5d68);
  color: #fff;
  font-weight: 800;
}

.session-avatar {
  width: 46px;
  height: 46px;
}

.session-copy {
  min-width: 0;
}

.session-name {
  color: #1d2939;
  font-size: 14px;
  font-weight: 800;
}

.session-sub {
  margin-top: 5px;
  overflow: hidden;
  color: #98a2b3;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-extra {
  align-self: start;
  color: #12b76a;
}

.session-footer-btn {
  height: 38px;
  margin-top: 12px;
  border: 1px solid #eee;
  border-radius: 999px;
  background: #fff;
  color: #667085;
  cursor: pointer;
}

.conversation {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-head {
  flex: 0 0 auto;
  min-height: 72px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid #eee;
}

.chat-head-main,
.chat-actions {
  display: flex;
  align-items: center;
  gap: 11px;
}

.chat-head-logo {
  width: 44px;
  height: 44px;
}

.head-title {
  color: #1d2939;
  font-size: 16px;
  font-weight: 800;
}

.head-sub {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 12px;
}

.online-dot {
  width: 7px;
  height: 7px;
  margin-right: 5px;
  display: inline-block;
  border-radius: 50%;
  background: #12b76a;
}

.conversation .ps-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 22px;
  background: #fafbfc;
}

.conversation .ps-quick {
  max-width: 520px;
  margin-left: 52px;
  border-color: #f2d9db;
  background: #fff;
}

.conversation .ps-foot {
  flex: 0 0 auto;
  padding: 12px 16px 16px;
  background: #fff;
}

.ps-tools :deep(.el-button),
.chat-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 999px;
}

.ps-input :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 13px;
  background: #fafafa;
}

.ps-send {
  min-width: 78px;
  border-radius: 999px;
}

.assistant-side {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-right: 2px;
}

.side-card {
  padding: 16px;
}

.side-title {
  margin-bottom: 13px;
  color: #1d2939;
  font-size: 14px;
  font-weight: 800;
}

.contact-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 13px;
}

.contact-summary div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-bottom: 9px;
  border-bottom: 1px solid #f0f1f3;
}

.contact-summary span,
.recent-ticket span,
.side-empty {
  color: #98a2b3;
  font-size: 11px;
}

.contact-summary strong,
.recent-ticket strong {
  color: #475467;
  font-size: 12px;
  font-weight: 600;
  word-break: break-all;
}

.side-wide-btn {
  width: 100%;
  border-radius: 999px;
}

.recent-ticket {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 11px;
  border: 1px solid #f0f1f3;
  border-radius: 12px;
  background: #fafafa;
}

.side-empty {
  padding: 16px 0;
  text-align: center;
}

.side-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}

.side-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 999px;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.quick-btn {
  min-height: 48px;
  padding: 8px;
  border: 1px solid #f0f1f3;
  border-radius: 12px;
  background: #fff;
  color: #475467;
  font-size: 12px;
  cursor: pointer;
}

.quick-btn:hover {
  border-color: #ffc9cd;
  background: #fff6f6;
  color: #e60012;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #667085;
  font-size: 12px;
  line-height: 1.9;
}

.chat-shell-footer {
  padding: 2px 0 22px;
  color: #98a2b3;
  font-size: 12px;
}

.chat-shell-footer-inner {
  display: flex;
  justify-content: center;
  gap: 28px;
}

@media (max-width: 1200px) {
  .platform-support-shell {
    grid-template-columns: 300px minmax(560px, 1fr);
  }
  .assistant-side {
    display: none;
  }
}

@media (max-width: 900px) {
  .chat-shell-breadcrumb-inner,
  .chat-shell-footer-inner,
  .platform-support-shell {
    width: calc(100vw - 28px);
  }
  .platform-support-shell {
    height: auto;
    min-height: 0;
    grid-template-columns: 1fr;
  }
  .session-panel {
    height: 280px;
  }
  .conversation {
    height: 720px;
  }
}
</style>
