<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="User Management"
      title="用户管理"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="180" />
        <el-table-column prop="nickname" label="昵称" min-width="160" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag>{{ roleText(row.roleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? "正常" : "封禁" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="success" @click="setStatus(row.id, 1)">启用</el-button>
            <el-button text type="warning" @click="setStatus(row.id, 0)">封禁</el-button>
            <el-button text type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);

function roleText(roleType) {
  const map = {
    1: "普通用户",
    2: "商家",
    3: "管理员"
  };
  return map[roleType] || "未知";
}

async function load() {
  list.value = await api.get("/admin/users");
}

async function setStatus(id, status) {
  await api.put(`/admin/users/${id}/status`, { status });
  await load();
}

async function remove(id) {
  await api.del(`/admin/users/${id}`);
  await load();
}

onMounted(load);
</script>
