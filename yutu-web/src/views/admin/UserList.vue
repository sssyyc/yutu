<template>
  <div class="admin-module-page">
    <AdminPageHero kicker="User Management" title="用户管理" />

    <section class="page-card module-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">用户总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">当前后台检索结果中的平台账号总量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">正常用户</span>
          <strong class="overview-value success">{{ overview.active }}</strong>
          <p class="overview-note">状态为正常、可继续登录和使用功能的账号数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">封禁用户</span>
          <strong class="overview-value warning">{{ overview.disabled }}</strong>
          <p class="overview-note">已被平台限制登录和操作的账号数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">商户数量</span>
          <strong class="overview-value primary">{{ overview.merchant }}</strong>
          <p class="overview-note">当前用户列表中角色为商户的账号数量汇总。</p>
        </article>
      </div>
    </section>

    <section class="page-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          clearable
          placeholder="请输入用户名或昵称"
          @clear="load"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="160" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag effect="light">{{ roleText(row.roleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? "正常" : "封禁" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="success"
              :disabled="row.status === 1"
              @click="confirmSetStatus(row, 1)"
            >
              启用
            </el-button>
            <el-button
              text
              type="warning"
              :disabled="row.status === 0"
              @click="confirmSetStatus(row, 0)"
            >
              封禁
            </el-button>
            <el-button text type="danger" @click="confirmRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");

const overview = computed(() => {
  const items = list.value;
  return {
    total: items.length,
    active: items.filter((item) => item.status === 1).length,
    disabled: items.filter((item) => item.status === 0).length,
    merchant: items.filter((item) => item.roleType === 2).length
  };
});

function roleText(roleType) {
  const map = {
    1: "普通用户",
    2: "商家",
    3: "管理员"
  };
  return map[roleType] || "未知";
}

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/users", params);
}

async function confirmSetStatus(row, status) {
  const isEnable = status === 1;
  const actionText = isEnable ? "启用" : "封禁";

  if (row.status === status) {
    ElMessage.info(`该用户当前已经是${actionText}状态`);
    return;
  }

  await ElMessageBox.confirm(
    `确认要${actionText}用户“${row.username}”吗？`,
    `${actionText}提醒`,
    {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      type: isEnable ? "success" : "warning"
    }
  );

  await api.put(`/admin/users/${row.id}/status`, { status });
  ElMessage.success(`用户已${actionText}`);
  await load();
}

async function confirmRemove(row) {
  await ElMessageBox.confirm(
    `确认要删除用户“${row.username}”吗？删除后该用户不会继续出现在当前列表中。`,
    "删除提醒",
    {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning"
    }
  );

  await api.del(`/admin/users/${row.id}`);
  ElMessage.success("用户已删除");
  await load();
}

function reset() {
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
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-search {
  width: min(360px, 100%);
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
