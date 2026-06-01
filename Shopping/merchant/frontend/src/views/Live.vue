<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <template #header>
        <div class="card-header">
          <span>直播管理</span>
          <el-button type="primary" @click="showCreateDialog">创建直播</el-button>
        </div>
      </template>
      <el-table :data="list" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="直播标题" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开播时间" width="180" />
        <el-table-column prop="watchNum" label="观看人数" width="120" />
        <el-table-column prop="interactNum" label="互动数" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              :disabled="isBeforeStart(row)"
              @click="updateStatus(row, 1)"
            >
              {{ isBeforeStart(row) ? '未到时间' : '开播' }}
            </el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" @click="updateStatus(row, 2)">暂停</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" type="danger" size="small" @click="updateStatus(row, 3)">结束</el-button>
            <el-button size="small" @click="showGoodsDialog(row)">商品管理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="创建直播" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="直播标题">
          <el-input v-model="form.title" placeholder="请输入直播标题" />
        </el-form-item>
        <el-form-item label="开播时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="不填则立即开播"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="form.coverUrl" placeholder="请输入封面图片URL" />
        </el-form-item>
        <el-form-item label="直播URL">
          <el-input v-model="form.liveUrl" placeholder="可选，填写真实直播链接（http:// 或 https://）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createLive">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="goodsDialogVisible" title="直播间商品管理" width="800px">
      <div style="margin-bottom: 10px">
        <el-button type="primary" size="small" @click="showAddGoodsDialog">添加商品</el-button>
      </div>
      <el-table :data="goodsList" style="width: 100%">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div style="display:flex; align-items:center; gap:10px;">
              <img :src="resolveImg(row.goodsPic)" alt="" style="width:42px;height:42px;border-radius:10px;object-fit:cover;background:#f5f5f5;" />
              <div style="display:flex; flex-direction:column; gap:4px;">
                <div style="font-weight:700; color:#111827;">{{ row.goodsName || '-' }}</div>
                <div style="font-size:12px;color:#6b7280;">{{ row.skuSpec || '默认' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originPrice" label="原价" width="120">
          <template #default="{ row }">¥ {{ row.originPrice ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="livePrice" label="直播价格" width="120">
          <template #default="{ row }">¥ {{ row.livePrice }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="removeGoods(row.id)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="addGoodsDialogVisible" title="添加商品" width="500px">
      <el-form :model="goodsForm" label-width="100px">
        <el-form-item label="商品名称">
          <el-select
            v-model="goodsForm.goodsId"
            filterable
            placeholder="输入名称搜索并选择商品"
            style="width: 100%"
            @change="onGoodsChange"
          >
            <el-option v-for="g in merchantGoods" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格/SKU">
          <el-select v-model="goodsForm.skuId" placeholder="选择规格" style="width: 100%">
            <el-option
              v-for="s in skuOptions"
              :key="s.id"
              :label="`${s.spec || '默认'}（原价 ¥${Number(s.price ?? 0).toFixed(2)}）`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原价">
          <el-input :model-value="originPriceText" disabled />
        </el-form-item>
        <el-form-item label="直播价格">
          <el-input-number v-model="goodsForm.livePrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="goodsForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addGoodsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addGoods">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { goodsApi, goodsSkuApi, liveApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const list = ref([])
const goodsList = ref([])
const createDialogVisible = ref(false)
const goodsDialogVisible = ref(false)
const addGoodsDialogVisible = ref(false)
const currentLive = ref(null)
const merchantGoods = ref([])
const skuOptions = ref([])
const defaultImage = 'https://via.placeholder.com/80x80?text=Goods'

const form = reactive({
  merchantId: getMerchantId(),
  title: '',
  startTime: '',
  coverUrl: '',
  liveUrl: ''
})

const goodsForm = reactive({
  liveId: null,
  goodsId: null,
  skuId: null,
  livePrice: 0,
  sort: 0
})

const resolveImg = (src) => {
  const v = String(src || '').trim()
  if (!v) return defaultImage
  if (v.startsWith('http://') || v.startsWith('https://')) return v
  if (v.startsWith('/uploads/')) return v
  if (v.startsWith('uploads/')) return `/${v}`
  return defaultImage
}

const originPriceText = computed(() => {
  const skuId = goodsForm.skuId
  const found = skuOptions.value.find(s => Number(s.id) === Number(skuId))
  const price = found?.price
  if (price == null) return '-'
  const num = Number(price ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : String(price)
})

const loadList = async () => {
  try {
    const res = await liveApi.list(getMerchantId())
    list.value = res.data || []
  } catch (error) {
    list.value = []
  }
}

const parseDate = (value) => {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const s = String(value).trim()
  if (!s) return null
  const normalized = s.includes(' ') ? s.replace(' ', 'T') : s
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

const isBeforeStart = (row) => {
  const status = Number(row?.status)
  if (status !== 0) return false
  const start = parseDate(row?.startTime)
  if (!start) return false
  return start.getTime() > Date.now()
}

const showCreateDialog = () => {
  form.title = ''
  form.startTime = ''
  form.coverUrl = ''
  form.liveUrl = ''
  form.merchantId = getMerchantId()
  createDialogVisible.value = true
}

const createLive = async () => {
  try {
    form.merchantId = getMerchantId()
    if (!String(form.title || '').trim()) {
      ElMessage.warning('请填写直播标题')
      return
    }
    if (String(form.liveUrl || '').trim() && !(String(form.liveUrl || '').trim().startsWith('http://') || String(form.liveUrl || '').trim().startsWith('https://'))) {
      ElMessage.warning('直播URL必须以 http:// 或 https:// 开头')
      return
    }
    await liveApi.create(form)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadList()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '创建失败')
  }
}

const updateStatus = async (row, status) => {
  try {
    await liveApi.updateStatus(row.id, status)
    ElMessage.success('操作成功')
    loadList()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '操作失败')
  }
}

const showGoodsDialog = async (row) => {
  try {
    currentLive.value = row
    const res = await liveApi.listGoods(row.id)
    goodsList.value = res.data || []
    goodsDialogVisible.value = true
  } catch (error) {
    goodsList.value = []
    ElMessage.error(error?.response?.data?.message || '加载失败')
  }
}

const showAddGoodsDialog = () => {
  goodsForm.liveId = currentLive.value.id
  goodsForm.goodsId = null
  goodsForm.skuId = null
  goodsForm.livePrice = 0
  goodsForm.sort = 0
  skuOptions.value = []
  loadMerchantGoods()
  addGoodsDialogVisible.value = true
}

const loadMerchantGoods = async () => {
  try {
    const res = await goodsApi.list(getMerchantId())
    merchantGoods.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    merchantGoods.value = []
  }
}

const onGoodsChange = async (goodsId) => {
  skuOptions.value = []
  goodsForm.skuId = null
  if (!goodsId) return
  try {
    const res = await goodsSkuApi.list(goodsId)
    const skus = Array.isArray(res.data) ? res.data : []
    skuOptions.value = skus.map(s => ({
      id: s.id,
      spec: s.spec,
      price: s.price
    }))
    if (skuOptions.value.length) {
      goodsForm.skuId = skuOptions.value[0].id
    }
  } catch (e) {
    skuOptions.value = []
  }
}

const addGoods = async () => {
  try {
    if (!goodsForm.goodsId) {
      ElMessage.warning('请选择商品名称')
      return
    }
    if (!goodsForm.skuId) {
      ElMessage.warning('请选择规格/SKU')
      return
    }
    await liveApi.addGoods(goodsForm)
    ElMessage.success('添加成功')
    addGoodsDialogVisible.value = false
    showGoodsDialog(currentLive.value)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '添加失败')
  }
}

const removeGoods = async (id) => {
  try {
    await liveApi.removeGoods(id)
    ElMessage.success('移除成功')
    showGoodsDialog(currentLive.value)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '移除失败')
  }
}

const getStatusType = (status) => {
  const types = ['info', 'success', 'warning', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['未开播', '直播中', '暂停', '已结束']
  return texts[status] || '未知'
}

onMounted(loadList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
