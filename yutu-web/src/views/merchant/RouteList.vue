<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="ROUTE MANAGEMENT"
      title="路线管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <el-button type="primary" @click="router.push('/merchant/route/edit')">新增路线</el-button>

        <div class="departure-shortcut">
          <el-select
            v-model="shortcutRouteId"
            filterable
            clearable
            placeholder="选择路线后直接维护出发日期"
            class="route-select"
          >
            <el-option
              v-for="item in list"
              :key="item.id"
              :label="item.routeName"
              :value="item.id"
            >
              <div class="route-option">
                <span class="route-option-name">{{ item.routeName }}</span>
                <span class="route-option-meta">
                  {{ auditTag(item.auditStatus).text }} / {{ publishTag(item.publishStatus).text }}
                </span>
              </div>
            </el-option>
          </el-select>

          <el-button
            type="primary"
            plain
            :disabled="!shortcutRouteId"
            @click="openDepartureManager()"
          >
            管理出发日期
          </el-button>
        </div>
      </div>

      <el-table :data="list" border>
        <el-table-column label="路线名称" min-width="220">
          <template #default="{ row }">
            <el-button text type="primary" class="route-link" @click="showDetail(row)">
              {{ row.routeName }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120" />
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="auditTag(row.auditStatus).type">{{ auditTag(row.auditStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架状态" width="120">
          <template #default="{ row }">
            <el-tag :type="publishTag(row.publishStatus).type">{{ publishTag(row.publishStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情标识" width="120">
          <template #default="{ row }">
            <el-tag :type="hasDetailText(row.detailContent) ? 'success' : 'warning'">
              {{ hasDetailText(row.detailContent) ? "可查看" : "待完善" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.auditRemark || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="400">
          <template #default="{ row }">
            <el-button text type="info" @click="showDetail(row)">详情</el-button>
            <el-button text type="primary" @click="router.push(`/merchant/route/edit/${row.id}`)">编辑</el-button>
            <el-button text type="primary" @click="openDepartureManager(row.id)">出发日期</el-button>
            <el-button text type="success" @click="publish(row.id)">上架</el-button>
            <el-button text type="warning" @click="unpublish(row.id)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" width="920px" class="route-detail-dialog" destroy-on-close>
      <template #header>
        <div class="detail-header">
          <div>
            <div class="detail-kicker">ROUTE DETAIL</div>
            <h3 class="detail-title">{{ currentRoute.routeName || "路线详情" }}</h3>
          </div>
          <div class="detail-status">
            <el-tag :type="auditTag(currentRoute.auditStatus).type">{{ auditTag(currentRoute.auditStatus).text }}</el-tag>
            <el-tag :type="publishTag(currentRoute.publishStatus).type">{{ publishTag(currentRoute.publishStatus).text }}</el-tag>
          </div>
        </div>
      </template>

      <div class="detail-content">
        <div class="cover-card">
          <el-image
            v-if="currentRoute.coverImage"
            :src="currentRoute.coverImage"
            fit="cover"
            class="cover-image"
            :preview-src-list="[currentRoute.coverImage]"
            preview-teleported
          />
          <div v-else class="cover-fallback">暂无封面图</div>
        </div>

        <div class="meta-grid">
          <article class="meta-card">
            <div class="meta-label">价格</div>
            <div class="meta-value">￥{{ formatPrice(currentRoute.price) }}</div>
          </article>
          <article class="meta-card">
            <div class="meta-label">库存</div>
            <div class="meta-value">{{ currentRoute.stock ?? "-" }}</div>
          </article>
          <article class="meta-card">
            <div class="meta-label">审核意见</div>
            <div class="meta-value meta-text">{{ currentRoute.auditRemark || "-" }}</div>
          </article>
          <article class="meta-card">
            <div class="meta-label">路线 ID</div>
            <div class="meta-value">{{ currentRoute.id ?? "-" }}</div>
          </article>
        </div>

        <div class="text-card">
          <div class="text-title">路线简介</div>
          <div class="text-body">{{ currentRoute.summary || "暂无简介" }}</div>
        </div>

        <div class="text-card">
          <div class="text-title">详细介绍</div>
          <div class="text-body">{{ currentRoutePlainDetail || "暂无详细介绍" }}</div>
        </div>

        <div class="text-card">
          <div class="text-title">出发日期审核情况</div>
          <el-table :data="currentRouteDates" border>
            <el-table-column prop="departDate" label="出发日期" min-width="150" />
            <el-table-column label="售价" width="120">
              <template #default="{ row }">￥{{ formatPrice(row.salePrice) }}</template>
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
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" plain @click="openDepartureManager(currentRoute.id)">管理出发日期</el-button>
        <el-button type="primary" @click="router.push(`/merchant/route/edit/${currentRoute.id}`)">编辑路线</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="departureManagerVisible"
      width="980px"
      class="departure-manager-dialog"
      destroy-on-close
    >
      <template #header>
        <div class="detail-header">
          <div>
            <div class="detail-kicker">DEPARTURE DATES</div>
            <h3 class="detail-title">出发日期快捷维护</h3>
          </div>
        </div>
      </template>

      <div class="departure-manager">
        <div class="departure-manager-toolbar">
          <el-select
            v-model="managedRouteId"
            filterable
            placeholder="选择需要维护出发日期的路线"
            class="route-select route-select-wide"
            @change="handleManagedRouteChange"
          >
            <el-option
              v-for="item in list"
              :key="item.id"
              :label="item.routeName"
              :value="item.id"
            />
          </el-select>

          <div v-if="selectedManagedRoute" class="route-status-group">
            <el-tag :type="auditTag(selectedManagedRoute.auditStatus).type">
              路线{{ auditTag(selectedManagedRoute.auditStatus).text }}
            </el-tag>
            <el-tag :type="publishTag(selectedManagedRoute.publishStatus).type">
              {{ publishTag(selectedManagedRoute.publishStatus).text }}
            </el-tag>
          </div>
        </div>

        <el-skeleton v-if="departureLoading" :rows="6" animated />

        <template v-else-if="selectedManagedRoute">
          <div class="departure-panel">
            <div class="departure-toolbar">
              <div class="departure-title">维护可预约的出发批次</div>
              <el-button type="primary" plain @click="addDepartureDate">新增日期</el-button>
            </div>

            <div class="departure-list">
              <div v-for="item in departureDates" :key="item.key" class="departure-row">
                <div class="departure-main">
                  <el-date-picker
                    v-model="item.departDate"
                    type="date"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    placeholder="选择出发日期"
                    class="departure-date-input"
                  />
                  <el-input-number
                    v-model="item.salePrice"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="售价"
                    class="departure-number-input"
                  />
                  <el-input-number
                    v-model="item.remainCount"
                    :min="0"
                    controls-position="right"
                    placeholder="余量"
                    class="departure-number-input"
                  />
                  <el-button
                    type="danger"
                    plain
                    :icon="Delete"
                    @click="removeDepartureDate(item.key)"
                  >
                    删除
                  </el-button>
                </div>

                <div v-if="item.id" class="departure-audit-info">
                  <el-tag size="small" :type="auditTag(item.auditStatus).type">
                    {{ auditTag(item.auditStatus).text }}
                  </el-tag>
                  <span class="departure-audit-text">
                    {{ item.auditRemark || "暂无审核说明" }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <el-empty v-else description="先选择路线，再维护出发日期" />
      </div>

      <template #footer>
        <el-button @click="departureManagerVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="departureSaving"
          :disabled="!managedRouteId"
          @click="saveDepartureDates"
        >
          保存出发日期
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Delete } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";
import { parseRouteDetailContent } from "../../utils/routeDetailMeta";

const router = useRouter();

const list = ref([]);
const detailVisible = ref(false);
const currentRoute = ref({});
const currentRouteDates = ref([]);

const shortcutRouteId = ref(null);
const managedRouteId = ref(null);
const departureManagerVisible = ref(false);
const departureLoading = ref(false);
const departureSaving = ref(false);

const currentRoutePlainDetail = computed(() => parseRouteDetailContent(currentRoute.value.detailContent).plainText);
const selectedManagedRoute = computed(
  () => list.value.find((item) => Number(item.id) === Number(managedRouteId.value)) || null
);

let departureDateKeySeed = 0;
const departureDates = ref([createDepartureDateItem()]);

function nextDepartureDateKey() {
  departureDateKeySeed += 1;
  return `departure-${departureDateKeySeed}`;
}

function createDepartureDateItem(source = {}) {
  return {
    key: nextDepartureDateKey(),
    id: source.id ?? null,
    departDate: source.departDate || "",
    salePrice: source.salePrice ?? Number(selectedManagedRoute.value?.price || 0),
    remainCount: source.remainCount ?? Number(selectedManagedRoute.value?.stock || 0),
    auditStatus: source.auditStatus ?? 0,
    auditRemark: source.auditRemark || ""
  };
}

function hasDetailText(detailContent) {
  return Boolean(parseRouteDetailContent(detailContent).plainText);
}

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

function normalizeDepartureDates(rows = []) {
  if (!rows.length) {
    return [createDepartureDateItem()];
  }
  return rows.map((item) =>
    createDepartureDateItem({
      id: item.id,
      departDate: item.departDate,
      salePrice: Number(item.salePrice || 0),
      remainCount: Number(item.remainCount || 0),
      auditStatus: Number(item.auditStatus ?? 0),
      auditRemark: item.auditRemark || ""
    })
  );
}

function addDepartureDate() {
  departureDates.value.push(createDepartureDateItem());
}

function removeDepartureDate(key) {
  if (departureDates.value.length === 1) {
    ElMessage.warning("请至少保留一个出发日期");
    return;
  }
  departureDates.value = departureDates.value.filter((item) => item.key !== key);
}

function buildDepartureDatesPayload() {
  return departureDates.value.map((item) => ({
    id: item.id ?? null,
    departDate: String(item.departDate || "").trim(),
    salePrice: Number(item.salePrice),
    remainCount: Number(item.remainCount)
  }));
}

function validateDepartureDates(payload) {
  if (!payload.length) {
    ElMessage.warning("请至少添加一个出发日期");
    return false;
  }

  const departDateSet = new Set();
  for (const item of payload) {
    if (!item.departDate) {
      ElMessage.warning("请完整填写出发日期");
      return false;
    }
    if (departDateSet.has(item.departDate)) {
      ElMessage.warning("出发日期不能重复");
      return false;
    }
    departDateSet.add(item.departDate);

    if (!Number.isFinite(item.salePrice) || item.salePrice < 0) {
      ElMessage.warning("请填写正确的出发价格");
      return false;
    }
    if (!Number.isFinite(item.remainCount) || item.remainCount < 0) {
      ElMessage.warning("请填写正确的出发余量");
      return false;
    }
  }

  return true;
}

async function load() {
  list.value = await api.get("/merchant/routes");
}

async function showDetail(row) {
  currentRoute.value = { ...row };
  currentRouteDates.value = await api.get(`/merchant/routes/${row.id}/dates`, null, { silent: true }).catch(() => []);
  detailVisible.value = true;
}

async function loadManagedRouteDates() {
  if (!managedRouteId.value) {
    departureDates.value = [createDepartureDateItem()];
    return;
  }

  departureLoading.value = true;
  try {
    const rows = await api.get(`/merchant/routes/${managedRouteId.value}/dates`);
    departureDates.value = normalizeDepartureDates(rows);
  } finally {
    departureLoading.value = false;
  }
}

async function openDepartureManager(routeId = shortcutRouteId.value) {
  const normalizedId = Number(routeId);
  if (!Number.isFinite(normalizedId) || normalizedId <= 0) {
    ElMessage.warning("请先选择需要维护的路线");
    return;
  }

  shortcutRouteId.value = normalizedId;
  managedRouteId.value = normalizedId;
  departureManagerVisible.value = true;
  await loadManagedRouteDates();
}

async function handleManagedRouteChange(value) {
  managedRouteId.value = Number(value);
  shortcutRouteId.value = managedRouteId.value;
  await loadManagedRouteDates();
}

async function saveDepartureDates() {
  if (!managedRouteId.value) {
    ElMessage.warning("请先选择路线");
    return;
  }

  const payload = buildDepartureDatesPayload();
  if (!validateDepartureDates(payload)) {
    return;
  }

  departureSaving.value = true;
  try {
    await api.put(`/merchant/routes/${managedRouteId.value}/dates`, {
      departureDates: payload
    });
    ElMessage.success("出发日期保存成功，新的或修改后的日期会进入单独审核");
    await loadManagedRouteDates();
    if (detailVisible.value && Number(currentRoute.value.id) === Number(managedRouteId.value)) {
      currentRouteDates.value = await api.get(`/merchant/routes/${managedRouteId.value}/dates`);
    }
  } finally {
    departureSaving.value = false;
  }
}

async function publish(id) {
  await api.post(`/merchant/routes/${id}/publish`);
  ElMessage.success("已上架");
  await load();
}

async function unpublish(id) {
  await api.post(`/merchant/routes/${id}/unpublish`);
  ElMessage.success("已下架");
  await load();
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.departure-shortcut {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.route-select {
  width: 320px;
}

.route-select-wide {
  width: 420px;
}

.route-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.route-option-name {
  color: #0f172a;
  font-weight: 600;
}

.route-option-meta {
  color: #64748b;
  font-size: 12px;
}

.route-link {
  padding: 0;
  font-weight: 600;
}

:deep(.route-detail-dialog .el-dialog),
:deep(.departure-manager-dialog .el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  color: #334155;
  font-size: 12px;
  letter-spacing: 0.18em;
}

.detail-title {
  margin: 12px 0 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.2;
}

.detail-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.cover-card {
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
}

.cover-image {
  width: 100%;
  height: 260px;
  display: block;
}

.cover-fallback {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 15px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-card,
.text-card {
  padding: 18px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.meta-label,
.text-title {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.meta-value {
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.meta-text,
.text-body {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.7;
  white-space: pre-wrap;
}

.text-body {
  margin-top: 10px;
  color: #334155;
}

.departure-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.departure-manager-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-status-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.departure-panel {
  width: 100%;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.departure-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.departure-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.departure-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.departure-row {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #dbe4ef;
  background: #fff;
}

.departure-main {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) repeat(2, minmax(160px, 1fr)) auto;
  gap: 12px;
  align-items: center;
}

.departure-audit-info {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.departure-audit-text {
  line-height: 1.5;
}

.departure-date-input,
.departure-number-input {
  width: 100%;
}

@media (max-width: 980px) {
  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .departure-main {
    grid-template-columns: 1fr;
  }

  .departure-toolbar,
  .detail-header,
  .departure-manager-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .departure-audit-info,
  .route-status-group {
    flex-wrap: wrap;
  }

  .route-select,
  .route-select-wide {
    width: 100%;
  }

  .departure-shortcut {
    justify-content: stretch;
  }
}

@media (max-width: 640px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
