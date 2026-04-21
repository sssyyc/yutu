<template>
  <div class="page-card complaint-detail-page">
    <el-descriptions v-if="data.complaint" border :column="2">
      <el-descriptions-item label="投诉号">{{ data.complaint.complaintNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ formatComplaintStatus(data.complaint.status) }}</el-descriptions-item>
      <el-descriptions-item label="投诉路线">{{ data.complaint.routeName || "-" }}</el-descriptions-item>
      <el-descriptions-item label="关联订单">{{ data.complaint.orderNo || "-" }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ data.complaint.title }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ data.complaint.complaintType || "-" }}</el-descriptions-item>
      <el-descriptions-item label="内容" :span="2">{{ data.complaint.content }}</el-descriptions-item>
      <el-descriptions-item label="处理结果" :span="2">{{ data.complaint.resultContent || "-" }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />

    <h3>流程记录</h3>
    <el-timeline>
      <el-timeline-item
        v-for="flow in data.flows || []"
        :key="flow.id"
        :timestamp="flow.createTime"
      >
        {{ flow.operatorRole }} - {{ flow.actionType }} - {{ flow.actionContent }}
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { api } from "../../api";

const route = useRoute();
const data = ref({});

function formatComplaintStatus(status) {
  const map = {
    PENDING_ACCEPT: "待受理",
    ACCEPTED: "已受理",
    ASSIGNED: "处理中",
    REPLIED: "已回复",
    CLOSED: "已关闭",
    REJECTED: "已驳回"
  };
  return map[status] || String(status || "-");
}

onMounted(async () => {
  data.value = await api.get(`/complaints/${route.params.id}`);
});
</script>

<style scoped>
.complaint-detail-page {
  padding: 24px 26px;
  border-radius: 24px;
}

.complaint-detail-page h3 {
  margin: 0 0 14px;
  color: #0f172a;
}
</style>
