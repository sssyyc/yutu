<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="STATISTICS"
      title="统计分析"
    />

    <section class="page-card filter-card">
      <div class="filter-left">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          unlink-panels
        />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetRange">重置</el-button>
      </div>
      <el-button type="success" @click="exportCurrent">导出当前分析</el-button>
    </section>

    <el-tabs v-model="activeType" class="stats-tabs">
      <el-tab-pane label="商家数据分析" name="merchant">
        <section class="metrics-grid">
          <div class="metric-card">
            <div class="metric-label">商户总量</div>
            <div class="metric-value">{{ merchantOverview.totalMerchantCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">已通过商户</div>
            <div class="metric-value">{{ merchantOverview.approvedMerchantCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">区间入驻量</div>
            <div class="metric-value">{{ merchantOverview.newMerchantCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">活跃商户数</div>
            <div class="metric-value">{{ merchantOverview.activeMerchantCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">活跃占比</div>
            <div class="metric-value">{{ merchantOverview.activeRatio || 0 }}%</div>
          </div>
        </section>

        <section class="chart-grid">
          <div class="page-card chart-card">
            <div class="chart-title">商户入驻趋势</div>
            <div ref="merchantTrendRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">商户活跃趋势</div>
            <div ref="merchantActiveRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">交易额排行</div>
            <div ref="merchantAmountRankRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">投诉量排行</div>
            <div ref="merchantComplaintRankRef" class="chart-box"></div>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="客户数据分析" name="customer">
        <section class="metrics-grid">
          <div class="metric-card">
            <div class="metric-label">用户总量</div>
            <div class="metric-value">{{ customerOverview.totalUserCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">区间注册量</div>
            <div class="metric-value">{{ customerOverview.newRegisterCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">活跃用户数</div>
            <div class="metric-value">{{ customerOverview.activeUserCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">活跃度</div>
            <div class="metric-value">{{ customerOverview.activeRate || 0 }}%</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">复购率</div>
            <div class="metric-value">{{ customerOverview.repeatPurchaseRate || 0 }}%</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">平均客单价</div>
            <div class="metric-value">¥{{ customerOverview.avgOrderAmount || 0 }}</div>
          </div>
        </section>

        <section class="profile-card page-card">
          <div>主要地域：{{ customerProfile.topRegion || "暂无" }}</div>
          <div>偏好品类：{{ customerProfile.topPreference || "暂无" }}</div>
          <div>高价值用户：{{ customerProfile.highValueUserCount || 0 }}</div>
          <div>复购用户：{{ customerProfile.repeatPurchaseUserCount || 0 }}</div>
        </section>

        <section class="chart-grid">
          <div class="page-card chart-card">
            <div class="chart-title">用户注册趋势</div>
            <div ref="customerRegisterTrendRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">地域分布</div>
            <div ref="customerRegionRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">消费偏好排行</div>
            <div ref="customerPreferenceRef" class="chart-box"></div>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="平台运营数据分析" name="platform">
        <section class="metrics-grid">
          <div class="metric-card">
            <div class="metric-label">订单量</div>
            <div class="metric-value">{{ platformOverview.orderCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">支付订单量</div>
            <div class="metric-value">{{ platformOverview.paidOrderCount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">交易额</div>
            <div class="metric-value">¥{{ platformOverview.turnover || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">平均客单价</div>
            <div class="metric-value">¥{{ platformOverview.avgOrderAmount || 0 }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">节假日营收</div>
            <div class="metric-value">¥{{ platformOverview.holidayTurnover || 0 }}</div>
          </div>
        </section>

        <section class="chart-grid">
          <div class="page-card chart-card">
            <div class="chart-title">订单量/交易额趋势</div>
            <div ref="platformOrderTurnoverRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">品类销售占比</div>
            <div ref="platformCategoryShareRef" class="chart-box"></div>
          </div>
          <div class="page-card chart-card">
            <div class="chart-title">节假日营收趋势</div>
            <div ref="platformHolidayTrendRef" class="chart-box"></div>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const activeType = ref("merchant");
const dateRange = ref(defaultDateRange());
const legacyMode = ref(false);

const merchantData = ref({});
const customerData = ref({});
const platformData = ref({});

const merchantTrendRef = ref(null);
const merchantActiveRef = ref(null);
const merchantAmountRankRef = ref(null);
const merchantComplaintRankRef = ref(null);
const customerRegisterTrendRef = ref(null);
const customerRegionRef = ref(null);
const customerPreferenceRef = ref(null);
const platformOrderTurnoverRef = ref(null);
const platformCategoryShareRef = ref(null);
const platformHolidayTrendRef = ref(null);

const chartMap = {};

const merchantOverview = computed(() => merchantData.value?.overview || {});
const customerOverview = computed(() => customerData.value?.overview || {});
const customerProfile = computed(() => customerData.value?.profile || {});
const platformOverview = computed(() => platformData.value?.overview || {});

onMounted(async () => {
  await loadData();
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  Object.keys(chartMap).forEach((key) => disposeChart(key));
});

watch(activeType, async () => {
  await nextTick();
  renderActiveTabCharts();
});

function defaultDateRange() {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 29);
  return [formatDate(start), formatDate(end)];
}

function formatDate(date) {
  const y = date.getFullYear();
  const m = `${date.getMonth() + 1}`.padStart(2, "0");
  const d = `${date.getDate()}`.padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function buildParams() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    return {};
  }
  return { startDate: dateRange.value[0], endDate: dateRange.value[1] };
}

async function silentGet(url, params) {
  return api.get(url, params, { silent: true });
}

async function tryGet(url, params) {
  try {
    return await silentGet(url, params);
  } catch (error) {
    if (error?.response?.status === 404) {
      return null;
    }
    throw error;
  }
}

async function loadData() {
  const params = buildParams();
  try {
    const [merchant, customer, platform] = await Promise.all([
      tryGet("/admin/statistics/merchant", params),
      tryGet("/admin/statistics/customer", params),
      tryGet("/admin/statistics/platform", params)
    ]);

    if (merchant && customer && platform) {
      legacyMode.value = false;
      merchantData.value = merchant;
      customerData.value = customer;
      platformData.value = platform;
    } else {
      legacyMode.value = true;
      await loadLegacyData();
    }

    await nextTick();
    renderActiveTabCharts();
  } catch (error) {
    merchantData.value = {};
    customerData.value = {};
    platformData.value = {};
    ElMessage.error(error?.message || "统计数据加载失败");
  }
}

async function loadLegacyData() {
  const [overview, orderTrend, complaintTrend, topRoutes] = await Promise.all([
    silentGet("/admin/statistics"),
    silentGet("/admin/dashboard/order-trend").catch(() => []),
    silentGet("/admin/dashboard/complaint-trend").catch(() => []),
    silentGet("/admin/dashboard/top-routes").catch(() => [])
  ]);

  const userCount = Number(overview?.userCount || 0);
  const merchantCount = Number(overview?.merchantCount || 0);
  const orderCount = Number(overview?.orderCount || 0);
  const turnover = Number(overview?.turnover || 0);
  const avgOrderAmount = orderCount > 0 ? (turnover / orderCount).toFixed(2) : 0;
  const turnoverTrend = buildTurnoverTrendFromOrder(orderTrend, turnover);

  merchantData.value = {
    overview: {
      totalMerchantCount: merchantCount,
      approvedMerchantCount: merchantCount,
      newMerchantCount: 0,
      activeMerchantCount: merchantCount,
      activeRatio: merchantCount > 0 ? 100 : 0
    },
    settlementTrend: orderTrend || [],
    activeTrend: orderTrend || [],
    transactionRank: (topRoutes || []).map((item) => ({
      merchantName: item.routeName || "示例项",
      amount: Number(item.orderCount || 0)
    })),
    complaintRank: (complaintTrend || []).slice(-10).map((item) => ({
      merchantName: item.date,
      complaintCount: Number(item.count || 0)
    }))
  };

  customerData.value = {
    overview: {
      totalUserCount: userCount,
      newRegisterCount: 0,
      activeUserCount: userCount,
      activeRate: userCount > 0 ? 100 : 0,
      repeatPurchaseRate: 0,
      avgOrderAmount
    },
    profile: {
      topRegion: "暂缺（旧版接口）",
      topPreference: "暂缺（旧版接口）",
      highValueUserCount: 0,
      repeatPurchaseUserCount: 0
    },
    registerTrend: orderTrend || [],
    regionDistribution: [],
    preferenceRank: []
  };

  platformData.value = {
    overview: {
      orderCount,
      paidOrderCount: orderCount,
      turnover,
      avgOrderAmount,
      holidayTurnover: 0
    },
    orderTrend: orderTrend || [],
    turnoverTrend,
    categoryShare: [],
    holidayRevenueTrend: []
  };
}

function buildTurnoverTrendFromOrder(orderTrend, turnover) {
  const list = orderTrend || [];
  const totalCount = list.reduce((sum, item) => sum + Number(item.count || 0), 0);
  if (totalCount <= 0) {
    return list.map((item) => ({ date: item.date, amount: 0 }));
  }
  return list.map((item) => ({
    date: item.date,
    amount: Number(((Number(item.count || 0) * turnover) / totalCount).toFixed(2))
  }));
}

function resetRange() {
  dateRange.value = defaultDateRange();
  loadData();
}

async function exportCurrent() {
  const params = { ...buildParams(), type: activeType.value };
  try {
    const payload = await silentGet("/admin/statistics/export", params);
    triggerDownload(payload.fileName, payload.content, "text/csv;charset=utf-8");
    ElMessage.success("导出成功");
  } catch (error) {
    if (error?.response?.status === 404) {
      const content = buildLocalCsv(activeType.value);
      triggerDownload(`${activeType.value}-statistics-local.csv`, content, "text/csv;charset=utf-8");
      ElMessage.success("当前后端版本不支持导出，已使用前端兼容导出");
      return;
    }
    ElMessage.error(error?.message || "导出失败");
  }
}

function buildLocalCsv(type) {
  const lines = ["type,key,value"];
  const source =
    type === "merchant"
      ? merchantData.value
      : type === "customer"
      ? customerData.value
      : platformData.value;
  const overview = source?.overview || {};
  Object.keys(overview).forEach((key) => {
    lines.push(`${type},${key},${overview[key] ?? ""}`);
  });
  return lines.join("\n");
}

function triggerDownload(fileName, content, mimeType) {
  const blob = new Blob([content || ""], { type: mimeType || "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName || "statistics.csv";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function resizeCharts() {
  Object.keys(chartMap).forEach((key) => chartMap[key]?.resize());
}

function disposeChart(key) {
  if (chartMap[key]) {
    chartMap[key].dispose();
    delete chartMap[key];
  }
}

function createChart(key, el, option) {
  if (!el) return;
  disposeChart(key);
  const chart = echarts.init(el);
  chart.setOption(option);
  chartMap[key] = chart;
}

function renderActiveTabCharts() {
  if (activeType.value === "merchant") {
    renderMerchantCharts();
  } else if (activeType.value === "customer") {
    renderCustomerCharts();
  } else {
    renderPlatformCharts();
  }
}

function renderMerchantCharts() {
  const settlementTrend = merchantData.value?.settlementTrend || [];
  const activeTrend = merchantData.value?.activeTrend || [];
  const txRank = merchantData.value?.transactionRank || [];
  const complaintRank = merchantData.value?.complaintRank || [];

  createChart("merchantTrend", merchantTrendRef.value, {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: settlementTrend.map((i) => i.date) },
    yAxis: { type: "value" },
    series: [{ type: "line", smooth: true, areaStyle: {}, data: settlementTrend.map((i) => Number(i.count || 0)) }]
  });

  createChart("merchantActive", merchantActiveRef.value, {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: activeTrend.map((i) => i.date) },
    yAxis: { type: "value" },
    series: [{ type: "line", smooth: true, areaStyle: {}, data: activeTrend.map((i) => Number(i.count || 0)) }]
  });

  createChart("merchantAmountRank", merchantAmountRankRef.value, {
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: 24, right: 24, top: 20, bottom: 36, containLabel: true },
    xAxis: { type: "value" },
    yAxis: { type: "category", data: txRank.map((i) => i.merchantName) },
    series: [{ type: "bar", data: txRank.map((i) => Number(i.amount || 0)), barWidth: 16 }]
  });

  createChart("merchantComplaintRank", merchantComplaintRankRef.value, {
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: 24, right: 24, top: 20, bottom: 36, containLabel: true },
    xAxis: { type: "value" },
    yAxis: { type: "category", data: complaintRank.map((i) => i.merchantName) },
    series: [{ type: "bar", data: complaintRank.map((i) => Number(i.complaintCount || 0)), barWidth: 16 }]
  });
}

function renderCustomerCharts() {
  const registerTrend = customerData.value?.registerTrend || [];
  const regionDist = customerData.value?.regionDistribution || [];
  const preferenceRank = customerData.value?.preferenceRank || [];

  createChart("customerRegisterTrend", customerRegisterTrendRef.value, {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: registerTrend.map((i) => i.date) },
    yAxis: { type: "value" },
    series: [{ type: "line", smooth: true, areaStyle: {}, data: registerTrend.map((i) => Number(i.count || 0)) }]
  });

  createChart("customerRegion", customerRegionRef.value, {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [{ type: "pie", radius: ["40%", "68%"], data: regionDist.map((i) => ({ name: i.region, value: Number(i.count || 0) })) }]
  });

  createChart("customerPreference", customerPreferenceRef.value, {
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: 24, right: 24, top: 20, bottom: 36, containLabel: true },
    xAxis: { type: "category", data: preferenceRank.map((i) => i.categoryName) },
    yAxis: { type: "value" },
    series: [{ type: "bar", data: preferenceRank.map((i) => Number(i.orderCount || 0)), barWidth: 24 }]
  });
}

function renderPlatformCharts() {
  const orderTrend = platformData.value?.orderTrend || [];
  const turnoverTrend = platformData.value?.turnoverTrend || [];
  const categoryShare = platformData.value?.categoryShare || [];
  const holidayTrend = platformData.value?.holidayRevenueTrend || [];

  createChart("platformOrderTurnover", platformOrderTurnoverRef.value, {
    tooltip: { trigger: "axis" },
    legend: { data: ["订单量", "交易额"] },
    grid: { left: 24, right: 36, top: 40, bottom: 36, containLabel: true },
    xAxis: { type: "category", data: orderTrend.map((i) => i.date) },
    yAxis: [{ type: "value", name: "订单量" }, { type: "value", name: "交易额" }],
    series: [
      { name: "订单量", type: "bar", data: orderTrend.map((i) => Number(i.count || 0)), barWidth: 14 },
      { name: "交易额", type: "line", yAxisIndex: 1, smooth: true, data: turnoverTrend.map((i) => Number(i.amount || 0)) }
    ]
  });

  createChart("platformCategoryShare", platformCategoryShareRef.value, {
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [{ type: "pie", radius: ["36%", "66%"], data: categoryShare.map((i) => ({ name: i.categoryName, value: Number(i.amount || 0) })) }]
  });

  createChart("platformHolidayTrend", platformHolidayTrendRef.value, {
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: holidayTrend.map((i) => i.date) },
    yAxis: { type: "value" },
    series: [{ type: "line", smooth: true, areaStyle: {}, data: holidayTrend.map((i) => Number(i.amount || 0)) }]
  });
}
</script>

<style scoped>
.filter-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.stats-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.metric-card {
  padding: 16px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.profile-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
  font-size: 14px;
  color: #334155;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.chart-card {
  border-radius: 16px;
}

.chart-title {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.chart-box {
  width: 100%;
  height: 320px;
}

@media (max-width: 1280px) {
  .profile-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .profile-card {
    grid-template-columns: 1fr;
  }
}
</style>
