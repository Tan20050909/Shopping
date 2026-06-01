<template>
  <div class="banner-page">
    <h2>轮播图管理</h2>
    
    <el-button type="primary" @click="showAddDialog">添加轮播图</el-button>
    
    <el-table :data="banners" style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="description" label="简介" />
      <el-table-column label="图片" width="120">
        <template #default="{ row }">
          <img :src="row.image" style="width: 80px; height: 60px; object-fit: cover" />
        </template>
      </el-table-column>
      <el-table-column prop="bgColor" label="背景色" width="100" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="editBanner(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteBanner(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑轮播图' : '添加轮播图'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" placeholder="请输入简介" type="textarea" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.image" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.link" placeholder="请输入跳转链接" />
        </el-form-item>
        <el-form-item label="背景色">
          <el-input v-model="form.bgColor" placeholder="例如: linear-gradient(135deg, #ff9a3c 0%, #ff6b35 100%)" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBanner">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { bannerApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const banners = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  title: '',
  description: '',
  image: '',
  link: '',
  bgColor: '',
  sort: 0,
  status: 1
})

const loadBanners = async () => {
  const res = await bannerApi.list()
  banners.value = res.data
}

const showAddDialog = () => {
  isEdit.value = false
  form.value = {
    title: '',
    description: '',
    image: '',
    link: '',
    bgColor: '',
    sort: 0,
    status: 1
  }
  dialogVisible.value = true
}

const editBanner = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const saveBanner = async () => {
  if (isEdit.value) {
    await bannerApi.update(form.value)
    ElMessage.success('更新成功')
  } else {
    await bannerApi.add(form.value)
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
  loadBanners()
}

const deleteBanner = async (id) => {
  await ElMessageBox.confirm('确定删除吗？')
  await bannerApi.delete(id)
  ElMessage.success('删除成功')
  loadBanners()
}

onMounted(() => {
  loadBanners()
})
</script>

<style scoped>
.banner-page {
  padding: 20px;
}
</style>
