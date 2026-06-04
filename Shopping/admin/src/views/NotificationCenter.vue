<template>
  <div class="admin-order-page">
    <!-- Hero Banner -->
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">NOTIFICATION CENTER</span>
          <h1 class="admin-page-title">通知中心</h1>
          <p class="admin-page-desc">查看系统通知与待办提醒</p>
        </div>
      </div>
    </section>

    <!-- Content -->
    <div class="admin-page-container">
      <div class="admin-panel">
        <!-- 筛选栏 -->
        <div class="admin-filter-bar">
          <el-select v-model="typeFilter" placeholder="通知类型" clearable class="admin-status-select" @change="loadData">
            <el-option label="系统通知" :value="1" />
            <el-option label="待办提醒" :value="2" />
            <el-option label="审核通知" :value="3" />
            <el-option label="预警通知" :value="4" />
          </el-select>
          <el-select v-model="readFilter" placeholder="已读状态" clearable class="admin-status-select" @change="loadData">
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
          <div style="flex:1" />
          <el-button type="primary" :disabled="unreadCount===0" @click="markAllRead">全部已读</el-button>
        </div>

        <!-- 通知列表 -->
        <div v-loading="loading" class="admin-table-wrap">
          <div v-if="notifications.length===0">
            <el-empty description="暂无通知" />
          </div>
          <div
            v-for="item in notifications"
            :key="item.notificationId"
            :class="['notif-row', { unread: item.isRead === 0 }]"
            @click="handleRead(item)"
          >
            <div class="notif-indicator">
              <span v-if="item.isRead === 0" class="notif-dot" />
            </div>
            <div class="notif-icon" :style="{ background: typeColors[item.type] || '#909399' }">
              <el-icon color="#fff" :size="16"><component :is="typeIcons[item.type] || 'Bell'" /></el-icon>
            </div>
            <div class="notif-body">
              <div class="notif-title-row">
                <span class="notif-title">{{ item.title }}</span>
                <el-tag v-if="item.isRead === 0" type="danger" size="small" effect="dark" class="notif-new-tag">NEW</el-tag>
              </div>
              <div class="notif-content">{{ item.content }}</div>
              <div class="notif-meta">
                <span class="notif-time">{{ item.createTime }}</span>
                <button
                  v-if="getRouteByType(item.bizType)"
                  type="button"
                  class="admin-action-btn notif-handle-btn"
                  @click.stop="handleRead(item)"
                >点击前往处理</button>
              </div>
            </div>
            <div class="notif-actions">
              <button
                type="button"
                class="admin-action-btn admin-action-danger notif-delete-btn"
                title="删除"
                @click.stop="handleDelete(item.notificationId)"
              >
                <el-icon :size="14"><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="admin-pagination-bar">
          <el-pagination
            v-model:current-page="current"
            v-model:page-size="size"
            :page-sizes="[10,20,50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
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
const loading = ref(false)
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
  loading.value = true
  try {
    const params = { current: current.value, size: size.value, adminId }
    if (typeFilter.value != null) params.type = typeFilter.value
    if (readFilter.value != null) params.isRead = readFilter.value
    const res = await getNotificationList(params)
    notifications.value = res.data?.records || []
    total.value = res.data?.total || 0
    const uc = await getNotificationUnreadCount({ adminId })
    unreadCount.value = uc.data?.count || 0
  } catch (e) {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const handleRead = async (item) => {
  if (item.isRead === 0) {
    try {
      await markNotificationRead(item.notificationId)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) {
      // ignore
    }
  }
  const path = getRouteByType(item.bizType)
  if (path) router.push(path)
}

const markAllRead = async () => {
  try {
    await markAllNotificationRead({ adminId })
    notifications.value.forEach(n => n.isRead = 1)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await deleteNotification(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.admin-order-page {
  color: var(--text-main);
}

/* 通知列表 — 类表格行风格 */
.admin-table-wrap {
  overflow: visible;
}

.notif-row {
  display: flex;
  align-items: stretch;
  gap: 0;
  padding: 0;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-light);
  min-height: 80px;
}

.notif-row:last-child {
  border-bottom: none;
}

.notif-row:hover {
  background: #fbfbfb;
}

.notif-row.unread {
  background: #fffdfd;
}

.notif-row.unread:hover {
  background: #fff8f8;
}

/* 未读红点指示器 */
.notif-indicator {
  width: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notif-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-red);
  flex-shrink: 0;
}

/* 类型图标 */
.notif-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 22px;
  margin-right: 14px;
}

/* 主体内容 */
.notif-body {
  flex: 1;
  min-width: 0;
  padding: 18px 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.notif-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-new-tag {
  flex-shrink: 0;
  border-radius: 999px !important;
  height: 20px;
  line-height: 20px;
  padding: 0 8px;
  font-size: 11px;
  font-weight: 700;
}

.notif-content {
  font-size: 13px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notif-time {
  font-size: 12px;
  color: var(--text-muted);
}

/* 操作区 */
.notif-actions {
  display: flex;
  align-items: center;
  padding-left: 12px;
  flex-shrink: 0;
}

.notif-delete-btn {
  border: none;
  background: transparent;
  color: var(--text-muted);
  padding: 0 8px;
  height: 28px;
}

.notif-delete-btn:hover {
  color: var(--brand-red);
  background: var(--brand-red-light);
  border: none;
}

/* 前往处理按钮继承 admin-action-btn 风格 */
.notif-handle-btn {
  font-size: 11px;
  height: 24px;
  padding: 0 8px;
}
</style>
