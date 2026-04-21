<template>
  <div class="page-card">
    <h3>平台概览</h3>
    <el-descriptions :column="4" border v-if="overview">
      <el-descriptions-item label="用户总数">{{ overview.userCount }}</el-descriptions-item>
      <el-descriptions-item label="活跃商家">{{ overview.merchantCount }}</el-descriptions-item>
      <el-descriptions-item label="上线产品">{{ overview.routeCount }}</el-descriptions-item>
      <el-descriptions-item label="总订单">{{ overview.orderCount }}</el-descriptions-item>
      <el-descriptions-item label="交易总额">{{ overview.turnover }}</el-descriptions-item>
      <el-descriptions-item label="投诉量">{{ overview.complaintCount }}</el-descriptions-item>
      <el-descriptions-item label="预警数">{{ overview.warning }}</el-descriptions-item>
    </el-descriptions>
    <el-divider />
    <div class="form-row">
      <div ref="orderTrendRef" style="width: 48%; height: 320px"></div>
      <div ref="complaintTrendRef" style="width: 48%; height: 320px"></div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from "vue";
import * as echarts from "echarts";
import { api } from "../../api";

const overview = ref(null);
const orderTrendRef = ref(null);
const complaintTrendRef = ref(null);

onMounted(async () => {
  overview.value = await api.get("/admin/dashboard/overview");
  const orderTrend = await api.get("/admin/dashboard/order-trend");
  const complaintTrend = await api.get("/admin/dashboard/complaint-trend");
  await nextTick();
  renderChart(orderTrendRef.value, "订单趋势", orderTrend);
  renderChart(complaintTrendRef.value, "投诉趋势", complaintTrend);
});

function renderChart(el, title, list) {
  const chart = echarts.init(el);
  chart.setOption({
    title: { text: title },
    tooltip: {
      trigger: "axis",
      valueFormatter: (value) => `${Math.round(Number(value ?? 0))}`
    },
    xAxis: { type: "category", data: list.map((i) => i.date) },
    yAxis: {
      type: "value",
      minInterval: 1,
      axisLabel: {
        formatter: (value) => `${Math.round(Number(value))}`
      }
    },
    series: [{ type: "line", data: list.map((i) => Number(i.count ?? 0)), smooth: false }]
  });
}
</script>
