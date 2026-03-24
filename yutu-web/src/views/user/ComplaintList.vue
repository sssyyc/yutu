<template>
  <div class="page-card">
    <h3>发起投诉</h3>
    <div class="form-row mb-12">
      <el-input v-model="form.orderId" placeholder="订单ID" style="width: 140px" />
      <el-input v-model="form.title" placeholder="投诉标题" style="width: 220px" />
      <el-input v-model="form.content" placeholder="投诉内容" style="width: 360px" />
      <el-button type="primary" @click="createComplaint">提交</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="complaintNo" label="投诉号" min-width="180" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/complaint/detail/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { api } from "../../api";
import { ElMessage } from "element-plus";

const list = ref([]);
const form = reactive({
  orderId: "",
  title: "",
  content: ""
});

async function load() {
  list.value = await api.get("/complaints");
}

async function createComplaint() {
  await api.post("/complaints", {
    orderId: Number(form.orderId),
    title: form.title,
    content: form.content
  });
  ElMessage.success("投诉已提交");
  form.orderId = "";
  form.title = "";
  form.content = "";
  load();
}

onMounted(load);
</script>
