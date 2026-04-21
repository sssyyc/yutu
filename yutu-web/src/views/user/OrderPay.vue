<template>
  <div class="order-pay-page">
    <section v-if="order.id" class="page-card hero-card">
      <div class="hero-copy">
        <p class="eyebrow">PAYMENT CENTER</p>
        <h1>{{ heroTitle }}</h1>

        <div class="deadline-board" :class="deadlineBoardTone">
          <span class="deadline-label">支付截止时间</span>
          <strong>{{ paymentDeadlineText }}</strong>
          <p>{{ deadlineDescription }}</p>
        </div>

        <div class="flow-strip">
          <div class="flow-item done">
            <span>1</span>
            <strong>提交订单</strong>
          </div>
          <div class="flow-item" :class="{ done: !needsContractSign }">
            <span>2</span>
            <strong>签署合同</strong>
          </div>
          <div class="flow-item" :class="{ done: isPaid, active: canPreparePayment || shouldSyncExpiredPayment }">
            <span>3</span>
            <strong>完成支付</strong>
          </div>
        </div>
      </div>

      <aside class="hero-panel">
        <div class="status-pill" :class="statusTone">{{ statusText }}</div>
        <div class="hero-metric">
          <span>订单号</span>
          <strong>{{ order.orderNo || "-" }}</strong>
        </div>
        <div class="hero-metric">
          <span>支付金额</span>
          <strong>¥ {{ formatMoney(order.payAmount) }}</strong>
        </div>
        <div class="hero-metric">
          <span>支付剩余时间</span>
          <strong>{{ paymentCountdownText }}</strong>
        </div>
        <div class="hero-metric">
          <span>最近同步</span>
          <strong>{{ lastSyncText }}</strong>
        </div>
      </aside>
    </section>

    <section v-if="order.id" class="pay-grid">
      <article class="page-card qr-card">
        <div class="section-head">
          <div>
            <h3>扫码支付</h3>
          </div>
          <div class="sync-badge">{{ syncHint }}</div>
        </div>

        <template v-if="isPaid">
          <div class="success-board">
            <el-result
              icon="success"
              title="支付成功"
              :sub-title="`支付结果已经确认，${successRedirectSeconds} 秒后为你跳转到订单详情页。`"
            />
            <div class="success-actions">
              <el-button type="primary" @click="goDetail">立即查看订单</el-button>
              <el-button plain @click="stopSuccessRedirect">先停留在此页</el-button>
            </div>
          </div>
        </template>

        <template v-else-if="overdueCancelled">
          <el-result
            icon="warning"
            title="订单已超时取消"
            sub-title="该订单在下单后半小时内未完成支付，系统已自动取消。"
          >
            <template #extra>
              <div class="error-actions">
                <el-button type="primary" plain @click="goDetail">查看订单详情</el-button>
                <el-button plain @click="$router.push('/order/list')">返回订单列表</el-button>
              </div>
            </template>
          </el-result>
        </template>

        <template v-else-if="needsContractSign">
          <el-result
            icon="warning"
            title="请先签署合同"
            sub-title="当前订单还没有完成合同签署，签完后会直接回到支付页。"
          >
            <template #extra>
              <el-button type="warning" @click="goContract(true)">前往签署合同</el-button>
            </template>
          </el-result>
        </template>

        <template v-else-if="shouldSyncExpiredPayment">
          <el-result
            icon="info"
            title="支付结果确认中"
            sub-title="订单已到支付截止时间，系统正在确认最终支付结果。你可以手动同步一次。"
          >
            <template #extra>
              <div class="error-actions">
                <el-button type="primary" plain :loading="checking" @click="checkStatus({ manual: true })">立即同步</el-button>
                <el-button plain @click="goDetail">查看订单详情</el-button>
              </div>
            </template>
          </el-result>
        </template>

        <template v-else-if="paymentError">
          <el-result icon="info" title="支付暂不可用" :sub-title="paymentError">
            <template #extra>
              <div class="error-actions">
                <el-button type="primary" plain @click="refreshPayment">重新尝试</el-button>
                <el-button plain @click="goDetail">返回订单详情</el-button>
              </div>
            </template>
          </el-result>
        </template>

        <template v-else>
          <div class="countdown-ribbon">
            <strong>{{ paymentCountdownText }}</strong>
            <span>请在 {{ paymentTimeoutMinutes }} 分钟内完成支付，超时订单会自动取消。</span>
          </div>

          <div class="qr-shell">
            <div class="qr-stage">
              <div v-if="payment.qrCodeImage" class="qr-wrap">
                <img
                  :key="`${payment.payNo || 'pay'}-${qrRenderKey}`"
                  :src="payment.qrCodeImage"
                  alt="支付宝支付二维码"
                  @error="handleQrImageError"
                />
              </div>
              <el-empty v-else description="正在生成支付二维码" />
            </div>

            <div class="qr-guide">
              <div class="guide-item">
                <span>01</span>
                <p>打开支付宝扫一扫当前二维码</p>
              </div>
              <div class="guide-item">
                <span>02</span>
                <p>完成付款后，系统会自动同步结果</p>
              </div>
              <div class="guide-item">
                <span>03</span>
                <p>如果网络稍慢，也可以手动点击“立即同步”确认</p>
              </div>
            </div>
          </div>

          <div class="pay-actions">
            <el-button type="primary" :loading="preparing" @click="refreshPayment">换一张新码</el-button>
            <el-button plain :disabled="!payment.qrCode" @click="openOnCurrentDevice">当前设备打开支付</el-button>
            <el-button type="success" plain :loading="checking" @click="checkStatus({ manual: true })">
              我已支付，立即同步
            </el-button>
          </div>
        </template>
      </article>

      <aside class="page-card summary-card">
        <div class="section-head">
          <div>
            <h3>订单摘要</h3>
          </div>
        </div>

        <div class="summary-list">
          <div class="summary-item">
            <span>订单状态</span>
            <strong>{{ orderStatusLabel }}</strong>
          </div>
          <div class="summary-item">
            <span>支付状态</span>
            <strong>{{ payStatusLabel }}</strong>
          </div>
          <div class="summary-item">
            <span>合同状态</span>
            <strong>{{ contractStatusLabel }}</strong>
          </div>
          <div class="summary-item">
            <span>支付单号</span>
            <strong>{{ payment.payNo || paymentStatus.payNo || "-" }}</strong>
          </div>
          <div class="summary-item">
            <span>支付截止时间</span>
            <strong>{{ paymentDeadlineText }}</strong>
          </div>
        </div>

        <div class="summary-actions">
          <el-button plain @click="goDetail">查看订单详情</el-button>
          <el-button v-if="primaryContract" plain @click="goContract(false)">查看合同</el-button>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from "element-plus"
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
const payment = ref({})
const paymentStatus = ref({})
const paymentError = ref("")
const preparing = ref(false)
const checking = ref(false)
const qrRenderKey = ref(0)
const hasRetriedEmptyQr = ref(false)
const lastStatusSyncAt = ref("")
const successRedirectSeconds = ref(5)
const nowMs = ref(Date.now())

let pollTimer = null
let successCountdownTimer = null
let successRedirectTimer = null
let clockTimer = null
let expiryRefreshLock = false
let lastExpiryRefreshAt = 0

const order = computed(() => detail.value.order || {})
const primaryContract = computed(() => (detail.value.contracts || [])[0] || null)
const paymentTimeoutMinutes = computed(() => Number(order.value.paymentTimeoutMinutes) || 30)
const isPaid = computed(() => order.value.payStatus === "PAID")
const needsContractSign = computed(() => order.value.contractStatus !== "SIGNED")
const isPendingPayment = computed(() => isPaymentCountdownActive(order.value))
const paymentExpired = computed(() => isPaymentExpired(order.value, nowMs.value))
const overdueCancelled = computed(() => order.value.orderStatus === "CANCELLED" && paymentExpired.value)
const shouldSyncExpiredPayment = computed(() => isPendingPayment.value && paymentExpired.value && !isPaid.value)
const canPreparePayment = computed(() => isPendingPayment.value && !needsContractSign.value && !paymentExpired.value)

const statusTone = computed(() => ({
  success: isPaid.value,
  warning: !isPaid.value && !overdueCancelled.value,
  danger: overdueCancelled.value
}))

const heroTitle = computed(() => {
  if (isPaid.value) return "支付完成，订单已经确认"
  if (overdueCancelled.value) return "订单已因超时未支付自动取消"
  if (shouldSyncExpiredPayment.value) return "支付时限已到，系统正在确认最终支付结果"
  return `请在 ${paymentTimeoutMinutes.value} 分钟内完成支付`
})

const statusText = computed(() => {
  if (isPaid.value) return "支付成功"
  if (overdueCancelled.value) return "已超时取消"
  if (shouldSyncExpiredPayment.value) return "结果确认中"
  if (needsContractSign.value) return "待签署合同"
  if (paymentError.value) return "支付暂不可用"
  return "等待支付"
})

const syncHint = computed(() => {
  if (isPaid.value) return "已确认支付成功"
  if (overdueCancelled.value) return "订单已自动取消"
  if (shouldSyncExpiredPayment.value) return "正在确认支付结果"
  if (paymentError.value) return "请先恢复支付"
  return "每 3 秒自动同步一次"
})

const paymentDeadlineText = computed(() => formatPaymentDeadlineTime(order.value))
const paymentCountdownText = computed(() => {
  if (isPaid.value) return "已完成"
  if (overdueCancelled.value) return "已超时"
  if (shouldSyncExpiredPayment.value) return "系统处理中"
  if (!isPendingPayment.value) return "-"
  return formatPaymentCountdown(getPaymentRemainingSeconds(order.value, nowMs.value))
})
const deadlineBoardTone = computed(() => ({
  warning: canPreparePayment.value,
  danger: overdueCancelled.value || shouldSyncExpiredPayment.value,
  info: !canPreparePayment.value && !overdueCancelled.value && !shouldSyncExpiredPayment.value
}))
const deadlineDescription = computed(() => {
  if (overdueCancelled.value) {
    return "该订单在支付时限内未完成付款，系统已自动取消。"
  }
  if (shouldSyncExpiredPayment.value) {
    return "截止时间已到，系统正在确认最终支付结果，请稍后同步。"
  }
  if (canPreparePayment.value) {
    return `当前剩余支付时间 ${paymentCountdownText.value}，请在 ${paymentTimeoutMinutes.value} 分钟内完成支付，超时后系统会自动取消订单。`
  }
  if (needsContractSign.value) {
    return "请先完成合同签署，签完后会自动回到支付流程。"
  }
  return "当前订单无需继续支付。"
})

const lastSyncText = computed(() => lastStatusSyncAt.value || "等待首次同步")
const orderStatusLabel = computed(() => mapOrderStatus(order.value.orderStatus))
const payStatusLabel = computed(() => mapPayStatus(order.value.payStatus))
const contractStatusLabel = computed(() => mapContractStatus(order.value.contractStatus))

async function loadOrder() {
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
        await loadOrder()
        if (shouldSyncExpiredPayment.value) {
          await checkStatus()
        }
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
  if (Date.now() - lastExpiryRefreshAt < 5000) return false
  return isPendingPayment.value && getPaymentRemainingSeconds(order.value, nowMs.value) <= 0
}

function bumpQrRender() {
  qrRenderKey.value += 1
}

function resetPaymentState() {
  payment.value = {}
  paymentStatus.value = {}
  paymentError.value = ""
  hasRetriedEmptyQr.value = false
  lastStatusSyncAt.value = ""
  stopSuccessRedirect()
}

async function wait(ms) {
  await new Promise((resolve) => window.setTimeout(resolve, ms))
}

function markSynced() {
  lastStatusSyncAt.value = formatDateTime(new Date())
}

async function preparePayment(options = {}) {
  const { notify = false, retryOnEmpty = false } = options
  await loadOrder()
  if (!canPreparePayment.value) {
    paymentError.value = ""
    return
  }

  preparing.value = true
  paymentError.value = ""
  try {
    payment.value = await api.post(`/orders/${route.params.id}/pay`, null, { silent: true })
    bumpQrRender()
    markSynced()

    if (!payment.value?.qrCodeImage && payment.value?.qrCode && retryOnEmpty && !hasRetriedEmptyQr.value) {
      hasRetriedEmptyQr.value = true
      await wait(500)
      payment.value = await api.post(`/orders/${route.params.id}/pay`, null, { silent: true })
      bumpQrRender()
      markSynced()
    }

    if (notify) {
      ElMessage.success(payment.value?.qrCodeImage ? "新的支付二维码已经准备好" : "已重新获取支付信息")
    }
  } catch (error) {
    payment.value = {}
    paymentError.value = extractErrorMessage(error)
    await loadOrder()
    stopPolling()
  } finally {
    preparing.value = false
  }
}

async function refreshPayment() {
  hasRetriedEmptyQr.value = false
  await preparePayment({ notify: true, retryOnEmpty: true })
  if (!paymentError.value && !pollTimer && (canPreparePayment.value || shouldSyncExpiredPayment.value)) {
    startPolling()
  }
}

async function checkStatus(options = {}) {
  const { manual = false } = options
  if (checking.value || isPaid.value || needsContractSign.value || overdueCancelled.value) {
    return
  }

  checking.value = true
  try {
    paymentStatus.value = await api.get(`/orders/${route.params.id}/payment-status`, null, { silent: true })
    await loadOrder()
    markSynced()

    if (paymentStatus.value?.paid || order.value.payStatus === "PAID") {
      stopPolling()
      startSuccessRedirect()
      ElMessage.success("支付成功，订单已完成")
      return
    }

    if (overdueCancelled.value) {
      stopPolling()
      if (manual) {
        ElMessage.warning("订单已超时未支付，系统已自动取消")
      }
      return
    }

    if (manual) {
      if (shouldSyncExpiredPayment.value) {
        ElMessage.info("系统仍在确认支付结果，请稍后再同步一次")
      } else {
        ElMessage.info("暂未确认到支付结果，完成付款后再同步一次就可以")
      }
    }
  } catch (error) {
    if (manual) {
      ElMessage.warning(extractErrorMessage(error))
    }
  } finally {
    checking.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(() => {
    checkStatus()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function startSuccessRedirect() {
  stopSuccessRedirect()
  successRedirectSeconds.value = 5
  successCountdownTimer = window.setInterval(() => {
    if (successRedirectSeconds.value > 1) {
      successRedirectSeconds.value -= 1
    }
  }, 1000)
  successRedirectTimer = window.setTimeout(() => {
    stopSuccessRedirect()
    router.replace(`/order/detail/${route.params.id}`)
  }, 5000)
}

function stopSuccessRedirect() {
  if (successCountdownTimer) {
    window.clearInterval(successCountdownTimer)
    successCountdownTimer = null
  }
  if (successRedirectTimer) {
    window.clearTimeout(successRedirectTimer)
    successRedirectTimer = null
  }
}

function goDetail() {
  stopSuccessRedirect()
  router.push(`/order/detail/${route.params.id}`)
}

function goContract(withSignAction = false) {
  if (!primaryContract.value?.id) {
    ElMessage.warning("当前订单暂时没有合同信息")
    return
  }
  router.push(`/contract/detail/${primaryContract.value.id}${withSignAction ? "?action=sign" : ""}`)
}

function openOnCurrentDevice() {
  if (!payment.value.qrCode) {
    return
  }
  window.location.href = payment.value.qrCode
}

async function handleQrImageError() {
  if (preparing.value || hasRetriedEmptyQr.value) {
    return
  }
  hasRetriedEmptyQr.value = true
  await preparePayment({ retryOnEmpty: true })
}

function extractErrorMessage(error) {
  const message = error?.response?.data?.message || error?.message || "支付请求失败，请稍后重试"
  if (message.includes("未配置") || message.includes("沙箱")) {
    return "支付服务暂不可用，请稍后重试或联系管理员。"
  }
  return message
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
  if (value instanceof Date) {
    return value.toLocaleString("zh-CN", { hour12: false })
  }
  return String(value).replace("T", " ").slice(0, 19)
}

async function initializePage() {
  stopPolling()
  resetPaymentState()
  await loadOrder()

  if (isPaid.value) {
    markSynced()
    startSuccessRedirect()
    return
  }
  if (needsContractSign.value) {
    return
  }
  if (overdueCancelled.value) {
    return
  }
  if (shouldSyncExpiredPayment.value) {
    startPolling()
    return
  }

  await preparePayment({ retryOnEmpty: true })
  if (!paymentError.value && (canPreparePayment.value || shouldSyncExpiredPayment.value)) {
    startPolling()
  }
}

onMounted(async () => {
  startClock()
  await initializePage()
})

watch(
  () => route.params.id,
  async (nextId, prevId) => {
    if (nextId && nextId !== prevId) {
      await initializePage()
    }
  }
)

watch(
  () => isPaid.value,
  (value) => {
    if (value) {
      startSuccessRedirect()
    } else {
      stopSuccessRedirect()
    }
  }
)

onBeforeUnmount(() => {
  stopClock()
  stopPolling()
  stopSuccessRedirect()
})
</script>

<style scoped>
.order-pay-page {
  display: grid;
  gap: 20px;
  --pay-card: #ffffff;
  --pay-soft: #f8fafc;
  --pay-soft-alt: #f1f5f9;
  --pay-border: #e2e8f0;
  --pay-border-strong: #cbd5e1;
  --pay-text: #0f172a;
  --pay-muted: #64748b;
  --pay-accent: #334155;
  --pay-accent-soft: #eaf0f6;
  --pay-success: #0f766e;
  --pay-success-soft: #ecfdf5;
  --pay-warning: #b45309;
  --pay-warning-soft: #fffbeb;
  --pay-danger: #b91c1c;
  --pay-danger-soft: #fef2f2;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.9fr);
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(180deg, #fbfdff 0%, #f3f6fa 100%);
  border: 1px solid var(--pay-border);
  color: var(--pay-text);
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.07);
}

.eyebrow {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(18px, 1.7vw, 28px);
  line-height: 1.28;
  color: var(--pay-text);
  letter-spacing: -0.01em;
}

.deadline-board {
  display: grid;
  gap: 5px;
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--pay-border);
  background: var(--pay-card);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.deadline-board.warning {
  background: var(--pay-warning-soft);
  border-color: rgba(180, 83, 9, 0.16);
  color: var(--pay-warning);
}

.deadline-board.danger {
  background: var(--pay-danger-soft);
  border-color: rgba(185, 28, 28, 0.16);
  color: var(--pay-danger);
}

.deadline-board.info {
  background: var(--pay-accent-soft);
  border-color: rgba(51, 65, 85, 0.12);
  color: var(--pay-accent);
}

.deadline-label {
  font-size: 10px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--pay-muted);
  opacity: 1;
}

.deadline-board strong {
  font-size: 16px;
  line-height: 1.25;
}

.deadline-board p {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
}

.flow-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.flow-item {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--pay-card);
  border: 1px solid var(--pay-border);
}

.flow-item span {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--pay-soft-alt);
  color: var(--pay-muted);
  font-size: 13px;
  font-weight: 700;
}

.flow-item strong {
  color: var(--pay-text);
  font-size: 12px;
  line-height: 1.5;
}

.flow-item.done {
  background: var(--pay-success-soft);
  border-color: rgba(15, 118, 110, 0.18);
}

.flow-item.done span {
  background: rgba(15, 118, 110, 0.12);
  color: var(--pay-success);
}

.flow-item.active {
  background: var(--pay-accent-soft);
  border-color: rgba(51, 65, 85, 0.16);
}

.flow-item.active span {
  background: rgba(51, 65, 85, 0.1);
  color: var(--pay-accent);
}

.hero-panel {
  padding: 16px;
  border-radius: 20px;
  background: var(--pay-soft-alt);
  border: 1px solid var(--pay-border);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: var(--pay-accent-soft);
  color: var(--pay-accent);
}

.status-pill.success {
  background: var(--pay-success-soft);
  color: var(--pay-success);
}

.status-pill.warning {
  background: var(--pay-warning-soft);
  color: var(--pay-warning);
}

.status-pill.danger {
  background: var(--pay-danger-soft);
  color: var(--pay-danger);
}

.hero-metric {
  margin-top: 14px;
  display: grid;
  gap: 4px;
}

.hero-metric span {
  color: var(--pay-muted);
  font-size: 11px;
}

.hero-metric strong {
  color: var(--pay-text);
  font-size: 13px;
  line-height: 1.4;
  word-break: break-word;
}

.pay-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.8fr);
  gap: 20px;
}

.qr-card,
.summary-card {
  padding: 20px 22px;
  border-radius: 20px;
  background: var(--pay-card);
  border: 1px solid var(--pay-border);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
}

.sync-badge {
  padding: 7px 10px;
  border-radius: 999px;
  background: var(--pay-soft-alt);
  color: var(--pay-muted);
  border: 1px solid var(--pay-border);
  font-size: 12px;
  font-weight: 600;
}

.countdown-ribbon {
  display: grid;
  gap: 4px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: var(--pay-warning-soft);
  border: 1px solid rgba(180, 83, 9, 0.16);
}

.countdown-ribbon strong {
  color: var(--pay-warning);
  font-size: 18px;
  line-height: 1.2;
}

.countdown-ribbon span {
  color: #92400e;
  font-size: 14px;
  line-height: 1.6;
}

.qr-shell {
  display: grid;
  gap: 16px;
}

.qr-stage {
  padding: 16px;
  border-radius: 18px;
  background: var(--pay-soft);
  border: 1px solid var(--pay-border);
}

.qr-wrap {
  display: flex;
  justify-content: center;
}

.qr-wrap img {
  width: min(320px, 100%);
  max-width: 300px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid var(--pay-border);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
}

.qr-guide {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.guide-item {
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--pay-soft);
  border: 1px solid var(--pay-border);
}

.guide-item span {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--pay-accent-soft);
  color: var(--pay-accent);
  font-size: 12px;
  font-weight: 700;
}

.guide-item p {
  margin: 10px 0 0;
  color: #334155;
  line-height: 1.6;
  font-size: 13px;
}

.pay-actions,
.summary-actions,
.error-actions,
.success-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.pay-actions {
  margin-top: 16px;
}

.success-board {
  display: grid;
  gap: 16px;
}

.success-actions {
  justify-content: center;
}

.summary-card {
  display: grid;
  gap: 14px;
  align-content: start;
}

.summary-list {
  display: grid;
  gap: 10px;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--pay-soft);
  border: 1px solid var(--pay-border);
}

.summary-item span {
  display: block;
  color: var(--pay-muted);
  font-size: 12px;
}

.summary-item strong {
  display: block;
  margin-top: 6px;
  color: var(--pay-text);
  font-size: 15px;
  line-height: 1.45;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .hero-card,
  .pay-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .flow-strip,
  .qr-guide {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-card,
  .qr-card,
  .summary-card {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 20px;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
