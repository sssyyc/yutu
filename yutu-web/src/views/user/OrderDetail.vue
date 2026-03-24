<template>
  <div v-if="order.id" class="order-detail-page">
    <section class="page-card hero-card">
      <div class="hero-copy">
        <p class="eyebrow">ORDER CENTER</p>
        <h1>{{ order.orderNo }}</h1>

        <div class="hero-actions">
          <el-button
            v-if="primaryContract && order.contractStatus !== 'SIGNED'"
            type="warning"
            @click="goContract(true)"
          >
            继续签署合同
          </el-button>
          <el-button
            v-if="order.payStatus !== 'PAID'"
            type="success"
            :disabled="order.contractStatus !== 'SIGNED'"
            @click="goPay"
          >
            去支付
          </el-button>
          <el-button plain @click="$router.push('/order/list')">返回订单列表</el-button>
        </div>

        <div class="flow-strip">
          <div class="flow-item done">
            <span>1</span>
            <strong>订单已创建</strong>
          </div>
          <div class="flow-item" :class="{ done: order.contractStatus === 'SIGNED', active: order.contractStatus !== 'SIGNED' }">
            <span>2</span>
            <strong>{{ order.contractStatus === "SIGNED" ? "合同已签署" : "等待签署合同" }}</strong>
          </div>
          <div class="flow-item" :class="{ done: order.payStatus === 'PAID', active: order.contractStatus === 'SIGNED' && order.payStatus !== 'PAID' }">
            <span>3</span>
            <strong>{{ order.payStatus === "PAID" ? "支付已完成" : "等待支付" }}</strong>
          </div>
        </div>
      </div>

      <aside class="hero-side">
        <div class="status-pill" :class="statusTone">{{ orderStatusLabel }}</div>
        <div class="hero-metric">
          <span>支付金额</span>
          <strong>¥ {{ formatMoney(order.payAmount) }}</strong>
        </div>
        <div class="hero-metric">
          <span>出行人数</span>
          <strong>{{ order.travelerCount || 0 }} 人</strong>
        </div>
        <div class="hero-metric">
          <span>下单时间</span>
          <strong>{{ formatDateTime(order.createTime) }}</strong>
        </div>
      </aside>
    </section>

    <section class="metrics-grid">
      <article class="page-card metric-card">
        <span>订单状态</span>
        <strong>{{ orderStatusLabel }}</strong>
      </article>
      <article class="page-card metric-card">
        <span>支付状态</span>
        <strong>{{ payStatusLabel }}</strong>
      </article>
      <article class="page-card metric-card">
        <span>合同状态</span>
        <strong>{{ contractStatusLabel }}</strong>
      </article>
      <article class="page-card metric-card">
        <span>支付金额</span>
        <strong>¥ {{ formatMoney(order.payAmount) }}</strong>
      </article>
    </section>

    <section class="content-grid">
      <article class="page-card traveler-card">
        <div class="section-head">
          <div>
            <h3>出行人信息</h3>
          </div>
          <span>{{ travelers.length }} 位</span>
        </div>

        <div v-if="travelers.length" class="traveler-list">
          <div v-for="traveler in travelers" :key="traveler.id" class="traveler-item">
            <div class="traveler-name">{{ traveler.travelerName }}</div>
            <div class="traveler-meta">
              <span>身份证号：{{ traveler.idCard }}</span>
              <span>手机号：{{ traveler.phone || "-" }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="当前订单暂无出行人信息" />
      </article>

      <article class="page-card contract-card">
        <div class="section-head">
          <div>
            <h3>合同与操作</h3>
          </div>
          <span>{{ contracts.length }} 份</span>
        </div>

        <div v-if="contracts.length" class="contract-list">
          <div v-for="row in contracts" :key="row.id" class="contract-item">
            <div class="contract-main">
              <strong>{{ row.contractNo }}</strong>
              <span>{{ row.signStatus === "SIGNED" ? "已签署" : "待签署" }}</span>
            </div>
            <div class="contract-actions">
              <el-button type="primary" text @click="$router.push(`/contract/detail/${row.id}`)">查看合同</el-button>
              <el-button
                v-if="row.signStatus !== 'SIGNED'"
                type="warning"
                text
                @click="$router.push(`/contract/detail/${row.id}?action=sign`)"
              >
                去签署
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="当前订单还没有合同信息" />
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { api } from "../../api"

const route = useRoute()
const router = useRouter()
const detail = ref({})

const order = computed(() => detail.value.order || {})
const travelers = computed(() => detail.value.travelers || [])
const contracts = computed(() => detail.value.contracts || [])
const primaryContract = computed(() => contracts.value[0] || null)

const orderStatusLabel = computed(() => mapOrderStatus(order.value.orderStatus))
const payStatusLabel = computed(() => mapPayStatus(order.value.payStatus))
const contractStatusLabel = computed(() => mapContractStatus(order.value.contractStatus))

const statusTone = computed(() => ({
  success: order.value.payStatus === "PAID",
  warning: order.value.payStatus !== "PAID"
}))

async function load() {
  detail.value = await api.get(`/orders/${route.params.id}`)
}

function goContract(withSignAction = false) {
  if (!primaryContract.value?.id) return
  router.push(`/contract/detail/${primaryContract.value.id}${withSignAction ? "?action=sign" : ""}`)
}

function goPay() {
  router.push(`/order/pay/${route.params.id}`)
}

function mapOrderStatus(status) {
  const textMap = {
    PENDING_PAY: "待支付",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    REFUNDING: "退款中",
    REFUNDED: "已退款"
  }
  return textMap[status] || status || "-"
}

function mapPayStatus(status) {
  const textMap = {
    UNPAID: "未支付",
    PAID: "已支付",
    REFUNDED: "已退款"
  }
  return textMap[status] || status || "-"
}

function mapContractStatus(status) {
  const textMap = {
    GENERATED: "已生成待签署",
    SIGNED: "已签署"
  }
  return textMap[status] || status || "-"
}

function formatMoney(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return "--"
  return num % 1 === 0 ? String(num) : num.toFixed(2)
}

function formatDateTime(value) {
  if (!value) return "-"
  return String(value).replace("T", " ").slice(0, 19)
}

onMounted(load)

watch(
  () => route.params.id,
  async (nextId, prevId) => {
    if (nextId && nextId !== prevId) {
      await load()
    }
  }
)
</script>

<style scoped>
.order-detail-page {
  display: grid;
  gap: 20px;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.9fr);
  gap: 20px;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.22), transparent 36%),
    linear-gradient(135deg, #0f172a 0%, #12385b 45%, #164e63 100%);
  color: #f8fafc;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.18);
}

.eyebrow {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: rgba(226, 232, 240, 0.72);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(28px, 2.8vw, 40px);
  line-height: 1.18;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.flow-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.flow-item {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.flow-item span {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-weight: 700;
}

.flow-item strong {
  color: #ffffff;
}

.flow-item.done {
  background: rgba(16, 185, 129, 0.18);
  border-color: rgba(110, 231, 183, 0.34);
}

.flow-item.active {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.24);
}

.hero-side {
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(10px);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 9px 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  background: rgba(251, 191, 36, 0.18);
  color: #fef3c7;
}

.status-pill.success {
  background: rgba(16, 185, 129, 0.18);
  color: #d1fae5;
}

.status-pill.warning {
  background: rgba(251, 191, 36, 0.18);
  color: #fef3c7;
}

.hero-metric {
  margin-top: 18px;
  display: grid;
  gap: 6px;
}

.hero-metric span {
  color: rgba(226, 232, 240, 0.74);
  font-size: 13px;
}

.hero-metric strong {
  color: #ffffff;
  font-size: 18px;
  line-height: 1.5;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px 20px;
  border-radius: 22px;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.05);
}

.metric-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.35;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 20px;
}

.traveler-card,
.contract-card {
  padding: 24px 26px;
  border-radius: 24px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
}

.section-head > span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
}

.traveler-list,
.contract-list {
  display: grid;
  gap: 12px;
}

.traveler-item,
.contract-item {
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.traveler-name {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.traveler-meta {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  color: #475569;
}

.contract-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: #0f172a;
}

.contract-main strong {
  word-break: break-word;
}

.contract-main span {
  color: #64748b;
  font-size: 14px;
  white-space: nowrap;
}

.contract-actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .hero-card,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-card,
  .traveler-card,
  .contract-card,
  .metric-card {
    padding: 20px;
  }

  .flow-strip,
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }

  .contract-main {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
