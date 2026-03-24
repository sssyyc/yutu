<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Resource Management"
      title="形式管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-tip">当前共 {{ list.length }} 个形式</div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增形式</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="categoryName" label="形式名称" min-width="220" />
        <el-table-column prop="sortNum" label="显示顺序" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑形式' : '新增形式'"
      width="560px"
      destroy-on-close
      @closed="reset"
    >
      <el-form :model="form" label-width="92px">
        <el-form-item label="形式名称">
          <el-input v-model="form.categoryName" placeholder="请输入形式名称" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="form.sortNum" :min="0" :max="999" controls-position="right" style="width: 100%" />
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
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  categoryName: "",
  sortNum: 0,
  status: 1,
  parentId: 0
});

async function load() {
  list.value = await api.get("/admin/categories");
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
    categoryName: "",
    sortNum: 0,
    status: 1,
    parentId: 0
  });
}

async function save() {
  const payload = {
    ...form,
    sortNum: Number(form.sortNum || 0),
    parentId: Number(form.parentId || 0)
  };
  if (form.id) {
    await api.put(`/admin/categories/${form.id}`, payload);
  } else {
    await api.post("/admin/categories", payload);
  }
  dialogVisible.value = false;
  reset();
  await load();
}

async function remove(id) {
  await api.del(`/admin/categories/${id}`);
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

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}
</style>
