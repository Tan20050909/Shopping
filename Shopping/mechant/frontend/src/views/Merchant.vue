<template>
  <div class="merchant-page">
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-head">
          <span>店铺审核进度</span>
          <el-button plain @click="loadAll">刷新</el-button>
        </div>
      </template>

      <div class="audit-row">
        <el-tag :type="auditTagType">
          {{ auditStatusText }}
        </el-tag>
        <span class="audit-tip">粉丝数：{{ merchant.followerCount ?? 0 }}</span>
        <span class="audit-tip">审核意见：{{ merchant.auditRemark || '-' }}</span>
        <span class="audit-tip">审核时间：{{ merchant.auditTime || '-' }}</span>
      </div>
    </el-card>

    <el-card class="card" shadow="never">
      <template #header><span>资料维护（更换后会重新审核）</span></template>

      <el-form :model="merchant" label-width="110px">
        <div class="form-grid">
          <el-form-item label="店铺名称">
            <el-input v-model="merchant.merchantName" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="merchant.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="merchant.email" />
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="merchant.address" />
          </el-form-item>
          <el-form-item label="实名认证">
            <el-input v-model="merchant.legalPerson" placeholder="姓名" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="merchant.idCard" />
          </el-form-item>
          <el-form-item label="营业执照">
            <el-input v-model="merchant.businessLicense" placeholder="图片/文件地址" />
          </el-form-item>
          <el-form-item label="行业许可证">
            <el-input v-model="merchant.industryLicense" placeholder="图片/文件地址（可选）" />
          </el-form-item>
          <el-form-item label="店铺Logo">
            <el-input v-model="merchant.shopLogo" placeholder="图片地址（可选）" />
          </el-form-item>
          <el-form-item label="店铺简介" class="full">
            <el-input v-model="merchant.shopIntro" type="textarea" :rows="3" placeholder="用于店铺介绍展示" />
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存基础信息</el-button>
          <el-button type="warning" plain :loading="savingMaterials" @click="saveMaterials">更新入驻材料并提交审核</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="card" shadow="never">
      <template #header><span>审核日志</span></template>
      <el-table :data="auditLogs" size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="beforeStatus" label="变更前" width="120">
          <template #default="{ row }">{{ auditText(row.beforeStatus) }}</template>
        </el-table-column>
        <el-table-column prop="afterStatus" label="变更后" width="120">
          <template #default="{ row }">{{ auditText(row.afterStatus) }}</template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="说明/意见" min-width="220" />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { merchantApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const merchant = reactive({
  merchantId: getMerchantId(),
  merchantName: '',
  phone: '',
  email: '',
  address: '',
  legalPerson: '',
  idCard: '',
  businessLicense: '',
  industryLicense: '',
  shopLogo: '',
  shopIntro: '',
  followerCount: 0,
  auditStatus: 0,
  auditRemark: '',
  auditTime: ''
})

const auditLogs = ref([])
const savingProfile = ref(false)
const savingMaterials = ref(false)

const auditText = (s) => {
  const v = Number(s)
  if (v === 1) return '通过'
  if (v === 2) return '失败'
  return '待审核'
}

const auditStatusText = computed(() => auditText(merchant.auditStatus))

const auditTagType = computed(() => {
  const v = Number(merchant.auditStatus)
  if (v === 1) return 'success'
  if (v === 2) return 'danger'
  return 'warning'
})

const loadAll = async () => {
  try {
    const id = getMerchantId()
    const infoRes = await merchantApi.info(id)
    if (infoRes.data) Object.assign(merchant, infoRes.data)
    const auditRes = await merchantApi.audit(id)
    auditLogs.value = Array.isArray(auditRes?.data?.logs) ? auditRes.data.logs : []
  } catch (error) {
    auditLogs.value = []
  }
}

const saveProfile = async () => {
  if (savingProfile.value) return
  try {
    savingProfile.value = true
    merchant.merchantId = getMerchantId()
    await merchantApi.updateProfile({
      merchantId: merchant.merchantId,
      phone: merchant.phone,
      email: merchant.email,
      address: merchant.address,
      legalPerson: merchant.legalPerson,
      idCard: merchant.idCard
    })
    ElMessage.success('保存成功')
    await loadAll()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

const saveMaterials = async () => {
  if (savingMaterials.value) return
  try {
    savingMaterials.value = true
    merchant.merchantId = getMerchantId()
    await merchantApi.updateMaterials({
      merchantId: merchant.merchantId,
      merchantName: merchant.merchantName,
      businessLicense: merchant.businessLicense,
      industryLicense: merchant.industryLicense,
      shopLogo: merchant.shopLogo,
      shopIntro: merchant.shopIntro
    })
    ElMessage.success('已提交，等待审核')
    await loadAll()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    savingMaterials.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.merchant-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card {
  border-radius: 14px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.audit-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.audit-tip {
  color: #6b7280;
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
}

.full {
  grid-column: 1 / -1;
}

@media (max-width: 980px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
