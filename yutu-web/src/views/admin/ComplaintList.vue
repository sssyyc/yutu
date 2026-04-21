<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="Complaint Workflow" title="投诉处理" />

    <section class="page-card complaint-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">投诉总量</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">覆盖受理、分派、判定到结果归档的投诉全流程。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">处理中投诉</span>
          <strong class="overview-value warning">{{ overview.pending }}</strong>
          <p class="overview-note">包含待受理、待分派、待商户处理、待平台判定的投诉。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">已闭环投诉</span>
          <strong class="overview-value success">{{ overview.finished }}</strong>
          <p class="overview-note">已完成结果反馈并归档，可回溯查看完整处理记录。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">预警商户数</span>
          <strong class="overview-value danger">{{ warningMerchants.length }}</strong>
          <p class="overview-note">达到投诉红线的商户将进入联动管控名单。</p>
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
            placeholder="请输入投诉号、路线或商户名称"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-right">
          <el-button @click="exportReport">导出报表</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="displayList" border class="resource-table">
        <el-table-column prop="complaintNo" label="投诉号" min-width="260" show-overflow-tooltip />
        <el-table-column label="投诉类型" width="130">
          <template #default="{ row }">
            {{ complaintTypeText(row.complaintType) }}
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="涉事商户" min-width="170" />
        <el-table-column prop="routeName" label="投诉路线" min-width="200" show-overflow-tooltip />
        <el-table-column prop="title" label="投诉标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="处理状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light" round>
              {{ statusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button text type="primary" @click="detailComplaint(row.id)">详情</el-button>
            <el-button
              v-if="canAction(row.status, 'accept')"
              text
              type="success"
              @click="openAction(row, 'accept')"
            >
              受理
            </el-button>
            <el-button
              v-if="canAction(row.status, 'assign')"
              text
              type="warning"
              @click="openAction(row, 'assign')"
            >
              分派
            </el-button>
            <el-button
              v-if="canAction(row.status, 'judge')"
              text
              type="danger"
              @click="openAction(row, 'judge')"
            >
              判定
            </el-button>
            <el-button
              v-if="canAction(row.status, 'finish')"
              text
              @click="openAction(row, 'finish')"
            >
              完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="page-card stats-section">
      <div class="stats-grid">
        <article class="stats-card warning-card">
          <div class="stats-head">
            <div>
              <p class="stats-kicker">Complaint Analytics</p>
              <h3>投诉统计分析</h3>
            </div>
          </div>

          <div class="mini-table-block">
            <div class="mini-table-title">按投诉类型统计</div>
            <el-table :data="typeStats" border class="mini-stats-table type-stats-table">
              <el-table-column prop="label" label="投诉类型" min-width="220" show-overflow-tooltip />
              <el-table-column prop="count" label="投诉量" min-width="90" />
              <el-table-column prop="ratio" label="占比" min-width="90" />
            </el-table>
          </div>

          <div class="mini-table-block">
            <div class="mini-table-title">按时间维度统计</div>
            <el-table :data="timeStats" border class="mini-stats-table time-stats-table">
              <el-table-column prop="date" label="日期" min-width="240" />
              <el-table-column prop="count" label="新增投诉" min-width="90" />
            </el-table>
          </div>
        </article>

        <article class="stats-card process-trace-card">
          <div class="stats-head">
            <div>
              <p class="stats-kicker">Merchant Warning</p>
              <h3>投诉预警与管控</h3>
            </div>
            <div class="stats-head-side">
              <div class="redline-box warning-redline-box">
                <span class="redline-label">投诉红线</span>
                <el-input-number
                  v-model="warningLine"
                  :min="1"
                  :max="99"
                  controls-position="right"
                />
              </div>
            </div>
          </div>

          <el-table :data="warningMerchants" border>
            <el-table-column prop="merchantName" label="商户名称" min-width="170" />
            <el-table-column prop="complaintCount" label="投诉量" width="90" />
            <el-table-column prop="unfinishedCount" label="未结案" width="90" />
            <el-table-column label="风险等级" width="120">
              <template #default="{ row }">
                <el-tag :type="warningLevelMeta(row.level).type" effect="light" round>
                  {{ warningLevelMeta(row.level).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="经营状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.merchantStatus === 1 ? 'success' : 'danger'" effect="light" round>
                  {{ row.merchantStatus === 1 ? "正常营业" : "停业整改" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="latestTime" label="最近投诉时间" min-width="160" />
            <el-table-column prop="suggestion" label="管控建议" min-width="180" show-overflow-tooltip />
            <el-table-column label="联动操作" min-width="260" fixed="right">
              <template #default="{ row }">
                <el-button text type="warning" @click="handleMerchantControl(row, row.merchantStatus ?? 1, '发送预警')">
                  预警
                </el-button>
                <el-button text type="danger" @click="handleMerchantControl(row, row.merchantStatus ?? 1, '评级下调')">
                  下调评级
                </el-button>
                <el-button
                  text
                  :type="row.merchantStatus === 1 ? 'danger' : 'success'"
                  @click="handleMerchantControl(row, row.merchantStatus === 1 ? 0 : 1, row.merchantStatus === 1 ? '停业整改' : '恢复营业')"
                >
                  {{ row.merchantStatus === 1 ? "停业整改" : "恢复营业" }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </article>
      </div>

      <div class="stats-grid stats-grid-bottom">
        <article class="stats-card">
          <div class="stats-head compact">
            <div>
              <p class="stats-kicker">Merchant Ranking</p>
              <h3>涉事商户排行</h3>
            </div>
          </div>
          <el-table :data="merchantStats" border>
            <el-table-column prop="merchantName" label="商户名称" min-width="180" />
            <el-table-column prop="complaintCount" label="投诉总量" width="110" />
            <el-table-column prop="unfinishedCount" label="处理中" width="110" />
            <el-table-column prop="latestRoute" label="最近涉事路线" min-width="180" show-overflow-tooltip />
          </el-table>
        </article>

        <article class="stats-card">
          <div class="stats-head compact">
            <div>
              <p class="stats-kicker">Full Process Trace</p>
              <h3>全流程追踪说明</h3>
            </div>
          </div>
          <div class="process-intro">
            <div class="process-item">
              <strong>1. 投诉受理与处理</strong>
              <span>支持详情查看、受理、分派、进度跟进、结果反馈，形成完整闭环。</span>
            </div>
            <div class="process-item">
              <strong>2. 投诉统计分析</strong>
              <span>按投诉类型、涉事商户、时间维度统计，支持导出分析报表。</span>
            </div>
            <div class="process-item">
              <strong>3. 投诉预警与管控</strong>
              <span>设置投诉红线，超标商户自动预警，并联动评级与停业整改。</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="投诉详情" width="920px">
      <div v-if="currentComplaint" class="complaint-detail">
        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <p class="detail-kicker">Complaint Detail</p>
              <h3>{{ currentComplaint.title || "投诉详情" }}</h3>
            </div>
            <el-tag :type="statusMeta(currentComplaint.status).type" effect="light" round>
              {{ statusMeta(currentComplaint.status).text }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border class="detail-grid">
            <el-descriptions-item label="投诉单号">
              {{ currentComplaint.complaintNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉类型">
              {{ complaintTypeText(currentComplaint.complaintType) }}
            </el-descriptions-item>
            <el-descriptions-item label="涉事商户">
              {{ currentComplaint.merchantName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉路线">
              {{ currentComplaint.routeName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="关联订单">
              {{ currentComplaint.orderNo || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">
              {{ formatDateTime(currentComplaint.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="投诉内容" :span="2">
              <div class="detail-text-block">{{ currentComplaint.content || "-" }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="处理结果" :span="2">
              <div class="detail-text-block">
                {{ formatComplaintText(currentComplaint.resultContent) || "当前尚未形成最终处理结果。" }}
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <div>
              <p class="detail-kicker">Processing Timeline</p>
              <h3>投诉全流程追踪</h3>
            </div>
          </div>

          <el-empty v-if="!detailFlows.length" description="当前暂无处理记录" />

          <el-timeline v-else class="detail-timeline">
            <el-timeline-item
              v-for="flow in detailFlows"
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
                <p class="flow-card-content">{{ formatComplaintText(flow.actionContent) || "暂无补充说明" }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>

      <el-empty v-else description="未获取到投诉详情" />
    </el-dialog>

    <el-dialog v-model="actionDialogVisible" :title="actionDialogTitle" width="560px">
      <el-form label-position="top">
        <el-form-item :label="actionDialogLabel">
          <el-input
            v-model="actionContent"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            :placeholder="actionDialogPlaceholder"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="actionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAction">确认提交</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const warningLine = ref(3);

const dialogVisible = ref(false);
const currentComplaint = ref(null);
const detailFlows = ref([]);

const actionDialogVisible = ref(false);
const actionTarget = ref(null);
const actionType = ref("");
const actionContent = ref("");

const STATUS_META = {
  PENDING_ACCEPT: { text: "待受理", type: "warning" },
  ACCEPTED: { text: "已受理", type: "primary" },
  ASSIGNED: { text: "待商户处理", type: "warning" },
  MERCHANT_REPLIED: { text: "商户已回复", type: "success" },
  JUDGED: { text: "已判定", type: "danger" },
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

const ACTION_TEXT = {
  accept: "受理",
  assign: "分派",
  judge: "判定",
  finish: "完成"
};

const displayList = computed(() => {
  const search = keyword.value.trim().toLowerCase();
  if (!search) {
    return list.value;
  }
  return list.value.filter((item) => {
    const targets = [
      item.complaintNo,
      item.routeName,
      item.merchantName,
      item.title,
      complaintTypeText(item.complaintType)
    ];
    return targets.some((field) => String(field || "").toLowerCase().includes(search));
  });
});

const overview = computed(() => {
  const total = displayList.value.length;
  const finished = displayList.value.filter((item) => ["FINISHED", "CLOSED"].includes(item.status)).length;
  const pending = total - finished;
  return { total, finished, pending };
});

const typeStats = computed(() => {
  const total = displayList.value.length || 1;
  const counter = new Map();
  displayList.value.forEach((item) => {
    const label = complaintTypeText(item.complaintType);
    counter.set(label, (counter.get(label) || 0) + 1);
  });
  return Array.from(counter.entries())
    .map(([label, count]) => ({
      label,
      count,
      ratio: `${Math.round((count / total) * 100)}%`
    }))
    .sort((a, b) => b.count - a.count);
});

const timeStats = computed(() => {
  const counter = new Map();
  displayList.value.forEach((item) => {
    const date = formatDate(item.createTime);
    counter.set(date, (counter.get(date) || 0) + 1);
  });
  return Array.from(counter.entries())
    .map(([date, count]) => ({ date, count }))
    .sort((a, b) => a.date.localeCompare(b.date));
});

const merchantStats = computed(() => {
  const bucket = new Map();
  displayList.value.forEach((item) => {
    const key = item.merchantId || item.merchantName || `unknown-${item.id}`;
    const current = bucket.get(key) || {
      merchantId: item.merchantId,
      merchantName: item.merchantName || "未知商户",
      merchantStatus: item.merchantStatus ?? 1,
      complaintCount: 0,
      unfinishedCount: 0,
      latestTime: "",
      latestRoute: item.routeName || "-",
      level: "normal",
      suggestion: "继续观察"
    };
    current.complaintCount += 1;
    if (!["FINISHED", "CLOSED"].includes(item.status)) {
      current.unfinishedCount += 1;
    }
    const itemTime = formatDateTime(item.createTime);
    if (itemTime > current.latestTime) {
      current.latestTime = itemTime;
      current.latestRoute = item.routeName || "-";
    }
    current.merchantStatus = item.merchantStatus ?? current.merchantStatus;
    bucket.set(key, current);
  });

  return Array.from(bucket.values())
    .map((item) => {
      let level = "normal";
      let suggestion = "继续观察";
      if (item.complaintCount >= warningLine.value) {
        level = "red";
        suggestion = "建议立即停业整改并启动重点监管";
      } else if (item.complaintCount >= Math.max(2, warningLine.value - 1)) {
        level = "yellow";
        suggestion = "建议发送预警并下调平台评级";
      }
      return { ...item, level, suggestion };
    })
    .sort((a, b) => b.complaintCount - a.complaintCount || b.unfinishedCount - a.unfinishedCount);
});

const warningMerchants = computed(() => merchantStats.value.filter((item) => item.complaintCount >= warningLine.value - 1));

const actionDialogTitle = computed(() => `${ACTION_TEXT[actionType.value] || "处理"}投诉`);
const actionDialogLabel = computed(() => `${ACTION_TEXT[actionType.value] || "处理"}说明`);
const actionDialogPlaceholder = computed(() => `请输入${ACTION_TEXT[actionType.value] || "处理"}说明，便于后续全流程追踪`);

function statusMeta(status) {
  return STATUS_META[status] || { text: status || "-", type: "info" };
}

function complaintTypeText(type) {
  return COMPLAINT_TYPE_TEXT[type] || type || "-";
}

function flowActionText(action) {
  return FLOW_ACTION_TEXT[action] || action || "-";
}

function formatComplaintText(text) {
  const value = String(text || "").trim();
  if (!value) {
    return "";
  }

  const normalized = value.toLowerCase();
  const exactMap = {
    "accept by admin": "平台已受理",
    "assign by admin": "平台已分派给商户处理",
    "judge by admin": "平台已完成判定",
    "finish by admin": "平台已处理完成",
    "close by admin": "平台已关闭投诉",
    received: "收到",
    accepted: "已受理",
    assigned: "已分派",
    judged: "已判定",
    finished: "已完成",
    closed: "已关闭"
  };

  return exactMap[normalized] || value;
}

function operatorRoleText(role) {
  return OPERATOR_ROLE_TEXT[role] || role || "-";
}

function warningLevelMeta(level) {
  const map = {
    red: { text: "红色预警", type: "danger" },
    yellow: { text: "黄色预警", type: "warning" },
    normal: { text: "正常", type: "success" }
  };
  return map[level] || map.normal;
}

function timelineType(action) {
  const map = {
    CREATE: "primary",
    ACCEPT: "success",
    ASSIGN: "warning",
    REPLY: "primary",
    JUDGE: "danger",
    FINISH: "success",
    CLOSE: "info"
  };
  return map[action] || "primary";
}

function canAction(status, type) {
  const map = {
    accept: ["PENDING_ACCEPT"],
    assign: ["ACCEPTED"],
    judge: ["MERCHANT_REPLIED"],
    finish: ["JUDGED"]
  };
  return (map[type] || []).includes(status);
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 10);
}

async function load() {
  const response = await api.get("/admin/complaints");
  list.value = Array.isArray(response) ? response : [];
}

async function detailComplaint(id) {
  const response = await api.get(`/admin/complaints/${id}`);
  currentComplaint.value = response?.complaint || null;
  detailFlows.value = response?.flows || [];
  dialogVisible.value = true;
}

function openAction(row, type) {
  actionTarget.value = row;
  actionType.value = type;
  actionContent.value = "";
  actionDialogVisible.value = true;
}

async function submitAction() {
  if (!actionTarget.value || !actionType.value) {
    return;
  }
  const content = actionContent.value.trim();
  if (!content) {
    ElMessage.warning("请先填写处理说明");
    return;
  }
  await api.post(`/admin/complaints/${actionTarget.value.id}/${actionType.value}`, { content });
  ElMessage.success(`${ACTION_TEXT[actionType.value]}成功`);
  actionDialogVisible.value = false;
  await load();
  if (dialogVisible.value && currentComplaint.value?.id === actionTarget.value.id) {
    await detailComplaint(actionTarget.value.id);
  }
}

async function handleMerchantControl(row, targetStatus, actionName) {
  const result = await ElMessageBox.prompt(
    `请输入“${actionName}”说明，系统会写入商户管控备注`,
    `${actionName}确认`,
    {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      inputPlaceholder: `例如：近7日投诉量达到${row.complaintCount}条，启动${actionName}`
    }
  ).catch(() => null);

  if (!result) {
    return;
  }

  await api.post(
    `/admin/complaints/merchants/${row.merchantId}/control-status`,
    { status: targetStatus },
    { params: { remark: result.value || actionName } }
  );

  ElMessage.success(`${actionName}已执行`);
  await load();
}

function resetSearch() {
  keyword.value = "";
  load();
}

function exportReport() {
  const lines = [
    ["投诉号", "投诉类型", "涉事商户", "投诉路线", "投诉标题", "处理状态", "提交时间"].join(","),
    ...displayList.value.map((item) => [
      item.complaintNo,
      complaintTypeText(item.complaintType),
      csvEscape(item.merchantName),
      csvEscape(item.routeName),
      csvEscape(item.title),
      statusMeta(item.status).text,
      formatDateTime(item.createTime)
    ].join(","))
  ];

  const blob = new Blob(["\ufeff" + lines.join("\n")], { type: "text/csv;charset=utf-8;" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = `投诉分析报表-${formatDateTime(new Date().toISOString()).replace(/[: ]/g, "-")}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
  ElMessage.success("投诉分析报表已导出");
}

function csvEscape(value) {
  const text = String(value || "");
  return `"${text.replace(/"/g, '""')}"`;
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

.overview-card,
.stats-card {
  padding: 18px 28px 16px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e3edf8;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.overview-label,
.stats-tip,
.detail-kicker,
.stats-kicker,
.redline-label {
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

.overview-value.danger {
  color: #ef4444;
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
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.redline-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 14px;
  background: #f8fbff;
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

.stats-section {
  display: grid;
  gap: 14px;
  margin-top: -6px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 18px;
}

.stats-grid-bottom {
  grid-template-columns: 1fr;
}

.warning-card,
.process-trace-card {
  grid-column: 1 / -1;
}

.stats-grid-bottom > .stats-card:last-child {
  display: none;
}

.warning-card {
  overflow: hidden;
}

.warning-card :deep(.el-table__fixed-right),
.warning-card :deep(.el-table__fixed-right-patch) {
  display: none !important;
}

.warning-card :deep(.el-table__body-wrapper),
.warning-card :deep(.el-table__header-wrapper) {
  width: 100% !important;
}

.stats-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.stats-head.compact {
  margin-bottom: 16px;
}

.stats-head-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.stats-kicker,
.detail-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.stats-head h3,
.detail-section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
}

.stats-tip {
  max-width: 280px;
  font-size: 13px;
  line-height: 1.7;
  text-align: right;
}

.warning-redline-box {
  align-self: flex-end;
}

.mini-table-block + .mini-table-block {
  margin-top: 18px;
}

.mini-table-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.mini-stats-table {
  width: 100%;
}

.mini-stats-table :deep(.el-table__header-wrapper),
.mini-stats-table :deep(.el-table__body-wrapper) {
  width: 100%;
}

.mini-stats-table :deep(.el-table__body),
.mini-stats-table :deep(.el-table__header) {
  width: 100% !important;
}

.mini-stats-table :deep(.cell) {
  white-space: nowrap;
}

.process-intro {
  display: grid;
  gap: 14px;
}

.process-item {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e5edf7;
}

.process-item strong {
  display: block;
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 16px;
}

.process-item span {
  color: #64748b;
  line-height: 1.8;
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

@media (max-width: 1280px) {
  .overview-grid,
  .stats-grid,
  .stats-grid-bottom {
    grid-template-columns: 1fr;
  }

  .stats-tip {
    max-width: none;
    text-align: left;
  }
}

@media (max-width: 768px) {
  .detail-section,
  .overview-card,
  .stats-card {
    padding: 18px 16px;
  }

  .detail-section-head,
  .stats-head {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search {
    width: 100%;
    flex-basis: 100%;
  }
}
</style>
