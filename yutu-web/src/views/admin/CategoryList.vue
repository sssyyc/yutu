<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Resource Management"
      title="旅游形式"
    />

    <section class="page-card module-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">形式总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">当前资源形式列表中的分类数量汇总。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">启用形式</span>
          <strong class="overview-value success">{{ overview.active }}</strong>
          <p class="overview-note">正在前台和业务配置中生效的旅游形式数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">停用形式</span>
          <strong class="overview-value warning">{{ overview.inactive }}</strong>
          <p class="overview-note">已暂时停用、不参与展示和筛选的形式数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">最高排序值</span>
          <strong class="overview-value primary">{{ overview.maxSort }}</strong>
          <p class="overview-note">便于快速判断当前形式列表的排序区间。</p>
        </article>
      </div>
    </section>

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入形式名称"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增形式</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border class="resource-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="categoryName" label="形式名称" min-width="80" />
        <el-table-column prop="sortNum" label="显示顺序" width="180" />
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
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
      @closed="resetForm"
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
import { computed, onMounted, reactive, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  categoryName: "",
  sortNum: 0,
  status: 1,
  parentId: 0
});

const overview = computed(() => {
  const items = list.value;
  return {
    total: items.length,
    active: items.filter((item) => item.status === 1).length,
    inactive: items.filter((item) => item.status === 0).length,
    maxSort: items.reduce((max, item) => Math.max(max, Number(item.sortNum || 0)), 0)
  };
});

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/categories", params);
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
  resetForm();
  await load();
}

async function remove(id) {
  await api.del(`/admin/categories/${id}`);
  await load();
}

function resetSearch() {
  keyword.value = "";
  load();
}

onMounted(load);
</script>

<style scoped>
.module-overview {
  margin-bottom: 0;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.overview-card {
  padding: 18px 28px 16px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e3edf8;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.overview-label {
  display: inline-block;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 10px;
}

.overview-value {
  display: block;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
  margin-bottom: 12px;
}

.overview-value.success {
  color: #16a34a;
}

.overview-value.warning {
  color: #f59e0b;
}

.overview-value.primary {
  color: #2563eb;
}

.overview-note {
  margin: 0;
  color: #8ea0bc;
  font-size: 11px;
  line-height: 1.55;
}

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

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .overview-card {
    padding: 18px 16px;
  }
}
</style>
