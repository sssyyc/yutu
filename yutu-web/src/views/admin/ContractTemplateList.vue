<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CONTRACT TEMPLATE"
      title="合同模板管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-tip">当前共 {{ list.length }} 份模板</div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="templateCode" label="模板编码" min-width="160" />
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column prop="useCount" label="使用量" width="120" />
        <el-table-column prop="downloadCount" label="下载量" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="success" @click="enable(row.id)">启用</el-button>
            <el-button text type="warning" @click="disable(row.id)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑合同模板' : '新增合同模板'"
      width="760px"
      destroy-on-close
      @closed="reset"
    >
      <el-form :model="form" label-width="92px">
        <el-form-item label="模板名称">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.versionNo" placeholder="请输入版本号" />
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.templateContent" type="textarea" :rows="8" placeholder="请输入模板内容" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">{{ form.id ? "保存修改" : "确认新增" }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  templateName: "",
  templateCode: "",
  versionNo: "v1.0",
  templateContent: "标准旅游合同模板正文",
  remark: ""
});

async function load() {
  list.value = await api.get("/admin/contract-templates");
}

function openCreateDialog() {
  reset();
  dialogVisible.value = true;
}

function openEditDialog(row) {
  reset();
  Object.assign(form, row);
  dialogVisible.value = true;
}

function reset() {
  Object.assign(form, {
    id: null,
    templateName: "",
    templateCode: "",
    versionNo: "v1.0",
    templateContent: "标准旅游合同模板正文",
    remark: ""
  });
}

async function save() {
  if (form.id) {
    await api.put(`/admin/contract-templates/${form.id}`, form);
  } else {
    await api.post("/admin/contract-templates", form);
  }
  dialogVisible.value = false;
  reset();
  await load();
}

async function enable(id) {
  await api.post(`/admin/contract-templates/${id}/enable`);
  await load();
}

async function disable(id) {
  await api.post(`/admin/contract-templates/${id}/disable`);
  await load();
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

.toolbar-tip {
  color: #64748b;
  font-size: 14px;
}

.toolbar-actions,
.dialog-actions {
  display: flex;
  gap: 12px;
}
</style>
