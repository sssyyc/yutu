<template>
  <div class="page-card contract-list-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">MY CONTRACTS</p>
        <h2>电子合同</h2>
      </div>
    </div>

    <el-table :data="list" border class="contract-table">
      <el-table-column prop="contractNo" label="合同号" min-width="220" />
      <el-table-column prop="routeName" label="路线信息" min-width="220" />
      <el-table-column prop="contractTitle" label="合同标题" min-width="240" />
      <el-table-column label="签订时间" min-width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.signTime) }}
        </template>
      </el-table-column>
      <el-table-column label="合同金额" width="120" align="center">
        <template #default="{ row }">
          {{ formatAmount(row.payAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="合同状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.lifecycleStatus).type" effect="light" round>
            {{ statusTag(row.lifecycleStatus).text }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.lifecycleStatus === 'PENDING_SIGN'"
            type="success"
            text
            @click="openSign(row.id)"
          >
            签署
          </el-button>
          <el-button type="primary" text @click="openDetail(row.id)">查看</el-button>
          <el-button
            v-if="canShowDownload(row.lifecycleStatus)"
            type="warning"
            text
            :disabled="row.lifecycleStatus === 'TERMINATED'"
            :class="{ 'is-disabled-action': row.lifecycleStatus === 'TERMINATED' }"
            @click="download(row.id)"
          >
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="detailDialogVisible"
      title="合同详情"
      width="980px"
      destroy-on-close
      class="contract-detail-dialog"
    >
      <div v-if="detail.contract" class="detail-panel">
        <section class="detail-hero">
          <div class="detail-hero-copy">
            <h3>{{ detail.contractTitle || detail.contract.contractTitle }}</h3>
            <div class="detail-meta">
              <span>合同号：{{ detail.contract.contractNo }}</span>
              <span>订单号：{{ detail.orderNo || "-" }}</span>
              <span>商家：{{ detail.merchantName || "-" }}</span>
              <span>路线：{{ detail.routeName || "-" }}</span>
              <span>合同金额：{{ formatAmount(detail.payAmount) }}</span>
            </div>
          </div>
          <div class="detail-hero-actions">
            <el-tag :type="statusTag(detail.lifecycleStatus).type" effect="light" round>
              {{ statusTag(detail.lifecycleStatus).text }}
            </el-tag>
            <el-button
              v-if="detail.lifecycleStatus === 'SIGNED' || detail.lifecycleStatus === 'COMPLETED'"
              type="warning"
              plain
              @click="download(currentDetailId)"
            >
              下载合同
            </el-button>
          </div>
        </section>

        <section class="detail-grid">
          <article class="detail-card">
            <div class="section-head">
              <h4>合同正文</h4>
            </div>
            <div class="contract-content">
              <p v-for="(paragraph, index) in contractParagraphs" :key="`${index}-${paragraph}`">
                {{ paragraph }}
              </p>
            </div>
          </article>

          <aside class="summary-card">
            <div class="summary-item">
              <span>合同状态</span>
              <strong>{{ statusTag(detail.lifecycleStatus).text }}</strong>
            </div>
            <div class="summary-item">
              <span>签订时间</span>
              <strong>{{ formatDateTime(detail.contract.signTime) }}</strong>
            </div>
            <div class="summary-item">
              <span>签署进度</span>
              <strong>{{ signedCount }}/{{ requiredSignCount }}</strong>
            </div>
            <div class="summary-item">
              <span>待签署人</span>
              <strong>{{ pendingTravelerNamesDisplay }}</strong>
            </div>
            <div class="summary-item">
              <span>已签署人</span>
              <strong>{{ signedTravelerNamesDisplay }}</strong>
            </div>
          </aside>
        </section>

        <section class="detail-card">
          <div class="section-head">
            <h4>出行人信息</h4>
            <span>{{ travelers.length }} 位</span>
          </div>
          <div v-if="travelers.length" class="traveler-list">
            <article v-for="traveler in travelers" :key="traveler.id" class="traveler-item">
              <strong>{{ traveler.travelerName }}</strong>
              <p>身份证号：{{ traveler.idCard || "-" }}</p>
              <p>手机号：{{ traveler.phone || "-" }}</p>
            </article>
          </div>
          <el-empty v-else description="当前合同暂无出行人信息" />
        </section>

        <section class="detail-card">
          <div class="section-head">
            <h4>补充附件</h4>
            <span>{{ appendices.length }} 项</span>
          </div>
          <div v-if="appendices.length" class="appendix-list">
            <article v-for="item in appendices" :key="item.id" class="appendix-item">
              <h4>{{ item.appendixTitle || "未命名附件" }}</h4>
              <p>{{ item.appendixContent || "暂无附件内容" }}</p>
            </article>
          </div>
          <el-empty v-else description="当前合同暂无补充附件" />
        </section>

        <section class="detail-card">
          <div class="section-head">
            <h4>签署记录</h4>
            <span>{{ signatures.length }} 条</span>
          </div>
          <div v-if="signatures.length" class="record-list">
            <article v-for="item in signatures" :key="item.id" class="record-item">
              <div class="record-head">
                <strong>{{ item.signerName || "签署人" }}</strong>
                <span>{{ item.signTime ? formatDateTime(item.signTime) : "已签署" }}</span>
              </div>
              <img
                v-if="item.signatureImage"
                :src="item.signatureImage"
                :alt="`${item.signerName || '签署人'}电子签名`"
                class="signature-image"
              />
            </article>
          </div>
          <el-empty v-else description="当前合同暂无签署记录" />
        </section>
      </div>

      <el-empty v-else description="暂无合同详情" />

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="detail.lifecycleStatus === 'PENDING_SIGN' && currentDetailId"
          type="success"
          @click="openSign(currentDetailId)"
        >
          前往签署
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api";

const router = useRouter();
const list = ref([]);
const detail = ref({});
const detailDialogVisible = ref(false);
const currentDetailId = ref(null);

const travelers = computed(() => (
  Array.isArray(detail.value.travelers) ? detail.value.travelers.filter(Boolean) : []
));

const signatures = computed(() => (
  Array.isArray(detail.value.signatures) ? detail.value.signatures.filter(Boolean) : []
));

const appendices = computed(() => (
  Array.isArray(detail.value.appendices) ? detail.value.appendices.filter(Boolean) : []
));

const requiredSignCount = computed(() => {
  const count = Number(detail.value.requiredSignCount);
  return Number.isFinite(count) && count >= 0 ? count : travelers.value.length;
});

const signedCount = computed(() => {
  const count = Number(detail.value.signedCount);
  return Number.isFinite(count) && count >= 0 ? count : signatures.value.length;
});

const contractParagraphs = computed(() => {
  const content = detail.value.contract?.contractContent || "";
  return content
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
});

const signedTravelerNamesDisplay = computed(() => {
  const names = signatures.value
    .map((item) => normalizeName(item.signerName))
    .filter(Boolean);
  return names.length ? [...new Set(names)].join("、") : "暂无";
});

const pendingTravelerNamesDisplay = computed(() => {
  const signedNames = new Set(
    signatures.value
      .map((item) => normalizeName(item.signerName))
      .filter(Boolean)
  );
  const pendingNames = travelers.value
    .map((item) => normalizeName(item.travelerName))
    .filter((name) => name && !signedNames.has(name));
  return pendingNames.length ? pendingNames.join("、") : "全部完成";
});

async function load() {
  list.value = await api.get("/contracts");
}

async function openDetail(id) {
  currentDetailId.value = id;
  detail.value = await api.get(`/contracts/${id}`);
  detailDialogVisible.value = true;
}

function openSign(id) {
  detailDialogVisible.value = false;
  router.push(`/contract/detail/${id}`);
}

function canShowDownload(status) {
  return ["SIGNED", "COMPLETED", "TERMINATED"].includes(status);
}

function triggerDownload(fileName, content) {
  const blob = new Blob([content || ""], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName || "contract.txt";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

async function download(id) {
  const payload = await api.get(`/contracts/${id}/download`);
  triggerDownload(payload.fileName, payload.content);
  ElMessage.success("合同已下载");
}

function statusTag(status) {
  switch (status) {
    case "PENDING_SIGN":
      return { text: "待签署", type: "warning" };
    case "SIGNED":
      return { text: "已签署", type: "success" };
    case "COMPLETED":
      return { text: "已完成", type: "primary" };
    case "TERMINATED":
      return { text: "已解除", type: "danger" };
    default:
      return { text: "未知状态", type: "info" };
  }
}

function formatDateTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

function formatAmount(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return "-";
  return `¥ ${numeric.toFixed(2)}`;
}

function normalizeName(value) {
  return typeof value === "string" ? value.trim() : "";
}

onMounted(load);
</script>

<style scoped>
.contract-list-page {
  padding: 28px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: #6b7a90;
}

h2 {
  margin: 0;
  font-size: 26px;
  color: #12233d;
}

.contract-table {
  border-radius: 18px;
  overflow: hidden;
}

.is-disabled-action {
  opacity: 0.42;
}

.detail-panel {
  display: grid;
  gap: 16px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f7fbff 0%, #ffffff 65%, #eef5ff 100%);
  border: 1px solid #e4ecf7;
}

.detail-hero-copy h3 {
  margin: 0;
  font-size: 24px;
  color: #12233d;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(240px, 1fr));
  gap: 14px 24px;
  margin-top: 18px;
  color: #61728a;
  font-size: 14px;
}

.detail-meta span {
  display: block;
  line-height: 1.8;
}

.detail-hero-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) 280px;
  gap: 16px;
}

.detail-card,
.summary-card {
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e6edf7;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head h4 {
  margin: 0;
  font-size: 18px;
  color: #12233d;
}

.section-head span {
  color: #7b889d;
  font-size: 13px;
}

.contract-content {
  display: grid;
  gap: 12px;
  line-height: 1.8;
  color: #30435f;
}

.contract-content p {
  margin: 0;
  white-space: pre-line;
}

.summary-card {
  display: grid;
  gap: 12px;
  align-content: start;
}

.summary-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid #e4ebf6;
}

.summary-item span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #7b889d;
}

.summary-item strong {
  display: block;
  color: #152847;
  line-height: 1.6;
  word-break: break-word;
}

.traveler-list,
.record-list,
.appendix-list {
  display: grid;
  gap: 12px;
}

.traveler-item,
.record-item,
.appendix-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f9fbff;
  border: 1px solid #e7edf7;
}

.traveler-item strong,
.record-head strong,
.appendix-item h4 {
  display: block;
  color: #152847;
}

.traveler-item p,
.record-head span,
.appendix-item p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.appendix-item h4,
.appendix-item p {
  word-break: break-word;
  white-space: pre-line;
}

.record-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.signature-image {
  max-width: 100%;
  max-height: 120px;
  object-fit: contain;
  border-radius: 10px;
  background: #fff;
}

@media (max-width: 768px) {
  .detail-meta {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-hero {
    flex-direction: column;
  }

  .detail-hero-actions {
    align-items: flex-start;
  }
}
</style>
