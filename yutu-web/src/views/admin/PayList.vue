<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Payment Records"
      title="支付记录"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入支付单号或关联订单号"
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
        <el-table-column prop="payNo" label="支付单号" min-width="180" />
        <el-table-column prop="orderNo" label="关联订单号" min-width="180" />
        <el-table-column prop="payType" label="支付方式" width="120" />
        <el-table-column prop="payAmount" label="支付金额" width="120" />
        <el-table-column label="支付状态" width="120">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus).type">
              {{ payStatusTag(row.payStatus).text }}
            </el-tag>
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
const keyword = ref("");

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

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/pay-records", params);
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

.toolbar-left,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}
</style>
