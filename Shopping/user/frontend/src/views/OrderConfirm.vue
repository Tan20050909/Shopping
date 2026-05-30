<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, imageOf } from '../api/client'

const route = useRoute()
const router = useRouter()
const cart = ref([])
const addresses = ref([])
const addrId = ref()
const userCouponId = ref()
const preview = ref(null)
const loading = ref(false)
const submitting = ref(false)

const directOrderItems = computed(() => {
  const skuId = Number(route.query.skuId || 0)
  const num = Number(route.query.num || 1)
  if (!skuId) return []
  return [{ skuId, num }]
})
const fromCart = computed(() => directOrderItems.value.length === 0)
const selectedItems = computed(() => fromCart.value ? cart.value.filter((item) => Number(item.is_selected) === 1) : (preview.value?.items || []))
const canSubmitItems = computed(() => fromCart.value ? selectedItems.value.length > 0 : directOrderItems.value.length > 0)
const currentAddress = computed(() => addresses.value.find((item) => item.addr_id === addrId.value))
const previewGroups = computed(() => preview.value?.merchantGroups || [])
const availableOwnedCoupons = computed(() => preview.value?.availableCoupons || [])

async function load() {
  loading.value = true
  try {
    cart.value = fromCart.value ? await api('/api/user/cart') : []
    addresses.value = await api('/api/user/addresses')
    addrId.value = addresses.value.find((item) => item.is_default === 1)?.addr_id || addresses.value[0]?.addr_id
    await loadPreview()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadPreview() {
  if (!addrId.value || !canSubmitItems.value) {
    preview.value = null
    return
  }
  try {
    preview.value = await api('/api/user/orders/preview', {
      method: 'POST',
      body: {
        addrId: addrId.value,
        userCouponId: userCouponId.value || null,
        cartIds: fromCart.value ? selectedItems.value.map((item) => item.cart_id) : [],
        fromCart: fromCart.value,
        items: directOrderItems.value
      }
    })
  } catch (error) {
    preview.value = null
    ElMessage.error(error.message)
  }
}

async function createOrder() {
  if (!addrId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  if (!canSubmitItems.value) {
    ElMessage.warning(fromCart.value ? '请先回购物车勾选商品' : '请先从商品详情发起立即购买')
    return
  }
  if (!preview.value) {
    ElMessage.warning('请等待订单预览加载完成')
    return
  }
  submitting.value = true
  try {
    const result = await api('/api/user/orders', {
      method: 'POST',
      body: {
        addrId: addrId.value,
        userCouponId: userCouponId.value || null,
        cartIds: fromCart.value ? selectedItems.value.map((item) => item.cart_id) : [],
        fromCart: fromCart.value,
        items: directOrderItems.value
      }
    })
    ElMessage.success('订单已创建，等待支付')
    router.push(`/orders/${result.firstOrderId || result.orderId}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

watch(() => route.fullPath, () => {
  userCouponId.value = undefined
  load()
})

onMounted(load)
</script>

<template>
  <main class="page stack">
    <section class="band stack">
      <div class="row section-head">
        <div>
          <h1 class="section-title">确认订单</h1>
          <p class="muted">这里先预览金额，真正创建订单时后端会重新校验库存和价格。</p>
        </div>
        <router-link class="text-link" :to="`/profile/addresses?redirect=${encodeURIComponent('/order/confirm' + route.fullPath.replace(route.path, ''))}`">去管理地址</router-link>
      </div>

      <el-select v-model="addrId" placeholder="选择收货地址" @change="loadPreview">
        <el-option
          v-for="addr in addresses"
          :key="addr.addr_id"
          :value="addr.addr_id"
          :label="`${addr.consignee} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail_addr}`"
        />
      </el-select>

      <div v-if="currentAddress" class="address-preview">
        <strong>{{ currentAddress.consignee }} {{ currentAddress.phone }}</strong>
        <span class="muted">{{ currentAddress.province }}{{ currentAddress.city }}{{ currentAddress.district }}{{ currentAddress.detail_addr }}</span>
      </div>

      <div class="row coupon-row">
        <span class="muted">优惠券</span>
        <el-select v-model="userCouponId" clearable placeholder="选择优惠券" @change="loadPreview">
          <el-option
            v-for="coupon in availableOwnedCoupons"
            :key="coupon.userCouponId || coupon.user_coupon_id"
            :value="coupon.userCouponId || coupon.user_coupon_id"
            :label="`${coupon.couponName || coupon.coupon_name} 满${coupon.minAmount || coupon.min_amount}减${coupon.denomination}`"
          />
        </el-select>
      </div>
      <div v-if="!availableOwnedCoupons.length" class="row coupon-tip">
        <span class="muted">当前没有可用优惠券，可以先去领券中心看看。</span>
        <router-link class="text-link" to="/coupons">去领券</router-link>
      </div>
    </section>

    <section class="band stack">
      <h2 class="section-title">已选商品</h2>
      <div v-if="!selectedItems.length && !loading" class="muted">{{ fromCart ? '还没有勾选商品，先回购物车选中要结算的商品。' : '当前没有直接购买商品，请回到商品详情重新选择。' }}</div>
      <template v-if="previewGroups.length">
        <section v-for="group in previewGroups" :key="group.merchantId" class="merchant-preview">
          <div class="row merchant-head">
            <strong>{{ group.merchantName || `店铺 ${group.merchantId}` }}</strong>
            <span class="muted">商品 ¥{{ group.goodsAmount }} / 运费 ¥{{ group.freight }} / 优惠 ¥{{ group.discountAmount }}</span>
          </div>
          <div v-for="item in group.items" :key="item.sku_id" class="list-item">
            <img class="cover order-cover" :src="imageOf(item)" :alt="item.goods_name" />
            <div class="stack">
              <strong>{{ item.goods_name }}</strong>
              <span class="muted">{{ item.sku_name }} x {{ item.num }}</span>
              <span class="price">¥{{ item.price }}</span>
            </div>
            <strong>小计 ¥{{ item.total_price }}</strong>
          </div>
          <div class="row merchant-total">
            <span>店铺应付</span>
            <strong>¥{{ group.payAmount }}</strong>
          </div>
        </section>
      </template>
      <div v-else v-for="item in selectedItems" :key="item.cart_id || item.sku_id" class="list-item">
        <img class="cover order-cover" :src="imageOf(item)" :alt="item.goods_name" />
        <div class="stack">
          <strong>{{ item.goods_name }}</strong>
          <span class="muted">{{ item.sku_name }} x {{ item.buy_num || item.num }}</span>
          <span class="price">¥{{ item.price }}</span>
        </div>
      </div>
    </section>

    <section v-if="preview" class="band stack">
      <h2 class="section-title">金额预览</h2>
      <div class="row summary-row"><span>可用优惠券</span><strong>{{ availableOwnedCoupons.length }} 张</strong></div>
      <div class="row summary-row"><span>商品总价</span><strong>¥{{ preview.goodsAmount }}</strong></div>
      <div class="row summary-row"><span>运费</span><strong>¥{{ preview.freight }}</strong></div>
      <div class="row summary-row discount-row"><span>优惠抵扣</span><strong>-¥{{ preview.discountAmount }}</strong></div>
      <div class="row summary-row total-row"><span>应付金额</span><span class="price">¥{{ preview.payAmount }}</span></div>
      <el-button type="primary" :loading="submitting" @click="createOrder">提交订单</el-button>
    </section>
  </main>
</template>

<style scoped>
.section-head,
.summary-row,
.total-row {
  justify-content: space-between;
}

.address-preview {
  padding: 14px;
  background: #f8fbf9;
}

.order-cover {
  aspect-ratio: 1 / 1;
}

.text-link {
  color: #2c7a4b;
}

.coupon-tip {
  justify-content: space-between;
}

.merchant-preview {
  display: grid;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #e5ece8;
}

.merchant-preview:last-child {
  border-bottom: 0;
}

.merchant-head,
.merchant-total {
  justify-content: space-between;
}

.discount-row strong {
  color: var(--brand-red);
}
</style>
