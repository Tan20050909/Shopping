<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">系统设置</h2>
        <p class="page-subtitle">配置平台基础参数</p>
      </div>
    </div>
    <div class="content-card">
      <el-tabs v-model="activeGroup">
        <el-tab-pane label="基础设置" name="basic" />
        <el-tab-pane label="订单设置" name="order" />
        <el-tab-pane label="支付设置" name="payment" />
        <el-tab-pane label="物流设置" name="shipping" />
      </el-tabs>
      <el-form label-width="160px" v-loading="loading">
        <el-form-item v-for="item in configList" :key="item.configId" :label="item.configDesc || item.configKey">
          <el-input v-model="item.configValue" style="width:400px" />
          <el-button type="primary" link style="margin-left:12px" @click="handleSave(item)">保存</el-button>
        </el-form-item>
        <el-empty v-if="!loading && configList.length === 0" description="暂无配置项" />
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, updateConfig } from '../api/config'

const activeGroup = ref('basic')
const configList = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getConfigList({ configGroup: activeGroup.value })
    configList.value = res.data || []
  } finally { loading.value = false }
}

const handleSave = async (item) => {
  await updateConfig(item.configId, { configValue: item.configValue })
  ElMessage.success('保存成功')
}

watch(activeGroup, loadData)
onMounted(loadData)
</script>
