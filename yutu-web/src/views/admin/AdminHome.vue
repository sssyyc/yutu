<template>
  <div class="admin-home">
    <section class="hero-card">
      <div>
        <div class="hero-kicker">Admin Overview</div>
        <h2>平台首页</h2>
        <p>
          在这里统一查看用户增长、商户审核、资源上线、交易和投诉情况。常用模块已集中到左侧导航，首页用于承载核心概览。
        </p>
      </div>
    </section>

    <section class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-hint">{{ card.hint }}</div>
      </div>
    </section>

    <section class="chart-grid">
      <div class="page-card chart-card">
        <div class="section-title">订单趋势</div>
        <div ref="orderTrendRef" class="chart-box"></div>
      </div>

      <div class="page-card chart-card">
        <div class="section-title">投诉趋势</div>
        <div ref="complaintTrendRef" class="chart-box"></div>
      </div>
    </section>

    <section class="page-card ranking-card">
      <div class="section-title">热门路线排行</div>
      <div ref="topRoutesRef" class="chart-wide"></div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";
import { api } from "../../api";

const overview = ref(null);
const orderTrendRef = ref(null);
const complaintTrendRef = ref(null);
const topRoutesRef = ref(null);
const chartInstances = [];

const statCards = computed(() => [
  {
    label: "累计用户数",
    value: overview.value?.userCount ?? 0,
    hint: "平台已注册用户"
  },
  {
    label: "活跃商户数",
    value: overview.value?.merchantCount ?? 0,
    hint: "审核通过的商户"
  },
  {
    label: "上线产品数",
    value: overview.value?.routeCount ?? 0,
    hint: "当前可售路线"
  },
  {
    label: "累计订单量",
    value: overview.value?.orderCount ?? 0,
    hint: "平台订单总数"
  },
  {
    label: "交易总额",
    value: overview.value?.turnover ?? 0,
    hint: "含已支付与已退款订单"
  },
  {
    label: "投诉量",
    value: overview.value?.complaintCount ?? 0,
    hint: "累计投诉记录"
  }
]);

onMounted(async () => {
  overview.value = await api.get("/admin/dashboard/overview");
  const orderTrend = await api.get("/admin/dashboard/order-trend");
  const complaintTrend = await api.get("/admin/dashboard/complaint-trend");
  const topRoutes = await api.get("/admin/dashboard/top-routes");

  await nextTick();
  renderLineChart(orderTrendRef.value, "订单数", orderTrend, "#0f766e");
  renderLineChart(complaintTrendRef.value, "投诉数", complaintTrend, "#dc2626");
  renderBarChart(topRoutesRef.value, topRoutes);
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  chartInstances.forEach((chart) => chart.dispose());
});

function resizeCharts() {
  chartInstances.forEach((chart) => chart.resize());
}

function renderLineChart(el, name, list, color) {
  if (!el) return;
  const chart = echarts.init(el);
  chart.setOption({
    color: [color],
    tooltip: { trigger: "axis" },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: {
      type: "category",
      data: list.map((item) => item.date),
      axisLine: { lineStyle: { color: "#cbd5e1" } },
      axisLabel: { color: "#64748b" }
    },
    yAxis: {
      type: "value",
      splitLine: { lineStyle: { color: "#e2e8f0" } },
      axisLabel: { color: "#64748b" }
    },
    series: [
      {
        name,
        type: "line",
        smooth: true,
        data: list.map((item) => item.count),
        areaStyle: { opacity: 0.12 }
      }
    ]
  });
  chartInstances.push(chart);
}

function renderBarChart(el, list) {
  if (!el) return;
  const chart = echarts.init(el);
  chart.setOption({
    color: ["#2563eb"],
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: 40, right: 20, top: 24, bottom: 40 },
    xAxis: {
      type: "category",
      data: list.map((item) => item.routeName),
      axisLabel: {
        color: "#64748b",
        interval: 0,
        rotate: list.length > 4 ? 18 : 0
      },
      axisLine: { lineStyle: { color: "#cbd5e1" } }
    },
    yAxis: {
      type: "value",
      splitLine: { lineStyle: { color: "#e2e8f0" } },
      axisLabel: { color: "#64748b" }
    },
    series: [
      {
        name: "订单量",
        type: "bar",
        barWidth: 34,
        borderRadius: [10, 10, 0, 0],
        data: list.map((item) => item.orderCount)
      }
    ]
  });
  chartInstances.push(chart);
}
</script>

<style scoped>
.admin-home {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-card {
  display: flex;
  justify-content: flex-start;
  gap: 24px;
  padding: 26px 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f172a, #134e4a 55%, #1d4ed8);
  color: #f8fafc;
}

.hero-kicker {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-card h2 {
  margin: 14px 0 10px;
  font-size: 34px;
}

.hero-card p {
  max-width: 760px;
  margin: 0;
  color: rgba(248, 250, 252, 0.86);
  line-height: 1.8;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  padding: 18px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
}

.stat-hint {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.chart-card,
.ranking-card {
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.section-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.chart-box {
  width: 100%;
  height: 320px;
}

.chart-wide {
  width: 100%;
  height: 360px;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-card,
  .chart-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
