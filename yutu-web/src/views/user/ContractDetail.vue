<template>
  <div class="contract-detail-page">
    <section v-if="data.contract" class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">TRAVEL CONTRACT</p>
        <h1>{{ data.contract.contractTitle }}</h1>
        <div class="meta-row">
          <span>合同号 {{ data.contract.contractNo }}</span>
          <span>{{ data.contract.signStatus === "SIGNED" ? "已签署" : "待签署" }}</span>
          <span v-if="data.contract.signTime">签署时间 {{ formatDateTime(data.contract.signTime) }}</span>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="downloadContract">下载合同</el-button>
          <el-button v-if="canSign" type="success" plain @click="scrollToSignature">前往签署</el-button>
          <el-button
            v-else-if="data.contract?.orderId"
            type="warning"
            plain
            @click="goToPayment"
          >
            前往支付
          </el-button>
        </div>
      </div>
      <div class="hero-status">
        <div class="status-pill" :class="{ signed: !canSign }">
          {{ canSign ? "等待电子签名" : "签署完成" }}
        </div>
        <p>{{ canSign ? "阅读合同正文后，在下方签名板完成电子签名。" : "当前合同已经签署完成，可下载留存。" }}</p>
      </div>
    </section>

    <section v-if="data.contract" class="detail-grid">
      <article class="content-card">
        <div class="section-head">
          <h3>合同正文</h3>
          <span>标准合同模板 + 线路补充条款</span>
        </div>
        <div class="contract-content">
          <p v-for="(paragraph, index) in contractParagraphs" :key="`${index}-${paragraph}`">
            {{ paragraph }}
          </p>
        </div>
      </article>

      <aside class="summary-card">
        <div class="summary-item">
          <span>签署状态</span>
          <strong>{{ data.contract.signStatus === "SIGNED" ? "已签署" : "待签署" }}</strong>
        </div>
        <div class="summary-item">
          <span>合同编号</span>
          <strong>{{ data.contract.contractNo }}</strong>
        </div>
        <div class="summary-item">
          <span>签署人</span>
          <strong>{{ signerDisplayName }}</strong>
        </div>
        <div class="summary-item">
          <span>签署时间</span>
          <strong>{{ data.contract.signTime ? formatDateTime(data.contract.signTime) : "待签署" }}</strong>
        </div>
      </aside>
    </section>

    <section class="page-card appendix-card">
      <div class="section-head">
        <h3>补充附件</h3>
        <span>{{ (data.appendices || []).length }} 项</span>
      </div>
      <div v-if="data.appendices?.length" class="appendix-list">
        <article v-for="item in data.appendices" :key="item.id" class="appendix-item">
          <h4>{{ item.appendixTitle }}</h4>
          <p>{{ item.appendixContent || "暂无补充内容" }}</p>
        </article>
      </div>
      <el-empty v-else description="当前合同暂无补充附件" />
    </section>

    <section ref="signatureSectionRef" class="page-card signature-card">
      <div class="section-head">
        <h3>电子签名</h3>
        <span>{{ canSign ? "签名后将完成本合同确认" : "已保存电子签名" }}</span>
      </div>

      <template v-if="canSign">
        <div class="signer-form">
          <label>签署人</label>
          <el-input v-model="signForm.signerName" maxlength="64" placeholder="请输入签署人姓名" />
        </div>
        <p class="signature-hint">
          请在签名板中完成手写电子签名。提交后，系统会记录签署时间并更新合同状态。
        </p>
        <div class="signature-board">
          <canvas
            ref="signatureCanvasRef"
            class="signature-canvas"
            @pointerdown="startDraw"
            @pointermove="draw"
            @pointerup="endDraw"
            @pointerleave="endDraw"
          />
        </div>
        <div class="signature-actions">
          <el-button @click="clearSignature">清除签名</el-button>
          <el-button type="primary" :loading="signing" @click="submitSignature">确认签署</el-button>
        </div>
      </template>

      <template v-else>
        <div class="signed-panel">
          <div class="signed-meta">
            <div>
              <span>签署人</span>
              <strong>{{ signatureName }}</strong>
            </div>
            <div>
              <span>签署时间</span>
              <strong>{{ data.contract?.signTime ? formatDateTime(data.contract.signTime) : "已签署" }}</strong>
            </div>
          </div>
          <div class="signature-preview">
            <img
              v-if="data.signature?.signatureImage"
              :src="data.signature.signatureImage"
              alt="电子签名"
            />
            <div v-else class="signature-fallback">{{ signatureName }}</div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api";
import { useAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const data = ref({});
const signing = ref(false);
const signatureCanvasRef = ref(null);
const signatureSectionRef = ref(null);
const isDrawing = ref(false);
const hasSignatureStroke = ref(false);
let context2d = null;

const signForm = reactive({
  signerName: ""
});

const canSign = computed(() => data.value.contract?.signStatus !== "SIGNED");
const signerDisplayName = computed(() => data.value.signerDisplayName || auth.user?.nickname || auth.user?.username || "游客");
const signatureName = computed(() => data.value.signature?.signerName || signerDisplayName.value);
const contractParagraphs = computed(() => {
  const content = data.value.contract?.contractContent || "";
  return content
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
});

async function load() {
  data.value = await api.get(`/contracts/${route.params.id}`);
  signForm.signerName = signerDisplayName.value;
  await nextTick();
  if (canSign.value) {
    initCanvas();
    if (route.query.action === "sign") {
      scrollToSignature();
    }
  }
}

function initCanvas() {
  const canvas = signatureCanvasRef.value;
  if (!canvas) return;
  const ratio = window.devicePixelRatio || 1;
  const width = canvas.clientWidth || 720;
  const height = canvas.clientHeight || 200;
  canvas.width = width * ratio;
  canvas.height = height * ratio;
  context2d = canvas.getContext("2d");
  context2d.scale(ratio, ratio);
  context2d.lineWidth = 2.2;
  context2d.lineCap = "round";
  context2d.lineJoin = "round";
  context2d.strokeStyle = "#17324f";
  context2d.fillStyle = "#ffffff";
  context2d.fillRect(0, 0, width, height);
}

function pointInCanvas(event) {
  const canvas = signatureCanvasRef.value;
  const rect = canvas.getBoundingClientRect();
  return {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  };
}

function startDraw(event) {
  if (!context2d) return;
  const point = pointInCanvas(event);
  isDrawing.value = true;
  context2d.beginPath();
  context2d.moveTo(point.x, point.y);
}

function draw(event) {
  if (!isDrawing.value || !context2d) return;
  const point = pointInCanvas(event);
  context2d.lineTo(point.x, point.y);
  context2d.stroke();
  hasSignatureStroke.value = true;
}

function endDraw() {
  if (!context2d) return;
  isDrawing.value = false;
  context2d.closePath();
}

function clearSignature() {
  hasSignatureStroke.value = false;
  initCanvas();
}

function scrollToSignature() {
  signatureSectionRef.value?.scrollIntoView({ behavior: "smooth", block: "start" });
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

async function downloadContract() {
  const payload = await api.get(`/contracts/${route.params.id}/download`);
  triggerDownload(payload.fileName, payload.content);
  ElMessage.success("合同已下载");
}

async function submitSignature() {
  if (!signForm.signerName.trim()) {
    ElMessage.warning("请填写签署人");
    return;
  }
  if (!hasSignatureStroke.value || !signatureCanvasRef.value) {
    ElMessage.warning("请先完成电子签名");
    return;
  }
  signing.value = true;
  try {
    await api.post(`/contracts/${route.params.id}/sign`, {
      signerName: signForm.signerName.trim(),
      signatureImage: signatureCanvasRef.value.toDataURL("image/png")
    });
    ElMessage.success("合同签署成功");
    if (data.value.contract?.orderId) {
      router.replace(`/order/pay/${data.value.contract.orderId}`);
      return;
    }
    await load();
  } finally {
    signing.value = false;
  }
}

function goToPayment() {
  if (!data.value.contract?.orderId) return;
  router.push(`/order/pay/${data.value.contract.orderId}`);
}

function formatDateTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}

onMounted(load);
</script>

<style scoped>
.contract-detail-page {
  display: grid;
  gap: 20px;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) 320px;
  gap: 20px;
  padding: 28px 30px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f9fbff 0%, #ffffff 65%, #f2f6fd 100%);
  box-shadow: 0 20px 45px rgba(15, 33, 63, 0.08);
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  color: #77859b;
}

.hero-copy h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.18;
  color: #13233d;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
  color: #607089;
  font-size: 14px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.hero-status {
  padding: 20px;
  border-radius: 20px;
  background: #f5f8fd;
  border: 1px solid #e0e8f4;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #fff7e5;
  color: #a56a00;
  font-size: 13px;
  font-weight: 600;
}

.status-pill.signed {
  background: #edf8ef;
  color: #237a38;
}

.hero-status p {
  margin: 16px 0 0;
  line-height: 1.7;
  color: #5c6c84;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) 320px;
  gap: 20px;
}

.content-card,
.summary-card,
.appendix-card,
.signature-card {
  padding: 24px 28px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 16px 40px rgba(15, 33, 63, 0.06);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head h3 {
  margin: 0;
  font-size: 24px;
  color: #12233d;
}

.section-head span {
  color: #7a889d;
  font-size: 13px;
}

.contract-content {
  padding: 18px 20px;
  border-radius: 18px;
  background: #f7f9fc;
  border: 1px solid #e8eef7;
}

.contract-content p {
  margin: 0 0 14px;
  line-height: 1.9;
  color: #2a3f5d;
  white-space: pre-line;
}

.contract-content p:last-child {
  margin-bottom: 0;
}

.summary-card {
  display: grid;
  gap: 14px;
  align-content: start;
}

.summary-item {
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid #e2eaf5;
}

.summary-item span {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #7a879b;
}

.summary-item strong {
  display: block;
  color: #152847;
  line-height: 1.5;
  word-break: break-word;
}

.appendix-list {
  display: grid;
  gap: 14px;
}

.appendix-item {
  padding: 18px 20px;
  border-radius: 18px;
  background: #f9fbff;
  border: 1px solid #e7edf7;
}

.appendix-item h4 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #162844;
}

.appendix-item p {
  margin: 0;
  color: #576985;
  line-height: 1.8;
  white-space: pre-line;
}

.signer-form {
  display: grid;
  gap: 8px;
  max-width: 360px;
}

.signer-form label {
  font-size: 14px;
  color: #54657e;
}

.signature-hint {
  margin: 16px 0 14px;
  color: #64748b;
  line-height: 1.8;
}

.signature-board {
  padding: 14px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fbfcfe 0%, #f4f7fb 100%);
  border: 1px dashed #c9d4e5;
}

.signature-canvas {
  display: block;
  width: 100%;
  height: 200px;
  border-radius: 14px;
  background: #fff;
  touch-action: none;
  cursor: crosshair;
}

.signature-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

.signed-panel {
  display: grid;
  gap: 20px;
}

.signed-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.signed-meta div {
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid #e2eaf5;
}

.signed-meta span {
  display: block;
  margin-bottom: 8px;
  color: #7a879b;
  font-size: 13px;
}

.signed-meta strong {
  color: #162844;
}

.signature-preview {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: linear-gradient(180deg, #fbfcfe 0%, #f4f7fb 100%);
  border: 1px solid #e1e9f4;
  overflow: hidden;
}

.signature-preview img {
  max-width: 100%;
  max-height: 220px;
  object-fit: contain;
}

.signature-fallback {
  font-family: "KaiTi", "STKaiti", serif;
  font-size: 44px;
  color: #17324f;
}

@media (max-width: 1200px) {
  .hero-card,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-copy h1 {
    font-size: 28px;
  }

  .section-head h3 {
    font-size: 20px;
  }

  .hero-card,
  .content-card,
  .summary-card,
  .appendix-card,
  .signature-card {
    padding: 20px;
  }

  .signed-meta {
    grid-template-columns: 1fr;
  }
}
</style>
