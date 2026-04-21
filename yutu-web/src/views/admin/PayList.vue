<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Payment Records"
      title="支付记录"
    />

    <section class="page-card module-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">支付记录总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">当前检索结果中的支付流水记录总量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">成功支付</span>
          <strong class="overview-value success">{{ overview.success }}</strong>
          <p class="overview-note">支付状态为成功或已完成支付闭环的记录数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">退款记录</span>
          <strong class="overview-value warning">{{ overview.refunded }}</strong>
          <p class="overview-note">已经进入退款完成状态的支付记录数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">支付金额汇总</span>
          <strong class="overview-value primary">{{ overview.amount }}</strong>
          <p class="overview-note">按当前结果累计的支付金额，方便快速查看资金规模。</p>
        </article>
      </div>
    </section>

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
        <el-table-column label="支付方式" width="140">
          <template #default="{ row }">
            {{ formatPayType(row.payType) }}
          </template>
        </el-table-column>
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
import { computed, onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");

const overview = computed(() => {
  const items = list.value;
  const amount = items.reduce((sum, item) => sum + Number(item.payAmount || 0), 0);
  return {
    total: items.length,
    success: items.filter((item) => [1, "SUCCESS"].includes(item.payStatus)).length,
    refunded: items.filter((item) => [3, "REFUNDED"].includes(item.payStatus)).length,
    amount: `¥ ${formatAmount(amount)}`
  };
});

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

function formatPayType(payType) {
  const map = {
    ALIPAY_SANDBOX: "支付宝",
    ALIPAY: "支付宝",
    WECHAT_PAY: "微信支付",
    WECHAT: "微信支付",
    MOCK: "模拟支付",
    BALANCE: "余额支付",
    BANK_CARD: "银行卡"
  };
  return map[payType] || String(payType ?? "-");
}

function formatAmount(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "--";
  }
  return number.toFixed(2).replace(/\.00$/, "");
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
.module-overview {
  margin-bottom: 0;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.overview-card {
  padding: 18px 28px 16px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e3edf8;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.overview-label {
  display: inline-block;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 10px;
}

.overview-value {
  display: block;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
  margin-bottom: 12px;
}

.overview-value.success {
  color: #16a34a;
}

.overview-value.warning {
  color: #f59e0b;
}

.overview-value.primary {
  color: #2563eb;
}

.overview-note {
  margin: 0;
  color: #8ea0bc;
  font-size: 11px;
  line-height: 1.55;
}

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

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .overview-card {
    padding: 18px 16px;
  }
}
</style>
