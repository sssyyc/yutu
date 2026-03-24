<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Payment Records"
      title="支付记录"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="payNo" label="支付单号" min-width="180" />
        <el-table-column prop="orderNo" label="关联订单号" min-width="180" />
        <el-table-column prop="payType" label="支付方式" width="120" />
        <el-table-column prop="payAmount" label="支付金额" width="120" />
        <el-table-column label="支付状态" width="120">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus).type">{{ payStatusTag(row.payStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" min-width="180" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);

function payStatusTag(status) {
  const map = {
    0: { text: "未支付", type: "info" },
    1: { text: "已支付", type: "success" },
    2: { text: "支付失败", type: "danger" },
    3: { text: "已退款", type: "warning" },
    WAIT_BUYER_PAY: { text: "待支付", type: "warning" },
    SUCCESS: { text: "支付成功", type: "success" },
    FAILED: { text: "支付失败", type: "danger" },
    REFUNDED: { text: "已退款", type: "warning" }
  };
  return map[status] || { text: String(status ?? "-"), type: "info" };
}

onMounted(async () => {
  list.value = await api.get("/admin/pay-records");
});
</script>
