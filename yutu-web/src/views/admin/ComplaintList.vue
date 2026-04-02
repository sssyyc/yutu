<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Complaint Workflow"
      title="投诉处理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入投诉号或投诉路线"
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
        <el-table-column prop="complaintNo" label="投诉号" min-width="180" />
        <el-table-column prop="routeName" label="投诉路线" min-width="220" />
        <el-table-column prop="title" label="投诉标题" min-width="220" />
        <el-table-column label="处理状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light" round>
              {{ statusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="420" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="detailComplaint(row.id)">详情</el-button>
            <el-button text type="success" @click="action(row.id, 'accept')">受理</el-button>
            <el-button text type="warning" @click="action(row.id, 'assign')">分派</el-button>
            <el-button text type="danger" @click="action(row.id, 'judge')">判定</el-button>
            <el-button text @click="action(row.id, 'finish')">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialog" title="投诉详情" width="860px">
      <div v-if="current.complaint" class="complaint-detail">
        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <p class="detail-kicker">Complaint Detail</p>
              <h3>{{ current.complaint.title || "投诉详情" }}</h3>
            </div>
            <el-tag :type="statusMeta(current.complaint.status).type" effect="light" round>
              {{ statusMeta(current.complaint.status).text }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border class="detail-grid">
            <el-descriptions-item label="投诉单号">
              {{ current.complaint.complaintNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉类型">
              {{ complaintTypeText(current.complaint.complaintType) }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉路线">
              {{ current.complaint.routeName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="关联订单">
              {{ current.complaint.orderNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDateTime(current.complaint.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDateTime(current.complaint.updateTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉内容" :span="2">
              <div class="detail-text-block">{{ current.complaint.content || "-" }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="处理结果" :span="2">
              <div class="detail-text-block">{{ current.complaint.resultContent || "暂未形成处理结果" }}</div>
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

          <el-empty
            v-if="!(current.flows || []).length"
            description="当前还没有处理记录"
          />

          <el-timeline v-else class="detail-timeline">
            <el-timeline-item
              v-for="flow in current.flows"
              :key="flow.id"
              :timestamp="formatDateTime(flow.createTime)"
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
import { onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const dialog = ref(false);
const current = ref({});

const STATUS_META = {
  PENDING_ACCEPT: { text: "待受理", type: "warning" },
  ACCEPTED: { text: "已受理", type: "primary" },
  ASSIGNED: { text: "待商户处理", type: "warning" },
  MERCHANT_REPLIED: { text: "商户已回复", type: "success" },
  JUDGED: { text: "已判定", type: "success" },
  FINISHED: { text: "已完成", type: "info" },
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

function statusMeta(status) {
  return STATUS_META[status] || { text: String(status ?? "-"), type: "info" };
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

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/complaints", params);
}

async function detailComplaint(id) {
  current.value = await api.get(`/admin/complaints/${id}`);
  dialog.value = true;
}

async function action(id, type) {
  await api.post(`/admin/complaints/${id}/${type}`, { content: `${type} by admin` });
  await load();
}

function resetSearch() {
  keyword.value = "";
  load();
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

.toolbar-left,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
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
  color: #94a3b8;
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

@media (max-width: 768px) {
  .detail-section {
    padding: 18px 16px;
  }

  .detail-section-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
