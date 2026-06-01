<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">分类管理</h2>
        <p class="page-subtitle">管理商品分类层级</p>
      </div>
      <el-button type="primary" @click="openForm()">新增分类</el-button>
    </div>
    <div class="content-card">
      <el-table :data="tableData" row-key="categoryId" :tree-props="{children:'children'}" v-loading="loading" style="width:100%">
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'info'" size="small" effect="light" style="border-radius:999px">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="formVisible" :title="form.categoryId?'编辑分类':'新增分类'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.categoryName" /></el-form-item>
        <el-form-item label="上级"><el-input-number v-model="form.parentId" :min="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible=false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../api/common'

const tableData = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = reactive({ categoryId: null, categoryName: '', parentId: 0, sortNo: 0, status: 1 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    tableData.value = res.data || []
  } finally { loading.value = false }
}

const openForm = (row) => {
  if (row) Object.assign(form, row)
  else Object.assign(form, { categoryId: null, categoryName: '', parentId: 0, sortNo: 0, status: 1 })
  formVisible.value = true
}

const submitForm = async () => {
  if (form.categoryId) await updateCategory(form)
  else await addCategory(form)
  ElMessage.success('操作成功'); formVisible.value = false; loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除？', '删除确认', { type: 'warning' })
  await deleteCategory(row.categoryId); ElMessage.success('已删除'); loadData()
}

onMounted(loadData)
</script>
