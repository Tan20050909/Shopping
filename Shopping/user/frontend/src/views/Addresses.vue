<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/client'

const route = useRoute()
const router = useRouter()
const addresses = ref([])
const editingId = ref(null)
const selectingForOrder = computed(() => Boolean(route.query.orderId))
const orderId = computed(() => {
  const v = Number(route.query.orderId || 0)
  return Number.isFinite(v) && v > 0 ? v : null
})
const backTarget = computed(() => String(route.query.back || route.query.redirect || '/profile'))
const form = reactive({
  consignee: '张三',
  phone: '13800138001',
  province: '浙江省',
  provinceCode: '',
  city: '杭州市',
  cityCode: '',
  district: '西湖区',
  districtCode: '',
  detailAddr: '西湖大道 88 号',
  postalCode: '',
  remark: '',
  defaultAddress: true
})

async function load() {
  addresses.value = await api('/api/user/addresses')
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    consignee: '张三',
    phone: '13800138001',
    province: '浙江省',
    provinceCode: '',
    city: '杭州市',
    cityCode: '',
    district: '西湖区',
    districtCode: '',
    detailAddr: '西湖大道 88 号',
    postalCode: '',
    remark: '',
    defaultAddress: true
  })
}

function editAddress(address) {
  editingId.value = address.addr_id
  Object.assign(form, {
    consignee: address.consignee,
    phone: address.phone,
    province: address.province,
    provinceCode: address.province_code || '',
    city: address.city,
    cityCode: address.city_code || '',
    district: address.district,
    districtCode: address.district_code || '',
    detailAddr: address.detail_addr,
    postalCode: address.postal_code || '',
    remark: address.remark || '',
    defaultAddress: address.is_default === 1
  })
}

async function saveAddress() {
  try {
    if (editingId.value) {
      await api(`/api/user/addresses/${editingId.value}`, { method: 'PUT', body: form })
      ElMessage.success('地址已更新')
    } else {
      await api('/api/user/addresses', { method: 'POST', body: form })
      ElMessage.success('地址已新增')
    }
    resetForm()
    await load()
    if (route.query.redirect) {
      router.push(String(route.query.redirect))
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function setDefault(addrId) {
  try {
    await api(`/api/user/addresses/${addrId}/default`, { method: 'PUT' })
    ElMessage.success('默认地址已更新')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeAddress(addrId) {
  try {
    await api(`/api/user/addresses/${addrId}`, { method: 'DELETE' })
    ElMessage.success('地址已删除')
    if (editingId.value === addrId) resetForm()
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function selectAddressForOrder(addrId) {
  if (!orderId.value) return
  try {
    await api(`/api/user/orders/${orderId.value}/address?addrId=${addrId}`, { method: 'PUT' })
    ElMessage.success('收货地址已更新')
    router.push(backTarget.value || '/orders')
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
          <h1 class="section-title">收货地址</h1>
          <p class="muted">{{ selectingForOrder ? '选择要用于当前订单的收货地址。' : '先把地址准备好，确认订单时就能直接选择。' }}</p>
        </div>
        <div class="row">
          <el-button @click="router.push(backTarget)">返回</el-button>
        </div>
      </div>

      <div class="grid address-form-grid">
        <el-input v-model="form.consignee" placeholder="收货人" />
        <el-input v-model="form.phone" placeholder="手机号" />
        <el-input v-model="form.province" placeholder="省" />
        <el-input v-model="form.provinceCode" placeholder="省行政区划代码" />
        <el-input v-model="form.city" placeholder="市" />
        <el-input v-model="form.cityCode" placeholder="市行政区划代码" />
        <el-input v-model="form.district" placeholder="区" />
        <el-input v-model="form.districtCode" placeholder="区行政区划代码" />
        <el-input v-model="form.detailAddr" placeholder="详细地址" />
        <el-input v-model="form.postalCode" placeholder="邮编" />
        <el-input v-model="form.remark" placeholder="备注" />
      </div>
      <div class="row">
        <el-checkbox v-model="form.defaultAddress">设为默认地址</el-checkbox>
        <el-button type="primary" @click="saveAddress">{{ editingId ? '更新地址' : '新增地址' }}</el-button>
        <el-button v-if="editingId" @click="resetForm">取消编辑</el-button>
      </div>
    </section>

    <section class="band stack">
      <h2 class="section-title">我的地址</h2>
      <div v-if="!addresses.length" class="muted">还没有地址，先新增一条。</div>
      <div v-for="addr in addresses" :key="addr.addr_id" class="address-item">
        <div class="stack">
          <div class="row">
            <strong>{{ addr.consignee }}</strong>
            <span class="muted">{{ addr.phone }}</span>
            <span v-if="addr.is_default === 1" class="tag">默认地址</span>
          </div>
          <p class="muted">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail_addr }}</p>
        </div>
        <div class="row">
          <el-button v-if="selectingForOrder" size="small" type="primary" @click="selectAddressForOrder(addr.addr_id)">选择此地址</el-button>
          <el-button size="small" @click="editAddress(addr)">编辑</el-button>
          <el-button size="small" @click="setDefault(addr.addr_id)">设为默认</el-button>
          <el-button size="small" type="danger" plain @click="removeAddress(addr.addr_id)">删除</el-button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.section-head {
  justify-content: space-between;
}

.address-form-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.address-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #e5ece8;
}

.address-item:last-child {
  border-bottom: 0;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 8px;
  background: #edf7ef;
  color: #2c7a4b;
  font-size: 12px;
}
</style>
