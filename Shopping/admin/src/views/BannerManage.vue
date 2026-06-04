<template>
  <div class="admin-order-page">
    <section class="admin-page-hero">
      <div class="admin-page-container">
        <div class="admin-page-hero-inner">
          <span class="admin-page-kicker">BANNER MANAGEMENT</span>
          <h1 class="admin-page-title">轮播图管理</h1>
          <p class="admin-page-desc">管理各页面轮播图，支持定时上下架、排序和跳转配置</p>
        </div>
      </div>
    </section>
    <div class="admin-page-container">
      <div class="admin-panel">
        <!-- 头部操作区 -->
        <div class="admin-filter-bar" style="margin-bottom:16px">
          <el-button @click="openPreview">
            <el-icon style="margin-right:4px"><View /></el-icon>预览效果
          </el-button>
          <el-button type="primary" @click="openForm()">
            <el-icon style="margin-right:4px"><Plus /></el-icon>新增轮播图
          </el-button>
          <div style="flex:1" />
          <el-select v-model="filterPosition" @change="loadData" class="admin-status-select" placeholder="展示位置">
            <el-option label="全部位置" :value="null" />
            <el-option label="首页顶部" :value="1" />
            <el-option label="首页中部" :value="2" />
            <el-option label="活动页" :value="3" />
            <el-option label="分类页" :value="4" />
          </el-select>
          <el-select v-model="filterStatus" @change="loadData" class="admin-status-select" placeholder="状态">
            <el-option label="全部" :value="null" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
            <el-option label="待上架" :value="2" />
          </el-select>
        </div>

        <!-- 批量操作栏 -->
        <div v-if="selectedIds.length" style="display:flex;align-items:center;gap:12px;padding:10px 16px;margin-bottom:14px;background:var(--brand-red-light);border-radius:var(--radius-md);border:1px solid #ffccc7">
          <span style="font-size:13px;color:var(--brand-red);font-weight:600">已选择 {{ selectedIds.length }} 项</span>
          <button class="admin-action-btn" style="color:#047857;border-color:#a7f3d0" @click="handleBatchStatus(1)">批量启用</button>
          <button class="admin-action-btn admin-action-warning" @click="handleBatchStatus(0)">批量禁用</button>
          <button class="admin-action-btn admin-action-danger" @click="handleBatchDelete">批量删除</button>
          <button class="admin-action-btn" @click="selectedIds = []">取消选择</button>
        </div>

        <div class="admin-table-wrap">
          <el-table :data="tableData" stripe v-loading="loading" class="admin-table" style="width:100%" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="bannerId" label="ID" width="55" />
            <el-table-column label="轮播图片" width="170">
              <template #default="{ row }">
                <el-image
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  style="width:150px;height:60px;border-radius:6px"
                  fit="cover"
                  :preview-src-list="[row.imageUrl]"
                />
                <span v-else style="color:var(--text-muted)">暂无</span>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="140">
              <template #default="{ row }">
                <span style="font-weight:600">{{ row.bannerTitle || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="展示位置" width="95">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="positionClass(row.displayPosition)">{{ positionMap[row.displayPosition] || '未知' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="跳转" width="115">
              <template #default="{ row }">
                <template v-if="row.jumpType && row.jumpType !== 5">
                  <span class="admin-status-tag tag-info" style="padding:1px 6px;font-size:11px">{{ jumpTypeMap[row.jumpType] }}</span>
                  <span style="font-size:12px;color:var(--text-secondary);max-width:50px;display:inline-block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;vertical-align:middle">{{ row.jumpValue }}</span>
                </template>
                <span v-else style="color:var(--text-muted);font-size:12px">无跳转</span>
              </template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" width="60" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <span class="admin-status-tag" :class="statusClass(row.status)">{{ statusMap[row.status] || '未知' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="有效期" width="155">
              <template #default="{ row }">
                <div v-if="row.startTime || row.endTime" style="font-size:12px;color:var(--text-secondary);line-height:1.5">
                  <div v-if="row.startTime">{{ formatTime(row.startTime) }}</div>
                  <div v-if="row.endTime">至 {{ formatTime(row.endTime) }}</div>
                </div>
                <span v-else style="color:var(--text-muted);font-size:12px">永久有效</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="admin-table-actions">
                  <button class="admin-action-btn" @click="openForm(row)">编辑</button>
                  <button v-if="row.status===1" class="admin-action-btn admin-action-warning" @click="handleStatus(row, 0)">禁用</button>
                  <button v-else class="admin-action-btn" style="color:#047857;border-color:#a7f3d0" @click="handleStatus(row, 1)">启用</button>
                  <button class="admin-action-btn admin-action-danger" @click="handleDelete(row)">删除</button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:18px">
          <span style="font-size:13px;color:var(--text-muted)">共 {{ total }} 条</span>
          <el-pagination
            v-model:current-page="current"
            v-model:page-size="size"
            :page-sizes="[10,20,50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="form.bannerId ? '编辑轮播图' : '新增轮播图'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="90px" ref="formRef">
        <el-form-item label="标题">
          <el-input v-model="form.bannerTitle" placeholder="轮播图标题，如：618年中大促" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="轮播图片" required>
          <div style="width:100%">
            <div v-if="form.imageUrl" style="margin-bottom:8px;position:relative">
              <el-image :src="form.imageUrl" style="width:100%;max-height:180px;border-radius:8px" fit="cover" />
            </div>
            <div style="display:flex;gap:8px;align-items:center">
              <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="handleUpload" accept="image/*">
                <el-button size="small" :loading="uploading"><el-icon style="margin-right:4px"><Upload /></el-icon>{{ uploading ? '上传中' : '上传图片' }}</el-button>
              </el-upload>
              <span style="color:var(--text-muted);font-size:12px">或</span>
              <el-input v-model="form.imageUrl" placeholder="输入图片URL" style="flex:1" size="small" />
            </div>
            <div style="font-size:12px;color:var(--text-muted);margin-top:4px">建议尺寸：首页顶部 1200×400，首页中部/分类页 1200×300</div>
          </div>
        </el-form-item>
        <el-form-item label="展示位置" required>
          <el-radio-group v-model="form.displayPosition">
            <el-radio-button :value="1">首页顶部</el-radio-button>
            <el-radio-button :value="2">首页中部</el-radio-button>
            <el-radio-button :value="3">活动页</el-radio-button>
            <el-radio-button :value="4">分类页</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.jumpType" style="width:100%" clearable placeholder="选择跳转类型">
            <el-option label="商品详情" :value="1" />
            <el-option label="分类页面" :value="2" />
            <el-option label="外部链接" :value="3" />
            <el-option label="活动页面" :value="4" />
            <el-option label="无跳转" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.jumpType && form.jumpType !== 5" label="跳转目标">
          <el-input v-model="form.jumpValue" :placeholder="jumpPlaceholder" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" :max="999" />
          <span style="margin-left:8px;font-size:12px;color:var(--text-muted)">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">禁用</el-radio-button>
            <el-radio-button :value="2">待上架</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
            :shortcuts="dateShortcuts"
            :default-time="[new Date(0,0,0,0,0,0), new Date(0,0,0,23,59,59)]"
            unlink-panels
            popper-class="range-date-popper"
          />
          <div style="font-size:12px;color:var(--text-muted);margin-top:4px">不设置则永久有效；配合"待上架"状态可实现定时自动上架</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>

    <!-- 轮播预览弹窗 -->
    <el-dialog v-model="previewVisible" title="轮播效果预览" width="740px" destroy-on-close>
      <div style="margin-bottom:16px">
        <el-radio-group v-model="previewPosition" @change="loadPreviewBanners">
          <el-radio-button :value="1">首页顶部</el-radio-button>
          <el-radio-button :value="2">首页中部</el-radio-button>
          <el-radio-button :value="3">活动页</el-radio-button>
          <el-radio-button :value="4">分类页</el-radio-button>
        </el-radio-group>
      </div>
      <div class="carousel-preview" :style="{ height: previewPosition === 1 || previewPosition === 3 ? '260px' : '200px' }">
        <el-carousel v-if="previewBanners.length" :height="previewPosition === 1 || previewPosition === 3 ? '260px' : '200px'" :interval="3000" arrow="always" indicator-position="outside">
          <el-carousel-item v-for="item in previewBanners" :key="item.bannerId">
            <div class="carousel-slide" :style="{ backgroundImage: item.imageUrl ? `url(${item.imageUrl})` : 'none' }">
              <div v-if="!item.imageUrl" class="carousel-placeholder">
                <el-icon :size="40" color="#ccc"><Picture /></el-icon>
                <span>暂无图片</span>
              </div>
              <div class="carousel-caption" v-if="item.bannerTitle">{{ item.bannerTitle }}</div>
            </div>
          </el-carousel-item>
        </el-carousel>
        <el-empty v-else description="该位置暂无启用的轮播图" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBannerList, getActiveBanners, addBanner, updateBanner, updateBannerStatus, batchUpdateBannerStatus, deleteBanner, batchDeleteBanner } from '../api/common'
import { uploadFile } from '../api/file'

const positionMap = { 1: '首页顶部', 2: '首页中部', 3: '活动页', 4: '分类页' }
const jumpTypeMap = { 1: '商品', 2: '分类', 3: 'URL', 4: '活动', 5: '无' }
const statusMap = { 0: '禁用', 1: '启用', 2: '待上架' }

const positionClass = (p) => ({ 1: 'tag-danger', 2: 'tag-warning', 3: 'tag-success', 4: 'tag-info' }[p] || 'tag-info')
const statusClass = (s) => ({ 0: 'tag-info', 1: 'tag-success', 2: 'tag-warning' }[s] || 'tag-info')

const tableData = ref([])
const loading = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const filterStatus = ref(null)
const filterPosition = ref(null)
const selectedIds = ref([])

const formVisible = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref(null)
const dateRange = ref(null)
const form = reactive({
  bannerId: null, bannerTitle: '', imageUrl: '',
  displayPosition: 1, jumpType: 5, jumpValue: '',
  sortNo: 0, status: 1, startTime: null, endTime: null
})

const previewVisible = ref(false)
const previewPosition = ref(1)
const previewBanners = ref([])

const jumpPlaceholder = computed(() => {
  const map = { 1: '输入商品ID，如：101', 2: '输入分类ID，如：3', 3: '输入完整URL，如：https://...', 4: '输入活动页面路径' }
  return map[form.jumpType] || ''
})

const dateShortcuts = [
  { text: '未来7天', value: () => { const e = new Date(); const s = new Date(); s.setTime(s.getTime()); e.setTime(e.getTime() + 7*24*3600*1000); return [s, e] }},
  { text: '未来30天', value: () => { const e = new Date(); const s = new Date(); s.setTime(s.getTime()); e.setTime(e.getTime() + 30*24*3600*1000); return [s, e] }},
  { text: '未来90天', value: () => { const e = new Date(); const s = new Date(); s.setTime(s.getTime()); e.setTime(e.getTime() + 90*24*3600*1000); return [s, e] }},
  { text: '未来半年', value: () => { const e = new Date(); const s = new Date(); s.setTime(s.getTime()); e.setTime(e.getTime() + 180*24*3600*1000); return [s, e] }},
]

const formatTime = (t) => t ? t.replace('T', ' ').slice(0, 16) : ''

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (filterStatus.value !== null) params.status = filterStatus.value
    if (filterPosition.value !== null) params.displayPosition = filterPosition.value
    const res = await getBannerList(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载轮播图数据失败')
  } finally { loading.value = false }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.bannerId)
}

const openForm = (row) => {
  if (row) {
    Object.assign(form, {
      bannerId: row.bannerId, bannerTitle: row.bannerTitle || '',
      imageUrl: row.imageUrl || '', displayPosition: row.displayPosition || 1,
      jumpType: row.jumpType ?? 5, jumpValue: row.jumpValue || '',
      sortNo: row.sortNo || 0, status: row.status ?? 1,
      startTime: row.startTime || null, endTime: row.endTime || null
    })
    dateRange.value = row.startTime && row.endTime ? [row.startTime, row.endTime] : null
  } else {
    Object.assign(form, {
      bannerId: null, bannerTitle: '', imageUrl: '',
      displayPosition: filterPosition.value || 1, jumpType: 5, jumpValue: '',
      sortNo: 0, status: 1, startTime: null, endTime: null
    })
    dateRange.value = null
  }
  formVisible.value = true
}

const beforeUpload = (file) => {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片'); return false }
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('图片不能超过5MB'); return false }
  return true
}

const handleUpload = async ({ file }) => {
  uploading.value = true
  try {
    const fd = new FormData(); fd.append('file', file)
    const res = await uploadFile(fd)
    form.imageUrl = res.data.url
    ElMessage.success('上传成功')
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}

const submitForm = async () => {
  if (!form.imageUrl) { ElMessage.warning('请上传或输入图片地址'); return }
  submitting.value = true
  try {
    if (dateRange.value?.length === 2) {
      form.startTime = dateRange.value[0]
      form.endTime = dateRange.value[1]
    } else {
      form.startTime = null; form.endTime = null
    }
    if (form.jumpType === 5) form.jumpValue = ''
    if (form.bannerId) await updateBanner({ ...form })
    else await addBanner({ ...form })
    ElMessage.success(form.bannerId ? '修改成功' : '新增成功')
    formVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally { submitting.value = false }
}

const handleStatus = async (row, status) => {
  try {
    await updateBannerStatus(row.bannerId, status)
    ElMessage.success(status === 1 ? '已启用' : '已禁用')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该轮播图？', '删除确认', { type: 'warning' })
    await deleteBanner(row.bannerId)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败，请重试')
  }
}

const handleBatchStatus = async (status) => {
  const label = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${label}选中的 ${selectedIds.value.length} 项？`, '批量操作', { type: 'warning' })
    await batchUpdateBannerStatus(selectedIds.value, status)
    ElMessage.success(`已${label} ${selectedIds.value.length} 项`)
    selectedIds.value = []
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量操作失败，请重试')
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 项？`, '批量删除', { type: 'error' })
    await batchDeleteBanner(selectedIds.value)
    ElMessage.success(`已删除 ${selectedIds.value.length} 项`)
    selectedIds.value = []
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量删除失败，请重试')
  }
}

const openPreview = () => {
  previewPosition.value = filterPosition.value || 1
  previewVisible.value = true
  loadPreviewBanners()
}

const loadPreviewBanners = async () => {
  try {
    const res = await getActiveBanners({ displayPosition: previewPosition.value })
    previewBanners.value = res.data || []
  } catch { previewBanners.value = [] }
}

onMounted(loadData)
</script>

<style scoped>
.admin-order-page { color: var(--text-main); }
.carousel-preview {
  border-radius: 12px;
  overflow: hidden;
  background: #f0f0f0;
}
.carousel-slide {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e8e8e8;
}
.carousel-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 14px;
}
.carousel-caption {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  background: linear-gradient(transparent, rgba(0,0,0,0.55));
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}
</style>
<style>
.range-date-popper { z-index: 3000 !important; }
</style>
