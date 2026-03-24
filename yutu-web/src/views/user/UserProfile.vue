<template>
  <div class="profile-page">
    <div class="page-card section-card">
      <h3>个人资料</h3>
      <el-form :model="profile" label-width="110px" class="form-block">
        <el-form-item label="昵称">
          <el-input v-model="profile.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profile.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload-row">
            <el-avatar :size="72" :src="profile.avatar" class="profile-avatar" />
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :http-request="uploadProfileAvatar"
            >
              <el-button type="primary" plain>上传图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card section-card">
      <h3>修改密码</h3>
      <el-form :model="pwd" label-width="110px" class="form-block">
        <el-form-item label="旧密码">
          <el-input v-model="pwd.oldPassword" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="savePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card section-card">
      <div class="merchant-hero">
        <div class="merchant-hero-main">
          <div class="merchant-eyebrow">MERCHANT QUALIFICATION</div>
          <h3>商户资质申请</h3>
        </div>
        <div class="merchant-hero-side">
          <div class="hero-status-row">
            <span class="hero-status-label">当前状态</span>
            <el-tag :type="statusMeta.type" effect="plain" size="large">{{ statusMeta.text }}</el-tag>
          </div>
          <div v-if="auth.user?.roleType === 2" class="hero-actions">
            <el-button class="hero-action-btn" @click="$router.push('/merchant')">进入商家端</el-button>
            <el-button class="hero-action-btn" @click="handleCancelMerchant">注销商户</el-button>
          </div>
        </div>
      </div>

      <el-alert
        v-if="merchant.auditRemark"
        :title="merchant.auditRemark"
        :type="merchant.auditStatus === 2 ? 'error' : 'success'"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-descriptions v-if="merchant.createTime" :column="2" border class="status-panel">
        <el-descriptions-item label="申请时间">{{ merchant.createTime }}</el-descriptions-item>
        <el-descriptions-item label="最近更新时间">{{ merchant.updateTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ merchant.auditTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="当前角色">{{ roleText }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="merchant" label-width="110px" class="form-block">
        <el-form-item label="店铺名称">
          <el-input v-model="merchant.shopName" :disabled="merchantLocked" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="merchant.contactName" :disabled="merchantLocked" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="merchant.contactPhone" maxlength="11" :disabled="merchantLocked" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="营业执照号">
          <el-input v-model="merchant.licenseNo" :disabled="merchantLocked" placeholder="请输入营业执照号" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input
            v-model="merchant.description"
            :disabled="merchantLocked"
            type="textarea"
            :rows="3"
            placeholder="请输入店铺简介或主营线路说明"
          />
        </el-form-item>

        <el-form-item label="营业执照">
          <div class="upload-grid">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :disabled="merchantLocked"
              :http-request="(options) => uploadImage(options, 'licenseImage')"
            >
              <div class="upload-box">
                <el-image
                  v-if="merchant.licenseImage"
                  :src="merchant.licenseImage"
                  fit="cover"
                  class="upload-image"
                  :preview-src-list="[merchant.licenseImage]"
                  preview-teleported
                />
                <div v-else class="upload-placeholder">上传营业执照</div>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="身份证照片">
          <div class="upload-grid dual">
            <el-upload
              :show-file-list="false"
              accept="image/*"
              :disabled="merchantLocked"
              :http-request="(options) => uploadImage(options, 'idCardFrontImage')"
            >
              <div class="upload-box">
                <el-image
                  v-if="merchant.idCardFrontImage"
                  :src="merchant.idCardFrontImage"
                  fit="cover"
                  class="upload-image"
                  :preview-src-list="[merchant.idCardFrontImage]"
                  preview-teleported
                />
                <div v-else class="upload-placeholder">上传身份证人像面</div>
              </div>
            </el-upload>

            <el-upload
              :show-file-list="false"
              accept="image/*"
              :disabled="merchantLocked"
              :http-request="(options) => uploadImage(options, 'idCardBackImage')"
            >
              <div class="upload-box">
                <el-image
                  v-if="merchant.idCardBackImage"
                  :src="merchant.idCardBackImage"
                  fit="cover"
                  class="upload-image"
                  :preview-src-list="[merchant.idCardBackImage]"
                  preview-teleported
                />
                <div v-else class="upload-placeholder">上传身份证国徽面</div>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :disabled="merchantLocked" @click="submitMerchantApplication">
            {{ merchant.id ? "重新提交审核" : "提交商户申请" }}
          </el-button>
          <span v-if="merchant.auditStatus === 0" class="hint-text">材料提交后等待管理员审核</span>
          <span v-if="merchant.auditStatus === 1" class="hint-text">当前账号已具备商户资质</span>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { api } from "../../api";
import { useAuthStore } from "../../stores/auth";
import { isValidPhone } from "../../utils/phone";

const auth = useAuthStore();

const profile = reactive({
  nickname: "",
  phone: "",
  avatar: ""
});

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

const roleText = computed(() => {
  const map = {
    1: "普通用户",
    2: "商家",
    3: "管理员"
  };
  return map[auth.user?.roleType] || "普通用户";
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
}

async function loadMerchantApplication() {
  const data = await api.get("/user/merchant-application");
  Object.assign(merchant, merchantDefaults(), data || {});
}

async function saveProfile() {
  if (profile.phone && !isValidPhone(profile.phone)) {
    ElMessage.warning("手机号必须为11位数字");
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
    await api.put("/user/profile", { avatar: result.url });
    await auth.fetchMe();
    ElMessage.success("头像已更新");
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
    ElMessage.warning("手机号必须为11位数字");
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
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-card {
  padding: 20px 24px 22px;
}

.section-card h3 {
  margin: 0 0 16px;
  font-size: 22px;
  line-height: 1.3;
  font-weight: 800;
  color: #0f172a;
}

.merchant-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  margin-bottom: 16px;
}

.merchant-hero-main {
  flex: 1;
  min-width: 0;
}

.merchant-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #475569;
  background: #f1f5f9;
}

.merchant-hero-main h3 {
  margin: 10px 0 0;
  color: #0f172a;
  font-size: 26px;
}

.merchant-hero-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
  min-width: 300px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.hero-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-status-label {
  font-size: 13px;
  color: #64748b;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.hero-action-btn {
  flex: 1;
  min-width: 0;
  border-radius: 10px;
}

.status-panel {
  margin-bottom: 16px;
}

.form-block {
  width: min(860px, 100%);
}

.profile-page :deep(.el-form-item) {
  margin-bottom: 16px;
}

.profile-page :deep(.el-form-item__label) {
  padding-right: 16px;
  color: #475569;
  font-weight: 600;
}

.profile-page :deep(.el-form-item__content) {
  max-width: 740px;
}

.profile-page :deep(.el-input__wrapper),
.profile-page :deep(.el-textarea__inner) {
  border-radius: 10px;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.profile-avatar {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.upload-grid.dual .upload-box {
  width: 220px;
}

.upload-box {
  width: 320px;
  height: 180px;
  border: 1px dashed #cfd4dc;
  border-radius: 14px;
  overflow: hidden;
  background: #f8fafc;
  cursor: pointer;
}

.upload-image {
  width: 100%;
  height: 100%;
  display: block;
}

.upload-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667085;
  font-size: 14px;
}

.hint-text {
  margin-left: 12px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 900px) {
  .section-card {
    padding: 16px;
  }

  .section-card h3 {
    font-size: 20px;
  }

  .merchant-hero {
    flex-direction: column;
    padding: 16px;
  }

  .merchant-hero-main h3 {
    font-size: 24px;
  }

  .merchant-hero-side {
    width: 100%;
    min-width: 0;
  }

  .hero-actions {
    width: 100%;
    flex-direction: column;
  }

  .form-block {
    width: 100%;
  }

  .profile-page :deep(.el-form-item__content) {
    max-width: 100%;
  }

  .avatar-upload-row {
    flex-wrap: wrap;
    align-items: flex-start;
  }
}
</style>
