<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">角色权限</h2>
        <p class="page-subtitle">管理角色与权限配置</p>
      </div>
      <el-button type="primary" @click="openForm()">新增角色</el-button>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="roleId" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名" min-width="120" />
        <el-table-column prop="roleCode" label="角色编码" width="140" />
        <el-table-column label="数据范围" width="100">
          <template #default="{ row }">
            <el-tag :type="{1:'',2:'warning',3:'info'}[row.dataScope]||'info'" size="small" effect="light" style="border-radius:999px">{{ {1:'全部',2:'本部门',3:'本人'}[row.dataScope]||'未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.roleType===1?'warning':''" size="small" effect="light" style="border-radius:999px">{{ row.roleType===1?'系统内置':'自定义' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'info'" size="small" effect="light" style="border-radius:999px">{{ row.status===1?'启用':'禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openPermDialog(row)">配置权限</el-button>
            <el-button v-if="row.roleId!==1" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="padding:16px;text-align:right">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="current" @current-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.roleId?'编辑角色':'新增角色'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width:100%">
            <el-option :value="1" label="全部" /><el-option :value="2" label="本部门" /><el-option :value="3" label="本人" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" :title="`配置权限 - ${permRoleName}`" width="680px" top="5vh">
      <div v-loading="permLoading">
        <el-collapse v-model="expandedModules">
          <el-collapse-item v-for="mod in moduleList" :key="mod.key" :name="mod.key">
            <template #title>
              <div style="display:flex;align-items:center;gap:8px;width:100%">
                <span style="font-weight:600">{{ mod.label }}</span>
                <el-tag size="small" type="info" effect="plain" style="border-radius:999px">{{ countModuleChecked(mod.key) }}/{{ modulePerms(mod.key).length }}</el-tag>
                <el-checkbox :model-value="isModuleAll(mod.key)" :indeterminate="isModuleInd(mod.key)" @change="toggleModule(mod.key,$event)" style="margin-left:auto">全选</el-checkbox>
              </div>
            </template>
            <el-checkbox-group v-model="checkedPermIds" style="display:flex;flex-wrap:wrap;gap:8px 16px">
              <el-checkbox v-for="p in modulePerms(mod.key)" :key="p.permissionId" :value="p.permissionId">
                <el-tag size="small" :type="{1:'',2:'success',3:'warning'}[p.permissionType]||'info'" effect="plain" style="margin-right:4px;border-radius:999px">{{ {1:'菜单',2:'按钮',3:'数据'}[p.permissionType]||'其他' }}</el-tag>
                {{ p.permissionName }}
              </el-checkbox>
            </el-checkbox-group>
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <el-button @click="permVisible=false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="submitPermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, addRole, updateRole, deleteRole, getRolePermissions, setRolePermissions } from '../api/role'
import { getAllPermissions } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const current = ref(1), pageSize = ref(10), total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoleList({ current: current.value, size: pageSize.value })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const formVisible = ref(false)
const form = reactive({ roleId: null, roleName: '', roleCode: '', description: '', dataScope: 1, status: 1 })

const openForm = (row) => {
  if (row) Object.assign(form, { roleId: row.roleId, roleName: row.roleName, roleCode: row.roleCode, description: row.description, dataScope: row.dataScope, status: row.status })
  else Object.assign(form, { roleId: null, roleName: '', roleCode: '', description: '', dataScope: 1, status: 1 })
  formVisible.value = true
}

const submitForm = async () => {
  if (form.roleId) await updateRole(form)
  else await addRole(form)
  ElMessage.success('操作成功')
  formVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该角色？', '删除确认', { type: 'warning' })
  await deleteRole(row.roleId)
  ElMessage.success('已删除')
  loadData()
}

const moduleList = [
  { key: 'SYSTEM', label: '系统管理' }, { key: 'USER', label: '用户管理' },
  { key: 'MERCHANT', label: '商户管理' }, { key: 'GOODS', label: '商品管理' },
  { key: 'ORDER', label: '订单管理' }, { key: 'DATA', label: '数据管理' },
  { key: 'MARKETING', label: '营销管理' }, { key: 'CONTENT', label: '内容管理' },
  { key: 'DISPUTE', label: '争议管理' }, { key: 'FINANCE', label: '财务管理' },
  { key: 'MESSAGE', label: '消息管理' }
]

const permVisible = ref(false)
const permLoading = ref(false)
const permSaving = ref(false)
const permRoleId = ref(null)
const permRoleName = ref('')
const allPermissions = ref([])
const checkedPermIds = ref([])
const expandedModules = ref([])

const modulePerms = (mod) => allPermissions.value.filter(p => p.module === mod)
const countModuleChecked = (mod) => { const ids = modulePerms(mod).map(p => p.permissionId); return checkedPermIds.value.filter(id => ids.includes(id)).length }
const isModuleAll = (mod) => { const perms = modulePerms(mod); return perms.length > 0 && perms.every(p => checkedPermIds.value.includes(p.permissionId)) }
const isModuleInd = (mod) => { const perms = modulePerms(mod); const c = perms.filter(p => checkedPermIds.value.includes(p.permissionId)).length; return c > 0 && c < perms.length }
const toggleModule = (mod, val) => {
  const ids = modulePerms(mod).map(p => p.permissionId)
  if (val) checkedPermIds.value = [...new Set([...checkedPermIds.value, ...ids])]
  else checkedPermIds.value = checkedPermIds.value.filter(id => !ids.includes(id))
}

const openPermDialog = async (row) => {
  permRoleId.value = row.roleId
  permRoleName.value = row.roleName
  permVisible.value = true
  permLoading.value = true
  try {
    const [permRes, rolePermRes] = await Promise.all([getAllPermissions(), getRolePermissions(row.roleId)])
    allPermissions.value = permRes.data || []
    checkedPermIds.value = (rolePermRes.data || []).map(p => typeof p === 'object' ? p.permissionId : p)
    expandedModules.value = moduleList.filter(m => modulePerms(m.key).length > 0).map(m => m.key)
  } finally { permLoading.value = false }
}

const submitPermissions = async () => {
  permSaving.value = true
  try {
    await setRolePermissions(permRoleId.value, checkedPermIds.value)
    ElMessage.success('权限配置成功')
    permVisible.value = false
  } finally { permSaving.value = false }
}

onMounted(loadData)
</script>
