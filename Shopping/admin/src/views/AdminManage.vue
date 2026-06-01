<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">员工与身份管理</h2>
        <p class="page-subtitle">超级管理员为员工分配岗位身份，不同身份登录后只处理自己负责的事务</p>
      </div>
      <el-button type="primary" @click="openForm()">新增员工</el-button>
    </div>
    <div class="content-card">
      <el-alert title="现实岗位建议：运营管理员处理商家/商品/营销；客服处理售后、纠纷、发货提醒和退款协助；审计员以只读巡检和日志审计为主。" type="info" show-icon :closable="false" style="margin-bottom:16px" />
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="adminId" label="ID" width="70" />
        <el-table-column label="员工" min-width="170">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <el-avatar :size="32" style="background:#E60012;font-weight:600;font-size:13px">{{ (row.realName||row.username||'A')[0] }}</el-avatar>
              <div>
                <div style="font-weight:500">{{ row.realName || row.username }}</div>
                <div style="font-size:12px;color:var(--text-muted)">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'danger'" size="small" effect="light" style="border-radius:999px">{{ row.status===1?'正常':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openRoles(row)">分配身份</el-button>
            <el-button v-if="row.adminId!==1" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.adminId?'编辑员工':'新增员工'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.adminId" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="密码" v-if="!form.adminId"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配身份" width="760px">
      <el-alert title="员工重新登录后会按新身份加载菜单和接口权限；超级管理员必须保留超级管理员身份。" type="warning" show-icon :closable="false" style="margin-bottom:14px" />
      <el-checkbox-group v-model="selectedRoles" class="role-grid">
        <el-checkbox v-for="r in allRoles" :key="r.roleId" :value="r.roleId" class="role-card">
          <div class="role-name">{{ r.roleName }}</div>
          <div class="role-code">{{ r.roleCode }}</div>
          <div class="role-desc">{{ roleDesc(r) }}</div>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible=false">取消</el-button>
        <el-button type="primary" @click="submitRoles">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminList, addAdmin, updateAdmin, deleteAdmin, getAdminRoles, assignRoles } from '../api/admin'
import { getAllRoles } from '../api/role'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const formVisible = ref(false)
const submitting = ref(false)
const form = reactive({ adminId: null, username: '', realName: '', phone: '', password: '', status: 1 })
const roleVisible = ref(false), allRoles = ref([]), selectedRoles = ref([]), roleAdminId = ref(null)

const roleTips = {
  SUPER_ADMIN: '全平台配置、员工身份、角色权限、关键操作审批',
  OPERATOR: '商家入驻、商品审核、营销活动、订单运营提醒',
  OPS_MANAGER: '商家入驻、商品审核、营销活动、订单运营提醒',
  SERVICE: '客服消息、售后处理、纠纷协调、提醒商家发货、协助退款',
  CS_SUPERVISOR: '客服消息、售后处理、纠纷协调、提醒商家发货、协助退款',
  AUDITOR: '查看报表、巡检订单/商家/商品、审计操作日志',
  RISK_OFFICER: '异常订单、用户风控、商家信用、违规处置',
  FINANCE_OFFICER: '退款复核、结算、对账和财务数据查看',
  CONTENT_REVIEWER: '商品图文、评论、轮播与内容审核'
}

const roleDesc = (role) => roleTips[role.roleCode] || role.description || '按配置权限处理指定事务'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminList({ current: current.value, size: size.value })
    tableData.value = res.data?.records || []; total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载员工列表失败')
  } finally { loading.value = false }
}

const openForm = (row) => {
  if (row) Object.assign(form, { ...row, password: '' })
  else Object.assign(form, { adminId: null, username: '', realName: '', phone: '', password: '', status: 1 })
  formVisible.value = true
}

const submitForm = async () => {
  if (!form.username?.trim()) { ElMessage.warning('请输入用户名'); return }
  if (!form.adminId && !form.password) { ElMessage.warning('请设置初始密码'); return }
  if (!form.adminId && form.password.length < 6) { ElMessage.warning('密码长度不能少于6位'); return }
  submitting.value = true
  try {
    if (form.adminId) await updateAdmin(form)
    else await addAdmin(form)
    ElMessage.success('操作成功'); formVisible.value = false; loadData()
  } catch (e) {
    // axios interceptor already shows the error message
  } finally { submitting.value = false }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该员工？', '删除确认', { type: 'warning' })
  await deleteAdmin(row.adminId); ElMessage.success('已删除'); loadData()
}

const openRoles = async (row) => {
  roleAdminId.value = row.adminId
  const [roleRes, assignedRes] = await Promise.all([getAllRoles(), getAdminRoles(row.adminId)])
  allRoles.value = roleRes.data || []
  selectedRoles.value = assignedRes.data || []
  roleVisible.value = true
}

const submitRoles = async () => {
  if (roleAdminId.value === 1 && !selectedRoles.value.includes(1)) {
    ElMessage.warning('超级管理员必须保留超级管理员身份')
    return
  }
  await assignRoles(roleAdminId.value, selectedRoles.value)
  ElMessage.success('身份分配成功，员工重新登录后生效'); roleVisible.value = false; loadData()
}

onMounted(loadData)
</script>

<style scoped>
.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.role-card {
  height: auto;
  margin-right: 0;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  align-items: flex-start;
}
.role-card :deep(.el-checkbox__label) {
  white-space: normal;
  line-height: 1.5;
}
.role-name {
  font-weight: 700;
  color: var(--text-main);
}
.role-code {
  margin-top: 2px;
  font-size: 12px;
  color: var(--brand-red);
}
.role-desc {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
@media (max-width: 768px) {
  .role-grid { grid-template-columns: 1fr; }
}
</style>
