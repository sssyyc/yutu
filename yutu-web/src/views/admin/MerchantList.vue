<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="Merchant Review"
      title="商户管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="toolbar-search"
          clearable
          placeholder="请输入申请人或店铺名称"
          @clear="load"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="申请人" min-width="180">
          <template #default="{ row }">
            <div>{{ row.applicantUsername || "-" }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="店铺名称" min-width="180" />
        <el-table-column prop="contactPhone" label="联系方式" min-width="180" />
        <el-table-column prop="licenseNo" label="营业执照号" min-width="180" />
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="auditTag(row.auditStatus).type">{{ auditTag(row.auditStatus).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="170" />
        <el-table-column prop="auditRemark" label="审核说明" min-width="220">
          <template #default="{ row }">
            {{ row.auditRemark || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="资料" width="100">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 0">
              <el-button text type="success" @click="approve(row)">通过</el-button>
              <el-button text type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <span v-else class="sub-text">已审核</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailVisible" title="商户申请详情" width="760px">
      <template v-if="current">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请人">{{ current.applicantUsername || "-" }}</el-descriptions-item>
          <el-descriptions-item label="当前角色">{{ roleText(current.roleType) }}</el-descriptions-item>
          <el-descriptions-item label="店铺名称">{{ current.shopName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ current.contactPhone || "-" }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ current.contactName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="营业执照号">{{ current.licenseNo || "-" }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ current.createTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ current.auditTime || "-" }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditTag(current.auditStatus).type">{{ auditTag(current.auditStatus).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核说明">{{ current.auditRemark || "-" }}</el-descriptions-item>
          <el-descriptions-item label="店铺简介" :span="2">{{ current.description || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div class="image-grid">
          <div class="image-card">
            <div class="image-title">营业执照</div>
            <el-image
              v-if="current.licenseImage"
              :src="current.licenseImage"
              fit="cover"
              class="preview-image"
              :preview-src-list="[current.licenseImage]"
              preview-teleported
            />
            <div v-else class="image-empty">未上传</div>
          </div>
          <div class="image-card">
            <div class="image-title">身份证人像面</div>
            <el-image
              v-if="current.idCardFrontImage"
              :src="current.idCardFrontImage"
              fit="cover"
              class="preview-image"
              :preview-src-list="[current.idCardFrontImage]"
              preview-teleported
            />
            <div v-else class="image-empty">未上传</div>
          </div>
          <div class="image-card">
            <div class="image-title">身份证国徽面</div>
            <el-image
              v-if="current.idCardBackImage"
              :src="current.idCardBackImage"
              fit="cover"
              class="preview-image"
              :preview-src-list="[current.idCardBackImage]"
              preview-teleported
            />
            <div v-else class="image-empty">未上传</div>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const keyword = ref("");
const detailVisible = ref(false);
const current = ref(null);

function auditTag(status) {
  const map = {
    0: { text: "待审核", type: "warning" },
    1: { text: "已通过", type: "success" },
    2: { text: "已驳回", type: "danger" }
  };
  return map[status] || { text: "未知", type: "info" };
}

function roleText(roleType) {
  const map = {
    1: "普通用户",
    2: "商家",
    3: "管理员"
  };
  return map[roleType] || "普通用户";
}

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/merchants", params);
}

function reset() {
  keyword.value = "";
  load();
}

function openDetail(row) {
  current.value = row;
  detailVisible.value = true;
}

async function approve(row) {
  try {
    const { value } = await ElMessageBox.prompt("可填写审核说明，留空则直接通过。", "通过商户申请", {
      inputPlaceholder: "请输入审核说明（选填）",
      confirmButtonText: "通过",
      cancelButtonText: "取消"
    });
    await api.post(`/admin/merchants/${row.id}/approve`, { auditRemark: value || "" });
    ElMessage.success("商户申请已通过");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

async function reject(row) {
  try {
    const { value } = await ElMessageBox.prompt("请输入驳回原因，用户会在个人中心看到该说明。", "驳回商户申请", {
      inputPlaceholder: "请输入驳回原因",
      confirmButtonText: "驳回",
      cancelButtonText: "取消",
      inputValidator: (val) => (val ? true : "请填写驳回原因")
    });
    await api.post(`/admin/merchants/${row.id}/reject`, { auditRemark: value });
    ElMessage.success("商户申请已驳回");
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      throw error;
    }
  }
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-search {
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.sub-text {
  color: #667085;
  font-size: 12px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.image-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 12px;
  background: #fafafa;
}

.image-title {
  margin-bottom: 10px;
  font-weight: 600;
}

.preview-image,
.image-empty {
  width: 100%;
  height: 180px;
  border-radius: 10px;
  overflow: hidden;
}

.image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #667085;
}

@media (max-width: 900px) {
  .image-grid {
    grid-template-columns: 1fr;
  }
}
</style>
