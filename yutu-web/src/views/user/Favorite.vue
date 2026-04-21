<template>
  <div class="page-card">
    <el-table :data="list" border>
      <el-table-column prop="id" label="收藏ID" width="100" />
      <el-table-column prop="targetId" label="目标路线ID" />
      <el-table-column prop="createTime" label="收藏时间" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="primary" text @click="$router.push(`/route/detail/${row.targetId}`)">查看路线</el-button>
          <el-button type="danger" text @click="remove(row.id)">取消收藏</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { api } from "../../api";
import { ElMessage } from "element-plus";

const list = ref([]);

async function load() {
  list.value = await api.get("/favorites");
}

async function remove(id) {
  await api.del(`/favorites/${id}`);
  ElMessage.success("已取消");
  load();
}

onMounted(load);
</script>
