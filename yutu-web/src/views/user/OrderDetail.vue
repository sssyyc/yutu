<template>
  <div v-if="order.id" class="order-detail-page">
    <section class="page-card hero-card">
      <div class="hero-copy">
        <p class="eyebrow">ORDER CENTER</p>
        <h1>{{ order.orderNo }}</h1>

        <div v-if="showPaymentNotice" class="payment-notice" :class="paymentNoticeTone">
          <strong>{{ paymentNoticeTitle }}</strong>
          <span>{{ paymentNoticeText }}</span>
        </div>

        <div class="hero-actions">
          <el-button
            v-if="primaryContract && order.contractStatus !== 'SIGNED'"
            type="warning"
            :disabled="!canContinueContract"
            @click="goContract(true)"
          >
            继续签署合同
          </el-button>
          <el-button
            v-if="order.payStatus !== 'PAID'"
            type="success"
            :disabled="!canGoPay"
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
          <div class="flow-item" :class="{ done: order.payStatus === 'PAID', active: canGoPay }">
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
        <div class="hero-metric">
          <span>支付截止时间</span>
          <strong>{{ paymentDeadlineText }}</strong>
        </div>
        <div class="hero-metric" v-if="showPaymentNotice">
          <span>支付剩余时间</span>
          <strong>{{ paymentCountdownText }}</strong>
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
        <span>支付时限</span>
        <strong>{{ paymentWindowLabel }}</strong>
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
                :disabled="!canContinueContract"
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { api } from "../../api"
import {
  formatPaymentCountdown,
  formatPaymentDeadlineTime,
  getPaymentRemainingSeconds,
  isPaymentCountdownActive,
  isPaymentExpired
} from "../../utils/paymentDeadline"

const route = useRoute()
const router = useRouter()
const detail = ref({})
const nowMs = ref(Date.now())

let clockTimer = null
let expiryRefreshLock = false
let lastExpiryRefreshAt = 0

const order = computed(() => detail.value.order || {})
const travelers = computed(() => detail.value.travelers || [])
const contracts = computed(() => detail.value.contracts || [])
const primaryContract = computed(() => contracts.value[0] || null)
const paymentTimeoutMinutes = computed(() => Number(order.value.paymentTimeoutMinutes) || 30)

const orderStatusLabel = computed(() => mapOrderStatus(order.value.orderStatus))
const payStatusLabel = computed(() => mapPayStatus(order.value.payStatus))
const contractStatusLabel = computed(() => mapContractStatus(order.value.contractStatus))

const isPendingPayment = computed(() => isPaymentCountdownActive(order.value))
const paymentExpired = computed(() => isPaymentExpired(order.value, nowMs.value))
const overdueCancelled = computed(() => order.value.orderStatus === "CANCELLED" && paymentExpired.value)
const canContinueContract = computed(() => isPendingPayment.value && !paymentExpired.value)
const canGoPay = computed(() => isPendingPayment.value && order.value.contractStatus === "SIGNED" && !paymentExpired.value)
const paymentDeadlineText = computed(() => formatPaymentDeadlineTime(order.value))
const paymentCountdownText = computed(() => {
  if (overdueCancelled.value) return "已超时"
  if (paymentExpired.value && isPendingPayment.value) return "系统处理中"
  if (!isPendingPayment.value) return "-"
  return formatPaymentCountdown(getPaymentRemainingSeconds(order.value, nowMs.value))
})
const paymentWindowLabel = computed(() => {
  if (overdueCancelled.value) return "超时自动取消"
  if (paymentExpired.value && isPendingPayment.value) return "超时处理中"
  if (isPendingPayment.value) return paymentCountdownText.value
  return "已结束"
})
const showPaymentNotice = computed(() => isPendingPayment.value || overdueCancelled.value)
const paymentNoticeTone = computed(() => ({
  warning: isPendingPayment.value && !paymentExpired.value,
  danger: overdueCancelled.value || paymentExpired.value
}))
const paymentNoticeTitle = computed(() => {
  if (overdueCancelled.value) return "订单已超时取消"
  if (paymentExpired.value) return "支付结果确认中"
  return `请在 ${paymentTimeoutMinutes.value} 分钟内完成支付`
})
const paymentNoticeText = computed(() => {
  if (overdueCancelled.value) {
    return "该订单在下单后半小时内未完成支付，系统已自动取消。"
  }
  if (paymentExpired.value) {
    return "订单已到支付截止时间，系统正在确认最终支付结果，请稍后刷新。"
  }
  return `剩余支付时间 ${paymentCountdownText.value}，超时后系统会自动取消订单。`
})

const statusTone = computed(() => ({
  success: order.value.payStatus === "PAID",
  warning: order.value.payStatus !== "PAID" && !overdueCancelled.value,
  danger: overdueCancelled.value
}))

async function load() {
  detail.value = await api.get(`/orders/${route.params.id}`)
}

function startClock() {
  stopClock()
  clockTimer = window.setInterval(async () => {
    nowMs.value = Date.now()
    if (shouldRefreshExpiredOrder()) {
      lastExpiryRefreshAt = Date.now()
      expiryRefreshLock = true
      try {
        await load()
      } finally {
        expiryRefreshLock = false
      }
    }
  }, 1000)
}

function stopClock() {
  if (clockTimer) {
    window.clearInterval(clockTimer)
    clockTimer = null
  }
}

function shouldRefreshExpiredOrder() {
  if (expiryRefreshLock) return false
  if (Date.now() - lastExpiryRefreshAt < 10000) return false
  return isPendingPayment.value && getPaymentRemainingSeconds(order.value, nowMs.value) <= 0
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
    PENDING_TRAVEL: "待出行",
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

onMounted(async () => {
  await load()
  startClock()
})

watch(
  () => route.params.id,
  async (nextId, prevId) => {
    if (nextId && nextId !== prevId) {
      await load()
    }
  }
)

onBeforeUnmount(() => {
  stopClock()
})
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

.payment-notice {
  display: grid;
  gap: 8px;
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.1);
}

.payment-notice.warning {
  background: rgba(251, 191, 36, 0.16);
  color: #fef3c7;
}

.payment-notice.danger {
  background: rgba(248, 113, 113, 0.16);
  color: #fee2e2;
}

.payment-notice strong {
  font-size: 16px;
}

.payment-notice span {
  line-height: 1.7;
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

  .section-head,
  .contract-main {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
