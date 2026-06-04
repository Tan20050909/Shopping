<template>
  <div class="profile-page-bg">
    <div class="profile-container">
      <!-- ===== 顶部资料卡片 ===== -->
      <section class="profile-card profile-hero-card">
        <div class="hero-left">
          <el-avatar :size="104" :src="currentAvatar" class="hero-avatar">
            {{ (adminInfo.realName || adminInfo.username || 'A')[0] }}
          </el-avatar>
          <div class="hero-info">
            <h1 class="hero-name">{{ adminInfo.realName || adminInfo.username || '平台管理员' }}</h1>
            <div class="hero-tags">
              <span class="tag-pill brand-red">超级管理员</span>
              <span class="tag-pill subtle">正常</span>
            </div>
            <div class="hero-meta">
              <span><strong>{{ adminInfo.adminId || '-' }}</strong> · 编号</span>
            </div>
          </div>
        </div>

        <div class="hero-right">
          <div class="avatar-picker">
            <div class="avatar-picker-head">
              <div>
                <strong>选择头像</strong>
                <p>点击选择默认头像，或上传自己的图片</p>
              </div>
              <div class="avatar-actions">
                <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileUpload" />
                <button class="btn-outline-pill" @click="triggerUpload">上传头像</button>
                <button class="btn-primary-pill" :disabled="!selectedAvatar" :class="{ loading: savingAvatar }" @click="saveAvatar">
                  {{ savingAvatar ? '保存中...' : '保存头像' }}
                </button>
              </div>
            </div>
            <div class="avatar-grid">
              <button v-for="url in avatarCandidates" :key="url"
                class="avatar-option"
                :class="{ active: url === selectedAvatar }"
                type="button"
                @click="selectAvatar(url)"
              >
                <img :src="url" alt="头像" />
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== Tab：员工身份 ===== -->
      <section class="profile-card tab-card">
        <el-tabs v-model="activeTab" class="profile-tabs">
          <el-tab-pane label="员工身份" name="admin">
            <AdminManage />
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminManage from './AdminManage.vue'
import { uploadFile } from '../api/file'

const FILE_UPLOAD_BASE = 'http://localhost:8081'

const DEFAULT_AVATARS = Array.from(
  { length: 20 },
  (_, index) => `/brand-assets/avatars/default-avatar-${String(index + 1).padStart(2, '0')}.png`
)
const DEFAULT_ADMIN_AVATAR = DEFAULT_AVATARS[0]

function resolveAvatar(src, fallback = DEFAULT_ADMIN_AVATAR) {
  const raw = String(src || '').trim().replaceAll('\\', '/')
  if (!raw || raw === '[object Object]') return fallback
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  if (raw.startsWith('/brand-assets/')) return raw
  if (raw.startsWith('brand-assets/')) return `/${raw}`
  if (raw.startsWith('/uploads/')) return `${FILE_UPLOAD_BASE}${raw}`
  if (raw.startsWith('uploads/')) return `${FILE_UPLOAD_BASE}/${raw}`
  if (raw.startsWith('/')) return raw
  return `/${raw}`
}

const adminInfo = JSON.parse(localStorage.getItem('admin_info') || '{}')
const activeTab = ref('admin')
const avatarCandidates = DEFAULT_AVATARS
const selectedAvatar = ref('')
const savingAvatar = ref(false)
const fileInputRef = ref(null)

const currentAvatar = ref(resolveAvatar(adminInfo.avatar || ''))

function selectAvatar(url) {
  selectedAvatar.value = url
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileUpload(event) {
  const file = event.target?.files?.[0]
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return
  }
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await uploadFile(form)
    const url = res?.data?.url || ''
    if (!url) throw new Error('上传失败')
    selectedAvatar.value = url.startsWith('http') ? url : `${FILE_UPLOAD_BASE}${url}`
    ElMessage.success('头像已上传，请点击保存头像')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

async function saveAvatar() {
  if (savingAvatar.value || !selectedAvatar.value) return
  savingAvatar.value = true
  try {
    adminInfo.avatar = selectedAvatar.value
    localStorage.setItem('admin_info', JSON.stringify(adminInfo))
    currentAvatar.value = resolveAvatar(selectedAvatar.value)
    ElMessage.success('头像已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingAvatar.value = false
  }
}

onMounted(() => {
  selectedAvatar.value = adminInfo.avatar || DEFAULT_ADMIN_AVATAR
  currentAvatar.value = resolveAvatar(adminInfo.avatar || '')
})
</script>

<style scoped>
.profile-page-bg {
  background: var(--bg-section, #f7f7f7);
  min-height: calc(100vh - 72px);
  padding: 32px 0;
}

.profile-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== 通用卡片 ===== */
.profile-card {
  background: #fff;
  border: 1px solid #eeeeee;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}

/* ===== 顶部资料卡片 ===== */
.profile-hero-card {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 42px;
  padding: 34px;
}

.hero-left {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.hero-avatar {
  flex-shrink: 0;
  width: 104px;
  height: 104px;
  background: #E60012 !important;
  font-weight: 700;
  font-size: 22px;
  box-shadow: 0 0 0 5px #fff, 0 10px 24px rgba(0,0,0,0.08);
}

.hero-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 6px;
}

.hero-name {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #111;
  line-height: 1.25;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-pill {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.tag-pill.brand-red {
  color: #e60012;
  background: #fff0f1;
}

.tag-pill.subtle {
  color: #555;
  background: #f6f6f6;
}

.hero-meta {
  color: #999;
  font-size: 12px;
}

.hero-meta strong {
  color: #111;
}

/* ===== 右侧头像选择 ===== */
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
  flex-shrink: 0;
}

.btn-primary-pill,
.btn-outline-pill {
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
  height: 34px;
  padding: 0 18px;
  cursor: pointer;
  border: 1.5px solid;
  transition: all 0.2s;
  font-family: inherit;
  line-height: 1;
  white-space: nowrap;
}

.btn-primary-pill {
  color: #fff;
  background: #e60012;
  border-color: #e60012;
}
.btn-primary-pill:hover {
  background: #c4000f;
  border-color: #c4000f;
}
.btn-primary-pill:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-primary-pill.loading {
  opacity: 0.7;
}

.btn-outline-pill {
  color: #e60012;
  background: #fff;
  border-color: #ffd6d9;
}
.btn-outline-pill:hover {
  color: #c4000f;
  background: #fff7f7;
  border-color: #e60012;
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

/* ===== Tab 卡片 ===== */
.tab-card {
  overflow: hidden;
  padding: 0 30px 30px;
}

.profile-tabs {
  margin-top: 0;
}

.profile-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: #eee;
  height: 1px;
}

.profile-tabs :deep(.el-tabs__item) {
  height: 56px;
  padding: 0 18px;
  color: #555;
  font-size: 15px;
  font-weight: 800;
  transition: color 0.2s ease;
}

.profile-tabs :deep(.el-tabs__item:hover),
.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #e60012 !important;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background-color: #e60012 !important;
}

.profile-tabs :deep(.el-tabs__content) {
  padding-top: 20px;
}

/* ===== 响应式 ===== */
@media (max-width: 980px) {
  .profile-hero-card {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}

@media (max-width: 700px) {
  .profile-hero-card,
  .tab-card {
    padding: 22px;
  }
  .profile-hero-card {
    gap: 20px;
  }
  .hero-left {
    flex-direction: column;
  }
  .avatar-grid {
    grid-template-columns: repeat(5, 48px);
  }
  .avatar-picker-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
