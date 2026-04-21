<template>
  <div class="admin-module-page merchant-home-page">
    <AdminPageHero
      kicker="MERCHANT OVERVIEW"
      title="商家首页"
    />

    <section class="metrics-grid">
      <article class="metric-card metric-blue">
        <div class="metric-label">总订单</div>
        <div class="metric-value">{{ overview.totalOrders || 0 }}</div>
        <div class="metric-desc">本店累计订单数</div>
      </article>

      <article class="metric-card metric-cyan">
        <div class="metric-label">已支付</div>
        <div class="metric-value">{{ overview.paidOrders || 0 }}</div>
        <div class="metric-desc">已完成支付订单</div>
      </article>

      <article class="metric-card metric-amber">
        <div class="metric-label">退款单</div>
        <div class="metric-value">{{ overview.refundOrders || 0 }}</div>
        <div class="metric-desc">当前退款相关订单</div>
      </article>

      <article class="metric-card metric-emerald">
        <div class="metric-label">营业额</div>
        <div class="metric-value">¥{{ overview.turnover || 0 }}</div>
        <div class="metric-desc">累计支付金额</div>
      </article>
    </section>

    <section class="chart-grid">
      <div class="page-card chart-card">
        <div class="section-title">路线订单排行</div>
        <div ref="ordersChartRef" class="chart-box"></div>
      </div>
      <div class="page-card chart-card">
        <div class="section-title">评价星级分布</div>
        <div ref="reviewChartRef" class="chart-box"></div>
      </div>
    </section>

    <section class="page-card">
      <div class="section-title">最新评论</div>
      <el-table :data="reviewList.slice(0, 10)" border>
        <el-table-column prop="routeId" label="路线ID" width="100" />
        <el-table-column prop="score" label="评分" width="90" />
        <el-table-column prop="content" label="内容" min-width="260" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import * as echarts from "echarts";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const overview = reactive({
  totalOrders: 0,
  paidOrders: 0,
  refundOrders: 0,
  turnover: 0
});

const routeOrders = ref([]);
const reviewList = ref([]);

const ordersChartRef = ref(null);
const reviewChartRef = ref(null);

let ordersChart = null;
let reviewChart = null;

onMounted(async () => {
  await loadData();
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  ordersChart?.dispose();
  reviewChart?.dispose();
});

async function loadData() {
  const [overviewRes, ordersRes, reviewsRes] = await Promise.all([
    api.get("/merchant/stats/overview"),
    api.get("/merchant/stats/orders"),
    api.get("/merchant/stats/reviews")
  ]);

  Object.assign(overview, overviewRes || {});
  routeOrders.value = ordersRes || [];
  reviewList.value = reviewsRes || [];

  await nextTick();
  renderOrdersChart();
  renderReviewChart();
}

function resizeCharts() {
  ordersChart?.resize();
  reviewChart?.resize();
}

function renderOrdersChart() {
  if (!ordersChartRef.value) return;
  ordersChart?.dispose();
  ordersChart = echarts.init(ordersChartRef.value);
  ordersChart.setOption({
    tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    grid: { left: 22, right: 22, top: 28, bottom: 36, containLabel: true },
    xAxis: {
      type: "category",
      data: routeOrders.value.map((item) => item.routeName),
      axisLabel: {
        interval: 0,
        rotate: routeOrders.value.length > 4 ? 18 : 0
      }
    },
    yAxis: { type: "value" },
    series: [
      {
        type: "bar",
        data: routeOrders.value.map((item) => Number(item.orderCount || 0)),
        barWidth: 26,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: "#1d4ed8"
        }
      }
    ]
  });
}

function renderReviewChart() {
  if (!reviewChartRef.value) return;
  reviewChart?.dispose();
  reviewChart = echarts.init(reviewChartRef.value);

  const scoreCount = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  reviewList.value.forEach((review) => {
    const score = Number(review.score || 0);
    if (scoreCount[score] !== undefined) scoreCount[score] += 1;
  });

  reviewChart.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [
      {
        type: "pie",
        radius: ["40%", "66%"],
        data: Object.keys(scoreCount).map((score) => ({
          name: `${score}星`,
          value: scoreCount[score]
        }))
      }
    ]
  });
}
</script>

<style scoped>
.merchant-home-page {
  gap: 20px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  border-radius: 18px;
  padding: 18px 18px 16px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.metric-label {
  font-size: 13px;
  font-weight: 600;
  color: #5b6b84;
}

.metric-value {
  margin-top: 8px;
  font-size: 36px;
  line-height: 1.15;
  font-weight: 800;
  color: #0f172a;
}

.metric-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.metric-blue {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.13), rgba(59, 130, 246, 0.03));
}

.metric-cyan {
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.13), rgba(6, 182, 212, 0.03));
}

.metric-amber {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.16), rgba(245, 158, 11, 0.03));
}

.metric-emerald {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.16), rgba(16, 185, 129, 0.03));
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.chart-card {
  border-radius: 16px;
}

.section-title {
  margin-bottom: 12px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.chart-box {
  width: 100%;
  height: 320px;
}

@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .metric-value {
    font-size: 30px;
  }
}
</style>
