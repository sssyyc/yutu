<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CONTRACT MANAGEMENT"
      title="合同管理"
    />

    <section class="page-card">
      <el-table :data="list" border>
        <el-table-column prop="contractNo" label="合同号" min-width="220" />
        <el-table-column prop="contractTitle" label="合同标题" min-width="220" />
        <el-table-column prop="signStatus" label="签署状态" width="140" />
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <el-button text type="primary" @click="view(row.id)">详情</el-button>
            <el-button text type="success" @click="openAppendix(row.id)">补充附件</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="appendixDialog" title="补充附件" width="620px">
      <el-form :model="appendix" label-width="88px">
        <el-form-item label="附件标题">
          <el-input v-model="appendix.appendixTitle" placeholder="请输入附件标题" />
        </el-form-item>
        <el-form-item label="附件内容">
          <el-input
            v-model="appendix.appendixContent"
            type="textarea"
            :rows="5"
            placeholder="请输入附件内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appendixDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAppendix">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="合同详情" width="760px">
      <pre class="json-preview">{{ JSON.stringify(detail, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const detail = ref({});
const detailDialog = ref(false);
const appendixDialog = ref(false);
const currentId = ref(null);
const appendix = reactive({ appendixTitle: "", appendixContent: "" });

async function load() {
  list.value = await api.get("/merchant/contracts");
}

async function view(id) {
  detail.value = await api.get(`/merchant/contracts/${id}`);
  detailDialog.value = true;
}

function openAppendix(id) {
  currentId.value = id;
  appendix.appendixTitle = "";
  appendix.appendixContent = "";
  appendixDialog.value = true;
}

async function submitAppendix() {
  await api.post(`/merchant/contracts/${currentId.value}/appendix`, appendix);
  ElMessage.success("补充成功");
  appendixDialog.value = false;
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
