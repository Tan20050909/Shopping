<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'

const route = useRoute()
const router = useRouter()
const logistics = ref(null)
const statusText = {
  0: '待发货',
  1: '待揽收',
  2: '运输中',
  3: '派送中',
  4: '已签收',
  5: '拒收'
}

function field(item, camel, snake, fallback = '') {
  return item?.[camel] ?? item?.[snake] ?? fallback
}

async function load() {
  try {
    logistics.value = await api(`/api/user/orders/${route.params.id}/logistics`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <main class="page stack">
    <section class="band stack">
      <div class="row section-head">
        <div>
          <h1 class="section-title">物流信息</h1>
          <p class="muted">订单 {{ route.params.id }}</p>
        </div>
        <el-button @click="router.push(`/orders/${route.params.id}`)">返回订单详情</el-button>
      </div>

      <template v-if="logistics">
        <div v-if="logistics.logistics_id" class="stack logistics-card">
          <div class="row logistics-summary">
            <div class="stack">
              <strong>{{ field(logistics, 'expressCompany', 'express_company', '待填写物流公司') }}</strong>
              <span class="muted">运单号：{{ field(logistics, 'expressNo', 'express_no', '未填写') }}</span>
            </div>
            <span class="status-chip">{{ statusText[field(logistics, 'logisticsStatus', 'logistics_status')] || `状态 ${field(logistics, 'logisticsStatus', 'logistics_status')}` }}</span>
          </div>
          <p v-if="logistics.traces?.length" class="muted">最新进度：{{ field(logistics.traces[0], 'traceContent', 'trace_content') }}</p>
        </div>
        <div v-else class="empty-box stack">
          <strong>当前还没有物流信息</strong>
          <p class="muted">大概率是订单还未发出，或者物流单号还没录入。</p>
          <div class="row">
            <el-button type="primary" @click="router.push(`/orders/${route.params.id}`)">返回订单详情</el-button>
            <el-button @click="router.push('/orders')">看其他订单</el-button>
          </div>
        </div>

        <div v-if="logistics.traces?.length" class="stack trace-list">
          <div v-for="(trace, index) in logistics.traces" :key="trace.trace_id" class="trace-item">
            <div class="trace-dot" :class="{ active: index === 0 }" />
            <div class="stack">
              <strong>{{ field(trace, 'traceContent', 'trace_content') }}</strong>
              <span class="muted">{{ field(trace, 'traceLocation', 'trace_location', '物流节点') }}</span>
              <span class="muted">{{ field(trace, 'traceTime', 'trace_time') }}</span>
            </div>
          </div>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.section-head {
  justify-content: space-between;
}

.logistics-card,
.empty-box {
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-soft);
}

.logistics-summary {
  justify-content: space-between;
  align-items: flex-start;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--brand-red-light);
  color: var(--brand-red);
  font-weight: 600;
}

.trace-list {
  gap: 0;
}

.trace-item {
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f3ebe3;
}

.trace-item:last-child {
  border-bottom: 0;
}

.trace-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  background: #d8d8d8;
}

.trace-dot.active {
  background: var(--brand-red);
  box-shadow: 0 0 0 4px rgba(255, 80, 0, 0.12);
}
</style>
