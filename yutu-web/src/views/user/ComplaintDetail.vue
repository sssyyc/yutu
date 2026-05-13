<template>
  <div class="complaint-detail-page">
    <section v-if="data.complaint" class="hero-card">
      <div class="hero-left">
        <p class="eyebrow">COMPLAINT DETAIL</p>
        <h1>{{ data.complaint.title }}</h1>
        <div class="hero-meta">
          <span class="hero-no">{{ data.complaint.complaintNo }}</span>
          <span class="hero-divider">·</span>
          <el-tag
            :type="statusMeta.type"
            effect="light"
            round
            size="large"
          >
            {{ statusMeta.text }}
          </el-tag>
        </div>
      </div>
      <div class="hero-right">
        <div class="status-circle" :class="`circle-${statusMeta.variant}`">
          <el-icon :size="28"><component :is="statusMeta.icon" /></el-icon>
        </div>
        <span class="status-label">{{ statusMeta.text }}</span>
      </div>
    </section>

    <section v-if="data.complaint" class="detail-grid">
      <article class="info-card">
        <div class="card-header">
          <div class="card-icon-wrap">
            <el-icon :size="18"><Document /></el-icon>
          </div>
          <h3>投诉信息</h3>
        </div>

        <div class="info-rows">
          <div class="info-row">
            <span class="info-label">投诉路线</span>
            <span class="info-value">{{ data.complaint.routeName || "-" }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">关联订单</span>
            <span class="info-value">{{ data.complaint.orderNo || "-" }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">投诉类型</span>
            <span class="info-value">{{ data.complaint.complaintType || "未分类" }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">提交时间</span>
            <span class="info-value">{{ formatDateTime(data.complaint.createTime) }}</span>
          </div>
        </div>

        <div class="info-content-block">
          <h4>投诉内容</h4>
          <p>{{ data.complaint.content }}</p>
        </div>

        <div v-if="data.complaint.resultContent" class="info-content-block result-block">
          <h4>
            <el-icon :size="16"><CircleCheckFilled /></el-icon>
            处理结果
          </h4>
          <p>{{ data.complaint.resultContent }}</p>
        </div>
      </article>

      <article class="timeline-card">
        <div class="card-header">
          <div class="card-icon-wrap card-icon-secondary">
            <el-icon :size="18"><List /></el-icon>
          </div>
          <h3>流程记录</h3>
          <span class="flow-count">{{ (data.flows || []).length }} 步</span>
        </div>

        <div v-if="data.flows?.length" class="timeline-wrap">
          <el-timeline>
            <el-timeline-item
              v-for="flow in data.flows"
              :key="flow.id"
              :timestamp="formatDateTime(flow.createTime)"
              :type="flowType(flow.actionType)"
              :icon="flowIcon(flow.actionType)"
              size="large"
              placement="top"
            >
              <div class="flow-card">
                <p class="flow-action">{{ flow.actionType || "状态更新" }}</p>
                <p class="flow-content">{{ flow.actionContent || flow.operatorRole || "无备注" }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <el-empty v-else description="暂无流程记录" :image-size="80" />
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { Document, List, CircleCheckFilled, Clock, WarningFilled, Check, CloseBold, ChatLineSquare } from "@element-plus/icons-vue";
import { api } from "../../api";

const route = useRoute();
const data = ref({});

const statusMeta = computed(() => {
  const map = {
    PENDING_ACCEPT: { text: "待受理", type: "warning", variant: "pending", icon: Clock },
    ACCEPTED: { text: "已受理", type: "primary", variant: "accepted", icon: Check },
    ASSIGNED: { text: "处理中", type: "info", variant: "processing", icon: ChatLineSquare },
    REPLIED: { text: "已回复", type: "success", variant: "replied", icon: CircleCheckFilled },
    CLOSED: { text: "已关闭", type: "info", variant: "closed", icon: CloseBold },
    REJECTED: { text: "已驳回", type: "danger", variant: "rejected", icon: WarningFilled }
  };
  return map[data.value.complaint?.status] || { text: "-", type: "info", variant: "default", icon: Clock };
});

function flowType(actionType) {
  const map = {
    SUBMIT: "primary",
    ACCEPT: "primary",
    ASSIGN: "warning",
    REPLY: "success",
    CLOSE: "info",
    REJECT: "danger"
  };
  return map[actionType] || "info";
}

function flowIcon(actionType) {
  const map = {
    SUBMIT: ChatLineSquare,
    ACCEPT: Check,
    ASSIGN: Clock,
    REPLY: CircleCheckFilled,
    CLOSE: CloseBold,
    REJECT: WarningFilled
  };
  return map[actionType] || Clock;
}

function formatDateTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

onMounted(async () => {
  data.value = await api.get(`/complaints/${route.params.id}`);
});
</script>

<style scoped>
.complaint-detail-page {
  display: grid;
  gap: 20px;
}

/* --- hero --- */
.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 32px 34px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 60%, #fefce8 100%);
  border: 1px solid #f1f5f9;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.hero-left {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.hero-left h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(24px, 2.5vw, 34px);
  line-height: 1.22;
  word-break: break-word;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.hero-no {
  color: #64748b;
  font-size: 13px;
  font-family: "SF Mono", "JetBrains Mono", monospace;
}

.hero-divider {
  color: #cbd5e1;
}

.hero-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.status-circle {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
}

.circle-pending {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.circle-accepted {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.circle-processing {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}

.circle-replied {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.circle-closed {
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.circle-rejected {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.circle-default {
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.status-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

/* --- detail grid --- */
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 1fr);
  gap: 20px;
  align-items: start;
}

.info-card,
.timeline-card {
  padding: 24px 28px;
  border-radius: 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
}

/* --- card header --- */
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.card-icon-wrap {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #fef3c7;
  color: #d97706;
  flex-shrink: 0;
}

.card-icon-secondary {
  background: #dbeafe;
  color: #2563eb;
}

.card-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  flex: 1;
}

.flow-count {
  color: #94a3b8;
  font-size: 13px;
}

/* --- info rows --- */
.info-rows {
  display: grid;
  gap: 2px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #e8ecf1;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #94a3b8;
  font-size: 14px;
}

.info-value {
  color: #0f172a;
  font-weight: 600;
  font-size: 14px;
  text-align: right;
}

/* --- content blocks --- */
.info-content-block {
  margin-top: 18px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e8ecf1;
}

.info-content-block h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 16px;
}

.info-content-block p {
  margin: 0;
  color: #475569;
  line-height: 1.9;
  font-size: 15px;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-block {
  background: linear-gradient(135deg, #f0fdf4, #f8fafc);
  border-color: #bbf7d0;
}

.result-block h4 {
  color: #166534;
}

.result-block p {
  color: #14532d;
}

/* --- timeline --- */
.timeline-wrap {
  padding: 4px 0;
}

.timeline-wrap :deep(.el-timeline-item__timestamp) {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
}

.flow-card {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.flow-action {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.flow-content {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

/* --- responsive --- */
@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-right {
    flex-direction: row;
    gap: 14px;
  }

  .status-circle {
    width: 48px;
    height: 48px;
  }
}

@media (max-width: 640px) {
  .hero-card,
  .info-card,
  .timeline-card {
    padding: 20px 18px;
  }

  .hero-left h1 {
    font-size: 22px;
  }
}
</style>
