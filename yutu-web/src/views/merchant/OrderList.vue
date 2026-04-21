<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="ORDER CENTER" title="订单管理" />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入订单号"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-actions">
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
        <el-table-column prop="orderNo" label="订单号" min-width="220" />
        <el-table-column label="订单状态" width="130">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus).type" round effect="light">
              {{ orderStatusTag(row.orderStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="120">
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
            <span v-else class="muted-action">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="支付金额" width="130">
          <template #default="{ row }">¥ {{ formatAmount(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="travelerCount" label="人数" width="90">
          <template #default="{ row }">{{ row.travelerCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" @click="view(row.id)">详情</el-button>
              <el-button v-if="canProcessRefund(row)" text type="warning" @click="view(row.id)">
                处理退款
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="订单详情" width="1080px">
      <div v-if="detail.order" class="detail-wrap">
        <section class="detail-section">
          <h3 class="section-title">订单信息</h3>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="orderStatusTag(detail.order.orderStatus).type" round effect="light">
                {{ orderStatusTag(detail.order.orderStatus).text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="payStatusTag(detail.order.payStatus).type" round effect="light">
                {{ payStatusTag(detail.order.payStatus).text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付金额">¥ {{ formatAmount(detail.order.payAmount) }}</el-descriptions-item>
            <el-descriptions-item label="出行人数">{{ detail.order.travelerCount || 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ formatDateTime(detail.order.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section v-if="detail.refund?.id" class="detail-section refund-section">
          <div class="section-title-row">
            <h3 class="section-title">退款处理</h3>
            <el-tag :type="refundStatusTag(detail.refund.status).type" round effect="light">
              {{ refundStatusTag(detail.refund.status).text }}
            </el-tag>
          </div>

          <div class="refund-overview">
            <div class="refund-box">
              <span>退款单号</span>
              <strong>{{ detail.refund.refundNo }}</strong>
            </div>
            <div class="refund-box">
              <span>退款类型</span>
              <strong>{{ refundTypeLabel(detail.refund.refundType) }}</strong>
            </div>
            <div class="refund-box">
              <span>预估退款</span>
              <strong>¥ {{ formatAmount(detail.refund.expectedRefundAmount) }}</strong>
            </div>
            <div class="refund-box">
              <span>当前方案</span>
              <strong>¥ {{ formatAmount(detail.refund.proposedRefundAmount ?? detail.refund.finalRefundAmount ?? detail.refund.expectedRefundAmount) }}</strong>
            </div>
          </div>

          <div class="refund-note-list">
            <div class="refund-note">
              <label>用户申请原因</label>
              <p>{{ detail.refund.refundReason || "-" }}</p>
            </div>
            <div class="refund-note">
              <label>规则说明</label>
              <p>{{ detail.refund.policyNote || "-" }}</p>
            </div>
            <div v-if="detail.refund.merchantNote" class="refund-note">
              <label>商家意见</label>
              <p>{{ detail.refund.merchantNote }}</p>
            </div>
            <div v-if="detail.refund.adminNote" class="refund-note">
              <label>管理员裁定</label>
              <p>{{ detail.refund.adminNote }}</p>
            </div>
          </div>

          <div v-if="detail.refund.evidenceUrlList?.length" class="evidence-grid">
            <div v-for="url in detail.refund.evidenceUrlList" :key="url" class="evidence-item">
              <img :src="url" alt="退款凭证" />
            </div>
          </div>

          <div class="timeline">
            <div v-for="flow in detail.refundFlows || []" :key="`${flow.id}-${flow.createTime}`" class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="timeline-meta">
                  <strong>{{ refundActionLabel(flow.actionType) }}</strong>
                  <span>{{ formatDateTime(flow.createTime) }}</span>
                </div>
                <p>{{ flow.actionContent || "-" }}</p>
              </div>
            </div>
          </div>

          <div class="refund-actions">
            <el-button
              v-if="detail.refund.status === 'WAITING_MERCHANT_REVIEW'"
              type="primary"
              @click="openReviewDialog"
            >
              审核退款
            </el-button>
            <el-button
              v-if="detail.refund.status === 'WAITING_REFUND_EXECUTION'"
              type="success"
              @click="openExecuteDialog"
            >
              执行退款
            </el-button>
          </div>
        </section>
      </div>
      <el-empty v-else description="暂无订单详情" />
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="商家审核退款" width="720px">
      <el-form label-width="110px" class="review-form">
        <el-form-item label="处理动作">
          <el-select v-model="reviewForm.action" placeholder="请选择处理动作">
            <el-option label="同意退款" value="APPROVE" />
            <el-option label="部分退款方案" value="PARTIAL" />
            <el-option label="要求补充材料" value="SUPPLEMENT" />
            <el-option label="申请平台仲裁" value="ESCALATE" />
            <el-option label="驳回申请" value="REJECT" />
          </el-select>
        </el-form-item>

        <template v-if="['APPROVE', 'PARTIAL'].includes(reviewForm.action)">
          <el-form-item label="团费退款">
            <el-input-number v-model="reviewForm.tourFeeAmount" :min="0" :precision="2" :step="100" />
          </el-form-item>
          <el-form-item label="保险费扣减">
            <el-input-number v-model="reviewForm.insuranceFeeAmount" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item label="签证费扣减">
            <el-input-number v-model="reviewForm.visaFeeAmount" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item label="损失费扣减">
            <el-input-number v-model="reviewForm.lossFeeAmount" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item label="应退金额">
            <el-input-number v-model="reviewForm.proposedRefundAmount" :min="0" :precision="2" :step="100" />
          </el-form-item>
        </template>

        <el-form-item label="处理说明">
          <el-input
            v-model="reviewForm.note"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写核损依据、退款说明或补充材料要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交审核结果</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="executeDialogVisible" title="执行退款" width="520px">
      <el-form label-width="96px">
        <el-form-item label="执行说明">
          <el-input
            v-model="executeForm.note"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请填写退款到账说明或处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingExecute" @click="submitExecute">确认执行退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const dialogVisible = ref(false);
const reviewDialogVisible = ref(false);
const executeDialogVisible = ref(false);
const submittingReview = ref(false);
const submittingExecute = ref(false);
const detail = ref({});

const reviewForm = reactive({
  action: "APPROVE",
  tourFeeAmount: 0,
  insuranceFeeAmount: 0,
  visaFeeAmount: 0,
  lossFeeAmount: 0,
  proposedRefundAmount: 0,
  note: ""
});

const executeForm = reactive({
  note: ""
});

function canProcessRefund(row) {
  if (!row?.refundId || !row?.refundStatus) {
    return false;
  }
  return [
    "WAITING_MERCHANT_REVIEW",
    "WAITING_REFUND_EXECUTION",
    "REFUND_PROCESSING"
  ].includes(row.refundStatus);
}

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
    WAITING_USER_CONFIRM: { text: "待用户确认方案", type: "warning" },
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

function refundTypeLabel(type) {
  const map = {
    PRE_DEPARTURE: "出发前自愿退团",
    MERCHANT_REASON: "因商家原因退款",
    FORCE_MAJEURE: "因不可抗力退款",
    PARTIAL: "部分退款"
  };
  return map[type] || type || "-";
}

function refundActionLabel(action) {
  const map = {
    APPLY: "用户提交申请",
    SUPPLEMENT: "用户补充材料",
    CONFIRM: "用户接受方案",
    REJECT_PROPOSAL: "用户申请仲裁",
    REQUEST_SUPPLEMENT: "要求补充材料",
    PROPOSE_PARTIAL: "提出部分退款方案",
    APPROVE: "同意退款",
    ESCALATE: "转交管理员",
    REJECT: "驳回申请",
    EXECUTE: "发起退款执行",
    COMPLETE: "退款完成",
    ARBITRATE_APPROVE: "管理员裁定通过",
    ARBITRATE_PARTIAL: "管理员裁定部分退款",
    ARBITRATE_REJECT: "管理员裁定驳回",
    TIMEOUT_ESCALATE: "商家超时，系统转交管理员"
  };
  return map[action] || action || "-";
}

function resetSearch() {
  keyword.value = "";
  load();
}

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/merchant/orders", params);
}

async function view(id) {
  detail.value = await api.get(`/merchant/orders/${id}`);
  dialogVisible.value = true;
}

function openReviewDialog() {
  reviewForm.action = "APPROVE";
  reviewForm.tourFeeAmount = Number(detail.value.order?.payAmount || 0);
  reviewForm.insuranceFeeAmount = 0;
  reviewForm.visaFeeAmount = 0;
  reviewForm.lossFeeAmount = 0;
  reviewForm.proposedRefundAmount = Number(detail.value.refund?.expectedRefundAmount || 0);
  reviewForm.note = "";
  reviewDialogVisible.value = true;
}

function openExecuteDialog() {
  executeForm.note = "";
  executeDialogVisible.value = true;
}

async function submitReview() {
  if (!detail.value.refund?.id) {
    return;
  }
  submittingReview.value = true;
  try {
    await api.post(`/merchant/refunds/${detail.value.refund.id}/review`, {
      action: reviewForm.action,
      tourFeeAmount: reviewForm.tourFeeAmount,
      insuranceFeeAmount: reviewForm.insuranceFeeAmount,
      visaFeeAmount: reviewForm.visaFeeAmount,
      lossFeeAmount: reviewForm.lossFeeAmount,
      proposedRefundAmount: reviewForm.proposedRefundAmount,
      note: reviewForm.note
    });
    ElMessage.success("退款审核结果已提交");
    reviewDialogVisible.value = false;
    await view(detail.value.order.id);
    await load();
  } finally {
    submittingReview.value = false;
  }
}

async function submitExecute() {
  if (!detail.value.refund?.id) {
    return;
  }
  submittingExecute.value = true;
  try {
    await api.post(`/merchant/refunds/${detail.value.refund.id}/execute`, {
      note: executeForm.note
    });
    ElMessage.success("退款已执行");
    executeDialogVisible.value = false;
    await view(detail.value.order.id);
    await load();
  } finally {
    submittingExecute.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 560px;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.toolbar-search {
  width: 520px;
  max-width: 100%;
  flex: 0 1 520px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.muted-action {
  color: #94a3b8;
  font-size: 13px;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-section {
  padding: 4px 0;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.refund-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.refund-box,
.refund-note,
.timeline-content {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.refund-box span,
.refund-note label {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.refund-box strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px;
}

.refund-note-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.refund-note p {
  margin: 8px 0 0;
  color: #0f172a;
  line-height: 1.8;
}

.evidence-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.evidence-item img {
  width: 110px;
  height: 82px;
  border-radius: 14px;
  object-fit: cover;
  border: 1px solid #dbe5f0;
}

.timeline {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 999px;
  background: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.12);
}

.timeline-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.timeline-meta strong {
  color: #0f172a;
  font-size: 14px;
}

.timeline-meta span {
  color: #94a3b8;
  font-size: 12px;
}

.timeline-content p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.8;
}

.refund-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.review-form :deep(.el-select),
.review-form :deep(.el-input),
.review-form :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 960px) {
  .toolbar-left {
    flex-wrap: wrap;
  }

  .toolbar-search {
    width: 100%;
    flex-basis: 100%;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }

  .refund-overview {
    grid-template-columns: 1fr;
  }
}
</style>
