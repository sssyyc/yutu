<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="Complaint Center" title="投诉处理" />

    <section class="page-card complaint-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">投诉总量</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">商户侧收到的全部投诉单。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">待商户处理</span>
          <strong class="overview-value warning">{{ overview.needReply }}</strong>
          <p class="overview-note">平台已分派、等待商户回复的投诉。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">已回复待判定</span>
          <strong class="overview-value primary">{{ overview.replied }}</strong>
          <p class="overview-note">商户已提交说明，等待平台判定。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">已完成</span>
          <strong class="overview-value success">{{ overview.finished }}</strong>
          <p class="overview-note">已经完成反馈并形成处理结论。</p>
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
            placeholder="请输入投诉号、路线或投诉标题"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-right">
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
        <el-table-column prop="complaintNo" label="投诉单号" min-width="260" show-overflow-tooltip />
        <el-table-column prop="routeName" label="投诉路线" min-width="220" show-overflow-tooltip />
        <el-table-column prop="title" label="投诉标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="投诉类型" width="130">
          <template #default="{ row }">
            {{ complaintTypeText(row.complaintType) }}
          </template>
        </el-table-column>
        <el-table-column label="处理状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light" round>
              {{ statusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button text type="primary" @click="view(row.id)">详情</el-button>
            <el-button
              text
              type="success"
              :disabled="!canReply(row.status)"
              @click="reply(row.id)"
            >
              回复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="replyDialog" title="回复投诉" width="620px">
      <div class="reply-dialog-copy">
        请一次性说明订单核验情况、合同依据、资源损失或处理方案，平台将据此进行后续判定。
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="6"
        maxlength="300"
        show-word-limit
        placeholder="请输入投诉处理说明"
      />
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="replyDialog = false">取消</el-button>
          <el-button type="primary" @click="submitReply">提交回复</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="投诉详情" width="900px">
      <div v-if="detail.complaint" class="complaint-detail">
        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <p class="detail-kicker">Complaint Detail</p>
              <h3>{{ detail.complaint.title || "投诉详情" }}</h3>
            </div>
            <el-tag :type="statusMeta(detail.complaint.status).type" effect="light" round>
              {{ statusMeta(detail.complaint.status).text }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border class="detail-grid">
            <el-descriptions-item label="投诉单号">
              {{ detail.complaint.complaintNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉类型">
              {{ complaintTypeText(detail.complaint.complaintType) }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉路线">
              {{ detail.complaint.routeName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="关联订单">
              {{ detail.complaint.orderNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDateTime(detail.complaint.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDateTime(detail.complaint.updateTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="用户投诉内容" :span="2">
              <div class="detail-text-block">{{ detail.complaint.content || "-" }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="处理结果" :span="2">
              <div class="detail-text-block">{{ detail.complaint.resultContent || "当前尚未形成最终处理结果。" }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <p class="detail-kicker">Processing Flow</p>
              <h3>处理记录</h3>
            </div>
          </div>

          <el-empty v-if="!(detail.flows || []).length" description="当前暂无处理记录" />

          <el-timeline v-else class="detail-timeline">
            <el-timeline-item
              v-for="flow in detail.flows"
              :key="flow.id"
              :timestamp="formatDateTime(flow.createTime)"
              :type="timelineType(flow.actionType)"
            >
              <div class="flow-card">
                <div class="flow-card-head">
                  <span class="flow-card-title">
                    {{ operatorRoleText(flow.operatorRole) }} · {{ flowActionText(flow.actionType) }}
                  </span>
                </div>
                <p class="flow-card-content">{{ flow.actionContent || "暂无补充说明" }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>

      <el-empty v-else description="未获取到投诉详情" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const detail = ref({});
const detailDialog = ref(false);
const replyDialog = ref(false);
const replyText = ref("");
const currentId = ref(null);

const STATUS_META = {
  PENDING_ACCEPT: { text: "待平台受理", type: "warning" },
  ACCEPTED: { text: "平台已受理", type: "primary" },
  ASSIGNED: { text: "待商户处理", type: "warning" },
  MERCHANT_REPLIED: { text: "商户已回复", type: "success" },
  JUDGED: { text: "平台已判定", type: "danger" },
  FINISHED: { text: "已完成", type: "success" },
  CLOSED: { text: "已关闭", type: "info" }
};

const COMPLAINT_TYPE_TEXT = {
  SERVICE: "服务问题",
  TRAFFIC: "交通问题",
  HOTEL: "住宿问题",
  TICKET: "门票问题",
  OTHER: "其他问题"
};

const FLOW_ACTION_TEXT = {
  CREATE: "发起投诉",
  ACCEPT: "平台受理",
  ASSIGN: "平台分派",
  REPLY: "商户回复",
  JUDGE: "平台判定",
  FINISH: "处理完成",
  CLOSE: "关闭投诉"
};

const OPERATOR_ROLE_TEXT = {
  USER: "用户",
  MERCHANT: "商户",
  ADMIN: "平台"
};

const overview = computed(() => ({
  total: list.value.length,
  needReply: list.value.filter((item) => item.status === "ASSIGNED").length,
  replied: list.value.filter((item) => item.status === "MERCHANT_REPLIED").length,
  finished: list.value.filter((item) => ["FINISHED", "CLOSED"].includes(item.status)).length
}));

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/merchant/complaints", params);
}

async function view(id) {
  detail.value = await api.get(`/merchant/complaints/${id}`);
  detailDialog.value = true;
}

function reply(id) {
  currentId.value = id;
  replyText.value = "";
  replyDialog.value = true;
}

async function submitReply() {
  const content = replyText.value.trim();
  if (!content) {
    ElMessage.warning("请输入投诉处理说明");
    return;
  }
  await api.post(`/merchant/complaints/${currentId.value}/reply`, { content });
  ElMessage.success("投诉回复已提交");
  replyDialog.value = false;
  await load();
}

function resetSearch() {
  keyword.value = "";
  load();
}

function canReply(status) {
  return ["ACCEPTED", "ASSIGNED"].includes(status);
}

function statusMeta(status) {
  return STATUS_META[status] || { text: status || "-", type: "info" };
}

function complaintTypeText(type) {
  return COMPLAINT_TYPE_TEXT[type] || type || "-";
}

function flowActionText(action) {
  return FLOW_ACTION_TEXT[action] || action || "-";
}

function operatorRoleText(role) {
  return OPERATOR_ROLE_TEXT[role] || role || "-";
}

function timelineType(action) {
  const map = {
    CREATE: "primary",
    ACCEPT: "success",
    ASSIGN: "warning",
    REPLY: "success",
    JUDGE: "danger",
    FINISH: "success",
    CLOSE: "info"
  };
  return map[action] || "primary";
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

onMounted(load);
</script>

<style scoped>
.complaint-overview {
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

.overview-label,
.reply-dialog-copy,
.detail-kicker {
  color: #64748b;
}

.overview-label {
  display: inline-block;
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

.overview-value.warning {
  color: #f59e0b;
}

.overview-value.success {
  color: #16a34a;
}

.overview-value.primary {
  color: #2563eb;
}

.overview-note {
  display: block;
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

.toolbar-left,
.toolbar-right,
.dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  width: 360px;
  max-width: 100%;
  flex: 0 0 360px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}

.resource-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.resource-table :deep(td:first-child .cell),
.resource-table :deep(th:first-child .cell) {
  white-space: nowrap;
}

.resource-table :deep(td:last-child .cell),
.resource-table :deep(th:last-child .cell) {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.reply-dialog-copy {
  margin-bottom: 14px;
  line-height: 1.7;
}

.complaint-detail {
  display: grid;
  gap: 20px;
}

.detail-section {
  padding: 20px 22px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #e5edf7;
}

.detail-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.detail-section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
}

.detail-grid {
  overflow: hidden;
}

.detail-text-block {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #334155;
}

.detail-timeline {
  padding-top: 8px;
}

.flow-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.flow-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.flow-card-title {
  color: #0f172a;
  font-weight: 700;
}

.flow-card-content {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .detail-section,
  .overview-card {
    padding: 18px 16px;
  }

  .detail-section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search {
    width: 100%;
    flex-basis: 100%;
  }
}
</style>
