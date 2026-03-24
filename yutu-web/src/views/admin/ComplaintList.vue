<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Complaint Workflow"
      title="投诉处理"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="complaintNo" label="投诉号" min-width="180" />
        <el-table-column prop="title" label="投诉标题" min-width="220" />
        <el-table-column label="处理状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type">{{ statusTag(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="420" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="detail(row.id)">详情</el-button>
            <el-button text type="success" @click="action(row.id, 'accept')">受理</el-button>
            <el-button text type="warning" @click="action(row.id, 'assign')">分派</el-button>
            <el-button text type="danger" @click="action(row.id, 'judge')">判定</el-button>
            <el-button text @click="action(row.id, 'finish')">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialog" title="投诉详情" width="760px">
      <pre class="dialog-json">{{ JSON.stringify(current, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const dialog = ref(false);
const current = ref({});

function statusTag(status) {
  const map = {
    0: { text: "待受理", type: "warning" },
    1: { text: "已受理", type: "primary" },
    2: { text: "待商户处理", type: "warning" },
    3: { text: "已判定", type: "success" },
    4: { text: "已完成", type: "success" },
    5: { text: "已关闭", type: "info" }
  };
  return map[status] || { text: String(status ?? "-"), type: "info" };
}

async function load() {
  list.value = await api.get("/admin/complaints");
}

async function detail(id) {
  current.value = await api.get(`/admin/complaints/${id}`);
  dialog.value = true;
}

async function action(id, type) {
  await api.post(`/admin/complaints/${id}/${type}`, { content: `${type} by admin` });
  await load();
}

onMounted(load);
</script>

<style scoped>
.dialog-json {
  margin: 0;
  padding: 16px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
