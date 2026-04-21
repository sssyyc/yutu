<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="ORDER MANAGEMENT" title="订单管理" />

    <section class="page-card module-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">订单总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">当前后台检索结果中的订单记录总量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">已支付订单</span>
          <strong class="overview-value success">{{ overview.paid }}</strong>
          <p class="overview-note">支付状态为已支付或已完成支付闭环的订单数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">退款处理中</span>
          <strong class="overview-value warning">{{ overview.refunding }}</strong>
          <p class="overview-note">当前仍处于退款申请、仲裁或执行流程中的订单数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">支付金额汇总</span>
          <strong class="overview-value primary">{{ overview.amount }}</strong>
          <p class="overview-note">按当前结果汇总的订单支付金额，便于快速查看规模。</p>
        </article>
      </div>
    </section>

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
            <el-tag :type="orderStatusTag(row.orderStatus).type" effect="light" round>
              {{ orderStatusTag(row.orderStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="130">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus).type" effect="light" round>
              {{ payStatusTag(row.payStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款进度" width="160">
          <template #default="{ row }">
            <el-tag v-if="effectiveRefundStatus(row)" :type="refundStatusTag(effectiveRefundStatus(row)).type" effect="light" round>
              {{ refundStatusTag(effectiveRefundStatus(row)).text }}
            </el-tag>
            <span v-else class="muted-action">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="支付金额" width="120">
          <template #default="{ row }">¥ {{ formatAmount(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" @click="view(row.id)">详情</el-button>
              <el-button
                v-if="isCloseableOrder(row.orderStatus)"
                text
                type="warning"
                @click="handleException(row)"
              >
                关闭订单
              </el-button>
              <el-button
                v-if="row.refundStatus === 'WAITING_ADMIN_ARBITRATION'"
                text
                type="danger"
                @click="view(row.id)"
              >
                退款仲裁
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
            <h3 class="section-title">退款监管</h3>
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
              <label>商家说明</label>
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
              v-if="detail.refund.status === 'WAITING_ADMIN_ARBITRATION'"
              type="danger"
              @click="openArbitrateDialog"
            >
              发起仲裁裁定
            </el-button>
          </div>
        </section>
      </div>
      <el-empty v-else description="暂无订单详情" />
    </el-dialog>

    <el-dialog v-model="arbitrateDialogVisible" title="管理员退款仲裁" width="640px">
      <el-form label-width="110px" class="arbitrate-form">
        <el-form-item label="裁定动作">
          <el-select v-model="arbitrateForm.action" placeholder="请选择裁定动作">
            <el-option label="裁定退款通过" value="APPROVE" />
            <el-option label="裁定部分退款" value="PARTIAL" />
            <el-option label="要求补充材料" value="SUPPLEMENT" />
            <el-option label="裁定驳回申请" value="REJECT" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="['APPROVE', 'PARTIAL'].includes(arbitrateForm.action)" label="最终退款金额">
          <el-input-number v-model="arbitrateForm.finalRefundAmount" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="裁定说明">
          <el-input
            v-model="arbitrateForm.note"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请填写裁定依据、合同规则和平台说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arbitrateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingArbitrate" @click="submitArbitrate">提交裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const dialogVisible = ref(false);
const arbitrateDialogVisible = ref(false);
const submittingArbitrate = ref(false);
const detail = ref({});

const overview = computed(() => {
  const items = list.value;
  const amount = items.reduce((sum, item) => sum + Number(item.payAmount || 0), 0);
  return {
    total: items.length,
    paid: items.filter((item) => item.payStatus === "PAID" || item.payStatus === "REFUNDED").length,
    refunding: items.filter((item) => ["REFUNDING", "REFUNDED"].includes(item.orderStatus) || !!item.refundStatus).length,
    amount: `¥ ${formatAmount(amount)}`
  };
});

const arbitrateForm = reactive({
  action: "APPROVE",
  finalRefundAmount: 0,
  note: ""
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

function isCloseableOrder(status) {
  return status === "PENDING_PAY";
}

function resetSearch() {
  keyword.value = "";
  load();
}

async function load() {
  const params = keyword.value ? { keyword: keyword.value.trim() } : undefined;
  list.value = await api.get("/admin/orders", params);
}

async function view(id) {
  detail.value = await api.get(`/admin/orders/${id}`);
  dialogVisible.value = true;
}

async function handleException(row) {
  await ElMessageBox.confirm("确认关闭这个异常订单吗？系统会尝试同步状态并关闭未支付订单。", "关闭订单", {
    type: "warning"
  });
  await api.post(`/admin/orders/${row.id}/handle-exception`);
  ElMessage.success("订单处理完成");
  await load();
}

function openArbitrateDialog() {
  arbitrateForm.action = "APPROVE";
  arbitrateForm.finalRefundAmount = Number(
    detail.value.refund?.proposedRefundAmount ?? detail.value.refund?.expectedRefundAmount ?? 0
  );
  arbitrateForm.note = "";
  arbitrateDialogVisible.value = true;
}

async function submitArbitrate() {
  if (!detail.value.refund?.id) {
    return;
  }
  submittingArbitrate.value = true;
  try {
    await api.post(`/admin/refunds/${detail.value.refund.id}/arbitrate`, {
      action: arbitrateForm.action,
      finalRefundAmount: arbitrateForm.finalRefundAmount,
      note: arbitrateForm.note
    });
    ElMessage.success("退款裁定已提交");
    arbitrateDialogVisible.value = false;
    await view(detail.value.order.id);
    await load();
  } finally {
    submittingArbitrate.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.module-overview {
  margin-bottom: 0;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.overview-card {
  padding: 18px 28px 16px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e3edf8;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.overview-label {
  display: inline-block;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 10px;
}

.overview-value {
  display: block;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
  margin-bottom: 12px;
}

.overview-value.success {
  color: #16a34a;
}

.overview-value.warning {
  color: #f59e0b;
}

.overview-value.primary {
  color: #2563eb;
}

.overview-note {
  margin: 0;
  color: #8ea0bc;
  font-size: 11px;
  line-height: 1.55;
}

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

.arbitrate-form :deep(.el-select),
.arbitrate-form :deep(.el-input),
.arbitrate-form :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .overview-card {
    padding: 18px 16px;
  }

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
