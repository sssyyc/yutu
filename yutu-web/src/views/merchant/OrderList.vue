<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="ORDER CENTER"
      title="订单管理"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="orderNo" label="订单号" min-width="220" />
        <el-table-column label="订单状态" width="130">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus).type">{{ orderStatusTag(row.orderStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="120">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus).type">{{ payStatusTag(row.payStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="合同状态" width="130">
          <template #default="{ row }">
            <el-tag :type="contractStatusTag(row.contractStatus).type">
              {{ contractStatusTag(row.contractStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="支付金额" width="130">
          <template #default="{ row }">¥{{ row.payAmount ?? "-" }}</template>
        </el-table-column>
        <el-table-column prop="travelerCount" label="人数" width="90" />
        <el-table-column prop="createTime" label="下单时间" min-width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text type="primary" @click="view(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="订单详情" width="920px">
      <div v-if="detail.order" class="detail-wrap">
        <section class="detail-section">
          <h3 class="section-title">订单信息</h3>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="orderStatusTag(detail.order.orderStatus).type">
                {{ orderStatusTag(detail.order.orderStatus).text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="payStatusTag(detail.order.payStatus).type">
                {{ payStatusTag(detail.order.payStatus).text }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="合同状态">
              <el-tag :type="contractStatusTag(detail.order.contractStatus).type">
                {{ contractStatusTag(detail.order.contractStatus).text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付金额">¥{{ detail.order.payAmount ?? "-" }}</el-descriptions-item>
            <el-descriptions-item label="出行人数">{{ detail.order.travelerCount ?? "-" }}</el-descriptions-item>

            <el-descriptions-item label="下单时间">{{ detail.order.createTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ detail.order.updateTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ detail.order.source || "-" }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h3 class="section-title">出行人信息</h3>
          <el-table :data="detail.travelers || []" border>
            <el-table-column type="index" label="#" width="60" />
            <el-table-column prop="travelerName" label="姓名" min-width="140" />
            <el-table-column prop="idCard" label="身份证号" min-width="220" />
            <el-table-column prop="phone" label="手机号" min-width="150" />
          </el-table>
        </section>

        <section class="detail-section">
          <h3 class="section-title">关联合同</h3>
          <el-table :data="detail.contracts || []" border>
            <el-table-column prop="contractNo" label="合同号" min-width="220" />
            <el-table-column label="签署状态" width="120">
              <template #default="{ row }">
                <el-tag :type="signStatusTag(row.signStatus).type">{{ signStatusTag(row.signStatus).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="signTime" label="签署时间" min-width="180" />
            <el-table-column prop="createTime" label="生成时间" min-width="180" />
          </el-table>
        </section>
      </div>
      <el-empty v-else description="暂无订单详情" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const dialogVisible = ref(false);
const detail = ref({});

function orderStatusTag(status) {
  const map = {
    PENDING_PAY: { text: "待支付", type: "warning" },
    PENDING_TRAVEL: { text: "待出行", type: "primary" },
    COMPLETED: { text: "已完成", type: "success" },
    CANCELLED: { text: "已取消", type: "info" },
    REFUNDING: { text: "退款中", type: "warning" },
    REFUNDED: { text: "已退款", type: "danger" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function payStatusTag(status) {
  const map = {
    UNPAID: { text: "未支付", type: "info" },
    PAID: { text: "已支付", type: "success" },
    FAILED: { text: "支付失败", type: "danger" },
    REFUNDED: { text: "已退款", type: "warning" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function contractStatusTag(status) {
  const map = {
    NOT_GENERATED: { text: "未生成", type: "info" },
    GENERATED: { text: "已生成", type: "success" },
    SIGNED: { text: "已签署", type: "success" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function signStatusTag(status) {
  const map = {
    UNSIGNED: { text: "未签署", type: "warning" },
    SIGNED: { text: "已签署", type: "success" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

async function load() {
  list.value = await api.get("/merchant/orders");
}

async function view(id) {
  detail.value = await api.get(`/merchant/orders/${id}`);
  dialogVisible.value = true;
}

onMounted(load);
</script>

<style scoped>
.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  padding: 12px 0;
}

.section-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
</style>
