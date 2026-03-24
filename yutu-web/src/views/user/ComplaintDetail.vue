<template>
  <div class="page-card">
    <el-descriptions border :column="2" v-if="data.complaint">
      <el-descriptions-item label="投诉号">{{ data.complaint.complaintNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ data.complaint.status }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ data.complaint.title }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ data.complaint.complaintType }}</el-descriptions-item>
      <el-descriptions-item label="内容" :span="2">{{ data.complaint.content }}</el-descriptions-item>
      <el-descriptions-item label="处理结果" :span="2">{{ data.complaint.resultContent || "-" }}</el-descriptions-item>
    </el-descriptions>
    <el-divider />
    <h3>流程记录</h3>
    <el-timeline>
      <el-timeline-item v-for="f in data.flows || []" :key="f.id" :timestamp="f.createTime">
        {{ f.operatorRole }} - {{ f.actionType }} - {{ f.actionContent }}
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { api } from "../../api";

const route = useRoute();
const data = ref({});

onMounted(async () => {
  data.value = await api.get(`/complaints/${route.params.id}`);
});
</script>
