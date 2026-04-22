<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Type Management"
      title="旅游标签"
    />

    <section class="page-card module-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">标签总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">当前旅游标签列表中的标签总量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">启用标签</span>
          <strong class="overview-value success">{{ overview.active }}</strong>
          <p class="overview-note">正在前台和资源配置中可被选择的标签数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">停用标签</span>
          <strong class="overview-value warning">{{ overview.inactive }}</strong>
          <p class="overview-note">已保留但当前不对外展示的标签数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">标签类型数</span>
          <strong class="overview-value primary">{{ overview.typeCount }}</strong>
          <p class="overview-note">当前标签库中已覆盖的标签分类种类数量。</p>
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
        <el-table-column prop="tagName" label="标签名称" min-width="200" />
        <el-table-column prop="tagType" label="标签类型" min-width="160" />
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
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
import { computed, onMounted, reactive, ref } from "vue";
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

const overview = computed(() => {
  const items = list.value;
  return {
    total: items.length,
    active: items.filter((item) => item.status === 1).length,
    inactive: items.filter((item) => item.status === 0).length,
    typeCount: new Set(items.map((item) => String(item.tagType || "").trim()).filter(Boolean)).size
  };
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
