<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Order Management"
      title="订单管理"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="订单状态" width="140">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus).type">{{ orderStatusTag(row.orderStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="140">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus).type">{{ payStatusTag(row.payStatus).text }}</el-tag>
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
import { onMounted, ref } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import AdminPageHero from "../../components/admin/AdminPageHero.vue"
import { api } from "../../api"

const list = ref([])

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
  }
  return map[status] || { text: String(status ?? "-"), type: "info" }
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
  }
  return map[status] || { text: String(status ?? "-"), type: "info" }
}

function isCloseableOrder(status) {
  return status === 0 || status === "PENDING_PAY"
}

async function load() {
  list.value = await api.get("/admin/orders")
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
  )
  await api.post(`/admin/orders/${row.id}/handle-exception`)
  ElMessage.success("订单已关闭")
  await load()
}

onMounted(load)
</script>

<style scoped>
.muted-action {
  color: #94a3b8;
  font-size: 13px;
}
</style>
