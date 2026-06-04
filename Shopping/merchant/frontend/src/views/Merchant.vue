<template>
  <div class="merchant-profile-page">
    <section class="profile-card shop-profile-card">
      <div class="shop-identity">
        <img class="shop-avatar" :src="currentAvatar" alt="店铺头像" />
        <div class="shop-summary">
          <p class="eyebrow">商家版用户个人中心</p>
          <h1>{{ merchant.merchantName || '商家店铺' }}</h1>
          <div class="status-line">
            <span class="status-pill">{{ auditStatusText }}</span>
            <span class="status-pill subtle">{{ businessStatusText }}</span>
          </div>
          <div class="shop-metrics">
            <span>信誉 <strong>{{ merchant.creditScore ?? merchant.credit ?? 5 }}</strong></span>
            <span>评分 <strong>{{ scoreText }}</strong></span>
          </div>
        </div>
      </div>

      <div class="avatar-picker">
        <div class="avatar-picker-head">
          <div>
            <strong>默认头像</strong>
            <p>点击选择，或上传自己的头像</p>
          </div>
          <div class="avatar-actions">
            <el-upload action="" :http-request="uploadAvatar" :show-file-list="false" accept="image/*">
              <el-button class="outline-pill">上传头像</el-button>
            </el-upload>
            <el-button class="primary-pill" :loading="savingAvatar" :disabled="!selectedAvatar" @click="saveAvatar">保存头像</el-button>
          </div>
        </div>
        <div class="avatar-grid">
          <button
            v-for="url in avatarCandidates"
            :key="url"
            class="avatar-option"
            :class="{ active: url === selectedAvatar }"
            type="button"
            @click="selectAvatar(url)"
          >
            <img :src="url" alt="默认头像" />
          </button>
        </div>
      </div>
    </section>

    <section class="profile-card audit-card">
      <div class="single-tab">入驻审核</div>

      <div class="audit-content">
        <div class="audit-overview">
          <div v-for="item in auditOverview" :key="item.label" class="overview-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div class="audit-detail-grid">
          <div>
            <span>审核原因</span>
            <p>{{ merchant.auditRemark || '暂无审核原因' }}</p>
          </div>
          <div>
            <span>审核意见</span>
            <p>{{ merchant.auditRemark || '暂无审核意见' }}</p>
          </div>
          <div>
            <span>审核时间</span>
            <p>{{ merchant.auditTime || '暂无审核时间' }}</p>
          </div>
        </div>

        <div class="section-heading">
          <div>
            <h2>入驻材料维护</h2>
            <p>材料更新后将重新提交平台审核。</p>
          </div>
        </div>

        <el-form class="materials-form" :model="merchant" label-position="top">
          <div class="materials-grid">
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
              <el-input v-model="merchant.businessLicense" placeholder="图片或文件地址" />
            </el-form-item>
            <el-form-item label="行业许可证">
              <el-input v-model="merchant.industryLicense" placeholder="图片或文件地址（可选）" />
            </el-form-item>
            <el-form-item label="店铺 Logo">
              <el-input v-model="merchant.shopLogo" placeholder="图片地址（可选）" />
            </el-form-item>
            <el-form-item label="店铺简介" class="full-field">
              <el-input v-model="merchant.shopIntro" type="textarea" :rows="3" placeholder="用于店铺介绍展示" />
            </el-form-item>
          </div>
          <el-button class="primary-pill" :loading="savingMaterials" @click="saveMaterials">更新入驻材料并提交审核</el-button>
        </el-form>

        <div class="section-heading log-heading">
          <div>
            <h2>审核日志</h2>
            <p>平台审核记录会按时间展示在这里。</p>
          </div>
          <el-button class="outline-pill" @click="loadAll">刷新</el-button>
        </div>

        <el-table v-if="auditLogs.length" :data="auditLogs" class="audit-table">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="beforeStatus" label="变更前" width="120">
            <template #default="{ row }">{{ auditText(row.beforeStatus) }}</template>
          </el-table-column>
          <el-table-column prop="afterStatus" label="变更后" width="120">
            <template #default="{ row }">{{ auditText(row.afterStatus) }}</template>
          </el-table-column>
          <el-table-column prop="auditRemark" label="说明 / 意见" min-width="220" />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
        <el-empty v-else description="暂无审核日志" :image-size="88" />
      </div>
    </section>

  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { merchantApi, uploadApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'
import { DEFAULT_AVATARS, DEFAULT_USER_AVATAR, resolveAvatar } from '@/utils/avatar'

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
  auditTime: '',
  status: 0,
  shopScore: 0
})

const auditLogs = ref([])
const savingMaterials = ref(false)
const savingAvatar = ref(false)
const avatarCandidates = DEFAULT_AVATARS
const selectedAvatar = ref('')

const auditText = (status) => {
  const value = Number(status)
  if (value === 1) return '已通过'
  if (value === 2) return '未通过'
  return '待审核'
}

const auditStatusText = computed(() => auditText(merchant.auditStatus))
const businessStatusText = computed(() => {
  if (Number(merchant.status) === 3) return '平台冻结'
  if (Number(merchant.status) === 1) return '营业中'
  return '未营业'
})
const currentAvatar = computed(() => resolveAvatar(merchant.shopLogo, DEFAULT_USER_AVATAR))
const scoreText = computed(() => {
  const value = Number(merchant.shopScore ?? merchant.shop_score ?? merchant.rating ?? 0)
  return Number.isFinite(value) && value > 0 ? value.toFixed(1) : '4.8'
})
const auditOverview = computed(() => [
  { label: '当前审核状态', value: auditStatusText.value },
  { label: '营业状态', value: businessStatusText.value },
  { label: '粉丝数', value: merchant.followerCount ?? 0 }
])

const syncHeaderProfile = () => {
  try {
    const raw = localStorage.getItem('merchantUser')
    const local = raw ? JSON.parse(raw) : {}
    localStorage.setItem('merchantUser', JSON.stringify({
      ...(local || {}),
      merchantName: merchant.merchantName,
      shopLogo: merchant.shopLogo,
      shopScore: merchant.shopScore,
      followerCount: merchant.followerCount
    }))
  } catch (error) {
  }
}

const loadAll = async () => {
  try {
    const id = getMerchantId()
    const infoRes = await merchantApi.info(id)
    if (infoRes.data) Object.assign(merchant, infoRes.data)
    selectedAvatar.value = merchant.shopLogo || currentAvatar.value
    syncHeaderProfile()
    const auditRes = await merchantApi.audit(id)
    auditLogs.value = Array.isArray(auditRes?.data?.logs) ? auditRes.data.logs : []
  } catch (error) {
    auditLogs.value = []
  }
}

const saveAvatar = async () => {
  if (savingAvatar.value || !selectedAvatar.value) return
  try {
    savingAvatar.value = true
    merchant.merchantId = getMerchantId()
    await merchantApi.updateLogo({
      merchantId: merchant.merchantId,
      shopLogo: selectedAvatar.value
    })
    merchant.shopLogo = selectedAvatar.value
    syncHeaderProfile()
    ElMessage.success('头像已保存')
    await loadAll()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '头像保存失败')
  } finally {
    savingAvatar.value = false
  }
}

const saveMaterials = async () => {
  if (savingMaterials.value) return
  try {
    savingMaterials.value = true
    merchant.merchantId = getMerchantId()
    await merchantApi.updateProfile({
      merchantId: merchant.merchantId,
      phone: merchant.phone,
      email: merchant.email,
      address: merchant.address,
      legalPerson: merchant.legalPerson,
      idCard: merchant.idCard
    })
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

const selectAvatar = (url) => {
  selectedAvatar.value = url
  merchant.shopLogo = url
}

const uploadAvatar = async (options) => {
  try {
    const res = await uploadApi.uploadImage(options.file)
    const url = res?.data?.url || ''
    if (!url) throw new Error('上传失败')
    selectedAvatar.value = url
    merchant.shopLogo = url
    ElMessage.success('头像已上传，请点击保存头像')
    options?.onSuccess?.(res?.data, options.file)
  } catch (error) {
    options?.onError?.(error)
    ElMessage.error('上传失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.merchant-profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card {
  background: #fff;
  border: 1px solid #eeeeee;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}

.shop-profile-card {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 42px;
  padding: 34px;
}

.shop-identity {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.shop-avatar {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  object-fit: cover;
  background: #fafafa;
  border: 5px solid #fff;
  box-shadow: 0 0 0 1px #eeeeee, 0 10px 24px rgba(0, 0, 0, 0.08);
}

.shop-summary h1 {
  margin: 4px 0 10px;
  font-size: 24px;
  line-height: 1.25;
  color: #111;
}

.eyebrow {
  color: #999;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.status-line,
.shop-metrics {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-pill {
  padding: 5px 10px;
  border-radius: 999px;
  color: #e60012;
  background: #fff0f1;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.subtle {
  color: #555;
  background: #f6f6f6;
}

.shop-metrics {
  margin: 16px 0;
  color: #777;
  font-size: 13px;
}

.shop-metrics span + span {
  padding-left: 10px;
  border-left: 1px solid #eee;
}

.shop-metrics strong {
  color: #111;
}

.materials-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
}

.full-field {
  grid-column: 1 / -1;
}

.materials-form :deep(.el-form-item__label) {
  color: #555;
  font-weight: 700;
}

.materials-form :deep(.el-input__wrapper),
.materials-form :deep(.el-textarea__inner) {
  border-radius: 12px;
  background: #fafafa;
  box-shadow: 0 0 0 1px #eeeeee inset;
}

.materials-form :deep(.el-input__wrapper.is-focus),
.materials-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #e60012 inset;
}

.primary-pill,
.outline-pill {
  border-radius: 999px;
  font-weight: 700;
}

.primary-pill {
  color: #fff;
  background: #e60012;
  border-color: #e60012;
}

.primary-pill:hover {
  color: #fff;
  background: #c4000f;
  border-color: #c4000f;
}

.outline-pill {
  color: #e60012;
  background: #fff;
  border-color: #ffd6d9;
}

.outline-pill:hover {
  color: #c4000f;
  background: #fff7f7;
  border-color: #e60012;
}

.audit-card {
  overflow: hidden;
}

.single-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 58px;
  margin-left: 30px;
  color: #e60012;
  font-size: 15px;
  font-weight: 800;
}

.single-tab::after {
  content: "";
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  border-radius: 999px;
  background: #e60012;
}

.audit-content {
  padding: 28px 30px 34px;
  border-top: 1px solid #f1f1f1;
}

.audit-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.overview-item {
  padding: 18px;
  border: 1px solid #eeeeee;
  border-radius: 14px;
  background: #fafafa;
}

.overview-item span,
.audit-detail-grid span {
  color: #999;
  font-size: 12px;
}

.overview-item strong {
  display: block;
  margin-top: 8px;
  color: #111;
  font-size: 20px;
}

.audit-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.audit-detail-grid > div {
  min-height: 64px;
  padding: 0 2px;
}

.audit-detail-grid p {
  margin-top: 7px;
  color: #555;
  font-size: 13px;
  line-height: 1.7;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: 34px 0 18px;
  padding-top: 28px;
  border-top: 1px solid #f1f1f1;
}

.section-heading h2 {
  color: #111;
  font-size: 18px;
}

.section-heading p {
  margin-top: 5px;
  color: #999;
  font-size: 12px;
}

.audit-table {
  border: 1px solid #eeeeee;
  border-radius: 14px;
  overflow: hidden;
}

.audit-table :deep(th.el-table__cell) {
  color: #555;
  background: #fafafa;
}

.audit-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.avatar-picker {
  display: grid;
  gap: 18px;
  padding: 4px 0;
}

.avatar-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.avatar-picker-head strong {
  color: #111;
  font-size: 15px;
}

.avatar-picker-head p {
  margin-top: 5px;
  color: #999;
  font-size: 12px;
}

.avatar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(10, 48px);
  gap: 12px;
}

.avatar-option {
  position: relative;
  width: 48px;
  height: 48px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.avatar-option:hover,
.avatar-option.active {
  border-color: #e60012;
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(230, 0, 18, 0.14);
}

.avatar-option.active::after {
  content: "";
  position: absolute;
  right: -1px;
  bottom: 1px;
  width: 11px;
  height: 11px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #e60012;
}

.avatar-option img {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

@media (max-width: 980px) {
  .shop-profile-card {
    grid-template-columns: 1fr;
  }

  .audit-overview,
  .audit-detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .shop-profile-card,
  .audit-content {
    padding: 22px;
  }

  .shop-identity {
    flex-direction: column;
  }

  .materials-grid {
    grid-template-columns: 1fr;
  }

  .full-field {
    grid-column: auto;
  }

  .avatar-grid {
    grid-template-columns: repeat(5, 48px);
  }

  .avatar-picker-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
