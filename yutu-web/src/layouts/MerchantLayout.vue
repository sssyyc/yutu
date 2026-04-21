<template>
  <el-container class="merchant-layout">
    <el-aside class="merchant-aside" width="248px">
      <div class="brand-block">
        <div class="brand-mark">YT</div>
        <div>
          <div class="brand-title">豫途 · 商家端</div>
          <div class="brand-subtitle">Merchant Console</div>
        </div>
      </div>

      <el-menu :default-active="$route.path" class="merchant-menu" router>
        <el-menu-item index="/merchant">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/merchant/route/list">
          <el-icon><MapLocation /></el-icon>
          <span>路线管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/order/list">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/customer/list">
          <el-icon><User /></el-icon>
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/contract/list">
          <el-icon><Document /></el-icon>
          <span>合同管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/complaint/list">
          <el-icon><Warning /></el-icon>
          <span>投诉处理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>经营统计</span>
        </el-menu-item>
        <el-menu-item index="/merchant/shop">
          <el-icon><Shop /></el-icon>
          <span>店铺信息</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="merchant-header">
        <div class="header-title">
          <div class="header-title-main">商家运营后台</div>
        </div>

        <div class="header-actions">
          <el-button plain class="switch-btn" @click="router.push('/')">用户端</el-button>

          <el-dropdown trigger="click">
            <div class="profile-trigger">
              <el-avatar :size="40" :src="auth.user?.avatar">
                {{ avatarFallback }}
              </el-avatar>
              <div class="profile-meta">
                <div class="profile-name">{{ auth.user?.nickname || auth.user?.username || "商家用户" }}</div>
                <div class="profile-role">商家账号</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openSettings">个人设置</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="merchant-main">
        <router-view :key="$route.fullPath" />
      </el-main>
    </el-container>

    <el-drawer v-model="settingsVisible" title="个人设置" size="420px">
      <div class="settings-drawer">
        <div class="avatar-panel">
          <el-avatar :size="72" :src="profileForm.avatar">
            {{ avatarFallback }}
          </el-avatar>
          <div class="avatar-actions">
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadAvatar">
              <el-button type="primary" plain>上传头像</el-button>
            </el-upload>
          </div>
        </div>

        <el-form :model="profileForm" label-width="88px">
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" maxlength="11" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <el-form :model="passwordForm" label-width="88px">
          <el-form-item label="旧密码">
            <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入旧密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" @click="savePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  DataAnalysis,
  Document,
  House,
  MapLocation,
  Shop,
  Tickets,
  User,
  Warning
} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { api } from "../api";
import { isValidPhone } from "../utils/phone";

const router = useRouter();
const auth = useAuthStore();
const settingsVisible = ref(false);

const profileForm = reactive({
  nickname: "",
  phone: "",
  avatar: ""
});

const passwordForm = reactive({
  oldPassword: "",
  newPassword: ""
});

const avatarFallback = computed(() => {
  const text = auth.user?.nickname || auth.user?.username || "商";
  return text.slice(0, 1).toUpperCase();
});

function syncProfileForm() {
  profileForm.nickname = auth.user?.nickname || "";
  profileForm.phone = auth.user?.phone || "";
  profileForm.avatar = auth.user?.avatar || "";
}

function openSettings() {
  syncProfileForm();
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  settingsVisible.value = true;
}

async function uploadAvatar(options) {
  if (profileForm.phone && !isValidPhone(profileForm.phone)) {
    ElMessage.warning("手机号必须为11位数字");
    options.onError?.(new Error("invalid phone"));
    return;
  }
  const formData = new FormData();
  formData.append("file", options.file);
  try {
    const result = await api.upload("/files/upload", formData);
    profileForm.avatar = result.url;
    await api.put("/user/profile", profileForm);
    await auth.fetchMe();
    syncProfileForm();
    ElMessage.success("头像已更新");
    options.onSuccess?.(result);
  } catch (error) {
    options.onError?.(error);
  }
}

async function saveProfile() {
  if (profileForm.phone && !isValidPhone(profileForm.phone)) {
    ElMessage.warning("手机号必须为11位数字");
    return;
  }
  await api.put("/user/profile", profileForm);
  await auth.fetchMe();
  syncProfileForm();
  ElMessage.success("个人资料已更新");
}

async function savePassword() {
  await api.put("/user/password", passwordForm);
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  ElMessage.success("密码已更新");
}

function logout() {
  auth.logout();
  location.href = "/login";
}
</script>

<style scoped>
.merchant-layout {
  min-height: 100vh;
  background: #eef2f7;
}

.merchant-aside {
  display: flex;
  flex-direction: column;
  padding: 18px 14px;
  background: linear-gradient(180deg, #182333 0%, #111a27 100%);
  color: #fff;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px 18px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffdb8c, #f59e0b);
  color: #102131;
  font-weight: 800;
}

.brand-title {
  font-size: 20px;
  font-weight: 800;
  color: #f7fafc;
}

.brand-subtitle {
  margin-top: 3px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(226, 232, 240, 0.68);
  text-transform: uppercase;
}

.merchant-menu {
  border-right: none;
  background: transparent;
}

.merchant-menu :deep(.el-menu) {
  background: transparent;
}

.merchant-menu :deep(.el-menu-item) {
  height: 48px;
  margin-bottom: 6px;
  border-radius: 14px;
  color: rgba(226, 232, 240, 0.88);
}

.merchant-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.merchant-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(14, 116, 144, 0.92), rgba(21, 128, 61, 0.92));
  color: #ffffff;
}

.merchant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
}

.header-title-main {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-btn {
  border-radius: 12px;
}

.profile-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.profile-trigger:hover {
  background: rgba(15, 23, 42, 0.05);
}

.profile-meta {
  min-width: 112px;
  display: flex;
  align-items: center;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  color: #0f172a;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profile-role {
  display: none;
}

.merchant-main {
  padding: 24px;
  background: #eef2f7;
}

.settings-drawer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
