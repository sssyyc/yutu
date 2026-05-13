<template>
  <div class="profile-page">
    <section class="profile-grid">
      <article class="page-card section-card compact-card profile-card">
        <div class="card-head">
          <div>
            <h3>个人资料</h3>
            <p class="card-copy">更新昵称、手机号和头像信息</p>
          </div>
        </div>

        <el-form :model="profile" label-width="76px" class="form-block compact-form inline-form">
          <div class="profile-form-layout">
            <div class="profile-fields">
              <el-form-item label="昵称">
                <el-input v-model="profile.nickname" />
              </el-form-item>

              <el-form-item label="手机号">
                <el-input v-model="profile.phone" maxlength="11" />
              </el-form-item>
            </div>

            <div class="profile-avatar-panel">
              <div class="avatar-panel-label">头像</div>
              <div class="avatar-upload-stack">
                <el-avatar :size="58" :src="profile.avatar" class="profile-avatar" />
                <el-upload
                  :show-file-list="false"
                  accept="image/*"
                  :http-request="uploadProfileAvatar"
                >
                  <el-button type="primary" plain>上传图片</el-button>
                </el-upload>
              </div>
            </div>
          </div>

          <el-form-item class="form-submit-item profile-submit-item">
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </article>

      <article class="page-card section-card compact-card password-card">
        <div class="card-head">
          <div>
            <h3>修改密码</h3>
            <p class="card-copy">定期更新密码，保持账号安全</p>
          </div>
        </div>

        <el-form :model="pwd" label-width="76px" class="form-block compact-form inline-form">
          <el-form-item label="旧密码">
            <el-input v-model="pwd.oldPassword" show-password />
          </el-form-item>

          <el-form-item label="新密码">
            <el-input v-model="pwd.newPassword" show-password />
          </el-form-item>

          <el-form-item class="form-submit-item">
            <el-button type="warning" @click="savePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </article>
    </section>

    <section class="page-card section-card merchant-card">
      <div class="card-head">
        <div>
          <h3>商户资质</h3>
          <p class="card-copy">{{ statusMeta.text === '未申请' ? '申请成为商户，发布和管理旅游路线' : '管理店铺信息与资质材料' }}</p>
        </div>
        <el-tag :type="statusMeta.type" effect="light" round>{{ statusMeta.text }}</el-tag>
      </div>

      <div v-if="merchant.auditRemark" class="audit-remark" :class="merchant.auditStatus === 2 ? 'remark-rejected' : 'remark-passed'">
        {{ merchant.auditRemark }}
      </div>

      <div v-if="auth.user?.roleType === 2" class="merchant-actions-row">
        <el-button plain @click="$router.push('/merchant')">进入商家端</el-button>
        <el-button plain type="danger" @click="handleCancelMerchant">注销商户</el-button>
      </div>

      <el-form :model="merchant" label-width="100px" class="form-block merchant-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="店铺名称">
              <el-input v-model="merchant.shopName" :disabled="merchantLocked" placeholder="请输入店铺名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营业执照号">
              <el-input v-model="merchant.licenseNo" :disabled="merchantLocked" placeholder="营业执照注册号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="merchant.contactName" :disabled="merchantLocked" placeholder="联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="merchant.contactPhone" maxlength="11" :disabled="merchantLocked" placeholder="手机号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="店铺简介">
          <el-input v-model="merchant.description" :disabled="merchantLocked" type="textarea" :rows="3" placeholder="店铺简介或主营路线说明" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="营业执照">
              <el-upload :show-file-list="false" accept="image/*" :disabled="merchantLocked" :http-request="(options) => uploadImage(options, 'licenseImage')">
                <div class="upload-box" :class="{ filled: merchant.licenseImage }">
                  <el-image v-if="merchant.licenseImage" :src="merchant.licenseImage" fit="cover" class="upload-image" :preview-src-list="[merchant.licenseImage]" preview-teleported />
                  <div v-else class="upload-placeholder"><el-icon :size="22"><Plus /></el-icon><span>{{ merchantLocked ? '未上传' : '点击上传' }}</span></div>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证照片">
              <div class="upload-row">
                <el-upload :show-file-list="false" accept="image/*" :disabled="merchantLocked" :http-request="(options) => uploadImage(options, 'idCardFrontImage')">
                  <div class="upload-box" :class="{ filled: merchant.idCardFrontImage }">
                    <el-image v-if="merchant.idCardFrontImage" :src="merchant.idCardFrontImage" fit="cover" class="upload-image" :preview-src-list="[merchant.idCardFrontImage]" preview-teleported />
                    <div v-else class="upload-placeholder"><el-icon :size="22"><Plus /></el-icon><span>{{ merchantLocked ? '未上传' : '人像面' }}</span></div>
                  </div>
                </el-upload>
                <el-upload :show-file-list="false" accept="image/*" :disabled="merchantLocked" :http-request="(options) => uploadImage(options, 'idCardBackImage')">
                  <div class="upload-box" :class="{ filled: merchant.idCardBackImage }">
                    <el-image v-if="merchant.idCardBackImage" :src="merchant.idCardBackImage" fit="cover" class="upload-image" :preview-src-list="[merchant.idCardBackImage]" preview-teleported />
                    <div v-else class="upload-placeholder"><el-icon :size="22"><Plus /></el-icon><span>{{ merchantLocked ? '未上传' : '国徽面' }}</span></div>
                  </div>
                </el-upload>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item class="merchant-submit">
          <el-button type="primary" :disabled="merchantLocked" @click="submitMerchantApplication">
            {{ merchant.id ? "重新提交审核" : "提交申请" }}
          </el-button>
          <span v-if="merchant.auditStatus === 0" class="hint-text">提交后等待管理员审核</span>
          <span v-if="merchant.auditStatus === 1" class="hint-text hint-success">已具备商户资质</span>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { api } from "../../api";
import { useAuthStore } from "../../stores/auth";
import { isValidPhone } from "../../utils/phone";

const auth = useAuthStore();

const profile = reactive({
  nickname: "",
  phone: "",
  avatar: ""
});

const pendingAvatar = ref("");

const pwd = reactive({
  oldPassword: "",
  newPassword: ""
});

const merchantDefaults = () => ({
  id: null,
  shopName: "",
  contactName: "",
  contactPhone: "",
  description: "",
  licenseNo: "",
  licenseImage: "",
  idCardFrontImage: "",
  idCardBackImage: "",
  auditStatus: null,
  auditRemark: "",
  createTime: "",
  updateTime: "",
  auditTime: "",
  roleType: 1
});

const merchant = reactive(merchantDefaults());

const statusMeta = computed(() => {
  const map = {
    null: { text: "未申请", type: "info" },
    0: { text: "待审核", type: "warning" },
    1: { text: "已通过", type: "success" },
    2: { text: "已驳回", type: "danger" }
  };
  return map[merchant.auditStatus] || map.null;
});

const merchantLocked = computed(() => merchant.auditStatus === 1);

onMounted(async () => {
  await auth.fetchMe();
  syncProfile();
  await loadMerchantApplication();
});

function syncProfile() {
  profile.nickname = auth.user?.nickname || "";
  profile.phone = auth.user?.phone || "";
  profile.avatar = auth.user?.avatar || "";
  pendingAvatar.value = "";
}

async function loadMerchantApplication() {
  const data = await api.get("/user/merchant-application");
  Object.assign(merchant, merchantDefaults(), data || {});
}

async function saveProfile() {
  if (profile.phone && !isValidPhone(profile.phone)) {
    ElMessage.warning("手机号必须为 11 位数字");
    return;
  }
  await api.put("/user/profile", profile);
  ElMessage.success("资料已更新");
  await auth.fetchMe();
  syncProfile();
}

async function savePwd() {
  await api.put("/user/password", pwd);
  ElMessage.success("密码已更新");
  pwd.oldPassword = "";
  pwd.newPassword = "";
}

async function uploadProfileAvatar(options) {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const result = await api.upload("/files/upload", formData);
    profile.avatar = result.url;
    pendingAvatar.value = result.url;
    ElMessage.success("头像已选择，点击“保存资料”后生效");
    if (options.onSuccess) {
      options.onSuccess(result);
    }
  } catch (error) {
    if (options.onError) {
      options.onError(error);
    }
  }
}

async function uploadImage(options, field) {
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const result = await api.upload("/files/upload", formData);
    merchant[field] = result.url;
    ElMessage.success("图片上传成功");
    if (options.onSuccess) {
      options.onSuccess(result);
    }
  } catch (error) {
    if (options.onError) {
      options.onError(error);
    }
  }
}

async function submitMerchantApplication() {
  if (!merchant.shopName || !merchant.contactName || !merchant.contactPhone || !merchant.licenseNo) {
    ElMessage.warning("请先完整填写申请信息");
    return;
  }
  if (!isValidPhone(merchant.contactPhone)) {
    ElMessage.warning("手机号必须为 11 位数字");
    return;
  }
  if (!merchant.licenseImage || !merchant.idCardFrontImage || !merchant.idCardBackImage) {
    ElMessage.warning("请上传营业执照和身份证照片");
    return;
  }

  await api.post("/user/merchant-application", {
    shopName: merchant.shopName,
    contactName: merchant.contactName,
    contactPhone: merchant.contactPhone,
    description: merchant.description,
    licenseNo: merchant.licenseNo,
    licenseImage: merchant.licenseImage,
    idCardFrontImage: merchant.idCardFrontImage,
    idCardBackImage: merchant.idCardBackImage
  });
  ElMessage.success("商户申请已提交，请等待管理员审核");
  await auth.fetchMe();
  syncProfile();
  await loadMerchantApplication();
}

async function handleCancelMerchant() {
  try {
    await ElMessageBox.confirm(
      "注销后将停用店铺并下架路线，仅当所有订单和投诉都处理完成时才可提交。确认继续吗？",
      "注销商户",
      {
        confirmButtonText: "确认注销",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    await api.post("/user/merchant-cancel");
    ElMessage.success("商户已注销，当前账号已切换为普通用户");
    await auth.fetchMe();
    syncProfile();
    await loadMerchantApplication();
  } catch (error) {
    if (error !== "cancel") {
      // keep global error handling; no extra message required here
    }
  }
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(420px, 0.88fr);
  gap: 18px;
  align-items: start;
}

.section-card {
  padding: 18px 20px 12px;
}

.compact-card {
  align-self: start;
  min-height: 228px;
}

.profile-card {
  padding-bottom: 4px;
  overflow: hidden;
}

.section-card h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
  font-weight: 800;
  color: #0f172a;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.card-copy {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.form-block {
  width: 100%;
}

.compact-form {
  max-width: none;
}

.profile-form-layout {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  flex-wrap: nowrap;
}

.profile-fields {
  flex: 0 1 640px;
  min-width: 0;
  max-width: 640px;
}

.profile-avatar-panel {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.avatar-panel-label {
  margin: 0;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  line-height: 40px;
  white-space: nowrap;
}

.avatar-upload-stack {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  border: 1px dashed #dbe3ee;
  border-radius: 10px;
  background: #f8fafc;
}

.inline-form :deep(.el-form-item) {
  align-items: flex-start;
}

.inline-form :deep(.el-form-item__label) {
  padding: 0 14px 0 0;
  line-height: 40px;
}

.inline-form :deep(.el-form-item__content) {
  min-width: 0;
  max-width: none;
}

.merchant-form {
  width: min(860px, 100%);
}

.profile-page :deep(.el-form-item) {
  margin-bottom: 12px;
}

.profile-page :deep(.el-form-item__label) {
  color: #475569;
  font-weight: 600;
  padding-bottom: 6px;
}

.profile-page :deep(.el-form-item__content) {
  max-width: 100%;
}

.profile-page :deep(.el-input__wrapper),
.profile-page :deep(.el-textarea__inner) {
  border-radius: 10px;
}

.form-submit-item {
  margin-bottom: 0;
}

.inline-form .form-submit-item :deep(.el-form-item__content) {
  margin-left: 76px !important;
}

.profile-submit-item {
  margin-top: -2px;
}

.password-card .form-block {
  max-width: 560px;
}

.profile-avatar {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  flex-shrink: 0;
}

/* ===== Merchant Section ===== */
.merchant-card {
  padding-bottom: 20px;
}

.audit-remark {
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 14px;
  font-size: 14px;
  line-height: 1.6;
}

.remark-rejected {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.remark-passed {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.merchant-actions-row {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
}

.merchant-form {
  width: 100%;
}

.upload-row {
  display: flex;
  gap: 10px;
}

.upload-box {
  width: 100%;
  height: 88px;
  border: 1px dashed #d6dbe4;
  border-radius: 10px;
  overflow: hidden;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.upload-box:hover {
  border-color: #94a3b8;
  background: #f1f5f9;
}

.upload-box.filled {
  border-style: solid;
  border-color: #e2e8f0;
}

.upload-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.upload-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 13px;
}

.upload-placeholder .el-icon {
  color: #cbd5e1;
}

.merchant-submit {
  margin-top: 0;
}

.hint-text {
  margin-left: 12px;
  color: #64748b;
  font-size: 13px;
}

.hint-success {
  color: #16a34a;
}

@media (max-width: 980px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .password-card {
    order: 2;
  }
}

@media (max-width: 900px) {
  .section-card {
    padding: 16px;
  }

  .section-card h3 {
    font-size: 20px;
  }

  .profile-form-layout {
    flex-direction: column;
    gap: 12px;
  }

  .profile-avatar-panel {
    align-items: flex-start;
  }

  .profile-fields {
    max-width: 100%;
  }

  .merchant-form {
    width: 100%;
  }

  .inline-form .form-submit-item :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
