<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CUSTOMER MANAGEMENT"
      title="客户管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入用户名、昵称或手机号"
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
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="160" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
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

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/merchant/customers", params);
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
