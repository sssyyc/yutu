<template>
  <div class="page-card contract-list-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">MY CONTRACTS</p>
        <h2>电子合同</h2>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="contractNo" label="合同号" min-width="220" />
      <el-table-column prop="contractTitle" label="标题" min-width="280" />
      <el-table-column label="签署状态" width="140">
        <template #default="{ row }">
          <el-tag :type="row.signStatus === 'SIGNED' ? 'success' : 'warning'" effect="light">
            {{ row.signStatus === "SIGNED" ? "已签署" : "待签署" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="280">
        <template #default="{ row }">
          <el-button type="primary" text @click="openDetail(row.id)">详情</el-button>
          <el-button
            type="success"
            text
            :disabled="row.signStatus === 'SIGNED'"
            @click="openSign(row.id)"
          >
            签署
          </el-button>
          <el-button type="warning" text @click="download(row.id)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api";

const router = useRouter();
const list = ref([]);

async function load() {
  list.value = await api.get("/contracts");
}

function openDetail(id) {
  router.push(`/contract/detail/${id}`);
}

function openSign(id) {
  router.push(`/contract/detail/${id}?action=sign`);
}

function triggerDownload(fileName, content) {
  const blob = new Blob([content || ""], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName || "contract.txt";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

async function download(id) {
  const payload = await api.get(`/contracts/${id}/download`);
  triggerDownload(payload.fileName, payload.content);
  ElMessage.success("合同已下载");
}

onMounted(load);
</script>

<style scoped>
.contract-list-page {
  padding: 28px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: #6b7a90;
}

h2 {
  margin: 0;
  font-size: 26px;
  color: #12233d;
}

</style>
