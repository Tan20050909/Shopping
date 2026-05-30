<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, getDebugUserId, setDebugUserId } from '../api/client'

const currentUserId = computed(() => getDebugUserId())
const afterSales = ref([])
const loading = ref(false)
const handlingId = ref(null)

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

function switchUser(userId) {
  setDebugUserId(userId)
  window.location.reload()
}

async function loadAfterSales() {
  loading.value = true
  try {
    afterSales.value = await api('/api/user/after-sales')
  } finally {
    loading.value = false
  }
}

async function handleAfterSale(afterSaleId, action) {
  handlingId.value = `${afterSaleId}-${action}`
  const actionText = {
    approve: '同意售后',
    reject: '驳回售后',
    'refund-success': '退款成功'
  }[action]
  try {
    await api(`/api/dev/after-sales/${afterSaleId}/${action}`, { method: 'POST' })
    ElMessage.success(`${actionText}成功`)
    await loadAfterSales()
  } catch (error) {
    ElMessage.error(error.message || `${actionText}失败`)
  } finally {
    handlingId.value = null
  }
}

function statusText(status) {
  return {
    0: '待审核',
    1: '商家同意',
    2: '商家拒绝',
    3: '平台介入',
    4: '处理完成',
    5: '已撤销'
  }[Number(status)] || `状态 ${status}`
}

onMounted(loadAfterSales)
</script>

<template>
  <main class="page stack">
    <section class="band stack">
      <div>
        <h1 class="section-title">开发入口</h1>
        <p class="muted">当前阶段只做用户端商品浏览闭环，统一通过 X-User-Id 模拟用户。</p>
      </div>
      <div class="row">
        <strong>当前模拟用户：</strong>
        <span>{{ currentUserId }}</span>
        <el-button size="small" @click="switchUser(1001)">切换到 1001</el-button>
        <el-button size="small" @click="switchUser(1002)">切换到 1002</el-button>
      </div>
      <div class="row">
        <router-link class="dev-link" to="/products">进入商品列表</router-link>
        <router-link class="dev-link" to="/cart">购物车</router-link>
        <router-link class="dev-link" to="/profile/addresses">地址管理</router-link>
        <router-link class="dev-link" to="/orders">我的订单</router-link>
        <router-link class="dev-link" to="/profile">个人中心</router-link>
        <router-link class="dev-link" to="/after-sales">我的售后</router-link>
        <router-link class="dev-link" to="/profile/favorites">我的收藏</router-link>
        <router-link class="dev-link" to="/profile/history">浏览记录</router-link>
        <router-link class="dev-link" to="/recommend">推荐</router-link>
        <router-link class="dev-link" to="/rankings">榜单</router-link>
        <router-link class="dev-link" to="/live">直播</router-link>
      </div>
      <p class="muted">售后申请请从“我的订单 -> 订单详情 -> 申请售后”进入，这样会自动带上真实订单和订单商品。</p>
    </section>

    <section class="band stack">
      <div class="row section-head">
        <div>
          <h2 class="section-title">售后调试</h2>
          <p class="muted">用于本地联调售后审核、退款成功和返券流程，只操作当前模拟用户的售后单。</p>
        </div>
        <el-button :loading="loading" @click="loadAfterSales">刷新</el-button>
      </div>

      <div v-loading="loading" class="stack">
        <article
          v-for="item in afterSales"
          :key="field(item, 'afterSaleId', 'after_sale_id')"
          class="after-sale-dev-card"
        >
          <div class="stack">
            <strong>{{ field(item, 'afterSaleNo', 'after_sale_no') }}</strong>
            <span class="muted">
              商品：{{ field(item, 'goodsName', 'goods_name', '未知商品') }}
              · 订单 {{ field(item, 'orderNo', 'order_no') }}
            </span>
            <span class="muted">
              金额 ¥{{ field(item, 'applyAmount', 'apply_amount', 0) }}
              · 原因：{{ field(item, 'applyReason', 'apply_reason', '未填写') }}
            </span>
          </div>
          <div class="row dev-actions">
            <span class="status-pill">{{ statusText(field(item, 'handleStatus', 'handle_status')) }}</span>
            <router-link
              class="dev-link compact"
              :to="`/after-sales/${field(item, 'afterSaleId', 'after_sale_id')}`"
            >
              详情
            </router-link>
            <el-button
              size="small"
              type="primary"
              :disabled="Number(field(item, 'handleStatus', 'handle_status')) !== 0"
              :loading="handlingId === `${field(item, 'afterSaleId', 'after_sale_id')}-approve`"
              @click="handleAfterSale(field(item, 'afterSaleId', 'after_sale_id'), 'approve')"
            >
              同意
            </el-button>
            <el-button
              size="small"
              :disabled="Number(field(item, 'handleStatus', 'handle_status')) !== 0"
              :loading="handlingId === `${field(item, 'afterSaleId', 'after_sale_id')}-reject`"
              @click="handleAfterSale(field(item, 'afterSaleId', 'after_sale_id'), 'reject')"
            >
              驳回
            </el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="Number(field(item, 'handleStatus', 'handle_status')) !== 1"
              :loading="handlingId === `${field(item, 'afterSaleId', 'after_sale_id')}-refund-success`"
              @click="handleAfterSale(field(item, 'afterSaleId', 'after_sale_id'), 'refund-success')"
            >
              退款成功
            </el-button>
          </div>
        </article>

        <el-empty v-if="!loading && !afterSales.length" description="当前用户暂无售后单" />
      </div>
    </section>
  </main>
</template>

<style scoped>
.dev-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 160px;
  padding: 10px 14px;
  border: 1px solid #dfe7e2;
  border-radius: 8px;
  background: #ffffff;
}

.section-head {
  justify-content: space-between;
  align-items: flex-start;
}

.after-sale-dev-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 20px;
  align-items: center;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: #ffffff;
}

.dev-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.dev-link.compact {
  min-width: auto;
  padding: 6px 10px;
  font-size: 13px;
}

.status-pill {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 700;
  font-size: 13px;
}

@media (max-width: 720px) {
  .after-sale-dev-card {
    grid-template-columns: 1fr;
  }

  .dev-actions {
    justify-content: flex-start;
  }
}
</style>
