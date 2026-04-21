<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="BUSINESS STATISTICS"
      title="经营统计"
    />

    <section class="metrics-grid">
      <div class="metric-card">
        <div class="metric-title">总订单</div>
        <div class="metric-value">{{ overview.totalOrders || 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">已支付</div>
        <div class="metric-value">{{ overview.paidOrders || 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">退款单</div>
        <div class="metric-value">{{ overview.refundOrders || 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-title">营业额</div>
        <div class="metric-value">¥{{ overview.turnover || 0 }}</div>
      </div>
    </section>

    <section class="page-card">
      <div class="section-title">路线订单统计</div>
      <el-table :data="orders" border>
        <el-table-column prop="routeName" label="路线名称" min-width="260" />
        <el-table-column prop="orderCount" label="订单量" width="120" />
      </el-table>
    </section>

    <section class="page-card">
      <div class="section-title">用户评价</div>
      <el-table :data="reviews" border>
        <el-table-column prop="routeId" label="路线ID" width="100" />
        <el-table-column prop="score" label="评分" width="100" />
        <el-table-column prop="content" label="内容" min-width="260" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const overview = reactive({
  totalOrders: 0,
  paidOrders: 0,
  refundOrders: 0,
  turnover: 0
});
const orders = ref([]);
const reviews = ref([]);

onMounted(async () => {
  const [overviewRes, ordersRes, reviewsRes] = await Promise.all([
    api.get("/merchant/stats/overview"),
    api.get("/merchant/stats/orders"),
    api.get("/merchant/stats/reviews")
  ]);
  Object.assign(overview, overviewRes || {});
  orders.value = ordersRes || [];
  reviews.value = reviewsRes || [];
});
</script>

<style scoped>
.metrics-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  padding: 16px 18px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.metric-title {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 6px;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 800;
  color: #0f172a;
}

.section-title {
  margin-bottom: 12px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
