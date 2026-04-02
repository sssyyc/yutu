<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CONTRACT MANAGEMENT"
      title="合同管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入合同号或合同标题"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-actions">
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
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
const keyword = ref("");
const detail = ref({});
const detailDialog = ref(false);
const appendixDialog = ref(false);
const currentId = ref(null);
const appendix = reactive({ appendixTitle: "", appendixContent: "" });

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/merchant/contracts", params);
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

function resetSearch() {
  keyword.value = "";
  load();
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 560px;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.toolbar-search {
  width: 520px;
  max-width: 100%;
  flex: 0 1 520px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}

.json-preview {
  margin: 0;
  max-height: 460px;
  overflow: auto;
  padding: 14px;
  border-radius: 10px;
  background: #f8fafc;
}

@media (max-width: 768px) {
  .toolbar-left {
    flex: 1 1 100%;
    flex-wrap: wrap;
  }

  .toolbar-search {
    width: 100%;
    flex-basis: 100%;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }
}
</style>
