<template>
  <div class="complaint-page">
    <section class="page-card complaint-create-card">
      <div class="section-head">
        <div class="section-head-left">
          <div class="head-icon-wrap">
            <el-icon :size="22"><WarningFilled /></el-icon>
          </div>
          <div>
            <p class="eyebrow">COMPLAINT CENTER</p>
            <h3>发起投诉</h3>
          </div>
        </div>
        <p class="section-copy">完成出行后，选择对应路线提交投诉，我们会在第一时间处理。</p>
      </div>

      <el-empty
        v-if="!routeOptions.length"
        description="当前没有可投诉的已完成路线"
        :image-size="120"
      />

      <el-form v-else label-position="top" class="complaint-form">
        <div class="step-indicator">
          <span class="step active">1. 选择路线</span>
          <span class="step-arrow">→</span>
          <span class="step" :class="{ active: form.orderId }">2. 填写内容</span>
          <span class="step-arrow">→</span>
          <span class="step">3. 提交投诉</span>
        </div>

        <el-form-item label="投诉路线">
          <el-select
            v-model="form.orderId"
            placeholder="请选择要投诉的路线"
            filterable
            clearable
            class="route-select"
            size="large"
          >
            <el-option
              v-for="item in routeOptions"
              :key="item.orderId"
              :label="routeOptionLabel(item)"
              :value="item.orderId"
            >
              <div class="route-option">
                <span class="route-option-name">{{ item.routeName }}</span>
                <el-tag size="small" effect="plain" round>{{ item.orderNo }}</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <div v-if="selectedRoute" class="selected-route">
          <div class="selected-route-badge">
            <el-icon :size="16"><Check /></el-icon>
            <span>已选择</span>
          </div>
          <div class="selected-route-name">{{ selectedRoute.routeName }}</div>
          <div class="selected-route-meta">
            <span>
              <el-icon :size="14"><Document /></el-icon>
              订单号：{{ selectedRoute.orderNo }}
            </span>
            <span>
              <el-icon :size="14"><Clock /></el-icon>
              完成时间：{{ formatDateTime(selectedRoute.completedTime) }}
            </span>
          </div>
        </div>

        <el-form-item label="投诉标题">
          <el-input
            v-model="form.title"
            maxlength="128"
            show-word-limit
            placeholder="用一句话概括投诉问题"
            size="large"
          />
        </el-form-item>

        <el-form-item label="投诉内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请详细描述投诉内容，提供准确信息有助于尽快解决问题"
            resize="none"
          />
        </el-form-item>

        <div class="form-actions">
          <el-button
            type="primary"
            size="large"
            :disabled="!routeOptions.length"
            :icon="Promotion"
            @click="createComplaint"
          >
            提交投诉
          </el-button>
          <span class="form-actions-hint">提交后平台工作人员将尽快受理</span>
        </div>
      </el-form>
    </section>

    <section class="page-card complaint-list-card">
      <div class="section-head">
        <div class="section-head-left">
          <div class="head-icon-wrap head-icon-secondary">
            <el-icon :size="22"><List /></el-icon>
          </div>
          <div>
            <p class="eyebrow">MY RECORDS</p>
            <h3>我的投诉</h3>
          </div>
        </div>
        <el-tag effect="plain" round size="large">{{ list.length }} 条记录</el-tag>
      </div>

      <el-empty
        v-if="!list.length"
        description="暂无投诉记录"
        :image-size="120"
      />

      <div v-else class="complaint-cards">
        <article
          v-for="item in list"
          :key="item.id"
          class="complaint-card"
          :class="`card-${complaintStatusMeta(item.status).variant}`"
          @click="$router.push(`/complaint/detail/${item.id}`)"
        >
          <div class="card-top">
            <div class="card-title-row">
              <h4>{{ item.title }}</h4>
              <el-tag
                :type="complaintStatusMeta(item.status).type"
                effect="light"
                round
                size="large"
              >
                {{ complaintStatusMeta(item.status).text }}
              </el-tag>
            </div>
            <p class="card-no">{{ item.complaintNo }}</p>
          </div>

          <div class="card-body">
            <div class="card-route">
              <el-icon :size="15"><MapLocation /></el-icon>
              <span>{{ item.routeName || "未命名路线" }}</span>
            </div>
            <div class="card-order">
              <el-icon :size="15"><Tickets /></el-icon>
              <span>{{ item.orderNo || "-" }}</span>
            </div>
          </div>

          <div class="card-footer">
            <span class="card-date">{{ formatDateTime(item.createTime) }}</span>
            <el-button text type="primary" class="card-action">
              查看详情
              <el-icon :size="14"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Promotion, WarningFilled, List, Check, Document, Clock, MapLocation, Tickets, ArrowRight } from "@element-plus/icons-vue";
import { api } from "../../api";

const list = ref([]);
const routeOptions = ref([]);
const form = reactive({
  orderId: null,
  title: "",
  content: ""
});

const selectedRoute = computed(() =>
  routeOptions.value.find((item) => Number(item.orderId) === Number(form.orderId)) || null
);

function complaintStatusMeta(status) {
  const map = {
    PENDING_ACCEPT: { text: "待受理", type: "warning", variant: "pending" },
    ACCEPTED: { text: "已受理", type: "primary", variant: "accepted" },
    ASSIGNED: { text: "处理中", type: "info", variant: "processing" },
    REPLIED: { text: "已回复", type: "success", variant: "replied" },
    CLOSED: { text: "已关闭", type: "info", variant: "closed" },
    REJECTED: { text: "已驳回", type: "danger", variant: "rejected" }
  };
  return map[status] || { text: String(status || "-"), type: "info", variant: "default" };
}

async function load() {
  const [complaints, options] = await Promise.all([
    api.get("/complaints"),
    api.get("/complaints/route-options")
  ]);
  list.value = complaints || [];
  routeOptions.value = options || [];
}

function routeOptionLabel(item) {
  return `${item.routeName || "未命名路线"} / ${item.orderNo || "-"}`;
}

async function createComplaint() {
  if (!form.orderId) {
    ElMessage.warning("请先选择要投诉的路线");
    return;
  }
  if (!form.title.trim()) {
    ElMessage.warning("请输入投诉标题");
    return;
  }
  if (!form.content.trim()) {
    ElMessage.warning("请输入投诉内容");
    return;
  }

  await api.post("/complaints", {
    orderId: Number(form.orderId),
    title: form.title.trim(),
    content: form.content.trim()
  });
  ElMessage.success("投诉已提交");
  form.orderId = null;
  form.title = "";
  form.content = "";
  await load();
}

function formatDateTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

onMounted(load);
</script>

<style scoped>
.complaint-page {
  display: grid;
  grid-template-columns: minmax(380px, 440px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.complaint-create-card,
.complaint-list-card {
  padding: 28px 30px;
  border-radius: 24px;
}

.complaint-create-card {
  position: sticky;
  top: 16px;
}

.complaint-list-card {
  min-width: 0;
}

/* --- section head --- */
.section-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 22px;
}

.section-head-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.head-icon-wrap {
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #d97706;
  flex-shrink: 0;
}

.head-icon-secondary {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #2563eb;
}

.eyebrow {
  margin: 0 0 4px;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
}

.section-copy {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
  font-size: 14px;
}

/* --- step indicator --- */
.step-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  padding: 10px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.step {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
  white-space: nowrap;
}

.step.active {
  color: #0f172a;
  font-weight: 700;
}

.step-arrow {
  color: #cbd5e1;
  font-size: 13px;
  margin: 0 2px;
}

/* --- form --- */
.complaint-form {
  display: grid;
  gap: 6px;
}

.complaint-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 600;
}

.route-select {
  width: 100%;
}

.route-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-option-name {
  color: #0f172a;
  font-weight: 500;
}

.selected-route {
  margin-bottom: 12px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #f0fdf4 100%);
  border: 1px solid #dbeafe;
}

.selected-route-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  background: #22c55e;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 10px;
}

.selected-route-name {
  color: #0f172a;
  font-size: 17px;
  font-weight: 700;
}

.selected-route-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.selected-route-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 4px;
}

.form-actions-hint {
  color: #94a3b8;
  font-size: 12px;
}

/* --- complaint cards --- */
.complaint-cards {
  display: grid;
  gap: 12px;
}

.complaint-card {
  padding: 20px 22px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.complaint-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
  border-color: #bfdbfe;
}

.card-top {
  margin-bottom: 14px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title-row h4 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.4;
}

.card-no {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
  font-family: "SF Mono", "JetBrains Mono", monospace;
}

.card-body {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.card-route,
.card-order {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 14px;
}

.card-route .el-icon {
  color: #f59e0b;
}

.card-order .el-icon {
  color: #64748b;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.card-date {
  color: #94a3b8;
  font-size: 13px;
}

.card-action {
  font-weight: 600;
}

/* --- card status variants --- */
.card-pending {
  border-left: 4px solid #f59e0b;
}

.card-accepted {
  border-left: 4px solid #3b82f6;
}

.card-processing {
  border-left: 4px solid #6366f1;
}

.card-replied {
  border-left: 4px solid #22c55e;
}

.card-closed {
  border-left: 4px solid #94a3b8;
  opacity: 0.75;
}

.card-rejected {
  border-left: 4px solid #ef4444;
  opacity: 0.75;
}

/* --- responsive --- */
@media (max-width: 1100px) {
  .complaint-page {
    grid-template-columns: 1fr;
  }

  .complaint-create-card {
    position: static;
  }
}

@media (max-width: 640px) {
  .complaint-create-card,
  .complaint-list-card {
    padding: 20px 18px;
  }

  .card-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
