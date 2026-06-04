<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">CUSTOMER SERVICE</span>
          <h1 class="admin-page-title">客服消息</h1>
          <p class="admin-page-desc">管理用户与客服之间的消息往来，及时回复用户咨询</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel">
        <div class="admin-filter-bar">
          <el-input v-model="fromIdFilter" placeholder="发送者ID" clearable class="admin-search-input" style="width:160px" @clear="loadData" />
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
        </div>
        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table" style="width:100%">
            <template #empty><el-empty description="暂无客服消息" /></template>
            <el-table-column prop="messageId" label="ID" width="70" />
            <el-table-column prop="fromId" label="发送者" width="90" />
            <el-table-column label="发送者类型" width="100">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="row.fromType===1?'tag-primary':row.fromType===2?'tag-warning':'tag-success'">{{ ['','用户','商家','客服'][row.fromType] }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button v-if="row.fromType!==3" class="admin-action-btn" @click="handleReply(row)">回复</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="admin-pagination-bar">
          <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
        </div>
      </div>
    </div>
    <el-dialog v-model="replyVisible" title="回复消息" width="480px">
      <div style="margin-bottom:12px;padding:12px;background:var(--bg-soft);border-radius:8px">
        <div style="color:var(--text-muted);font-size:12px;margin-bottom:4px">原消息 (用户ID: {{ replyTo.fromId }})</div>
        <div>{{ replyTo.content }}</div>
      </div>
      <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyVisible=false">取消</el-button>
        <el-button type="primary" @click="submitReply">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getChatList, sendChatMessage } from '../api/chat'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const fromIdFilter = ref('')
const replyVisible = ref(false), replyContent = ref(''), replyTo = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (fromIdFilter.value) params.fromId = Number(fromIdFilter.value)
    const res = await getChatList(params)
    tableData.value = res.data?.records ?? []; total.value = res.data?.total ?? 0
  } catch (e) {
    ElMessage.error('加载消息失败')
  } finally { loading.value = false }
}

const handleReply = (row) => { replyTo.value = row; replyContent.value = ''; replyVisible.value = true }

const submitReply = async () => {
  if (!replyContent.value.trim()) { ElMessage.warning('请输入回复'); return }
  try {
    await sendChatMessage({ toId: replyTo.value.fromId, toType: replyTo.value.fromType, content: replyContent.value, msgType: 1 })
    ElMessage.success('发送成功'); replyVisible.value = false; loadData()
  } catch (e) {
    ElMessage.error('发送失败，请重试')
  }
}

onMounted(loadData)
</script>
<style scoped>
.admin-order-page { color: var(--text-main); }
</style>
