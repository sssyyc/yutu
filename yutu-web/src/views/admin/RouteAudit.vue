<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="ROUTE AUDIT"
      title="路线审核"
    />

    <section class="page-card">
      <el-tabs v-model="activeTab" class="audit-tabs">
        <el-tab-pane label="路线资料审核" name="route">
          <el-table :data="routeList" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="路线名称" min-width="220">
              <template #default="{ row }">
                <el-button text type="primary" class="route-link" @click="showDetail(row.id)">
                  {{ row.routeName }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="路线概述" min-width="220" show-overflow-tooltip />
            <el-table-column label="路线审核" width="140">
              <template #default="{ row }">
                <el-tag :type="auditTag(row.auditStatus).type">{{ auditTag(row.auditStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上架状态" width="140">
              <template #default="{ row }">
                <el-tag :type="publishTag(row.publishStatus).type">{{ publishTag(row.publishStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.auditRemark || "-" }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="280" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="showDetail(row.id)">详情</el-button>
                <el-button text type="success" @click="approveRoute(row.id)">通过</el-button>
                <el-button text type="danger" @click="rejectRoute(row)">驳回</el-button>
                <el-button text type="warning" @click="offlineRoute(row.id)">下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="出发日期审核" name="departure">
          <div class="departure-toolbar">
            <el-select
              v-model="selectedDepartureRouteId"
              clearable
              filterable
              placeholder="按路线筛选出发日期审核"
              class="route-filter"
            >
              <el-option
                v-for="item in routeList"
                :key="item.id"
                :label="item.routeName"
                :value="item.id"
              />
            </el-select>
            <div class="toolbar-tip">当前展示 {{ filteredDepartureList.length }} 条出发日期记录</div>
          </div>

          <el-table :data="filteredDepartureList" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="所属路线" min-width="220">
              <template #default="{ row }">
                <el-button text type="primary" class="route-link" @click="showDetail(row.routeId)">
                  {{ row.routeName || `路线 #${row.routeId}` }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="departDate" label="出发日期" min-width="150" />
            <el-table-column label="售价" width="120">
              <template #default="{ row }">¥ {{ formatPrice(row.salePrice) }}</template>
            </el-table-column>
            <el-table-column prop="remainCount" label="余量" width="100" />
            <el-table-column label="路线审核" width="120">
              <template #default="{ row }">
                <el-tag :type="auditTag(row.routeAuditStatus).type">{{ auditTag(row.routeAuditStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="日期审核" width="120">
              <template #default="{ row }">
                <el-tag :type="auditTag(row.auditStatus).type">{{ auditTag(row.auditStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.auditRemark || "-" }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="200" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="showDetail(row.routeId)">查看路线</el-button>
                <el-button text type="success" @click="approveDeparture(row.id)">通过</el-button>
                <el-button text type="danger" @click="rejectDeparture(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="detailVisible" title="路线详情" width="920px" destroy-on-close>
      <template v-if="currentRoute">
        <div class="detail-cover" v-if="currentRoute.coverImage">
          <el-image
            :src="currentRoute.coverImage"
            fit="cover"
            class="cover-image"
            :preview-src-list="[currentRoute.coverImage]"
            preview-teleported
          />
        </div>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="路线名称">{{ currentRoute.routeName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥ {{ formatPrice(currentRoute.price) }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ currentRoute.stock ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ currentRoute.score ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="路线审核">
            <el-tag :type="auditTag(currentRoute.auditStatus).type">{{ auditTag(currentRoute.auditStatus).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上架状态">
            <el-tag :type="publishTag(currentRoute.publishStatus).type">{{ publishTag(currentRoute.publishStatus).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="路线概述" :span="2">{{ currentRoute.summary || "-" }}</el-descriptions-item>
          <el-descriptions-item label="详细介绍" :span="2">{{ currentPlainDetail || "-" }}</el-descriptions-item>
          <el-descriptions-item label="审核说明" :span="2">{{ currentRoute.auditRemark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div class="date-block">
          <div class="date-block-title">出发日期审核情况</div>
          <el-table :data="currentDepartureDates" border>
            <el-table-column prop="departDate" label="出发日期" min-width="150" />
            <el-table-column label="售价" width="120">
              <template #default="{ row }">¥ {{ formatPrice(row.salePrice) }}</template>
            </el-table-column>
            <el-table-column prop="remainCount" label="余量" width="100" />
            <el-table-column label="审核状态" width="120">
              <template #default="{ row }">
                <el-tag :type="auditTag(row.auditStatus).type">{{ auditTag(row.auditStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.auditRemark || "-" }}
              </template>
            </el-table-column>
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
import { parseRouteDetailContent } from "../../utils/routeDetailMeta";

const activeTab = ref("route");
const routeList = ref([]);
const departureList = ref([]);
const selectedDepartureRouteId = ref(null);
const detailVisible = ref(false);
const currentRoute = ref(null);
const currentDepartureDates = ref([]);

const currentPlainDetail = computed(() => parseRouteDetailContent(currentRoute.value?.detailContent).plainText);
const filteredDepartureList = computed(() => {
  if (!selectedDepartureRouteId.value) {
    return departureList.value;
  }
  return departureList.value.filter((item) => Number(item.routeId) === Number(selectedDepartureRouteId.value));
});

function auditTag(status) {
  const map = {
    0: { text: "待审核", type: "warning" },
    1: { text: "已通过", type: "success" },
    2: { text: "已驳回", type: "danger" }
  };
  return map[status] || { text: "-", type: "info" };
}

function publishTag(status) {
  const map = {
    0: { text: "未上架", type: "info" },
    1: { text: "已上架", type: "success" }
  };
  return map[status] || { text: "-", type: "info" };
}

function formatPrice(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return "--";
  return Number.isInteger(num) ? String(num) : num.toFixed(2);
}

async function load() {
  const [routes, departureDates] = await Promise.all([
    api.get("/admin/routes"),
    api.get("/admin/routes/departure-dates")
  ]);
  routeList.value = routes;
  departureList.value = departureDates;
}

async function showDetail(routeId) {
  const found = routeList.value.find((item) => Number(item.id) === Number(routeId));
  if (!found) {
    ElMessage.warning("未找到路线详情");
    return;
  }
  currentRoute.value = found;
  currentDepartureDates.value = await api.get(`/admin/routes/${routeId}/departure-dates`);
  detailVisible.value = true;
}

async function approveRoute(id) {
  await api.post(`/admin/routes/${id}/approve`);
  ElMessage.success("路线资料已审核通过");
  await load();
}

async function rejectRoute(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写对“${row.routeName}”的修改意见，商户会按意见调整后重新提交。`,
      "驳回路线资料",
      {
        inputPlaceholder: "请输入驳回原因或修改建议",
        confirmButtonText: "确认驳回",
        cancelButtonText: "取消",
        inputValidator: (val) => (val ? true : "请填写修改意见")
      }
    );
    await api.post(`/admin/routes/${row.id}/reject`, { auditRemark: value });
    ElMessage.success("路线资料已驳回");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

async function offlineRoute(id) {
  await api.post(`/admin/routes/${id}/offline`);
  ElMessage.success("路线已下架");
  await load();
}

async function approveDeparture(id) {
  await api.post(`/admin/routes/departure-dates/${id}/approve`);
  ElMessage.success("出发日期已审核通过");
  await load();
  if (detailVisible.value && currentRoute.value?.id) {
    currentDepartureDates.value = await api.get(`/admin/routes/${currentRoute.value.id}/departure-dates`);
  }
}

async function rejectDeparture(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写“${row.routeName || `路线#${row.routeId}`}-${row.departDate}”的修改意见。`,
      "驳回出发日期",
      {
        inputPlaceholder: "请输入驳回原因或修改建议",
        confirmButtonText: "确认驳回",
        cancelButtonText: "取消",
        inputValidator: (val) => (val ? true : "请填写修改意见")
      }
    );
    await api.post(`/admin/routes/departure-dates/${row.id}/reject`, { auditRemark: value });
    ElMessage.success("出发日期已驳回");
    await load();
    if (detailVisible.value && currentRoute.value?.id === row.routeId) {
      currentDepartureDates.value = await api.get(`/admin/routes/${currentRoute.value.id}/departure-dates`);
    }
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

onMounted(load);
</script>

<style scoped>
.audit-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.departure-toolbar {
  margin-bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-filter {
  width: 320px;
}

.toolbar-tip {
  color: #64748b;
  font-size: 13px;
}

.route-link {
  padding: 0;
  font-weight: 600;
}

.detail-cover {
  margin-bottom: 16px;
}

.cover-image {
  width: 100%;
  height: 240px;
  border-radius: 14px;
}

.detail-descriptions {
  margin-bottom: 18px;
}

.date-block {
  margin-top: 8px;
}

.date-block-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

@media (max-width: 720px) {
  .route-filter {
    width: 100%;
  }
}
</style>
