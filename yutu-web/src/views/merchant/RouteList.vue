<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="ROUTE MANAGEMENT" title="路线管理" />

    <section class="page-card">
      <el-tabs v-model="activeTab" class="route-tabs">
        <el-tab-pane label="路线管理" name="route">
          <div class="route-toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="routeKeyword"
                class="toolbar-search"
                clearable
                placeholder="请输入路线名称"
                @clear="applyRouteSearch"
                @keyup.enter="applyRouteSearch"
              />
              <el-button type="primary" @click="applyRouteSearch">查询</el-button>
              <el-button @click="resetRouteSearch">重置</el-button>
              <el-button type="primary" plain @click="router.push('/merchant/route/edit')">新增路线</el-button>
            </div>
            <div class="toolbar-tip">当前展示 {{ filteredRouteList.length }} 条路线记录</div>
          </div>

          <el-table :data="filteredRouteList" border class="route-table">
            <el-table-column label="路线名称" min-width="220">
              <template #default="{ row }">
                <el-button text type="primary" class="route-link" @click="showDetail(row)">
                  {{ row.routeName }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="120">
              <template #default="{ row }">￥{{ formatPrice(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="100" />
            <el-table-column label="审核状态" width="120">
              <template #default="{ row }">
                <el-tag :type="auditTag(row.auditStatus).type" effect="light" round>
                  {{ auditTag(row.auditStatus).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上架状态" width="120">
              <template #default="{ row }">
                <el-tag :type="publishTag(row.publishStatus).type" effect="light" round>
                  {{ publishTag(row.publishStatus).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="详情标识" width="120">
              <template #default="{ row }">
                <el-tag :type="hasDetailText(row.detailContent) ? 'success' : 'warning'" effect="light" round>
                  {{ hasDetailText(row.detailContent) ? "可查看" : "待完善" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核意见" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.auditRemark || "-" }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="320" fixed="right">
              <template #default="{ row }">
                <el-button text type="info" @click="showDetail(row)">详情</el-button>
                <el-button text type="primary" @click="router.push(`/merchant/route/edit/${row.id}`)">编辑</el-button>
                <el-button text type="success" @click="publish(row.id)">上架</el-button>
                <el-button text type="warning" @click="unpublish(row.id)">下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="出发日期管理" name="departure">
          <div class="departure-manager-shell">
            <div class="departure-toolbar">
              <div class="toolbar-left">
                <el-select
                  v-model="managedRouteId"
                  clearable
                  filterable
                  class="route-select"
                  placeholder="按路线筛选出发日期管理"
                  @change="handleManagedRouteChange"
                >
                  <el-option
                    v-for="item in sortedManagedRoutes"
                    :key="item.id"
                    :label="item.routeName"
                    :value="item.id"
                  />
                </el-select>
              </div>

              <div class="departure-toolbar-actions">
                <span class="toolbar-tip">当前共 {{ departureRecordCount }} 条日期记录</span>
                <span v-if="selectedManagedRouteExpired" class="toolbar-warning">当前路线已过期，不能修改</span>
                <el-button type="primary" plain :disabled="!canEditDepartureDates" @click="addDepartureDate">
                  新增日期
                </el-button>
                <el-button
                  type="primary"
                  :loading="departureSaving"
                  :disabled="!canEditDepartureDates"
                  @click="saveDepartureDates"
                >
                  保存出发日期
                </el-button>
              </div>
            </div>

            <el-empty v-if="!list.length" description="请先新增路线，再维护出发日期" />
            <el-skeleton v-else-if="departureLoading" :rows="6" animated />
            <div v-else class="departure-table-shell">
              <el-table :data="departureTableRows" :row-key="getDepartureRowKey" border class="departure-table">
                <el-table-column label="ID" width="90">
                  <template #default="{ row, $index }">
                    {{ row.id ?? `新建-${$index + 1}` }}
                  </template>
                </el-table-column>
                <el-table-column v-if="showAllDepartureDates" label="所属路线" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ row.routeName || `路线 #${row.routeId}` }}
                  </template>
                </el-table-column>
                <el-table-column label="出发日期" min-width="190">
                  <template #default="{ row }">
                    <el-date-picker
                      v-if="canEditDepartureDates"
                      v-model="row.departDate"
                      type="date"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      placeholder="选择出发日期"
                      class="departure-date-input"
                    />
                    <span v-else class="departure-cell-text">{{ row.departDate || "-" }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="售价" width="190">
                  <template #default="{ row }">
                    <el-input-number
                      v-if="canEditDepartureDates"
                      v-model="row.salePrice"
                      :min="0"
                      :precision="2"
                      controls-position="right"
                      class="departure-number-input"
                    />
                    <span v-else class="departure-cell-text">￥{{ formatPrice(row.salePrice) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="余量" width="160">
                  <template #default="{ row }">
                    <el-input-number
                      v-if="canEditDepartureDates"
                      v-model="row.remainCount"
                      :min="0"
                      controls-position="right"
                      class="departure-number-input"
                    />
                    <span v-else class="departure-cell-text">{{ row.remainCount ?? "-" }}</span>
                  </template>
                </el-table-column>
                <el-table-column v-if="showAllDepartureDates" label="路线审核" width="120">
                  <template #default="{ row }">
                    <el-tag :type="auditTag(row.routeAuditStatus).type" effect="light" round>
                      {{ auditTag(row.routeAuditStatus).text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="日期审核" width="120">
                  <template #default="{ row }">
                    <el-tag :type="auditTag(row.auditStatus).type" effect="light" round>
                      {{ auditTag(row.auditStatus).text }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="是否过期" width="120">
                  <template #default="{ row }">
                    <el-tag :type="isRouteExpired(row.routeId) ? 'danger' : 'success'" effect="light" round>
                      {{ isRouteExpired(row.routeId) ? "已过期" : "未过期" }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="审核说明" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ row.auditRemark || "-" }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="110" fixed="right">
                  <template #default="{ row }">
                    <el-button v-if="canEditDepartureDates" text type="danger" @click="removeDepartureDate(row.key)">
                      下架
                    </el-button>
                    <el-button
                      v-else-if="!isRouteExpired(row.routeId)"
                      text
                      type="danger"
                      :loading="deletingDepartureId === row.id"
                      @click="deleteOverviewDepartureDate(row)"
                    >
                      下架
                    </el-button>
                    <span v-else class="departure-cell-text">-</span>
                  </template>
                </el-table-column>
              </el-table>

              <div v-if="canEditDepartureDates" class="departure-helper">
                新增或修改后的日期会进入单独审核，通过后才可正常售卖。
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="detailVisible" width="920px" class="route-detail-dialog" destroy-on-close>
      <template #header>
        <div class="detail-header">
          <div>
            <div class="detail-kicker">ROUTE DETAIL</div>
            <h3 class="detail-title">{{ currentRoute.routeName || "路线详情" }}</h3>
          </div>
          <div class="detail-status">
            <el-tag :type="auditTag(currentRoute.auditStatus).type" effect="light" round>
              {{ auditTag(currentRoute.auditStatus).text }}
            </el-tag>
            <el-tag :type="publishTag(currentRoute.publishStatus).type" effect="light" round>
              {{ publishTag(currentRoute.publishStatus).text }}
            </el-tag>
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
          <el-table :data="currentRouteDates" border class="detail-date-table">
            <el-table-column prop="departDate" label="出发日期" min-width="150" />
            <el-table-column label="售价" width="120">
              <template #default="{ row }">￥{{ formatPrice(row.salePrice) }}</template>
            </el-table-column>
            <el-table-column prop="remainCount" label="余量" width="100" />
            <el-table-column label="审核状态" width="120">
              <template #default="{ row }">
                <el-tag :type="auditTag(row.auditStatus).type" effect="light" round>
                  {{ auditTag(row.auditStatus).text }}
                </el-tag>
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
        <el-button type="primary" @click="router.push(`/merchant/route/edit/${currentRoute.id}`)">编辑路线</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";
import { parseRouteDetailContent } from "../../utils/routeDetailMeta";

const router = useRouter();

const activeTab = ref("route");
const routeKeyword = ref("");
const appliedRouteKeyword = ref("");
const list = ref([]);
const allDepartureDates = ref([]);
const detailVisible = ref(false);
const currentRoute = ref({});
const currentRouteDates = ref([]);

const managedRouteId = ref(null);
const departureLoading = ref(false);
const departureSaving = ref(false);
const departureDataLoaded = ref(false);
const deletingDepartureId = ref(null);

const currentRoutePlainDetail = computed(() => parseRouteDetailContent(currentRoute.value?.detailContent).plainText);
const filteredRouteList = computed(() => {
  const keyword = appliedRouteKeyword.value.trim();
  if (!keyword) {
    return list.value;
  }
  return list.value.filter((item) => String(item.routeName || "").includes(keyword));
});
const selectedManagedRoute = computed(
  () => list.value.find((item) => Number(item.id) === Number(managedRouteId.value)) || null
);
const routeExpiryMap = computed(() => {
  const expiryMap = new Map();
  const groupedDates = new Map();

  allDepartureDates.value.forEach((item) => {
    const routeId = Number(item.routeId);
    if (!Number.isFinite(routeId)) {
      return;
    }
    if (!groupedDates.has(routeId)) {
      groupedDates.set(routeId, []);
    }
    if (item.departDate) {
      groupedDates.get(routeId).push(String(item.departDate));
    }
  });

  list.value.forEach((route) => {
    const routeId = Number(route.id);
    const routeDates = groupedDates.get(routeId) || [];
    const latestDate = routeDates.length ? [...routeDates].sort().at(-1) : "";
    expiryMap.set(routeId, Boolean(latestDate) && latestDate < getTodayDateString());
  });

  return expiryMap;
});
const sortedManagedRoutes = computed(() => [...list.value].sort(compareRoutesByExpiryAndId));
const selectedManagedRouteExpired = computed(
  () => Boolean(selectedManagedRoute.value) && isRouteExpired(selectedManagedRoute.value.id)
);
const canEditDepartureDates = computed(
  () => Boolean(selectedManagedRoute.value) && !selectedManagedRouteExpired.value
);
const showAllDepartureDates = computed(() => !selectedManagedRoute.value);

let departureDateKeySeed = 0;
const departureDates = ref([createDepartureDateItem()]);

const departureTableRows = computed(() => {
  const rows = showAllDepartureDates.value ? allDepartureDates.value : departureDates.value;
  return [...rows].sort(compareDepartureRows);
});
const departureRecordCount = computed(() => departureTableRows.value.length);

function nextDepartureDateKey() {
  departureDateKeySeed += 1;
  return `departure-${departureDateKeySeed}`;
}

function getTodayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getSortableId(item) {
  const value = Number(item?.id);
  return Number.isFinite(value) && value > 0 ? value : Number.MAX_SAFE_INTEGER;
}

function isRouteExpired(routeId) {
  return routeExpiryMap.value.get(Number(routeId)) === true;
}

function compareRoutesByExpiryAndId(left, right) {
  const expiryDiff = Number(isRouteExpired(left?.id)) - Number(isRouteExpired(right?.id));
  if (expiryDiff !== 0) {
    return expiryDiff;
  }
  return getSortableId(left) - getSortableId(right);
}

function compareDepartureRows(left, right) {
  const leftRouteId = left?.routeId ?? selectedManagedRoute.value?.id;
  const rightRouteId = right?.routeId ?? selectedManagedRoute.value?.id;
  const expiryDiff = Number(isRouteExpired(leftRouteId)) - Number(isRouteExpired(rightRouteId));
  if (expiryDiff !== 0) {
    return expiryDiff;
  }
  return getSortableId(left) - getSortableId(right);
}

function createDepartureDateItem(source = {}) {
  return {
    key: nextDepartureDateKey(),
    id: source.id ?? null,
    routeId: source.routeId ?? selectedManagedRoute.value?.id ?? null,
    routeName: source.routeName ?? selectedManagedRoute.value?.routeName ?? "",
    routeAuditStatus: source.routeAuditStatus ?? selectedManagedRoute.value?.auditStatus ?? 0,
    routePublishStatus: source.routePublishStatus ?? selectedManagedRoute.value?.publishStatus ?? 0,
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

function applyRouteSearch() {
  appliedRouteKeyword.value = routeKeyword.value.trim();
}

function resetRouteSearch() {
  routeKeyword.value = "";
  appliedRouteKeyword.value = "";
}

function normalizeDepartureDates(rows = []) {
  if (!rows.length) {
    return [createDepartureDateItem()];
  }
  return rows.map((item) =>
    createDepartureDateItem({
      id: item.id,
      routeId: selectedManagedRoute.value?.id ?? null,
      routeName: selectedManagedRoute.value?.routeName ?? "",
      routeAuditStatus: selectedManagedRoute.value?.auditStatus ?? 0,
      routePublishStatus: selectedManagedRoute.value?.publishStatus ?? 0,
      departDate: item.departDate,
      salePrice: Number(item.salePrice || 0),
      remainCount: Number(item.remainCount || 0),
      auditStatus: Number(item.auditStatus ?? 0),
      auditRemark: item.auditRemark || ""
    })
  );
}

function normalizeOverviewDepartureDates(rows = []) {
  return rows.map((item) =>
    createDepartureDateItem({
      id: item.id,
      routeId: item.routeId,
      routeName: item.routeName || "",
      routeAuditStatus: Number(item.routeAuditStatus ?? 0),
      routePublishStatus: Number(item.routePublishStatus ?? 0),
      departDate: item.departDate,
      salePrice: Number(item.salePrice || 0),
      remainCount: Number(item.remainCount || 0),
      auditStatus: Number(item.auditStatus ?? 0),
      auditRemark: item.auditRemark || ""
    })
  );
}

function getDepartureRowKey(row) {
  return row.key || row.id || `${row.routeId || "all"}-${row.departDate || "empty"}`;
}

function addDepartureDate() {
  if (!canEditDepartureDates.value) {
    ElMessage.warning(selectedManagedRouteExpired.value ? "当前路线已过期，不能修改" : "请先选择路线");
    return;
  }
  departureDates.value.push(createDepartureDateItem());
}

function removeDepartureDate(key) {
  if (!canEditDepartureDates.value) {
    ElMessage.warning(selectedManagedRouteExpired.value ? "当前路线已过期，不能修改" : "请先选择路线");
    return;
  }
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

function buildRouteDatesPayload(rows = []) {
  return rows.map((item) => ({
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
      ElMessage.warning("请填写正确的剩余名额");
      return false;
    }
  }

  return true;
}

async function loadAllDepartureDates() {
  const rows = await api.get("/merchant/routes/departure-dates");
  allDepartureDates.value = normalizeOverviewDepartureDates(rows);
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

async function loadRouteList() {
  list.value = await api.get("/merchant/routes");
  const selectedStillExists = list.value.some((item) => Number(item.id) === Number(managedRouteId.value));
  if (!selectedStillExists) {
    managedRouteId.value = null;
  }
}

async function loadDepartureTabData(force = false) {
  if (departureDataLoaded.value && !force) {
    return;
  }

  departureLoading.value = true;
  try {
    await loadAllDepartureDates();
    if (managedRouteId.value) {
      const rows = await api.get(`/merchant/routes/${managedRouteId.value}/dates`);
      departureDates.value = normalizeDepartureDates(rows);
    } else {
      departureDates.value = [createDepartureDateItem()];
    }
    departureDataLoaded.value = true;
  } finally {
    departureLoading.value = false;
  }
}

async function load() {
  await loadRouteList();
  if (activeTab.value === "departure") {
    await loadDepartureTabData(true);
  }
}

async function showDetail(row) {
  currentRoute.value = { ...row };
  currentRouteDates.value = await api.get(`/merchant/routes/${row.id}/dates`, null, { silent: true }).catch(() => []);
  detailVisible.value = true;
}

async function handleManagedRouteChange(value) {
  const normalizedId = Number(value);
  managedRouteId.value = Number.isFinite(normalizedId) && normalizedId > 0 ? normalizedId : null;

  if (!managedRouteId.value) {
    departureDates.value = [createDepartureDateItem()];
    return;
  }

  await loadManagedRouteDates();
}

async function saveDepartureDates() {
  if (!managedRouteId.value) {
    ElMessage.warning("请先选择路线");
    return;
  }
  if (selectedManagedRouteExpired.value) {
    ElMessage.warning("当前路线已过期，不能修改");
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
    ElMessage.success("出发日期保存成功，新增或修改后的日期会进入单独审核");
    await loadManagedRouteDates();
    await loadAllDepartureDates();
    departureDataLoaded.value = true;
    if (detailVisible.value && Number(currentRoute.value.id) === Number(managedRouteId.value)) {
      currentRouteDates.value = await api.get(`/merchant/routes/${managedRouteId.value}/dates`);
    }
  } finally {
    departureSaving.value = false;
  }
}

async function deleteOverviewDepartureDate(row) {
  const routeId = Number(row?.routeId);
  const departureId = Number(row?.id);
  if (!Number.isFinite(routeId) || !Number.isFinite(departureId)) {
    ElMessage.warning("未找到可删除的出发日期");
    return;
  }
  if (isRouteExpired(routeId)) {
    ElMessage.warning("当前路线已过期，不能修改");
    return;
  }

  try {
    await ElMessageBox.confirm("确定下架这条出发日期吗？", "下架确认", {
      type: "warning",
      confirmButtonText: "下架",
      cancelButtonText: "取消"
    });
  } catch {
    return;
  }

  deletingDepartureId.value = departureId;
  try {
    const routeDates = await api.get(`/merchant/routes/${routeId}/dates`);
    if (!Array.isArray(routeDates) || routeDates.length <= 1) {
      ElMessage.warning("请至少保留一个出发日期");
      return;
    }

    const nextRows = routeDates.filter((item) => Number(item.id) !== departureId);
    await api.put(`/merchant/routes/${routeId}/dates`, {
      departureDates: buildRouteDatesPayload(nextRows)
    });

    ElMessage.success("下架成功");
    await loadAllDepartureDates();
    departureDataLoaded.value = true;
    if (Number(managedRouteId.value) === routeId) {
      await loadManagedRouteDates();
    }
    if (detailVisible.value && Number(currentRoute.value.id) === routeId) {
      currentRouteDates.value = await api.get(`/merchant/routes/${routeId}/dates`);
    }
  } finally {
    deletingDepartureId.value = null;
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

watch(activeTab, async (value) => {
  if (value === "departure") {
    await loadDepartureTabData();
  }
});

onMounted(load);
</script>

<style scoped>
.route-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.route-toolbar,
.departure-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1 1 720px;
  min-width: 0;
}

.toolbar-search {
  flex: 0 1 520px;
  width: 520px;
  max-width: 100%;
}

.toolbar-tip {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.route-table,
.departure-table {
  border-radius: 16px;
  overflow: hidden;
}

.route-link {
  padding: 0;
  font-weight: 600;
}

.departure-manager-shell {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.route-select {
  width: 420px;
  max-width: 100%;
}

.departure-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-warning {
  color: #dc2626;
  font-size: 13px;
  white-space: nowrap;
}

.departure-table-shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 2px;
}

.departure-date-input,
.departure-number-input {
  width: 100%;
}

.departure-cell-text {
  color: #334155;
}

.departure-helper {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  text-align: right;
}

:deep(.route-detail-dialog .el-dialog) {
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
  flex-wrap: wrap;
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

.detail-date-table {
  margin-top: 10px;
}

@media (max-width: 980px) {
  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .detail-header {
    flex-direction: column;
    align-items: stretch;
  }

  .route-select,
  .toolbar-search {
    width: 100%;
    flex-basis: 100%;
  }

  .route-toolbar,
  .departure-toolbar,
  .toolbar-left {
    align-items: stretch;
  }

  .departure-toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
