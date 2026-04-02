<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CONTRACT TEMPLATE"
      title="合同模板管理"
    />

    <section class="page-card">
      <div class="toolbar toolbar-search-row">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入模板名称或模板编码"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <div class="toolbar-tip">当前共 {{ list.length }} 份模板</div>

      <el-table :data="list" border class="resource-table">
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
          <el-input
            v-model="form.templateContent"
            type="textarea"
            :rows="10"
            placeholder="请输入模板内容"
          />
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

const defaultTemplateContent = `团队旅游合同模板

合同编号：______________
甲方（旅游者）：______________
乙方（旅行社）：______________

一、旅游产品及服务内容
1. 旅游线路：______________
2. 出发日期：______________
3. 结束日期：______________
4. 费用包含：交通、住宿、餐饮、门票、导游服务等。

二、费用与支付方式
1. 合同总金额：￥______________
2. 支付方式：______________

三、双方权利义务
1. 甲方应如实提供出行信息并按时付款。
2. 乙方应按约定提供对应旅游服务。

四、违约责任
双方应按照合同约定承担相应违约责任。

五、其他约定
本合同自双方签字或盖章之日起生效。`;

const list = ref([]);
const keyword = ref("");
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  templateName: "",
  templateCode: "",
  versionNo: "v1.0",
  templateContent: defaultTemplateContent,
  remark: ""
});

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/contract-templates", params);
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
    templateContent: defaultTemplateContent,
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
  flex-wrap: wrap;
}

.toolbar-search-row {
  margin-bottom: 12px;
}

.toolbar-left,
.toolbar-actions,
.dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.toolbar-tip {
  margin-bottom: 18px;
  color: #64748b;
  font-size: 14px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}
</style>
