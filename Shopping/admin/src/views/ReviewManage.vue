<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-title">评论管理</h2>
        <p class="page-subtitle">管理商品评价与回复</p>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索评论内容" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="显示状态" clearable style="width:100px" @change="loadData">
          <el-option label="显示" :value="0" />
          <el-option label="隐藏" :value="1" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>
    <div class="content-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <template #empty><el-empty description="暂无评论数据" /></template>
        <el-table-column prop="reviewId" label="ID" width="70" />
        <el-table-column label="商品评分" width="100">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="服务评分" width="90">
          <template #default="{ row }">{{ row.serviceScore || '-' }}分</template>
        </el-table-column>
        <el-table-column label="物流评分" width="90">
          <template #default="{ row }">{{ row.logisticsScore || '-' }}分</template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <span v-if="row.images">有</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.isHidden===1?'info':'success'" size="small" effect="light" style="border-radius:999px">{{ row.isHidden===1?'隐藏':'显示' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="置顶" width="60">
          <template #default="{ row }"><el-tag v-if="row.isTop===1" type="warning" size="small" effect="light">置顶</el-tag><span v-else>-</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="170" />
        <el-table-column label="商家回复" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.merchantReply || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openReply(row)">回复</el-button>
            <el-button v-if="row.isHidden===0" type="warning" link size="small" @click="toggleHide(row,1)">隐藏</el-button>
            <el-button v-if="row.isHidden===1" type="success" link size="small" @click="toggleHide(row,0)">显示</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:24px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="current" v-model:page-size="size" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="评论详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评论ID">{{ detail.reviewId }}</el-descriptions-item>
        <el-descriptions-item label="商品ID">{{ detail.goodsId }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="商家ID">{{ detail.merchantId }}</el-descriptions-item>
        <el-descriptions-item label="商品评分"><el-rate v-model="detail.rating" disabled size="small" /></el-descriptions-item>
        <el-descriptions-item label="服务评分">{{ detail.serviceScore || '-' }}分</el-descriptions-item>
        <el-descriptions-item label="物流评分">{{ detail.logisticsScore || '-' }}分</el-descriptions-item>
        <el-descriptions-item label="是否匿名">{{ detail.isAnonymous === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="评论内容" :span="2">{{ detail.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评论图片" :span="2">
          <div v-if="detail.images" style="display:flex;gap:8px;flex-wrap:wrap">
            <el-image v-for="(img, i) in detail.images.split(',')" :key="i" :src="img.trim()" style="width:60px;height:60px;border-radius:4px;object-fit:cover" fit="cover" :preview-src-list="detail.images.split(',')" />
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="商家回复" :span="2">{{ detail.merchantReply || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回复时间">{{ detail.replyTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评论时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复评论" width="480px">
      <div style="margin-bottom:12px;padding:12px;background:var(--bg-soft);border-radius:8px">
        <div style="color:var(--text-muted);font-size:12px;margin-bottom:4px">原评论</div>
        <div>{{ replyTo.content }}</div>
      </div>
      <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="请输入回复内容" />
      <template #footer>
        <el-button @click="replyVisible=false">取消</el-button>
        <el-button type="primary" @click="submitReply" :loading="submitting">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewList, getReviewDetail, replyReview, updateReviewStatus, deleteReview as deleteReviewApi } from '../api/review'

const tableData = ref([])
const loading = ref(false)
const submitting = ref(false)
const current = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), statusFilter = ref(null)
const detailVisible = ref(false), detail = ref({})
const replyVisible = ref(false), replyContent = ref(''), replyTo = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value != null) params.isHidden = statusFilter.value
    const res = await getReviewList(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载评论数据失败，请重试')
  } finally { loading.value = false }
}

const showDetail = async (row) => {
  try {
    const res = await getReviewDetail(row.reviewId)
    detail.value = res.data || row
    detailVisible.value = true
  } catch (e) {
    detail.value = row
    detailVisible.value = true
  }
}

const openReply = (row) => { replyTo.value = row; replyContent.value = ''; replyVisible.value = true }
const submitReply = async () => {
  if (!replyContent.value.trim()) { ElMessage.warning('请输入回复'); return }
  submitting.value = true
  try {
    await replyReview(replyTo.value.reviewId, { merchantReply: replyContent.value })
    ElMessage.success('回复成功'); replyVisible.value = false; loadData()
  } catch (e) {
    ElMessage.error('回复失败，请重试')
  } finally { submitting.value = false }
}
const toggleHide = async (row, isHidden) => {
  try {
    await updateReviewStatus(row.reviewId, { isHidden })
    ElMessage.success(isHidden ? '已隐藏' : '已显示'); loadData()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？此操作不可撤销', '删除确认', { type: 'warning' })
    await deleteReviewApi(row.reviewId)
    ElMessage.success('已删除'); loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败，请重试')
  }
}

onMounted(loadData)
</script>
