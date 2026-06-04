<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">USER MANAGEMENT</span>
          <h1 class="admin-page-title">用户管理</h1>
          <p class="admin-page-desc">管理平台注册用户，审核与管控用户账户状态</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel">
        <div class="admin-filter-bar">
          <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable class="admin-search-input" @clear="loadData" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button plain @click="loadData">刷新</el-button>
        </div>
        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table" style="width:100%">
            <template #empty><el-empty description="暂无用户数据" /></template>
            <el-table-column prop="userId" label="ID" width="70" />
            <el-table-column label="用户" min-width="200">
              <template #default="{ row }">
                <div style="display:flex;align-items:center;gap:12px">
                  <el-avatar :size="36" :src="avatarSrc(row.avatar)" style="background:#E60012;font-weight:600">{{ (row.nickname||row.username||'U')[0] }}</el-avatar>
                  <div>
                    <div style="font-weight:600">{{ row.nickname || row.username }}</div>
                    <div style="font-size:12px;color:var(--text-muted)">{{ row.phone || '-' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" width="180" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="row.status===1?'tag-success':'tag-danger'">{{ row.status===1?'正常':'禁用' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" width="170" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="showDetail(row)">详情</button>
                  <button class="admin-action-btn" :class="row.status===1?'admin-action-danger':''" :style="row.status!==1?'color:#047857;border-color:#a7f3d0':''" @click="toggleStatus(row)">{{ row.status===1?'禁用':'启用' }}</button>
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
    <el-dialog v-model="detailVisible" title="用户详情" width="560px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="admin-status-tag" :class="detail.status===1?'tag-success':'tag-danger'">{{ detail.status===1?'正常':'禁用' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, getUserDetail, updateUserStatus } from '../api/user'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref('')
const detailVisible = ref(false), detail = ref({})
const defaultAvatar = '/brand-assets/avatars/default-avatar-01.png'

const avatarSrc = (src) => {
  const v = String(src || '').trim()
  if (!v || v.startsWith('https://api.dicebear.com/')) return defaultAvatar
  return v
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await getUserList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载用户数据失败，请重试')
  } finally { loading.value = false }
}

const showDetail = async (row) => {
  try {
    const res = await getUserDetail(row.userId)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取用户详情失败')
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户"${row.nickname || row.username}"吗？`, '操作确认', { type: 'warning' })
    await updateUserStatus(row.userId, newStatus)
    ElMessage.success(`已${action}`)
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败，请重试')
  }
}

onMounted(loadData)
</script>
<style scoped>
.admin-order-page { color: var(--text-main); }
</style>
