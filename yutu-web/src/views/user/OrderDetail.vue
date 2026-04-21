<template>
  <div v-if="order.id" class="order-detail-page">
    <section class="page-card summary-card">
      <div class="summary-main">
        <div class="summary-head">
          <div class="summary-identity">
            <p class="eyebrow">ORDER DETAIL</p>
            <div class="headline-tags">
              <el-tag class="status-chip" :type="orderStatusTag(order.orderStatus).type" effect="light" round>
                {{ orderStatusTag(order.orderStatus).text }}
              </el-tag>
              <el-tag class="status-chip" :type="contractStatusTag(order.contractStatus).type" effect="light" round>
                {{ contractStatusTag(order.contractStatus).text }}
              </el-tag>
              <el-tag
                v-if="refund.id"
                class="status-chip"
                :type="refundStatusTag(refund.status).type"
                effect="light"
                round
              >
                {{ refundStatusTag(refund.status).text }}
              </el-tag>
            </div>
            <p class="summary-subtitle">下单时间 {{ formatDateTime(order.createTime) }}</p>
          </div>

          <div class="summary-actions">
            <el-button v-if="order.payStatus === 'UNPAID'" type="primary" @click="goPay">
              去支付
            </el-button>
            <el-button v-if="canApplyRefund" type="danger" @click="openRefundDialog">
              申请退款
            </el-button>
            <el-button plain @click="$router.push('/order/list')">返回订单列表</el-button>
          </div>
        </div>

        <div class="summary-body">
          <div class="business-panel">
            <article class="business-card route-card">
              <div class="route-card-top">
                <div class="route-copy">
                  <span class="card-kicker">当前路线</span>
                  <strong class="route-title">{{ routeInfo.routeName || "-" }}</strong>
                </div>
                <div class="order-mini">
                  <span class="order-mini-label">订单号</span>
                  <strong class="order-mini-no">{{ order.orderNo }}</strong>
                </div>
              </div>
              <p>{{ routeInfo.summary || "暂无路线简介" }}</p>
              <div class="meta-strip">
                <div class="meta-item">
                  <span class="meta-label">出行人数</span>
                  <strong class="meta-value">{{ order.travelerCount || 0 }} 人</strong>
                </div>
                <div class="meta-item">
                  <span class="meta-label">出发时间</span>
                  <strong class="meta-value">{{ departureDate.departDate || "-" }}</strong>
                </div>
              </div>
            </article>

            <div class="status-progress">
              <div class="status-progress-title">订单进度</div>
              <div class="status-track">
                <div
                  v-for="item in orderProgressSteps"
                  :key="item.key"
                  class="progress-step"
                  :class="[`is-${item.state}`, { 'is-current': item.isCurrent }]"
                >
                  <div class="progress-dot">{{ item.icon }}</div>
                  <div class="progress-copy">
                    <span class="progress-name">{{ item.label }}</span>
                    <span class="progress-desc">{{ item.value }}</span>
                    <span class="progress-time">{{ item.time }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <aside class="summary-side">
            <div class="amount-card">
              <span class="amount-label">支付金额</span>
              <div class="amount-value">¥ {{ formatAmount(order.payAmount) }}</div>
              <div class="amount-breakdown">
                ({{ order.travelerCount || 0 }}人 × ¥{{ formatAmount(singleTravelerPrice) }})
              </div>
              <p class="amount-note">
                {{ refund.id ? "退款处理将以最终审核方案为准" : "当前订单金额已按实际出行人数结算" }}
              </p>
            </div>
          </aside>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <article class="page-card">
        <div class="section-head">
          <div>
            <h3>路线信息</h3>
            <p>查看当前订单绑定的路线和出发安排。</p>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="路线名称">{{ routeInfo.routeName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="出发日期">{{ departureDate.departDate || "-" }}</el-descriptions-item>
          <el-descriptions-item label="路线简介">{{ routeInfo.summary || "-" }}</el-descriptions-item>
          <el-descriptions-item label="支付金额">¥ {{ formatAmount(order.payAmount) }}</el-descriptions-item>
        </el-descriptions>
      </article>

      <article class="page-card">
        <div class="section-head">
          <div>
            <h3>出行人信息</h3>
            <p>当前订单绑定的出行人名单。</p>
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

      <article class="page-card">
        <div class="section-head">
          <div>
            <h3>合同信息</h3>
            <p>查看合同并继续签署流程。</p>
          </div>
          <span>{{ contracts.length }} 份</span>
        </div>

        <div v-if="contracts.length" class="contract-list">
          <div v-for="row in contracts" :key="row.id" class="contract-item">
            <div class="contract-main">
              <strong>{{ row.contractNo }}</strong>
              <span>{{ mapContractStatus(row.signStatus) }}</span>
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
        <el-empty v-else description="当前订单暂无合同信息" />
      </article>

      <article class="page-card refund-card">
        <div class="section-head">
          <div>
            <h3>退款生命周期</h3>
            <p>用户发起、商家审核、管理员仲裁和退款执行全过程都会留痕。</p>
          </div>
          <el-tag v-if="refund.id" :type="refundStatusTag(refund.status).type" effect="light" round>
            {{ refundStatusTag(refund.status).text }}
          </el-tag>
        </div>

        <template v-if="refund.id">
          <div class="refund-summary">
            <div class="refund-metric">
              <span>退款单号</span>
              <strong>{{ refund.refundNo }}</strong>
            </div>
            <div class="refund-metric">
              <span>退款类型</span>
              <strong>{{ refundTypeLabel(refund.refundType) }}</strong>
            </div>
            <div class="refund-metric">
              <span>预估退款</span>
              <strong>¥ {{ formatAmount(refund.expectedRefundAmount) }}</strong>
            </div>
            <div class="refund-metric">
              <span>当前方案</span>
              <strong>¥ {{ formatAmount(refund.finalRefundAmount ?? refund.proposedRefundAmount ?? refund.expectedRefundAmount) }}</strong>
            </div>
          </div>

          <div class="refund-note-list">
            <div class="refund-note">
              <label>退款原因</label>
              <p>{{ refund.refundReason || "-" }}</p>
            </div>
            <div class="refund-note">
              <label>规则说明</label>
              <p>{{ refund.policyNote || "-" }}</p>
            </div>
            <div v-if="refund.merchantNote" class="refund-note">
              <label>商家意见</label>
              <p>{{ refund.merchantNote }}</p>
            </div>
            <div v-if="refund.adminNote" class="refund-note">
              <label>管理员裁定</label>
              <p>{{ refund.adminNote }}</p>
            </div>
          </div>

          <div v-if="refund.evidenceUrlList?.length" class="evidence-grid">
            <div v-for="url in refund.evidenceUrlList" :key="url" class="evidence-item">
              <img :src="url" alt="退款凭证" />
            </div>
          </div>

          <div class="timeline">
            <div v-for="flow in refundFlows" :key="`${flow.id}-${flow.createTime}`" class="timeline-item">
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
              v-if="refund.status === 'WAITING_USER_SUPPLEMENT'"
              type="primary"
              @click="supplementDialogVisible = true"
            >
              补充材料
            </el-button>
            <el-button
              v-if="refund.status === 'WAITING_USER_CONFIRM'"
              type="success"
              @click="confirmRefundProposal(true)"
            >
              接受方案
            </el-button>
            <el-button
              v-if="refund.status === 'WAITING_USER_CONFIRM'"
              type="danger"
              plain
              @click="confirmRefundProposal(false)"
            >
              申请仲裁
            </el-button>
          </div>
        </template>

        <el-empty v-else description="当前订单还没有退款申请，需要时可在这里发起退款。" />
      </article>
    </section>

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="720px">
      <el-form label-width="96px" class="refund-form">
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
            placeholder="请填写退款原因"
          />
        </el-form-item>
        <el-form-item label="凭证材料">
          <div class="evidence-stack">
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadRefundEvidence">
              <el-button type="primary" plain>上传凭证</el-button>
            </el-upload>
            <div v-if="refundForm.evidenceUrls.length" class="evidence-grid">
              <div v-for="(url, index) in refundForm.evidenceUrls" :key="url" class="evidence-item">
                <img :src="url" alt="退款凭证" />
                <el-button text type="danger" @click="removeRefundEvidence(index)">移除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="退款账户">
          <div class="refund-account-row">
            <el-select v-model="refundForm.refundAccountType" placeholder="账户类型">
              <el-option label="原路退回" value="ORIGINAL" />
              <el-option label="支付宝" value="ALIPAY" />
              <el-option label="银行卡" value="BANK_CARD" />
            </el-select>
            <el-input v-model="refundForm.refundAccountNo" placeholder="如需指定账户，请填写账号" />
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

    <el-dialog v-model="supplementDialogVisible" title="补充材料" width="640px">
      <el-form label-width="96px" class="refund-form">
        <el-form-item label="补充说明">
          <el-input
            v-model="supplementForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请一次性补充完整材料和说明"
          />
        </el-form-item>
        <el-form-item label="新增凭证">
          <div class="evidence-stack">
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadSupplementEvidence">
              <el-button type="primary" plain>上传凭证</el-button>
            </el-upload>
            <div v-if="supplementForm.evidenceUrls.length" class="evidence-grid">
              <div v-for="(url, index) in supplementForm.evidenceUrls" :key="url" class="evidence-item">
                <img :src="url" alt="补充凭证" />
                <el-button text type="danger" @click="removeSupplementEvidence(index)">移除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplementDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingSupplement" @click="submitSupplement">提交补充材料</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { api } from "../../api";

const route = useRoute();
const router = useRouter();

const detail = ref({});
const refundDialogVisible = ref(false);
const supplementDialogVisible = ref(false);
const submittingRefund = ref(false);
const submittingSupplement = ref(false);
const refundEstimate = ref(null);

const refundForm = reactive({
  refundType: "",
  refundReason: "",
  evidenceUrls: [],
  refundAccountType: "ORIGINAL",
  refundAccountNo: ""
});

const supplementForm = reactive({
  content: "",
  evidenceUrls: []
});

const order = computed(() => detail.value.order || {});
const routeInfo = computed(() => detail.value.route || {});
const departureDate = computed(() => detail.value.departureDate || {});
const travelers = computed(() => detail.value.travelers || []);
const contracts = computed(() => detail.value.contracts || []);
const refund = computed(() => detail.value.refund || {});
const refundFlows = computed(() => detail.value.refundFlows || []);
const canApplyRefund = computed(() => order.value.payStatus === "PAID" && !refund.value.id);
const singleTravelerPrice = computed(() => {
  const count = Number(order.value.travelerCount) || 0;
  const payAmount = Number(order.value.payAmount) || 0;
  if (!count) {
    return payAmount;
  }
  return payAmount / count;
});
const orderProgressSteps = computed(() => {
  const firstContract = contracts.value[0] || {};
  const steps = [
    {
      key: "order",
      label: "订单",
      value: orderStatusTag(order.value.orderStatus).text,
      time: formatShortTime(order.value.createTime),
      icon: "✓",
      state: statusLevel(order.value.orderStatus, ["COMPLETED", "REFUNDED", "EXPIRED"], ["REFUNDING", "PENDING_TRAVEL"], ["PENDING_PAY"])
    },
    {
      key: "pay",
      label: "支付",
      value: payStatusTag(order.value.payStatus).text,
      time: formatShortTime(order.value.payTime || order.value.paySuccessTime || order.value.updateTime || order.value.createTime),
      icon: "✓",
      state: statusLevel(order.value.payStatus, ["PAID", "REFUNDED"], ["FAILED"], ["UNPAID"])
    },
    {
      key: "contract",
      label: "合同",
      value: mapContractStatus(order.value.contractStatus),
      time: formatShortTime(firstContract.signTime || firstContract.updateTime || firstContract.createTime),
      icon: "✓",
      state: statusLevel(order.value.contractStatus, ["SIGNED"], ["GENERATED"], ["UNSIGNED"])
    },
    {
      key: "refund",
      label: "退款",
      value: refund.value.id ? refundStatusTag(refund.value.status).text : "未申请",
      time: refund.value.id ? formatShortTime(refund.value.updateTime || refund.value.createTime) : "未申请",
      icon: refund.value.id ? "✓" : "○",
      state: refund.value.id
        ? statusLevel(refund.value.status, ["REFUND_COMPLETED"], ["REFUND_PROCESSING", "WAITING_REFUND_EXECUTION"], ["WAITING_MERCHANT_REVIEW", "WAITING_USER_CONFIRM", "WAITING_USER_SUPPLEMENT", "WAITING_ADMIN_ARBITRATION"])
        : "idle"
    }
  ];

  const currentIndex = [...steps].reverse().findIndex((item) => item.state !== "idle");
  const activeIndex = currentIndex === -1 ? 0 : steps.length - 1 - currentIndex;

  return steps.map((item, index) => ({
    ...item,
    isCurrent: index === activeIndex
  }));
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

function formatShortTime(value) {
  if (!value) {
    return "--";
  }
  const normalized = String(value).replace("T", " ").slice(0, 16);
  if (normalized.length < 16) {
    return normalized;
  }
  return normalized.slice(5);
}

function orderStatusTag(status) {
  const map = {
    PENDING_PAY: { text: "待支付", type: "warning" },
    PENDING_TRAVEL: { text: "待出行", type: "primary" },
    COMPLETED: { text: "✓ 已完成", type: "success" },
    EXPIRED: { text: "✓ 已失效", type: "danger" },
    CANCELLED: { text: "已取消", type: "info" },
    REFUNDING: { text: "退款中", type: "warning" },
    REFUNDED: { text: "✓ 已退款", type: "danger" }
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

function contractStatusTag(status) {
  const map = {
    GENERATED: { text: "已生成", type: "info" },
    UNSIGNED: { text: "待签署", type: "warning" },
    SIGNED: { text: "✓ 合同已签署", type: "success" }
  };
  return map[status] || { text: status || "-", type: "info" };
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

function mapContractStatus(status) {
  const map = {
    GENERATED: "已生成",
    UNSIGNED: "待签署",
    SIGNED: "已签署"
  };
  return map[status] || status || "-";
}

function statusLevel(status, successList = [], runningList = [], pendingList = []) {
  if (successList.includes(status)) {
    return "success";
  }
  if (runningList.includes(status)) {
    return "running";
  }
  if (pendingList.includes(status)) {
    return "pending";
  }
  return "idle";
}

async function loadDetail() {
  detail.value = await api.get(`/orders/${route.params.id}`);
}

function goPay() {
  router.push(`/order/pay/${route.params.id}`);
}

function resetRefundForm() {
  refundForm.refundType = "";
  refundForm.refundReason = "";
  refundForm.evidenceUrls = [];
  refundForm.refundAccountType = "ORIGINAL";
  refundForm.refundAccountNo = "";
  refundEstimate.value = null;
}

function openRefundDialog() {
  resetRefundForm();
  refundDialogVisible.value = true;
}

async function loadRefundEstimate() {
  if (!refundForm.refundType) {
    refundEstimate.value = null;
    return;
  }
  refundEstimate.value = await api.get(`/orders/${route.params.id}/refund-estimate`, {
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

async function uploadSupplementEvidence(options) {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const result = await api.upload("/files/upload", formData);
    supplementForm.evidenceUrls.push(result.url);
    ElMessage.success("补充凭证上传成功");
    options.onSuccess?.(result);
  } catch (error) {
    options.onError?.(error);
  }
}

function removeRefundEvidence(index) {
  refundForm.evidenceUrls.splice(index, 1);
}

function removeSupplementEvidence(index) {
  supplementForm.evidenceUrls.splice(index, 1);
}

async function submitRefund() {
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
    await api.post(`/orders/${route.params.id}/refunds`, {
      refundType: refundForm.refundType,
      refundReason: refundForm.refundReason,
      evidenceUrls: refundForm.evidenceUrls,
      refundAccountType: refundForm.refundAccountType,
      refundAccountNo: refundForm.refundAccountNo
    });
    ElMessage.success("退款申请已提交");
    refundDialogVisible.value = false;
    await loadDetail();
  } finally {
    submittingRefund.value = false;
  }
}

async function submitSupplement() {
  if (!refund.value.id) {
    return;
  }
  if (!supplementForm.content.trim() && !supplementForm.evidenceUrls.length) {
    ElMessage.warning("请补充说明或上传材料");
    return;
  }

  submittingSupplement.value = true;
  try {
    await api.post(`/orders/refunds/${refund.value.id}/supplement`, {
      content: supplementForm.content,
      evidenceUrls: supplementForm.evidenceUrls
    });
    ElMessage.success("补充材料已提交");
    supplementDialogVisible.value = false;
    supplementForm.content = "";
    supplementForm.evidenceUrls = [];
    await loadDetail();
  } finally {
    submittingSupplement.value = false;
  }
}

async function confirmRefundProposal(accepted) {
  if (!refund.value.id) {
    return;
  }
  const title = accepted ? "接受退款方案" : "申请管理员仲裁";
  await ElMessageBox.confirm(
    accepted ? "确认接受当前退款方案吗？" : "确认申请管理员介入仲裁吗？",
    title,
    { type: accepted ? "success" : "warning" }
  );
  await api.post(`/orders/refunds/${refund.value.id}/confirm`, {
    accepted,
    note: accepted ? "用户接受退款方案" : "用户不同意商家方案，申请仲裁"
  });
  ElMessage.success(accepted ? "已接受退款方案" : "已提交仲裁申请");
  await loadDetail();
}

onMounted(loadDetail);
</script>

<style scoped>
.order-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  padding: 26px 28px;
  border: 1px solid #e4ebf4;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.06), transparent 24%),
    linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}

.summary-identity {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #64748b;
  letter-spacing: 0.16em;
  font-size: 12px;
  font-weight: 700;
}

.headline-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.headline-tags :deep(.status-chip) {
  height: 34px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
}

.summary-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 16px;
  line-height: 1.8;
}

.summary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.summary-body {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 20px;
  align-items: start;
}

.business-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.business-card {
  padding: 20px 22px;
  border-radius: 20px;
  border: 1px solid #deebf7;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.route-card {
  min-height: 198px;
}

.route-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.route-copy {
  flex: 1;
  min-width: 0;
}

.order-mini {
  flex: 0 0 240px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.order-mini-label {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.order-mini-no {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px !important;
  font-weight: 800;
  line-height: 1.45;
  word-break: break-all;
}

.card-kicker {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.route-title {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.25;
}

.business-card p {
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.8;
  font-size: 16px;
}

.meta-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #e2e8f0;
}

.meta-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.meta-label {
  color: #64748b;
  font-size: 13px;
}

.meta-value {
  color: #0f172a;
  font-size: 18px !important;
  font-weight: 800;
  margin-top: 0 !important;
}

.status-progress {
  padding: 20px 22px 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f7fbff 0%, #ffffff 100%);
  border: 1px solid #deebf7;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.status-progress-title {
  margin-bottom: 16px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.status-track {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.progress-step {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 10px 14px 0;
  min-height: 106px;
}

.progress-step::after {
  content: "";
  position: absolute;
  top: 10px;
  left: calc(100% - 8px);
  width: calc(100% - 6px);
  height: 2px;
  background: #d7e3f1;
}

.progress-step:last-child::after {
  display: none;
}

.progress-dot {
  position: relative;
  z-index: 1;
  width: 24px;
  height: 24px;
  margin-top: 2px;
  border-radius: 999px;
  background: #cbd5e1;
  box-shadow: 0 0 0 6px #ffffff;
  display: grid;
  place-items: center;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.progress-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 16px;
  min-width: 0;
}

.progress-name {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.progress-desc {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.progress-time {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.4;
}

.progress-step.is-current .progress-name {
  font-size: 16px;
  font-weight: 800;
}

.progress-step.is-current .progress-copy {
  background: linear-gradient(180deg, #ffffff 0%, #eef6ff 100%);
  border: 1px solid #dbeafe;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.08);
}

.progress-step.is-current .progress-dot {
  transform: scale(1.08);
  box-shadow: 0 0 0 8px #ffffff, 0 0 0 12px rgba(59, 130, 246, 0.08);
}

.progress-step.is-success .progress-dot {
  background: #22c55e;
}

.progress-step.is-success::after {
  background: linear-gradient(90deg, #22c55e 0%, #bbf7d0 100%);
}

.progress-step.is-running .progress-dot {
  background: #3b82f6;
}

.progress-step.is-running::after {
  background: linear-gradient(90deg, #3b82f6 0%, #bfdbfe 100%);
}

.progress-step.is-pending .progress-dot {
  background: #f59e0b;
}

.progress-step.is-idle .progress-dot {
  background: #cbd5e1;
}

.fact-item span,
.amount-label {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.fact-item strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.summary-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-self: stretch;
}

.amount-card {
  padding: 24px 24px 22px;
  border-radius: 24px;
  border: 1px solid #ffd591;
  background: linear-gradient(180deg, #fff7e6 0%, #ffffff 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.amount-value {
  margin-top: 10px;
  color: #ff8a00;
  font-size: clamp(38px, 3vw, 54px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.03em;
}

.amount-breakdown {
  margin-top: 18px;
  color: #334155;
  font-size: 18px;
  font-weight: 700;
}

.amount-note {
  margin: 14px 0 0;
  color: #7c5a45;
  font-size: 13px;
  line-height: 1.7;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.refund-card {
  grid-column: 1 / -1;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.section-head p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.traveler-list,
.contract-list,
.refund-note-list,
.timeline {
  display: grid;
  gap: 12px;
}

.traveler-item,
.contract-item,
.refund-note,
.timeline-content {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.traveler-name,
.contract-main strong {
  color: #0f172a;
  font-size: 17px;
  font-weight: 700;
}

.traveler-meta,
.contract-main {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
}

.contract-actions {
  margin-top: 10px;
  display: flex;
  gap: 12px;
}

.refund-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.refund-metric {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid #dbe5f0;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.refund-metric span,
.refund-note label {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.refund-metric strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 20px;
}

.refund-note p,
.timeline-content p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.8;
}

.timeline-item {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.timeline-dot {
  width: 12px;
  height: 12px;
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
  font-size: 15px;
}

.timeline-meta span {
  color: #94a3b8;
  font-size: 12px;
}

.refund-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.refund-form :deep(.el-select),
.refund-form :deep(.el-input),
.refund-form :deep(.el-textarea) {
  width: 100%;
}

.refund-account-row {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 12px;
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
  margin-top: 12px;
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

.estimate-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f7ff 100%);
  border: 1px solid #dbeafe;
}

.estimate-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.estimate-label {
  color: #64748b;
  font-size: 13px;
}

.estimate-card p {
  grid-column: 1 / -1;
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .summary-body,
  .content-grid,
  .status-track,
  .refund-summary,
  .estimate-card,
  .refund-account-row {
    grid-template-columns: 1fr;
  }

  .summary-head {
    flex-direction: column;
  }

  .summary-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .route-card-top {
    flex-direction: column;
  }

  .order-mini {
    flex: initial;
    width: 100%;
  }

  .meta-strip {
    flex-direction: column;
    gap: 12px;
  }
}

@media (max-width: 760px) {
  .summary-card {
    padding: 20px;
  }

  .summary-head h1 {
    font-size: 30px;
  }

  .status-card strong,
  .fact-item strong {
    font-size: 18px;
  }

  .amount-value {
    font-size: 36px;
  }
}
</style>
