<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">AI 智能助手</h2>
        <p class="page-subtitle">智能运营辅助，帮你高效决策</p>
      </div>
    </div>
    <div style="display:flex;gap:20px;height:calc(100vh - 200px)">
      <div style="width:220px;flex-shrink:0;overflow-y:auto" class="content-card">
        <div style="font-weight:600;margin-bottom:16px;font-size:14px">快捷提问</div>
        <div v-for="item in quickQuestions" :key="item.q" class="quick-q" @click="askQuestion(item.q)">
          <el-icon style="margin-right:6px" :size="14"><component :is="item.icon" /></el-icon>
          <span style="font-size:13px">{{ item.q }}</span>
        </div>
      </div>
      <div style="flex:1;display:flex;flex-direction:column;padding:0" class="content-card">
        <div style="padding:16px 20px;border-bottom:1px solid var(--border-light);display:flex;justify-content:space-between;align-items:center">
          <div style="display:flex;align-items:center;gap:8px">
            <el-avatar :size="28" style="background:#E60012"><el-icon><MagicStick /></el-icon></el-avatar>
            <span style="font-weight:600">AI 智能助手</span>
            <el-tag size="small" type="success" effect="light" style="border-radius:999px">在线</el-tag>
          </div>
          <el-button text @click="clearChat"><el-icon><Delete /></el-icon> 清空</el-button>
        </div>
        <div ref="chatContainer" style="flex:1;overflow-y:auto;padding:20px">
          <div v-if="messages.length === 0" style="text-align:center;padding:60px 0;color:var(--text-muted)">
            <el-icon :size="40"><MagicStick /></el-icon>
            <div style="margin-top:12px;font-size:14px">你好！我是AI智能运营助手，有任何问题都可以问我</div>
          </div>
          <div v-for="(msg, idx) in messages" :key="idx" :style="{display:'flex',marginBottom:'16px',flexDirection: msg.role==='user'?'row-reverse':'row'}">
            <el-avatar :size="32" :style="{background:msg.role==='user'?'#E60012':'#555',flexShrink:0}">
              <el-icon><component :is="msg.role==='user'?'User':'MagicStick'" /></el-icon>
            </el-avatar>
            <div :style="{maxWidth:'70%',marginLeft:msg.role==='user'?'0':'12px',marginRight:msg.role==='user'?'12px':'0'}">
              <div :class="msg.role==='user'?'msg-user':'msg-bot'" v-html="renderMarkdown(msg.content)"></div>
              <div v-if="msg.role==='bot' && msg.logId" style="margin-top:4px;display:flex;gap:8px">
                <el-button size="small" :type="msg.helpful===1?'primary':''" text @click="feedback(msg.logId, 1, idx)"><el-icon><Select /></el-icon> 有用</el-button>
                <el-button size="small" :type="msg.helpful===0?'danger':''" text @click="feedback(msg.logId, 0, idx)"><el-icon><CloseBold /></el-icon> 无用</el-button>
              </div>
            </div>
          </div>
          <div v-if="chatLoading" style="display:flex;gap:8px;color:var(--text-muted)">
            <el-avatar :size="32" style="background:#555;flex-shrink:0"><el-icon class="is-loading"><Loading /></el-icon></el-avatar>
            <div class="msg-bot">正在思考中...</div>
          </div>
        </div>
        <div style="padding:16px 20px;border-top:1px solid var(--border-light);display:flex;gap:12px">
          <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入问题，按 Enter 发送..." resize="none" @keydown.enter.exact.prevent="sendMessage" style="flex:1" />
          <el-button type="primary" :loading="chatLoading" @click="sendMessage" style="height:56px;width:70px"><el-icon :size="18"><Promotion /></el-icon></el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chatBotAsk, chatBotFeedback, getChatBotQuickQuestions } from '../api/common'

const messages = ref([])
const inputText = ref('')
const chatLoading = ref(false)
const chatContainer = ref(null)
const quickQuestions = ref([])

const loadQuickQuestions = async () => {
  try {
    const res = await getChatBotQuickQuestions()
    quickQuestions.value = res.data || []
  } catch {}
}

const askQuestion = (q) => { inputText.value = q; sendMessage() }

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || chatLoading.value) return
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  chatLoading.value = true
  await nextTick(); scrollToBottom()
  try {
    const adminInfo = JSON.parse(localStorage.getItem('admin_info') || '{}')
    const res = await chatBotAsk({ question: text, adminId: adminInfo.adminId || null })
    messages.value.push({ role: 'bot', content: res.data.answer, logId: res.data.logId, helpful: null })
  } catch { messages.value.push({ role: 'bot', content: '抱歉，遇到一些问题，请稍后再试。' }) }
  chatLoading.value = false
  await nextTick(); scrollToBottom()
}

const feedback = async (logId, helpful, idx) => {
  try { await chatBotFeedback({ logId, helpful }); messages.value[idx].helpful = helpful } catch {}
}

const clearChat = () => { messages.value = [] }
const scrollToBottom = () => { if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight }
const renderMarkdown = (text) => { if (!text) return ''; return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br>') }

onMounted(loadQuickQuestions)
</script>

<style scoped>
.msg-user { background: #E60012; color: #fff; padding: 10px 16px; border-radius: 12px 4px 12px 12px; font-size: 14px; line-height: 1.6; }
.msg-bot { background: var(--bg-soft); color: var(--text-main); padding: 10px 16px; border-radius: 4px 12px 12px 12px; font-size: 14px; line-height: 1.6; }
.quick-q { display: flex; align-items: center; padding: 10px 12px; margin-bottom: 6px; border-radius: 8px; cursor: pointer; transition: all 0.2s; color: var(--text-secondary); }
.quick-q:hover { background: #FFF5F5; color: #E60012; }
</style>
