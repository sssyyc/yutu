<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="MERCHANT REVIEW" title="商户管理" />

    <section class="page-card module-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">商户总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">当前系统中全部商户入驻申请与正式商户数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">待审核商户</span>
          <strong class="overview-value warning">{{ overview.pending }}</strong>
          <p class="overview-note">仍需要平台人工审核处理的商户申请数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">预警商户</span>
          <strong class="overview-value danger">{{ overview.warned }}</strong>
          <p class="overview-note">已被平台发出经营预警、需要重点关注的商户数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">封禁商户</span>
          <strong class="overview-value muted">{{ overview.banned }}</strong>
          <p class="overview-note">已被平台限制经营、暂不可继续开展商家业务的数量。</p>
        </article>
      </div>
    </section>

    <section class="page-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          clearable
          placeholder="请输入申请人或店铺名称"
          @clear="load"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="申请人" min-width="150">
          <template #default="{ row }">
            <div>{{ row.applicantUsername || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="店铺名称" min-width="180" />
        <el-table-column prop="contactPhone" label="联系方式" min-width="150" />
        <el-table-column prop="licenseNo" label="营业执照号" min-width="190" />
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="auditTag(row.auditStatus).type">{{ auditTag(row.auditStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="经营状态" width="120">
          <template #default="{ row }">
            <el-tag :type="businessTag(row).type">{{ businessTag(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="经营概况" min-width="220">
          <template #default="{ row }">
            <div class="metric-line">路线 {{ row.routeCount || 0 }} 条</div>
            <div class="metric-line">订单 {{ row.orderCount || 0 }} 单</div>
            <div class="metric-line">成交 {{ currencyText(row.turnover) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="投诉数" width="100">
          <template #default="{ row }">
            <span :class="{ 'danger-text': Number(row.complaintCount || 0) > 0 }">{{ row.complaintCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="平台备注" min-width="220">
          <template #default="{ row }">
            {{ row.auditRemark || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="资料" width="100">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" @click="openRecords(row)">经营记录</el-button>
              <template v-if="row.auditStatus === 0">
                <el-button text type="success" @click="approve(row)">通过</el-button>
                <el-button text type="danger" @click="reject(row)">驳回</el-button>
              </template>
              <template v-else>
                <el-button text type="warning" @click="warnMerchant(row)">预警</el-button>
                <el-button
                  v-if="Number(row.status) === 1"
                  text
                  type="danger"
                  @click="banMerchant(row)"
                >
                  封禁
                </el-button>
                <el-button
                  v-else
                  text
                  type="success"
                  @click="unbanMerchant(row)"
                >
                  解封
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" title="商户申请详情" width="760px">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ current.applicantUsername || "-" }}</el-descriptions-item>
          <el-descriptions-item label="当前角色">{{ roleText(current.roleType) }}</el-descriptions-item>
          <el-descriptions-item label="店铺名称">{{ current.shopName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ current.contactPhone || "-" }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ current.contactName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="营业执照号">{{ current.licenseNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditTag(current.auditStatus).type">{{ auditTag(current.auditStatus).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="经营状态">
            <el-tag :type="businessTag(current).type">{{ businessTag(current).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ current.auditTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="平台备注">{{ current.auditRemark || "-" }}</el-descriptions-item>
          <el-descriptions-item label="店铺简介" :span="2">{{ current.description || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div class="image-grid">
          <div class="image-card">
            <div class="image-title">营业执照</div>
            <el-image
              v-if="current.licenseImage"
              :src="current.licenseImage"
              fit="cover"
              class="preview-image"
              :preview-src-list="[current.licenseImage]"
              preview-teleported
            />
            <div v-else class="image-empty">未上传</div>
          </div>
          <div class="image-card">
            <div class="image-title">身份证人像面</div>
            <el-image
              v-if="current.idCardFrontImage"
              :src="current.idCardFrontImage"
              fit="cover"
              class="preview-image"
              :preview-src-list="[current.idCardFrontImage]"
              preview-teleported
            />
            <div v-else class="image-empty">未上传</div>
          </div>
          <div class="image-card">
            <div class="image-title">身份证国徽面</div>
            <el-image
              v-if="current.idCardBackImage"
              :src="current.idCardBackImage"
              fit="cover"
              class="preview-image"
              :preview-src-list="[current.idCardBackImage]"
              preview-teleported
            />
            <div v-else class="image-empty">未上传</div>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="recordVisible" title="商户经营记录" width="920px">
      <template v-if="recordData">
        <div class="record-overview">
          <article class="record-card">
            <span class="record-label">路线数</span>
            <strong class="record-value">{{ recordData.routeCount || 0 }}</strong>
          </article>
          <article class="record-card">
            <span class="record-label">已上架路线</span>
            <strong class="record-value">{{ recordData.publishedRouteCount || 0 }}</strong>
          </article>
          <article class="record-card">
            <span class="record-label">订单数</span>
            <strong class="record-value">{{ recordData.orderCount || 0 }}</strong>
          </article>
          <article class="record-card">
            <span class="record-label">已支付订单</span>
            <strong class="record-value">{{ recordData.paidOrderCount || 0 }}</strong>
          </article>
          <article class="record-card">
            <span class="record-label">成交金额</span>
            <strong class="record-value">{{ currencyText(recordData.turnover) }}</strong>
          </article>
          <article class="record-card">
            <span class="record-label">投诉数</span>
            <strong class="record-value danger-text">{{ recordData.complaintCount || 0 }}</strong>
          </article>
        </div>

        <el-descriptions :column="2" border class="record-summary">
          <el-descriptions-item label="商户名称">{{ recordData.shopName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="businessTag(recordData).type">{{ businessTag(recordData).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditTag(recordData.auditStatus).type">{{ auditTag(recordData.auditStatus).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最新备注">{{ recordData.latestRemark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div class="record-section">
          <div class="record-section-title">最近路线</div>
          <el-table :data="recordData.recentRoutes || []" border size="small">
            <el-table-column prop="routeName" label="路线名称" min-width="180" />
            <el-table-column label="审核状态" width="110">
              <template #default="{ row }">
                <el-tag :type="routeAuditTag(row.auditStatus).type">{{ routeAuditTag(row.auditStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上架状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">{{ row.publishStatus === 1 ? "已上架" : "未上架" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" min-width="160" />
          </el-table>
        </div>

        <div class="record-section">
          <div class="record-section-title">最近订单</div>
          <el-table :data="recordData.recentOrders || []" border size="small">
            <el-table-column prop="orderNo" label="订单号" min-width="180" />
            <el-table-column label="订单状态" width="120">
              <template #default="{ row }">{{ orderStatusText(row.orderStatus) }}</template>
            </el-table-column>
            <el-table-column label="支付状态" width="120">
              <template #default="{ row }">{{ payStatusText(row.payStatus) }}</template>
            </el-table-column>
            <el-table-column label="支付金额" width="120">
              <template #default="{ row }">{{ currencyText(row.payAmount) }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" min-width="160" />
          </el-table>
        </div>

        <div class="record-section">
          <div class="record-section-title">最近投诉</div>
          <el-table :data="recordData.recentComplaints || []" border size="small">
            <el-table-column prop="complaintNo" label="投诉单号" min-width="170" />
            <el-table-column label="投诉类型" width="120">
              <template #default="{ row }">{{ complaintTypeText(row.complaintType) }}</template>
            </el-table-column>
            <el-table-column label="处理状态" width="120">
              <template #default="{ row }">{{ complaintStatusText(row.status) }}</template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="createTime" label="投诉时间" min-width="160" />
          </el-table>
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
const detailVisible = ref(false);
const current = ref(null);
const recordVisible = ref(false);
const recordData = ref(null);

const overview = computed(() => ({
  total: list.value.length,
  pending: list.value.filter((item) => Number(item.auditStatus) === 0).length,
  warned: list.value.filter((item) => Boolean(item.warned)).length,
  banned: list.value.filter((item) => Number(item.status) !== 1).length
}));

function auditTag(status) {
  const map = {
    0: { text: "待审核", type: "warning" },
    1: { text: "已通过", type: "success" },
    2: { text: "已驳回", type: "danger" }
  };
  return map[status] || { text: "未知", type: "info" };
}

function routeAuditTag(status) {
  const map = {
    0: { text: "待审核", type: "warning" },
    1: { text: "已通过", type: "success" },
    2: { text: "已驳回", type: "danger" }
  };
  return map[status] || { text: "未知", type: "info" };
}

function businessTag(row) {
  if (Number(row?.status) !== 1) {
    return { text: "已封禁", type: "danger" };
  }
  if (row?.warned || String(row?.latestRemark || row?.auditRemark || "").startsWith("【预警】")) {
    return { text: "已预警", type: "warning" };
  }
  return { text: "正常经营", type: "success" };
}

function roleText(roleType) {
  const map = {
    1: "普通用户",
    2: "商户",
    3: "管理员"
  };
  return map[roleType] || "普通用户";
}

function currencyText(value) {
  const amount = Number(value || 0);
  return `￥${amount.toFixed(2)}`;
}

function orderStatusText(status) {
  const map = {
    PENDING_PAY: "待支付",
    PENDING_TRAVEL: "待出行",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    REFUNDED: "已退款"
  };
  return map[status] || status || "-";
}

function payStatusText(status) {
  const map = {
    UNPAID: "未支付",
    PAID: "已支付",
    REFUNDED: "已退款",
    PARTIAL_REFUNDED: "部分退款",
    FAILED: "支付失败"
  };
  return map[status] || status || "-";
}

function complaintTypeText(type) {
  const map = {
    SERVICE: "服务问题",
    SCHEDULE: "行程问题",
    GUIDE: "导游问题",
    REFUND: "退款问题",
    OTHER: "其他"
  };
  return map[type] || type || "-";
}

function complaintStatusText(status) {
  const map = {
    PENDING_ACCEPT: "待受理",
    ACCEPTED: "已受理",
    ASSIGNED: "已分派",
    PROCESSING: "处理中",
    REPLIED: "已回复",
    JUDGED: "已判定",
    FINISHED: "已完成"
  };
  return map[status] || status || "-";
}

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/merchants", params);
}

function reset() {
  keyword.value = "";
  load();
}

function openDetail(row) {
  current.value = row;
  detailVisible.value = true;
}

async function openRecords(row) {
  recordData.value = await api.get(`/admin/merchants/${row.id}/records`);
  recordVisible.value = true;
}

async function approve(row) {
  try {
    const { value } = await ElMessageBox.prompt("可填写审核说明，留空则直接通过。", "通过商户申请", {
      inputPlaceholder: "请输入审核说明（选填）",
      confirmButtonText: "通过",
      cancelButtonText: "取消"
    });
    await api.post(`/admin/merchants/${row.id}/approve`, { auditRemark: value || "" });
    ElMessage.success("商户申请已通过");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

async function reject(row) {
  try {
    const { value } = await ElMessageBox.prompt("请输入驳回原因，用户会在个人中心看到该说明。", "驳回商户申请", {
      inputPlaceholder: "请输入驳回原因",
      confirmButtonText: "驳回",
      cancelButtonText: "取消",
      inputValidator: (val) => (val ? true : "请填写驳回原因")
    });
    await api.post(`/admin/merchants/${row.id}/reject`, { auditRemark: value });
    ElMessage.success("商户申请已驳回");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

async function warnMerchant(row) {
  try {
    const { value } = await ElMessageBox.prompt("请输入预警说明，平台将对该商户留痕提醒。", "发送经营预警", {
      inputPlaceholder: "请输入预警说明",
      confirmButtonText: "发送预警",
      cancelButtonText: "取消",
      inputValidator: (val) => (val ? true : "请填写预警说明")
    });
    await api.post(`/admin/merchants/${row.id}/warn`, { auditRemark: value });
    ElMessage.success("预警已发送");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

async function banMerchant(row) {
  try {
    const { value } = await ElMessageBox.prompt("请输入封禁原因，封禁后商户将无法继续经营。", "封禁商户", {
      inputPlaceholder: "请输入封禁原因",
      confirmButtonText: "确认封禁",
      cancelButtonText: "取消",
      inputValidator: (val) => (val ? true : "请填写封禁原因")
    });
    await api.post(`/admin/merchants/${row.id}/ban`, { auditRemark: value });
    ElMessage.success("商户已封禁");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

async function unbanMerchant(row) {
  try {
    const { value } = await ElMessageBox.prompt("可填写解封说明，留空则直接恢复。", "解封商户", {
      inputPlaceholder: "请输入解封说明（选填）",
      confirmButtonText: "确认解封",
      cancelButtonText: "取消"
    });
    await api.post(`/admin/merchants/${row.id}/unban`, { auditRemark: value || "" });
    ElMessage.success("商户已解封");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
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

.overview-value.warning {
  color: #f59e0b;
}

.overview-value.danger {
  color: #ef4444;
}

.overview-value.muted {
  color: #64748b;
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
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-search {
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.metric-line {
  line-height: 1.6;
}

.danger-text {
  color: #ef4444;
  font-weight: 700;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.image-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 12px;
  background: #fafafa;
}

.image-title {
  margin-bottom: 10px;
  font-weight: 600;
}

.preview-image,
.image-empty {
  width: 100%;
  height: 180px;
  border-radius: 10px;
  overflow: hidden;
}

.image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #667085;
}

.record-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 24px;
  margin-bottom: 14px;
  padding: 2px 0 6px;
}

.record-card {
  min-height: auto;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  display: flex;
  align-items: baseline;
  justify-content: flex-start;
  gap: 6px;
}

.record-label {
  display: inline;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 600;
  margin-bottom: 0;
}

.record-value {
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 600;
}

.record-summary {
  margin-bottom: 18px;
}

.record-section {
  margin-top: 18px;
}

.record-section-title {
  margin-bottom: 10px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

@media (max-width: 1200px) {
  .overview-grid,
  .record-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-grid,
  .record-overview,
  .image-grid {
    grid-template-columns: 1fr;
  }

  .overview-card {
    padding: 18px 16px;
  }
}
</style>

