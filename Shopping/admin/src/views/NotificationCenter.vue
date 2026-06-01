<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">通知中心</h2>
        <p class="page-subtitle">查看系统通知与待办提醒</p>
      </div>
    </div>
    <div class="content-card">
      <div style="margin-bottom:16px;display:flex;gap:12px;justify-content:space-between">
        <div style="display:flex;gap:10px">
          <el-select v-model="typeFilter" placeholder="通知类型" clearable style="width:130px" @change="loadData">
            <el-option label="系统通知" :value="1" /><el-option label="待办提醒" :value="2" /><el-option label="审核通知" :value="3" /><el-option label="预警通知" :value="4" />
          </el-select>
          <el-select v-model="readFilter" placeholder="已读状态" clearable style="width:110px" @change="loadData">
            <el-option label="未读" :value="0" /><el-option label="已读" :value="1" />
          </el-select>
        </div>
        <el-button type="primary" text @click="markAllRead" :disabled="unreadCount===0">全部已读</el-button>
      </div>
      <div v-for="item in notifications" :key="item.notificationId" :class="['notif-item', item.isRead===0?'unread':'']" @click="handleRead(item)">
        <div style="display:flex;align-items:flex-start;gap:12px">
          <div :style="{background: typeColors[item.type]||'#909399', borderRadius:'50%', width:'36px', height:'36px', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0}">
            <el-icon color="#fff" :size="16"><component :is="typeIcons[item.type]||'Bell'" /></el-icon>
          </div>
          <div style="flex:1;min-width:0">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:500;font-size:14px">{{ item.title }}</span>
              <el-tag v-if="item.isRead===0" type="danger" size="small" effect="dark" style="border-radius:999px">NEW</el-tag>
            </div>
            <div style="color:var(--text-muted);font-size:13px;margin-top:4px">{{ item.content }}</div>
            <div style="color:var(--text-muted);font-size:12px;margin-top:4px;display:flex;gap:8px;align-items:center">
              <span>{{ item.createTime }}</span>
              <el-tag v-if="getRouteByType(item.bizType)" size="small" effect="plain" style="border-radius:999px">点击前往处理</el-tag>
            </div>
          </div>
          <el-button type="danger" text size="small" @click.stop="handleDelete(item.notificationId)"><el-icon><Delete /></el-icon></el-button>
        </div>
      </div>
      <el-empty v-if="notifications.length===0" description="暂无通知" />
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNotificationList, getNotificationUnreadCount, markNotificationRead, markAllNotificationRead, deleteNotification } from '../api/common'

const router = useRouter()
const adminInfo = JSON.parse(localStorage.getItem('admin_info') || '{}')
const adminId = adminInfo.adminId || 1
const notifications = ref([])
const current = ref(1), size = ref(10), total = ref(0), unreadCount = ref(0)
const typeFilter = ref(null), readFilter = ref(null)
const typeColors = { 1: '#E60012', 2: '#FF5252', 3: '#67C23A', 4: '#F56C6C' }
const typeIcons = { 1: 'Bell', 2: 'Clock', 3: 'CircleCheck', 4: 'WarningFilled' }

const routeMap = {
  merchant: '/merchant', goods: '/goods', order: '/order', after_sale: '/after-sale', aftersale: '/after-sale',
  dispute: '/dispute', abnormal: '/abnormal', activity: '/activity', coupon: '/coupon', user: '/user'
}
const getRouteByType = (bizType) => bizType ? routeMap[String(bizType).toLowerCase()] : ''

const loadData = async () => {
  const params = { current: current.value, size: size.value, adminId }
  if (typeFilter.value != null) params.type = typeFilter.value
  if (readFilter.value != null) params.isRead = readFilter.value
  const res = await getNotificationList(params)
  notifications.value = res.data?.records || []
  total.value = res.data?.total || 0
  const uc = await getNotificationUnreadCount({ adminId })
  unreadCount.value = uc.data?.count || 0
}

const handleRead = async (item) => {
  if (item.isRead === 0) {
    await markNotificationRead(item.notificationId)
    item.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  const path = getRouteByType(item.bizType)
  if (path) router.push(path)
}

const markAllRead = async () => {
  await markAllNotificationRead({ adminId })
  notifications.value.forEach(n => n.isRead = 1)
  unreadCount.value = 0
  ElMessage.success('已全部标记为已读')
}

const handleDelete = async (id) => {
  await deleteNotification(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.notif-item { padding: 16px; border-bottom: 1px solid var(--border-light); cursor: pointer; transition: background 0.2s; border-radius: 8px; }
.notif-item:hover { background: var(--bg-soft); }
.notif-item.unread { background: #FFF8F8; }
.notif-item.unread:hover { background: #FFF5F5; }
</style>
