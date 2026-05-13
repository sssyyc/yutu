<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="ROUTE EDITOR"
      :title="isEdit ? '编辑路线' : '新增路线'"
    />

    <section class="page-card editor-shell">
      <el-form :model="form" label-position="top" class="route-form">
        <div class="editor-grid">
          <div class="editor-main">
            <div class="form-section">
              <div class="section-heading">
                  <div class="section-title">基础信息</div>
              </div>

              <el-form-item label="路线名称" class="inline-form-item inline-form-item-wide">
                <el-input v-model="form.routeName" placeholder="请输入路线名称" />
              </el-form-item>

              

              <div class="combined-selection-panel soft-subsection">
                <div class="combined-selection-grid">
                  <el-form-item label="路线形式" class="combined-selection-item">
                    <el-select
                      v-model="form.categoryId"
                      filterable
                      placeholder="请选择路线形式"
                      style="width: 100%"
                      :loading="metaLoading"
                    >
                      <el-option
                        v-for="item in categoryOptions"
                        :key="item.id"
                        :label="item.categoryName"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>

                  <el-form-item label="路线标签" class="combined-selection-item">
                    <el-select
                      v-model="form.tagIds"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      filterable
                      placeholder="可多选路线标签"
                      style="width: 100%"
                      :loading="metaLoading"
                    >
                      <el-option
                        v-for="item in tagOptions"
                        :key="item.id"
                        :label="item.tagName"
                        :value="item.id"
                      >
                        <div class="tag-option">
                          <span>{{ item.tagName }}</span>
                          <span class="tag-option-type">{{ item.tagType || "默认标签" }}</span>
                        </div>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </div>
              </div><div class="contract-template-panel soft-subsection">
                <div class="section-mini-title">合同模板</div>

                <el-form-item class="inline-form-item inline-form-item-wide">
                  <template #label>
                    <span><span class="required-star">*</span>国家制式合同</span>
                  </template>
                  <el-select
                    v-model="form.standardTemplateId"
                    filterable
                    clearable
                    placeholder="请选择国家制式合同模板"
                    style="width: 100%"
                    :loading="contractTemplateLoading"
                  >
                    <el-option
                      v-for="item in standardTemplateOptions"
                      :key="item.id"
                      :label="item.templateName"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item class="inline-form-item inline-form-item-wide">
                  <template #label>
                    <span><span class="required-star">*</span>线路附件</span>
                  </template>
                  <el-select
                    v-model="form.routeTemplateId"
                    filterable
                    clearable
                    placeholder="请选择线路附件模板"
                    style="width: 100%"
                    :loading="contractTemplateLoading"
                  >
                    <el-option
                      v-for="item in routeTemplateOptions"
                      :key="item.id"
                      :label="item.templateName"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>

              </div>

              <div class="soft-subsection">
                <div class="section-mini-title">注意事项</div>
                <el-form-item class="inline-form-item inline-form-item-wide">
                  <el-input
                    v-model="form.notes"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入该路线的注意事项，如：退改政策、携带物品、安全须知等"
                  />
                </el-form-item>
              </div>

              <div class="field-grid field-grid-two compact-grid soft-subsection">
                <el-form-item label="参考价格" class="inline-form-item">
                  <el-input-number
                    v-model="form.price"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>

                <el-form-item label="总库存" class="inline-form-item">
                  <el-input-number
                    v-model="form.stock"
                    :min="0"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </div>
            </div>

            <div class="form-section">
              <div class="section-heading">
                <div class="section-title">路线内容</div>
                <div class="section-subtitle">文案和行程说明集中在左侧，编辑时更连贯。</div>
              </div>

              <el-form-item label="路线简介">
                <el-input v-model="form.summary" placeholder="请输入路线简介" />
              </el-form-item>

              <el-form-item label="详细介绍" class="detail-field">
                <el-input
                  v-model="form.detailContent"
                  type="textarea"
                  :rows="13"
                  placeholder="请输入路线详细介绍"
                />
              </el-form-item>
            </div>
          </div>

          <div class="editor-side">
            <div class="form-section">
              <div class="section-heading">
                <div class="section-title">图片素材</div>
                <div class="section-subtitle">封面图和景点图集中在右侧，替换和预览都会更顺手。</div>
              </div>

              <el-form-item label="封面图片">
                <div class="upload-block cover-upload-stack">
                  <el-upload
                    :show-file-list="false"
                    accept="image/*"
                    :http-request="uploadCover"
                    :before-upload="beforeUpload"
                  >
                    <el-button :loading="coverUploading">
                      {{ coverUploading ? "上传中..." : "上传封面图" }}
                    </el-button>
                  </el-upload>

                  <el-image
                    v-if="form.coverImage"
                    :src="form.coverImage"
                    fit="cover"
                    class="cover-preview"
                    :preview-src-list="[form.coverImage]"
                  />
                  <div v-else class="empty-preview">暂未上传封面图</div>
                </div>
              </el-form-item>

              <el-form-item label="相关景点图" class="related-field">
                <div class="upload-block related-upload-wrap">
                  <el-upload
                    list-type="picture-card"
                    accept="image/*"
                    :file-list="relatedFileList"
                    :limit="4"
                    :http-request="uploadRelatedImage"
                    :before-upload="beforeUpload"
                    :on-remove="removeRelatedImage"
                    :on-preview="previewRelatedImage"
                    :on-exceed="handleExceed"
                  >
                    <el-icon><Plus /></el-icon>
                  </el-upload>
                </div>
              </el-form-item>
            </div>

            <div class="form-section">
              <div class="section-heading section-heading-inline">
                <div>
                  <div class="section-title">出发日期</div>
                  <div class="section-subtitle">集中维护每个批次的日期、售价和余量。</div>
                </div>
                <el-button type="primary" plain @click="addDepartureDate">新增日期</el-button>
              </div>

              <div class="departure-panel">
                <div class="departure-list">
                  <div
                    v-for="item in departureDates"
                    :key="item.key"
                    class="departure-row"
                  >
                    <div class="departure-main">
                      <el-date-picker
                        v-model="item.departDate"
                        type="date"
                        format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD"
                        placeholder="选择出发日期"
                        class="departure-date-input"
                      />
                      <el-input-number
                        v-model="item.salePrice"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                        placeholder="售价"
                        class="departure-number-input"
                      />
                      <el-input-number
                        v-model="item.remainCount"
                        :min="0"
                        controls-position="right"
                        placeholder="余量"
                        class="departure-number-input"
                      />
                      <el-button
                        class="departure-remove-button"
                        type="danger"
                        plain
                        :icon="Delete"
                        @click="removeDepartureDate(item.key)"
                      >
                        删除
                      </el-button>
                    </div>

                    <div v-if="isEdit && item.id" class="departure-audit-info">
                      <el-tag size="small" :type="auditTag(item.auditStatus).type">
                        {{ auditTag(item.auditStatus).text }}
                      </el-tag>
                      <span class="departure-audit-text">
                        {{ item.auditRemark || "暂无审核说明" }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <el-button @click="router.push('/merchant/route/list')">返回列表</el-button>
          <el-button type="primary" :loading="saveLoading" @click="save">保存路线</el-button>
        </div>
      </el-form>
    </section>

    <el-dialog v-model="previewVisible" title="图片预览" width="700px">
      <el-image v-if="previewUrl" :src="previewUrl" fit="contain" class="dialog-image" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Delete, Plus } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";
import { buildRouteDetailContent, parseRouteDetailContent } from "../../utils/routeDetailMeta";

const route = useRoute();
const router = useRouter();
const isEdit = computed(() => Boolean(route.params.id));

const form = reactive({
  categoryId: null,
  standardTemplateId: null,
  routeTemplateId: null,
  tagIds: [],
  routeName: "",
  coverImage: "",
  summary: "",
  detailContent: "",
  notes: "",
  price: 0,
  stock: 0
});

const relatedImages = ref([]);
const relatedFileList = ref([]);
const coverUploading = ref(false);
const previewVisible = ref(false);
const previewUrl = ref("");
const saveLoading = ref(false);
const metaLoading = ref(false);
const categoryOptions = ref([]);
const tagOptions = ref([]);
const contractTemplateLoading = ref(false);
const contractTemplateOptions = ref([]);
const routeLoadingTemplates = ref(false);

const standardTemplateOptions = computed(() =>
  contractTemplateOptions.value.filter((item) => item.templateType === "STANDARD")
);
const routeTemplateOptions = computed(() =>
  contractTemplateOptions.value.filter((item) => item.templateType === "ROUTE")
);
const supplementTemplateOptions = computed(() =>
  contractTemplateOptions.value.filter((item) => item.templateType === "SUPPLEMENT")
);

let departureDateKeySeed = 0;
const departureDates = ref([createDepartureDateItem()]);

function nextDepartureDateKey() {
  departureDateKeySeed += 1;
  return `departure-${departureDateKeySeed}`;
}

function normalizeTemplateId(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

function createDepartureDateItem(source = {}) {
  return {
    key: nextDepartureDateKey(),
    id: source.id ?? null,
    departDate: source.departDate || "",
    salePrice: source.salePrice ?? Number(form.price || 0),
    remainCount: source.remainCount ?? Number(form.stock || 0),
    auditStatus: source.auditStatus ?? 0,
    auditRemark: source.auditRemark || ""
  };
}

function auditTag(status) {
  const map = {
    0: { text: "待审核", type: "warning" },
    1: { text: "已通过", type: "success" },
    2: { text: "已驳回", type: "danger" }
  };
  return map[status] || { text: "-", type: "info" };
}

function normalizeFileList(urls) {
  relatedFileList.value = urls.map((url, idx) => ({
    name: `related-${idx + 1}.jpg`,
    url,
    status: "success"
  }));
}

function ensureTemplateSelectionValid() {
  const templateIds = new Set(contractTemplateOptions.value.map((item) => Number(item.id)));
  if (!templateIds.has(Number(form.standardTemplateId))) {
    form.standardTemplateId = null;
  }
  if (!templateIds.has(Number(form.routeTemplateId))) {
    form.routeTemplateId = null;
  }
}

function beforeUpload(file) {
  const isImage = String(file?.type || "").startsWith("image/");
  if (!isImage) {
    ElMessage.error("只能上传图片文件");
    return false;
  }
  return true;
}

async function uploadImageFile(file) {
  const formData = new FormData();
  formData.append("file", file);
  const resp = await api.upload("/files/upload", formData);
  return resp?.url || "";
}

async function uploadCover(option) {
  try {
    coverUploading.value = true;
    const url = await uploadImageFile(option.file);
    if (!url) {
      throw new Error("上传失败");
    }
    form.coverImage = url;
    ElMessage.success("封面上传成功");
    option.onSuccess?.({ url });
  } catch (error) {
    option.onError?.(error);
  } finally {
    coverUploading.value = false;
  }
}

async function uploadRelatedImage(option) {
  try {
    const url = await uploadImageFile(option.file);
    if (!url) {
      throw new Error("上传失败");
    }
    relatedImages.value = [...relatedImages.value, url].slice(0, 4);
    normalizeFileList(relatedImages.value);
    option.onSuccess?.({ url });
    ElMessage.success("相关图片上传成功");
  } catch (error) {
    option.onError?.(error);
  }
}

function removeRelatedImage(file) {
  relatedImages.value = relatedImages.value.filter((url) => url !== file.url);
  normalizeFileList(relatedImages.value);
}

function previewRelatedImage(file) {
  previewUrl.value = file.url;
  previewVisible.value = true;
}

function handleExceed() {
  ElMessage.warning("相关景点图片最多上传 4 张");
}

async function loadMeta() {
  metaLoading.value = true;
  try {
    const [categories, tags] = await Promise.all([
      api.get("/routes/categories"),
      api.get("/routes/tags")
    ]);
    categoryOptions.value = Array.isArray(categories) ? categories : [];
    tagOptions.value = Array.isArray(tags) ? tags : [];
    if (!isEdit.value && !form.categoryId && categoryOptions.value.length) {
      form.categoryId = Number(categoryOptions.value[0].id);
    }
  } finally {
    metaLoading.value = false;
  }
}

async function loadContractTemplates(categoryId) {
  const normalizedCategoryId = normalizeTemplateId(categoryId);
  if (!normalizedCategoryId) {
    contractTemplateOptions.value = [];
    ensureTemplateSelectionValid();
    return;
  }

  contractTemplateLoading.value = true;
  try {
    const response = await api.get(
      "/routes/contract-templates",
      { categoryId: normalizedCategoryId },
      { silent: true }
    );
    contractTemplateOptions.value = Array.isArray(response) ? response : [];
  } catch (error) {
    contractTemplateOptions.value = [];
  } finally {
    ensureTemplateSelectionValid();
    contractTemplateLoading.value = false;
  }
}

function addDepartureDate() {
  departureDates.value.push(createDepartureDateItem());
}

function removeDepartureDate(key) {
  if (departureDates.value.length === 1) {
    ElMessage.warning("至少保留一个出发日期");
    return;
  }
  departureDates.value = departureDates.value.filter((item) => item.key !== key);
}

watch(
  () => form.categoryId,
  async (categoryId, previousCategoryId) => {
    if (routeLoadingTemplates.value || categoryId === previousCategoryId) {
      return;
    }
    await loadContractTemplates(categoryId);
  }
);

function buildDepartureDatesPayload() {
  return departureDates.value.map((item) => ({
    id: item.id ?? null,
    departDate: String(item.departDate || "").trim(),
    salePrice: Number(item.salePrice),
    remainCount: Number(item.remainCount)
  }));
}

function validatePayload(payload) {
  if (!Number.isFinite(payload.categoryId) || payload.categoryId <= 0) {
    ElMessage.warning("请选择路线形式");
    return false;
  }
  if (!Number.isFinite(payload.standardTemplateId) || payload.standardTemplateId <= 0) {
    ElMessage.warning("请选择国家制式合同模板");
    return false;
  }
  if (!Number.isFinite(payload.routeTemplateId) || payload.routeTemplateId <= 0) {
    ElMessage.warning("请选择线路附件模板");
    return false;
  }
  if (!String(payload.routeName || "").trim()) {
    ElMessage.warning("请输入路线名称");
    return false;
  }
  if (!Number.isFinite(payload.price) || payload.price < 0) {
    ElMessage.warning("请输入正确的参考价格");
    return false;
  }
  if (!Number.isFinite(payload.stock) || payload.stock < 0) {
    ElMessage.warning("请输入正确的总库存");
    return false;
  }
  if (!payload.departureDates.length) {
    ElMessage.warning("请至少添加一个出发日期");
    return false;
  }

  const departDateSet = new Set();
  for (const item of payload.departureDates) {
    if (!item.departDate) {
      ElMessage.warning("请完整填写出发日期");
      return false;
    }
    if (departDateSet.has(item.departDate)) {
      ElMessage.warning("出发日期不能重复");
      return false;
    }
    departDateSet.add(item.departDate);

    if (!Number.isFinite(item.salePrice) || item.salePrice < 0) {
      ElMessage.warning("请填写正确的出发价格");
      return false;
    }
    if (!Number.isFinite(item.remainCount) || item.remainCount < 0) {
      ElMessage.warning("请填写正确的出发余量");
      return false;
    }
  }

  return true;
}

async function loadRoute() {
  if (!route.params.id) {
    if (!form.categoryId && categoryOptions.value.length) {
      form.categoryId = Number(categoryOptions.value[0].id);
    }
    await loadContractTemplates(form.categoryId);
    departureDates.value = [createDepartureDateItem()];
    return;
  }

  const routeId = Number(route.params.id);
  const [list, routeDetail] = await Promise.all([
    api.get("/merchant/routes"),
    api.get(`/routes/${routeId}`)
  ]);
  const found = Array.isArray(list) ? list.find((item) => item.id === routeId) : null;
  if (!found) {
    ElMessage.error("未找到路线数据");
    router.push("/merchant/route/list");
    return;
  }

  let dates = [];
  try {
    dates = await api.get(`/merchant/routes/${routeId}/dates`, null, { silent: true });
  } catch (error) {
    if (error?.response?.status !== 404) {
      throw error;
    }
  }

  const parsed = parseRouteDetailContent(found.detailContent);
  routeLoadingTemplates.value = true;
  Object.assign(form, {
    categoryId: Number(found.categoryId || categoryOptions.value[0]?.id || null),
    tagIds: Array.isArray(routeDetail?.tags)
      ? routeDetail.tags.map((item) => Number(item.id)).filter((id) => Number.isFinite(id) && id > 0)
      : [],
    routeName: found.routeName || "",
    coverImage: found.coverImage || "",
    summary: found.summary || "",
    detailContent: parsed.plainText || "",
    price: Number(found.price || 0),
    stock: Number(found.stock || 0)
  });
  await loadContractTemplates(form.categoryId);
  form.standardTemplateId = normalizeTemplateId(found.standardTemplateId);
  form.routeTemplateId = normalizeTemplateId(found.routeTemplateId);
  form.notes = found.notes || "";
  ensureTemplateSelectionValid();
  routeLoadingTemplates.value = false;

  relatedImages.value = parsed.relatedImages;
  normalizeFileList(relatedImages.value);
  departureDates.value = Array.isArray(dates) && dates.length
    ? dates.map((item) =>
        createDepartureDateItem({
          id: item.id,
          departDate: item.departDate,
          salePrice: Number(item.salePrice || 0),
          remainCount: Number(item.remainCount || 0),
          auditStatus: Number(item.auditStatus ?? 0),
          auditRemark: item.auditRemark || ""
        })
      )
    : [createDepartureDateItem()];
}

async function save() {
  const payload = {
    categoryId: Number(form.categoryId),
    standardTemplateId: normalizeTemplateId(form.standardTemplateId),
    routeTemplateId: normalizeTemplateId(form.routeTemplateId),
    notes: String(form.notes || "").trim(),
    tagIds: Array.isArray(form.tagIds)
      ? form.tagIds.map((item) => Number(item)).filter((id) => Number.isFinite(id) && id > 0)
      : [],
    routeName: String(form.routeName || "").trim(),
    coverImage: String(form.coverImage || "").trim(),
    summary: String(form.summary || "").trim(),
    detailContent: buildRouteDetailContent(form.detailContent, relatedImages.value),
    price: Number(form.price),
    stock: Number(form.stock),
    departureDates: buildDepartureDatesPayload()
  };

  if (!validatePayload(payload)) {
    return;
  }

  saveLoading.value = true;
  try {
    if (route.params.id) {
      await api.put(`/merchant/routes/${route.params.id}`, payload);
    } else {
      await api.post("/merchant/routes", payload);
    }
    ElMessage.success("保存成功");
    router.push("/merchant/route/list");
  } finally {
    saveLoading.value = false;
  }
}

onMounted(async () => {
  await loadMeta();
  await loadRoute();
});
</script>

<style scoped>
.editor-shell {
  padding: 22px;
}

.route-form {
  width: 100%;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 22px;
  align-items: start;
}

.editor-main,
.editor-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-section {
  padding: 24px 28px;
  border-radius: 22px;
  border: 1px solid #dbe6f2;
  background: linear-gradient(180deg, #fdfefe 0%, #f7fbff 100%);
  box-shadow:
    0 10px 24px rgba(15, 23, 42, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.section-heading {
  margin-bottom: 22px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section-heading-inline {
  flex-direction: row;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.section-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
}

.section-subtitle {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.field-grid {
  display: grid;
  gap: 16px;
}

.field-grid-two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.combined-selection-panel {
  margin-bottom: 16px;
}

.combined-selection-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.combined-selection-item {
  margin-bottom: 0;
}

.contract-template-panel {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.soft-subsection {
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid #e7eef7;
  background: linear-gradient(180deg, #fbfdff 0%, #f6faff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.section-mini-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 2px;
}

.required-star {
  margin-right: 4px;
  color: #ef4444;
}

.compact-grid {
  margin-top: 4px;
}

.route-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.route-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.route-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.inline-form-item {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 0;
}

.inline-form-item :deep(.el-form-item__label-wrap) {
  display: flex;
  align-items: center;
  flex: 0 0 138px;
  margin: 0;
}

.inline-form-item :deep(.el-form-item__label) {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding-bottom: 0;
  margin-bottom: 0;
  line-height: 1.4;
}

.inline-form-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
  width: auto;
  margin-left: 0 !important;
}

.inline-form-item-wide :deep(.el-form-item__label-wrap) {
  flex-basis: 138px;
}

.route-form :deep(.el-textarea__inner) {
  min-height: 280px;
  line-height: 1.7;
}

.upload-block {
  width: 100%;
}

.cover-upload-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cover-preview {
  display: block;
  width: 100%;
  height: 230px;
  border-radius: 16px;
  border: 1px solid #dbe2ea;
  background: #f8fafc;
  overflow: hidden;
}

.empty-preview {
  width: 100%;
  height: 230px;
  border-radius: 16px;
  border: 1px dashed #cbd5e1;
  background: linear-gradient(135deg, #f8fafc, #eef4ff);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.related-upload-wrap :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tag-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tag-option-type {
  color: #94a3b8;
  font-size: 12px;
}

.departure-panel {
  width: 100%;
}

.departure-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.departure-row {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid #dbe4ef;
  background: #ffffff;
}

.departure-main {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.departure-audit-info {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.departure-audit-text {
  line-height: 1.5;
}

.departure-date-input,
.departure-number-input {
  width: 100%;
  min-width: 0;
}

.departure-date-input {
  flex: 1.5 1 240px;
}

.departure-number-input {
  flex: 1 1 160px;
}

.departure-remove-button {
  margin-left: auto;
  flex: 0 0 auto;
}

.form-actions {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #e5edf5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-image {
  width: 100%;
  max-height: 70vh;
}

@media (max-width: 1280px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .combined-selection-grid,
  .field-grid-two {
    grid-template-columns: 1fr;
  }

  .inline-form-item {
    display: block;
  }

  .inline-form-item :deep(.el-form-item__label-wrap) {
    flex: none;
  }

  .inline-form-item :deep(.el-form-item__label) {
    min-height: auto;
    padding-bottom: 8px;
  }

  .inline-form-item :deep(.el-form-item__content) {
    width: 100%;
  }

  .section-heading-inline {
    flex-direction: column;
  }
}
</style>

