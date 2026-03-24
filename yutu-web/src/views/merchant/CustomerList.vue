<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CUSTOMER MANAGEMENT"
      title="客户管理"
    />

    <section class="page-card">
      <el-table :data="list" border>
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

onMounted(async () => {
  list.value = await api.get("/merchant/customers");
});
</script>
