<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="COMPLAINT CENTER"
      title="投诉处理"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="complaintNo" label="投诉单号" min-width="220" />
        <el-table-column prop="title" label="投诉标题" min-width="200" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <el-button text type="primary" @click="view(row.id)">详情</el-button>
            <el-button text type="success" @click="reply(row.id)">回复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="replyDialog" title="回复投诉" width="620px">
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="5"
        placeholder="请输入投诉处理意见"
      />
      <template #footer>
        <el-button @click="replyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReply">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="投诉详情" width="760px">
      <pre class="json-preview">{{ JSON.stringify(detail, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const detail = ref({});
const detailDialog = ref(false);
const replyDialog = ref(false);
const replyText = ref("");
const currentId = ref(null);

async function load() {
  list.value = await api.get("/merchant/complaints");
}

async function view(id) {
  detail.value = await api.get(`/merchant/complaints/${id}`);
  detailDialog.value = true;
}

function reply(id) {
  currentId.value = id;
  replyText.value = "";
  replyDialog.value = true;
}

async function submitReply() {
  await api.post(`/merchant/complaints/${currentId.value}/reply`, { content: replyText.value });
  ElMessage.success("回复成功");
  replyDialog.value = false;
  await load();
}

onMounted(load);
</script>

<style scoped>
.json-preview {
  margin: 0;
  max-height: 460px;
  overflow: auto;
  padding: 14px;
  border-radius: 10px;
  background: #f8fafc;
}
</style>
