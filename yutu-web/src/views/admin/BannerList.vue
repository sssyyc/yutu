<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="BANNER MANAGEMENT"
      title="轮播图管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入标题或关联路线"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增轮播图</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <div class="toolbar-tip">当前共有 {{ list.length }} 张轮播图</div>

      <el-table :data="list" border class="banner-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="170" />
        <el-table-column label="图片预览" min-width="220">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl"
              fit="cover"
              class="table-image"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="关联路线" min-width="220">
          <template #default="{ row }">
            <div class="route-link-cell">
              <div class="route-name">{{ resolveRouteName(row) }}</div>
              <div class="route-link">{{ row.linkUrl || "未设置跳转链接" }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortNum" label="顺序" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => updateStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑轮播图' : '新增轮播图'"
      width="720px"
      destroy-on-close
      @closed="reset"
    >
      <el-form :model="form" label-width="96px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" placeholder="请输入轮播图标题" />
        </el-form-item>

        <el-form-item label="轮播图片">
          <div class="upload-row">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="uploadImage"
            >
              <el-button type="primary" plain>上传图片</el-button>
            </el-upload>
          </div>
          <el-input
            v-model="form.imageUrl"
            placeholder="可直接粘贴图片 URL，或使用上方上传按钮"
            class="mt-10"
          />
          <el-image
            v-if="form.imageUrl"
            :src="form.imageUrl"
            fit="cover"
            class="preview-image"
            :preview-src-list="[form.imageUrl]"
            preview-teleported
          />
        </el-form-item>

        <el-form-item label="关联路线">
          <el-select
            v-model="form.routeId"
            filterable
            clearable
            placeholder="请选择要跳转的路线"
            style="width: 100%"
            @change="handleRouteChange"
          >
            <el-option
              v-for="item in routeOptions"
              :key="item.id"
              :label="item.routeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" readonly placeholder="选择路线后自动生成" />
        </el-form-item>

        <el-form-item label="显示顺序">
          <el-input-number
            v-model="form.sortNum"
            :min="0"
            :max="999"
            controls-position="right"
            style="width: 100%"
          />
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
import { ElMessage, ElMessageBox } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const routeOptions = ref([]);
const keyword = ref("");
const dialogVisible = ref(false);
const form = reactive({
  id: null,
  title: "",
  imageUrl: "",
  routeId: null,
  linkUrl: "",
  sortNum: 0,
  status: 1
});

function parseRouteIdFromLink(linkUrl) {
  const match = String(linkUrl || "").trim().match(/^\/route\/detail\/(\d+)$/);
  return match ? Number(match[1]) : null;
}

function buildRouteLink(routeId) {
  return routeId ? `/route/detail/${routeId}` : "";
}

function resolveRouteName(row) {
  const routeId = parseRouteIdFromLink(row.linkUrl);
  const found = routeOptions.value.find((item) => item.id === routeId);
  return found?.routeName || (row.linkUrl ? "已配置路线跳转" : "未关联路线");
}

async function load() {
  try {
    const trimmedKeyword = keyword.value.trim();
    const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
    list.value = await api.get("/admin/banners", params);
  } catch (error) {
    if (error?.response?.status === 404) {
      ElMessage.error("后端未加载轮播图管理接口，请重启后端服务");
      list.value = [];
      return;
    }
    throw error;
  }
}

async function loadRouteOptions() {
  routeOptions.value = await api.get("/routes");
}

function reset() {
  Object.assign(form, {
    id: null,
    title: "",
    imageUrl: "",
    routeId: null,
    linkUrl: "",
    sortNum: 0,
    status: 1
  });
}

function openCreateDialog() {
  reset();
  dialogVisible.value = true;
}

function openEditDialog(row) {
  reset();
  Object.assign(form, {
    id: row.id,
    title: row.title || "",
    imageUrl: row.imageUrl || "",
    routeId: parseRouteIdFromLink(row.linkUrl),
    linkUrl: row.linkUrl || "",
    sortNum: Number(row.sortNum || 0),
    status: Number(row.status === 0 ? 0 : 1)
  });
  dialogVisible.value = true;
}

function handleRouteChange(routeId) {
  form.linkUrl = buildRouteLink(routeId);
}

function toPayload(source) {
  return {
    title: String(source.title || "").trim(),
    imageUrl: String(source.imageUrl || "").trim(),
    linkUrl: String(source.linkUrl || "").trim() || null,
    sortNum: Number(source.sortNum || 0),
    status: Number(source.status === 0 ? 0 : 1)
  };
}

async function save() {
  const payload = toPayload(form);
  if (!payload.title) {
    ElMessage.warning("请输入轮播图标题");
    return;
  }
  if (!payload.imageUrl) {
    ElMessage.warning("请上传轮播图图片");
    return;
  }
  if (!form.routeId || !payload.linkUrl) {
    ElMessage.warning("请为轮播图选择对应的路线");
    return;
  }

  if (form.id) {
    await api.put(`/admin/banners/${form.id}`, payload);
  } else {
    await api.post("/admin/banners", payload);
  }
  dialogVisible.value = false;
  reset();
  await load();
}

async function updateStatus(row, enabled) {
  const payload = toPayload({
    ...row,
    status: enabled ? 1 : 0
  });
  await api.put(`/admin/banners/${row.id}`, payload, { silent: true });
  row.status = enabled ? 1 : 0;
  ElMessage.success("状态已更新");
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除轮播图「${row.title}」吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消"
  });
  await api.del(`/admin/banners/${row.id}`);
  await load();
}

async function uploadImage(options) {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const result = await api.upload("/files/upload", formData);
    form.imageUrl = result.url;
    ElMessage.success("图片上传成功");
    options.onSuccess?.(result);
  } catch (error) {
    options.onError?.(error);
  }
}

function resetSearch() {
  keyword.value = "";
  load();
}

onMounted(async () => {
  await Promise.all([load(), loadRouteOptions()]);
});
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
  color: #64748b;
  font-size: 14px;
  margin-bottom: 18px;
}

.banner-table {
  border-radius: 16px;
  overflow: hidden;
}

.table-image {
  width: 180px;
  height: 72px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.route-link-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.route-name {
  color: #0f172a;
  font-weight: 700;
}

.route-link {
  color: #64748b;
  font-size: 13px;
  word-break: break-all;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.mt-10 {
  margin-top: 10px;
}

.preview-image {
  margin-top: 10px;
  width: 100%;
  max-width: 420px;
  height: 150px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
</style>
