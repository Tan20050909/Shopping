<template>
  <div>
    <el-card>
      <template #header><span>店铺基础设置</span></template>
      <el-form :model="form" label-width="120px">
        <el-form-item label="营业时间">
          <el-input v-model="form.businessHours" placeholder="例如：09:00-21:00" />
        </el-form-item>
        <el-form-item label="AI自动回复">
          <el-switch v-model="form.aiReplyEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="AI恢复时间">
          <div class="freight-row">
            <el-input-number v-model="form.aiResumeMinutes" :min="1" :max="1440" :step="1" controls-position="right" />
            <span class="freight-unit">分钟（商家回复后，间隔该时间未发新消息则视为新一轮对话，AI重新参与）</span>
          </div>
        </el-form-item>
        <el-form-item label="售后规则">
          <el-input v-model="form.afterSaleRule" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="默认运费设置">
          <div class="freight-row">
            <el-input-number v-model="freight.baseFreight" :min="0" :step="1" controls-position="right" />
            <span class="freight-unit">元</span>
            <span class="freight-split">满</span>
            <el-input-number v-model="freight.freeAmount" :min="0" :step="1" controls-position="right" />
            <span class="freight-unit">元包邮</span>
          </div>
        </el-form-item>
        <el-form-item label="店铺公告/自定义区">
          <el-input v-model="form.shopDecoration" type="textarea" :rows="3" placeholder="用于店铺首页展示的自定义内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { merchantSettingApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getMerchantId } from '@/utils/merchant'

const form = reactive({
  merchantId: getMerchantId(),
  businessHours: '',
  aiReplyEnabled: 0,
  aiResumeMinutes: 30,
  afterSaleRule: '',
  freightTemplate: '',
  shopDecoration: ''
})

const freight = reactive({
  baseFreight: 0,
  freeAmount: 0
})

const load = async () => {
  const res = await merchantSettingApi.get(getMerchantId())
  if (res.data) Object.assign(form, res.data)
  try {
    const parsed = form.freightTemplate ? JSON.parse(form.freightTemplate) : null
    if (parsed && typeof parsed === 'object') {
      freight.baseFreight = Number(parsed.baseFreight ?? parsed.base ?? 0) || 0
      freight.freeAmount = Number(parsed.freeAmount ?? parsed.free ?? 0) || 0
    }
  } catch (e) {
  }
}

const save = async () => {
  form.merchantId = getMerchantId()
  form.freightTemplate = JSON.stringify({
    baseFreight: Number(freight.baseFreight || 0),
    freeAmount: Number(freight.freeAmount || 0)
  })
  await merchantSettingApi.update(form)
  ElMessage.success('保存成功')
}

onMounted(load)
</script>

<style scoped>
.freight-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.freight-unit {
  color: #666;
}

.freight-split {
  color: #999;
}
</style>
