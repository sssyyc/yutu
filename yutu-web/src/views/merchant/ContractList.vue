<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="CONTRACT MANAGEMENT" title="合同管理" />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入合同号或合同标题"
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
        <el-table-column prop="contractNo" label="合同号" min-width="220" />
        <el-table-column prop="contractTitle" label="合同标题" min-width="280" />
        <el-table-column label="签署状态" width="140">
          <template #default="{ row }">
            <el-tag :type="signStatusTagType(row.signStatus)" effect="light" round>
              {{ signStatusText(row.signStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <el-button text type="primary" @click="view(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailDialog" title="合同详情" width="980px">
      <div v-if="detail.contract" class="contract-detail">
        <header class="detail-hero">
          <div>
            <p>CONTRACT</p>
            <h3>{{ detail.contract.contractTitle }}</h3>
            <span>合同号：{{ detail.contract.contractNo }}</span>
          </div>
          <el-tag :type="signStatusTagType(detail.contract.signStatus)" effect="light" round>
            {{ signStatusText(detail.contract.signStatus) }}
          </el-tag>
        </header>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="线路">{{ detail.routeName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ detail.merchantName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥ {{ formatAmount(detail.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="应签人数">{{ detail.requiredSignCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="已签人数">{{ detail.signedCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <section class="detail-section">
          <div class="section-title">合同正文</div>
          <article class="contract-content">{{ detail.contract.contractContent || "暂无合同正文" }}</article>
        </section>
      </div>
      <el-empty v-else description="暂无合同详情" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const detail = ref({});
const detailDialog = ref(false);
async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/merchant/contracts", params);
}

async function view(id) {
  detail.value = await api.get(`/merchant/contracts/${id}`);
  detailDialog.value = true;
}

function signStatusText(status) {
  const map = {
    PENDING_SIGN: "待签署",
    UNSIGNED: "待签署",
    SIGNED: "已签署",
    COMPLETED: "已完成",
    TERMINATED: "已解除"
  };
  return map[status] || status || "-";
}

function signStatusTagType(status) {
  const map = {
    PENDING_SIGN: "warning",
    UNSIGNED: "warning",
    SIGNED: "success",
    COMPLETED: "primary",
    TERMINATED: "danger"
  };
  return map[status] || "info";
}

function formatAmount(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "--";
  }
  return number.toFixed(2).replace(/\.00$/, "");
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
  width: 620px;
  max-width: 100%;
  flex: 0 1 620px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}

.contract-detail {
  display: grid;
  gap: 18px;
}

.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fafc, #eef5ff);
  border: 1px solid #e2e8f0;
}

.detail-hero p {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 11px;
  letter-spacing: 0.12em;
}

.detail-hero h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.4;
}

.detail-hero span {
  display: inline-block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.detail-section {
  display: grid;
  gap: 10px;
}

.section-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.contract-content {
  max-height: 420px;
  overflow: auto;
  padding: 18px;
  border-radius: 16px;
  background: #fbfdff;
  border: 1px solid #e2e8f0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
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
