<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="Order Management" title="订单管理" />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入订单号"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-actions">
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="订单状态" width="140">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus).type" effect="light" round>
              {{ orderStatusTag(row.orderStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="140">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus).type" effect="light" round>
              {{ payStatusTag(row.payStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="支付金额" width="120" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="isCloseableOrder(row.orderStatus)"
              text
              type="warning"
              @click="closeOrder(row)"
            >
              关闭订单
            </el-button>
            <span v-else class="muted-action">-</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");

function orderStatusTag(status) {
  const map = {
    0: { text: "待支付", type: "warning" },
    1: { text: "已支付", type: "success" },
    2: { text: "待出行", type: "primary" },
    3: { text: "已完成", type: "success" },
    4: { text: "已取消", type: "info" },
    5: { text: "退款中", type: "warning" },
    6: { text: "已退款", type: "danger" },
    PENDING_PAY: { text: "待支付", type: "warning" },
    PENDING_TRAVEL: { text: "待出行", type: "primary" },
    COMPLETED: { text: "已完成", type: "success" },
    CANCELLED: { text: "已取消", type: "info" },
    REFUNDING: { text: "退款中", type: "warning" },
    REFUNDED: { text: "已退款", type: "danger" }
  };
  return map[status] || { text: String(status ?? "-"), type: "info" };
}

function payStatusTag(status) {
  const map = {
    0: { text: "未支付", type: "info" },
    1: { text: "已支付", type: "success" },
    2: { text: "支付失败", type: "danger" },
    3: { text: "已退款", type: "warning" },
    UNPAID: { text: "未支付", type: "info" },
    PAID: { text: "已支付", type: "success" },
    FAILED: { text: "支付失败", type: "danger" },
    REFUNDED: { text: "已退款", type: "warning" }
  };
  return map[status] || { text: String(status ?? "-"), type: "info" };
}

function isCloseableOrder(status) {
  return status === 0 || status === "PENDING_PAY";
}

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/orders", params);
}

async function closeOrder(row) {
  await ElMessageBox.confirm(
    `确认关闭订单「${row.orderNo}」吗？关闭后该订单将变为已取消。`,
    "关闭订单确认",
    {
      type: "warning",
      confirmButtonText: "关闭订单",
      cancelButtonText: "取消"
    }
  );
  await api.post(`/admin/orders/${row.id}/handle-exception`);
  ElMessage.success("订单已关闭");
  await load();
}

function resetSearch() {
  keyword.value = "";
  load();
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 560px;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.toolbar-search {
  width: 520px;
  max-width: 100%;
  flex: 0 1 520px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}

.muted-action {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 768px) {
  .toolbar-left {
    flex: 1 1 100%;
    flex-wrap: wrap;
  }

  .toolbar-search {
    width: 100%;
    flex-basis: 100%;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}
</style>
