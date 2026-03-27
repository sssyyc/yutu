<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Resource Management"
      title="标签管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入标签名称或类型"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增标签</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tagName" label="标签名称" min-width="320" />
        <el-table-column prop="tagType" label="标签类型" min-width="220" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑标签' : '新增标签'"
      width="560px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form :model="form" label-width="92px">
        <el-form-item label="标签名称">
          <el-input v-model="form.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签类型">
          <el-input v-model="form.tagType" placeholder="请输入标签类型，如主题/玩法/适用人群" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
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
const keyword = ref("");
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  tagName: "",
  tagType: "",
  status: 1
});

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/tags", params);
}

function openCreateDialog() {
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(row) {
  resetForm();
  Object.assign(form, row);
  dialogVisible.value = true;
}

function resetForm() {
  Object.assign(form, {
    id: null,
    tagName: "",
    tagType: "",
    status: 1
  });
}

async function save() {
  if (form.id) {
    await api.put(`/admin/tags/${form.id}`, form);
  } else {
    await api.post("/admin/tags", form);
  }
  dialogVisible.value = false;
  resetForm();
  await load();
}

async function remove(id) {
  await api.del(`/admin/tags/${id}`);
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
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-left,
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

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}
</style>
