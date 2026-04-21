<template>
  <div class="page-card order-page">
    <div class="page-head">
      <div>
        <h2>我的订单</h2>
        <p>查看订单、支付状态和退款处理进度。</p>
      </div>
    </div>

    <el-table :data="orders" border stripe class="order-table">
      <el-table-column prop="orderNo" label="订单号" min-width="220" />

      <el-table-column label="订单状态" width="130">
        <template #default="{ row }">
          <el-tag :type="orderStatusTag(row.orderStatus).type" round effect="light">
            {{ orderStatusTag(row.orderStatus).text }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="支付状态" width="130">
        <template #default="{ row }">
          <el-tag :type="payStatusTag(row.payStatus).type" round effect="light">
            {{ payStatusTag(row.payStatus).text }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="退款进度" width="150">
        <template #default="{ row }">
          <el-tag v-if="effectiveRefundStatus(row)" :type="refundStatusTag(effectiveRefundStatus(row)).type" round effect="light">
            {{ refundStatusTag(effectiveRefundStatus(row)).text }}
          </el-tag>
          <span v-else class="muted-text">未申请</span>
        </template>
      </el-table-column>

      <el-table-column prop="payAmount" label="金额" width="120">
        <template #default="{ row }">¥ {{ formatAmount(row.payAmount) }}</template>
      </el-table-column>

      <el-table-column prop="travelerCount" label="出行人数" width="100">
        <template #default="{ row }">{{ row.travelerCount || 0 }} 人</template>
      </el-table-column>

      <el-table-column prop="createTime" label="下单时间" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>

      <el-table-column label="操作" min-width="420" fixed="right">
        <template #default="{ row }">
          <div class="action-row">
            <el-button text type="primary" @click="$router.push(`/order/detail/${row.id}`)">详情</el-button>
            <el-button
              v-if="row.orderStatus === 'PENDING_PAY'"
              text
              type="success"
              @click="goPay(row.id)"
            >
              去支付
            </el-button>
            <el-button
              v-if="row.orderStatus === 'PENDING_PAY'"
              text
              type="warning"
              @click="cancelOrder(row.id)"
            >
              取消订单
            </el-button>
            <el-button
              v-if="canApplyRefund(row)"
              text
              type="danger"
              @click="openRefundDialog(row)"
            >
              申请退款
            </el-button>
            <el-button
              v-if="row.refundId"
              text
              type="primary"
              @click="$router.push(`/order/detail/${row.id}`)"
            >
              查看退款
            </el-button>
            <el-button
              v-if="row.orderStatus === 'COMPLETED' && !row.hasReviewed"
              text
              type="primary"
              @click="openReviewDialog(row)"
            >
              去评价
            </el-button>
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
            placeholder="请输入本次出行体验"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="760px">
      <el-form label-width="96px" class="refund-form">
        <el-form-item label="订单号">
          <span>{{ refundContext.orderNo || "-" }}</span>
        </el-form-item>
        <el-form-item label="退款类型">
          <el-select v-model="refundForm.refundType" placeholder="请选择退款类型" @change="loadRefundEstimate">
            <el-option label="出发前自愿退团" value="PRE_DEPARTURE" />
            <el-option label="因商家原因退款" value="MERCHANT_REASON" />
            <el-option label="因不可抗力退款" value="FORCE_MAJEURE" />
            <el-option label="部分退款" value="PARTIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input
            v-model="refundForm.refundReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请说明退款原因、已发生的问题和补充说明"
          />
        </el-form-item>
        <el-form-item label="凭证材料">
          <div class="evidence-stack">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="uploadRefundEvidence"
            >
              <el-button type="primary" plain>上传凭证</el-button>
            </el-upload>
            <div v-if="refundForm.evidenceUrls.length" class="evidence-grid">
              <div v-for="(url, index) in refundForm.evidenceUrls" :key="url" class="evidence-item">
                <img :src="url" alt="退款凭证" />
                <el-button text type="danger" @click="removeEvidence(index)">移除</el-button>
              </div>
            </div>
            <span v-else class="muted-text">可上传聊天截图、合同约定、停运证明等材料。</span>
          </div>
        </el-form-item>
        <el-form-item label="退款账户">
          <div class="refund-account-row">
            <el-select v-model="refundForm.refundAccountType" placeholder="账户类型">
              <el-option label="原路退回" value="ORIGINAL" />
              <el-option label="支付宝" value="ALIPAY" />
              <el-option label="银行卡" value="BANK_CARD" />
            </el-select>
            <el-input
              v-model="refundForm.refundAccountNo"
              placeholder="如需指定账户，请填写账号或银行卡号"
            />
          </div>
        </el-form-item>
      </el-form>

      <div v-if="refundEstimate" class="estimate-card">
        <div>
          <span class="estimate-label">预估退款金额</span>
          <strong>¥ {{ formatAmount(refundEstimate.estimatedRefundAmount) }}</strong>
        </div>
        <div>
          <span class="estimate-label">预估扣费</span>
          <strong>¥ {{ formatAmount(refundEstimate.deductAmount) }}</strong>
        </div>
        <p>{{ refundEstimate.policyNote }}</p>
      </div>

      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingRefund" @click="submitRefund">提交退款申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { api } from "../../api";

const router = useRouter();

const orders = ref([]);
const reviewDialogVisible = ref(false);
const refundDialogVisible = ref(false);
const submittingReview = ref(false);
const submittingRefund = ref(false);
const refundEstimate = ref(null);

const reviewForm = reactive({
  orderId: null,
  score: 5,
  content: ""
});

const refundContext = reactive({
  orderId: null,
  orderNo: ""
});

const refundForm = reactive({
  refundType: "",
  refundReason: "",
  evidenceUrls: [],
  refundAccountType: "ORIGINAL",
  refundAccountNo: ""
});

function formatAmount(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "--";
  }
  return number.toFixed(2).replace(/\.00$/, "");
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

function orderStatusTag(status) {
  const map = {
    PENDING_PAY: { text: "待支付", type: "warning" },
    PENDING_TRAVEL: { text: "待出行", type: "primary" },
    COMPLETED: { text: "已完成", type: "success" },
    EXPIRED: { text: "已失效", type: "danger" },
    CANCELLED: { text: "已取消", type: "info" },
    REFUNDING: { text: "退款中", type: "warning" },
    REFUNDED: { text: "已退款", type: "danger" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function payStatusTag(status) {
  const map = {
    UNPAID: { text: "未支付", type: "info" },
    PAID: { text: "已支付", type: "success" },
    FAILED: { text: "支付失败", type: "danger" },
    REFUNDED: { text: "已退款", type: "warning" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function refundStatusTag(status) {
  const map = {
    WAITING_MERCHANT_REVIEW: { text: "待商家审核", type: "warning" },
    WAITING_USER_SUPPLEMENT: { text: "待补充材料", type: "danger" },
    WAITING_USER_CONFIRM: { text: "待确认方案", type: "warning" },
    WAITING_ADMIN_ARBITRATION: { text: "待管理员仲裁", type: "danger" },
    WAITING_REFUND_EXECUTION: { text: "待执行退款", type: "primary" },
    REFUND_PROCESSING: { text: "退款处理中", type: "warning" },
    REFUND_COMPLETED: { text: "退款完成", type: "success" },
    REFUND_REJECTED: { text: "退款驳回", type: "info" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function effectiveRefundStatus(row) {
  if (row?.refundStatus) {
    return row.refundStatus;
  }
  if (row?.orderStatus === "REFUNDED" || row?.payStatus === "REFUNDED") {
    return "REFUND_COMPLETED";
  }
  return "";
}

function canApplyRefund(row) {
  return row.payStatus === "PAID" && !row.refundId && !["CANCELLED", "REFUNDED"].includes(row.orderStatus);
}

async function load() {
  orders.value = await api.get("/orders");
}

function goPay(id) {
  router.push(`/order/pay/${id}`);
}

async function cancelOrder(id) {
  await ElMessageBox.confirm("确认取消当前订单吗？", "取消订单", {
    type: "warning"
  });
  await api.post(`/orders/${id}/cancel`);
  ElMessage.success("订单已取消");
  await load();
}

function openReviewDialog(row) {
  reviewForm.orderId = row.id;
  reviewForm.score = 5;
  reviewForm.content = "";
  reviewDialogVisible.value = true;
}

async function submitReview() {
  if (!reviewForm.orderId) {
    return;
  }
  submittingReview.value = true;
  try {
    await api.post(`/orders/${reviewForm.orderId}/review`, {
      score: reviewForm.score,
      content: reviewForm.content
    });
    ElMessage.success("评价已提交");
    reviewDialogVisible.value = false;
    await load();
  } finally {
    submittingReview.value = false;
  }
}

function resetRefundForm() {
  refundForm.refundType = "";
  refundForm.refundReason = "";
  refundForm.evidenceUrls = [];
  refundForm.refundAccountType = "ORIGINAL";
  refundForm.refundAccountNo = "";
  refundEstimate.value = null;
}

function openRefundDialog(row) {
  refundContext.orderId = row.id;
  refundContext.orderNo = row.orderNo;
  resetRefundForm();
  refundDialogVisible.value = true;
}

async function loadRefundEstimate() {
  if (!refundContext.orderId || !refundForm.refundType) {
    refundEstimate.value = null;
    return;
  }
  refundEstimate.value = await api.get(`/orders/${refundContext.orderId}/refund-estimate`, {
    refundType: refundForm.refundType
  });
}

async function uploadRefundEvidence(options) {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const result = await api.upload("/files/upload", formData);
    refundForm.evidenceUrls.push(result.url);
    ElMessage.success("凭证上传成功");
    options.onSuccess?.(result);
  } catch (error) {
    options.onError?.(error);
  }
}

function removeEvidence(index) {
  refundForm.evidenceUrls.splice(index, 1);
}

async function submitRefund() {
  if (!refundContext.orderId) {
    return;
  }
  if (!refundForm.refundType) {
    ElMessage.warning("请选择退款类型");
    return;
  }
  if (!refundForm.refundReason.trim()) {
    ElMessage.warning("请填写退款原因");
    return;
  }

  submittingRefund.value = true;
  try {
    await api.post(`/orders/${refundContext.orderId}/refunds`, {
      refundType: refundForm.refundType,
      refundReason: refundForm.refundReason,
      evidenceUrls: refundForm.evidenceUrls,
      refundAccountType: refundForm.refundAccountType,
      refundAccountNo: refundForm.refundAccountNo
    });
    ElMessage.success("退款申请已提交");
    refundDialogVisible.value = false;
    await load();
  } finally {
    submittingRefund.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  font-weight: 800;
}

.page-head p {
  margin: 8px 0 0;
  color: #64748b;
}

.order-table {
  border-radius: 18px;
  overflow: hidden;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.muted-text {
  color: #94a3b8;
}

.refund-form :deep(.el-select),
.refund-form :deep(.el-input),
.refund-form :deep(.el-textarea) {
  width: 100%;
}

.evidence-stack {
  display: grid;
  gap: 12px;
}

.evidence-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.evidence-item {
  width: 110px;
}

.evidence-item img {
  width: 110px;
  height: 82px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid #dbe5f0;
}

.refund-account-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 12px;
  width: 100%;
}

.estimate-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f7ff 100%);
  border: 1px solid #dbeafe;
}

.estimate-label {
  color: #64748b;
  font-size: 13px;
}

.estimate-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.estimate-card p {
  grid-column: 1 / -1;
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .refund-account-row,
  .estimate-card {
    grid-template-columns: 1fr;
  }
}
</style>
