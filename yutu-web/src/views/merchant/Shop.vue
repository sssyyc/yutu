<template>
  <div class="admin-module-page shop-page">
    <AdminPageHero
      kicker="SHOP PROFILE"
      title="店铺信息"
    />

    <section class="shop-panel">
      <header class="shop-panel-head">
        <div class="panel-title-wrap">
          <h2>当前店铺信息</h2>
        </div>

        <div class="panel-actions">
          <el-tag :type="auditMeta.type" round effect="dark" class="audit-tag">{{ auditMeta.text }}</el-tag>
          <el-tooltip content="编辑可修改字段" placement="top">
            <el-button circle type="primary" class="edit-icon-btn" @click="openEditDialog">
              <el-icon><Edit /></el-icon>
            </el-button>
          </el-tooltip>
          <el-button plain :loading="loading" @click="loadShop">刷新</el-button>
        </div>
      </header>

      <div class="content-grid">
        <el-card shadow="never" class="display-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">店铺资料</span>
              <span class="editable-legend"><span class="editable-mark">*</span> 可修改字段</span>
            </div>
          </template>

          <el-descriptions :column="2" border class="shop-desc">
            <el-descriptions-item>
              <template #label>店铺名称</template>
              <div class="readonly-value">
                <span>{{ shop.shopName || "--" }}</span>
                <el-icon><Lock /></el-icon>
              </div>
            </el-descriptions-item>

            <el-descriptions-item>
              <template #label>联系人</template>
              <div class="readonly-value">
                <span>{{ shop.contactName || "--" }}</span>
                <el-icon><Lock /></el-icon>
              </div>
            </el-descriptions-item>

            <el-descriptions-item>
              <template #label>联系电话 <span class="editable-mark">*</span></template>
              <span class="normal-value">{{ shop.contactPhone || "--" }}</span>
            </el-descriptions-item>

            <el-descriptions-item>
              <template #label>营业执照号</template>
              <div class="readonly-value">
                <span>{{ shop.licenseNo || "--" }}</span>
                <el-icon><Lock /></el-icon>
              </div>
            </el-descriptions-item>

            <el-descriptions-item :span="2">
              <template #label>店铺简介 <span class="editable-mark">*</span></template>
              <div class="normal-value desc-value">{{ shop.description || "暂无简介" }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </section>

    <el-dialog v-model="editVisible" title="修改店铺信息" width="560px" destroy-on-close>
      <el-form label-width="110px" :model="editForm">
        <el-form-item>
          <template #label>
            <span>联系电话</span>
            <span class="editable-mark">*</span>
          </template>
          <el-input
            v-model="editForm.contactPhone"
            maxlength="11"
            clearable
            placeholder="请输入11位手机号"
          />
        </el-form-item>

        <el-form-item>
          <template #label>
            <span>店铺简介</span>
            <span class="editable-mark">*</span>
          </template>
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="5"
            maxlength="255"
            show-word-limit
            placeholder="请简要介绍主营路线、服务特色和售后保障"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Edit, Lock } from "@element-plus/icons-vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";
import { isValidPhone } from "../../utils/phone";

const loading = ref(false);
const saving = ref(false);
const editVisible = ref(false);

const shop = reactive({
  shopName: "",
  contactName: "",
  contactPhone: "",
  licenseNo: "",
  description: "",
  auditStatus: null
});

const editForm = reactive({
  contactPhone: "",
  description: ""
});

const auditMeta = computed(() => {
  if (shop.auditStatus === 1) return { text: "审核通过", type: "success" };
  if (shop.auditStatus === 2) return { text: "审核驳回", type: "danger" };
  if (shop.auditStatus === 0) return { text: "待审核", type: "warning" };
  return { text: "未提交", type: "info" };
});

async function loadShop() {
  loading.value = true;
  try {
    const data = await api.get("/merchant/shop");
    Object.assign(
      shop,
      {
        shopName: "",
        contactName: "",
        contactPhone: "",
        licenseNo: "",
        description: "",
        auditStatus: null
      },
      data || {}
    );
  } finally {
    loading.value = false;
  }
}

function openEditDialog() {
  editForm.contactPhone = shop.contactPhone || "";
  editForm.description = shop.description || "";
  editVisible.value = true;
}

async function save() {
  const phone = (editForm.contactPhone || "").trim();
  const description = (editForm.description || "").trim();
  if (!isValidPhone(phone)) {
    ElMessage.warning("联系电话必须为11位数字");
    return;
  }

  saving.value = true;
  try {
    await api.put("/merchant/shop", { contactPhone: phone, description });
    ElMessage.success("店铺信息已更新");
    editVisible.value = false;
    await loadShop();
  } finally {
    saving.value = false;
  }
}

onMounted(loadShop);
</script>

<style scoped>
.shop-page {
  --text-1: #0f172a;
  --text-2: #64748b;
  --line: #dbe4ef;
}

.shop-panel {
  margin-top: 16px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background:
    radial-gradient(circle at 92% -14%, rgba(56, 189, 248, 0.2), transparent 32%),
    linear-gradient(160deg, #f8fbff 0%, #f1f5f9 58%, #edf2f7 100%);
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.shop-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--line);
}

.panel-title-wrap h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  color: var(--text-1);
  font-weight: 900;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.audit-tag {
  font-size: 13px;
  font-weight: 700;
}

.edit-icon-btn {
  border: none;
  background: linear-gradient(135deg, #0e7490, #2563eb);
  color: #fff;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  padding: 16px 22px 22px;
}

.display-card {
  border: 1px solid var(--line);
  border-radius: 16px;
}

.display-card :deep(.el-card__header) {
  border-bottom: 1px solid #edf2f7;
  padding: 12px 14px;
}

.display-card :deep(.el-card__body) {
  padding: 14px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.editable-legend {
  font-size: 13px;
  color: #64748b;
}

.editable-mark {
  color: #ef4444;
  font-weight: 700;
}

.shop-desc :deep(.el-descriptions__label) {
  width: 140px;
  font-weight: 700;
  color: #334155;
}

.shop-desc :deep(.el-descriptions__content) {
  color: #0f172a;
}

.readonly-value {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #334155;
  font-weight: 700;
}

.readonly-value .el-icon {
  color: #94a3b8;
}

.normal-value {
  color: #0f172a;
  font-weight: 700;
}

.desc-value {
  font-weight: 500;
  color: #334155;
  white-space: pre-wrap;
  line-height: 1.7;
}


@media (max-width: 1120px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .shop-panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .panel-title-wrap h2 {
    font-size: 24px;
  }

  .content-grid {
    padding-left: 12px;
    padding-right: 12px;
  }

  .shop-desc :deep(.el-descriptions__label) {
    width: 110px;
  }
}
</style>
