<template>
  <div class="page-card">
    <el-table :data="orders" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="220" />

      <el-table-column label="订单状态" width="130">
        <template #default="{ row }">
          <el-tag :type="orderStatusTag(row.orderStatus).type">
            {{ orderStatusTag(row.orderStatus).text }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="支付状态" width="130">
        <template #default="{ row }">
          <el-tag :type="payStatusTag(row.payStatus).type">
            {{ payStatusTag(row.payStatus).text }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="支付时限" width="250">
        <template #default="{ row }">
          <div class="deadline-compact" :class="paymentDeadlineMode(row)">
            <template v-if="paymentDeadlineMode(row) === 'countdown'">
              <div class="deadline-main-line">
                <span class="deadline-title">剩余支付时间</span>
                <span class="deadline-inline-hint">{{ paymentTimeoutMinutes(row) }} 分钟内</span>
              </div>
              <div class="countdown-strip" aria-label="支付倒计时">
                <template v-for="(part, index) in paymentCountdownParts(row)" :key="`${row.id}-${index}-${part}`">
                  <span v-if="part === ':'" class="countdown-separator">:</span>
                  <span v-else class="countdown-box">{{ part }}</span>
                </template>
              </div>
              <div class="deadline-meta">{{ formatPaymentDeadlineTime(row) }}</div>
            </template>

            <template v-else-if="paymentDeadlineMode(row) === 'processing'">
              <div class="deadline-main-line">
                <span class="deadline-title">支付结果确认中</span>
                <span class="deadline-status-chip info">处理中</span>
              </div>
              <div class="deadline-meta">{{ formatPaymentDeadlineTime(row) }}</div>
            </template>

            <template v-else-if="paymentDeadlineMode(row) === 'expired'">
              <div class="deadline-main-line">
                <span class="deadline-title">支付已结束</span>
                <span class="deadline-status-chip muted">超时已取消</span>
              </div>
              <div class="deadline-meta">{{ formatPaymentDeadlineTime(row) }}</div>
            </template>

            <template v-else-if="paymentDeadlineMode(row) === 'cancelled'">
              <div class="deadline-main-line">
                <span class="deadline-title">支付已结束</span>
                <span class="deadline-status-chip subtle">已取消</span>
              </div>
              <div class="deadline-meta">{{ formatPaymentDeadlineTime(row) }}</div>
            </template>

            <template v-else>
              <div class="deadline-main-line">
                <span class="deadline-title">支付已完成</span>
                <span class="deadline-status-chip success">已完成</span>
              </div>
              <div class="deadline-meta">{{ formatPaymentDeadlineTime(row) }}</div>
            </template>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="payAmount" label="金额" width="120" />

      <el-table-column label="评价" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.hasReviewed" type="success" effect="light">已评价</el-tag>
          <span v-else class="muted-text">未评价</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="620">
        <template #default="{ row }">
          <div class="action-cell">
            <div class="action-row">
              <el-button size="small" type="primary" text @click="$router.push(`/order/detail/${row.id}`)">详情</el-button>
              <el-button
                v-if="row.contractStatus !== 'SIGNED'"
                size="small"
                type="warning"
                text
                :disabled="!canOperatePendingOrder(row)"
                @click="goContract(row.id, true)"
              >
                签合同
              </el-button>
              <el-button
                size="small"
                type="success"
                text
                :disabled="!canPayOrder(row)"
                @click="pay(row.id)"
              >
                支付
              </el-button>
              <el-button
                size="small"
                type="warning"
                text
                :disabled="!canOperatePendingOrder(row)"
                @click="cancel(row.id)"
              >
                取消
              </el-button>
              <el-button
                size="small"
                type="danger"
                text
                :disabled="row.payStatus !== 'PAID' || row.orderStatus === 'COMPLETED'"
                @click="refund(row.id)"
              >
                退款
              </el-button>
              <el-button
                size="small"
                type="info"
                text
                :disabled="row.orderStatus !== 'PENDING_TRAVEL'"
                @click="complete(row.id)"
              >
                确认完成
              </el-button>
              <el-button
                size="small"
                type="primary"
                text
                :disabled="row.orderStatus !== 'COMPLETED' || row.hasReviewed"
                @click="openReviewDialog(row)"
              >
                去评价
              </el-button>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="reviewDialogVisible" title="订单评价" width="520px">
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" :max="5" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请输入您对本次行程的评价"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import { useRouter } from "vue-router"
import { api } from "../../api"
import {
  formatPaymentCountdown,
  formatPaymentDeadlineTime,
  getPaymentRemainingSeconds,
  isPaymentExpired
} from "../../utils/paymentDeadline"

const orders = ref([])
const router = useRouter()
const reviewDialogVisible = ref(false)
const submittingReview = ref(false)
const reviewForm = reactive({
  orderId: null,
  score: 5,
  content: ""
})
const nowMs = ref(Date.now())

let clockTimer = null
let expiryRefreshLock = false
let lastExpiryRefreshAt = 0

async function load() {
  orders.value = await api.get("/orders")
}

function startClock() {
  stopClock()
  clockTimer = window.setInterval(async () => {
    nowMs.value = Date.now()
    if (shouldRefreshExpiredOrders()) {
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

function shouldRefreshExpiredOrders() {
  if (expiryRefreshLock) return false
  if (Date.now() - lastExpiryRefreshAt < 10000) return false
  return orders.value.some((row) => row?.orderStatus === "PENDING_PAY" && getPaymentRemainingSeconds(row, nowMs.value) <= 0)
}

function canOperatePendingOrder(row) {
  return row?.orderStatus === "PENDING_PAY" && row?.payStatus === "UNPAID" && !isPaymentExpired(row, nowMs.value)
}

function canPayOrder(row) {
  return canOperatePendingOrder(row) && row?.contractStatus === "SIGNED"
}

function paymentTimeoutMinutes(row) {
  return Number(row?.paymentTimeoutMinutes) || 30
}

function paymentDeadlineMode(row) {
  if (canOperatePendingOrder(row)) return "countdown"
  if (row?.orderStatus === "PENDING_PAY" && row?.payStatus === "UNPAID" && isPaymentExpired(row, nowMs.value)) {
    return "processing"
  }
  if (row?.orderStatus === "CANCELLED" && row?.paymentExpired) {
    return "expired"
  }
  if (row?.orderStatus === "CANCELLED") {
    return "cancelled"
  }
  return "finished"
}

function paymentCountdownParts(row) {
  const countdown = formatPaymentCountdown(getPaymentRemainingSeconds(row, nowMs.value))
  return countdown.split("")
}

function orderStatusTag(status) {
  const map = {
    PENDING_PAY: { text: "待支付", type: "warning" },
    PENDING_TRAVEL: { text: "待出行", type: "primary" },
    COMPLETED: { text: "已完成", type: "success" },
    CANCELLED: { text: "已取消", type: "info" },
    REFUNDING: { text: "退款中", type: "warning" },
    REFUNDED: { text: "已退款", type: "info" }
  }
  return map[status] || { text: status || "-", type: "info" }
}

function payStatusTag(status) {
  const map = {
    UNPAID: { text: "未支付", type: "warning" },
    PAID: { text: "已支付", type: "success" },
    REFUNDED: { text: "已退款", type: "info" }
  }
  return map[status] || { text: status || "-", type: "info" }
}

async function goContract(orderId, withSignAction = false) {
  const detail = await api.get(`/orders/${orderId}`, null, { silent: true })
  const contract = (detail.contracts || [])[0]
  if (!contract?.id) {
    ElMessage.warning("当前订单还没有可签署合同")
    return
  }
  router.push(`/contract/detail/${contract.id}${withSignAction ? "?action=sign" : ""}`)
}

function pay(id) {
  router.push(`/order/pay/${id}`)
}

async function cancel(id) {
  await api.post(`/orders/${id}/cancel`)
  ElMessage.success("订单已取消")
  await load()
}

async function refund(id) {
  await api.post(`/orders/${id}/refund`)
  ElMessage.success("退款已提交")
  await load()
}

async function complete(id) {
  await api.post(`/orders/${id}/complete`)
  ElMessage.success("订单已完成，现在可以评价")
  await load()
}

function openReviewDialog(row) {
  reviewForm.orderId = row.id
  reviewForm.score = 5
  reviewForm.content = ""
  reviewDialogVisible.value = true
}

async function submitReview() {
  if (!reviewForm.orderId) return
  submittingReview.value = true
  try {
    await api.post(`/orders/${reviewForm.orderId}/review`, {
      score: reviewForm.score,
      content: reviewForm.content
    })
    ElMessage.success("评价提交成功")
    reviewDialogVisible.value = false
    await load()
  } finally {
    submittingReview.value = false
  }
}

onMounted(async () => {
  await load()
  startClock()
})

onBeforeUnmount(() => {
  stopClock()
})
</script>

<style scoped>
.deadline-compact {
  display: grid;
  gap: 8px;
}

.deadline-main-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.deadline-title {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
}

.deadline-inline-hint {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.3;
  white-space: nowrap;
}

.countdown-strip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.countdown-box {
  min-width: 24px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(180deg, #134e4a 0%, #0f766e 100%);
  color: #f8fafc;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.14);
}

.countdown-separator {
  color: #0f766e;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.deadline-status-chip {
  flex: 0 0 auto;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.deadline-status-chip.info {
  background: #dbeafe;
  color: #1d4ed8;
}

.deadline-status-chip.muted {
  background: #eef2f7;
  color: #475569;
}

.deadline-status-chip.subtle {
  background: #f1f5f9;
  color: #64748b;
}

.deadline-status-chip.success {
  background: #dcfce7;
  color: #15803d;
}

.deadline-meta {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.3;
}

.deadline-compact.processing .deadline-title {
  color: #1d4ed8;
}

.deadline-compact.expired .deadline-title,
.deadline-compact.cancelled .deadline-title,
.deadline-compact.finished .deadline-title {
  color: #475569;
}

.action-cell {
  overflow-x: auto;
  padding-bottom: 2px;
}

.action-row {
  width: max-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.action-row :deep(.el-button) {
  margin-left: 0;
  padding-inline: 4px;
}

.muted-text {
  color: #94a3b8;
  font-size: 13px;
}
</style>
