<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">用户管理</h2>
        <p class="page-subtitle">管理平台注册用户</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无用户数据" /></template>
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:12px">
              <el-avatar :size="36" :src="row.avatar" style="background:#E60012;font-weight:600">{{ (row.nickname||row.username||'U')[0] }}</el-avatar>
              <div>
                <div style="font-weight:500">{{ row.nickname || row.username }}</div>
                <div style="font-size:12px;color:var(--text-muted)">{{ row.phone || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'danger'" size="small" effect="light" style="border-radius:999px">{{ row.status===1?'正常':'禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button :type="row.status===1?'danger':'success'" link size="small" @click="toggleStatus(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="用户详情" width="560px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="detail.status===1?'success':'danger'" size="small" style="border-radius:999px">{{ detail.status===1?'正常':'禁用' }}</el-tag></el-descriptions-item>
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
