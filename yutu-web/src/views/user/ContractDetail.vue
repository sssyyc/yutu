<template>
  <div class="contract-detail-page">
    <section v-if="data.contract" class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">TRAVEL CONTRACT</p>
        <h1>{{ data.contract.contractTitle }}</h1>
        <div class="meta-row">
          <span>合同号 {{ data.contract.contractNo }}</span>
          <span>{{ signStatusText }}</span>
          <span>已签 {{ signedCount }}/{{ requiredSignCount || 0 }}</span>
          <span v-if="lastSignatureTime">最近签署 {{ formatDateTime(lastSignatureTime) }}</span>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="downloadContract">下载合同</el-button>
          <el-button v-if="canSign" type="success" plain @click="scrollToSignature">前往签署</el-button>
          <el-button v-else-if="canPay" type="warning" plain @click="goToPayment">前往支付</el-button>
        </div>
      </div>
      <div class="hero-status">
        <div class="status-pill" :class="{ signed: allSigned }">
          {{ signStatusText }}
        </div>
        <p>{{ heroStatusMessage }}</p>
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
          <strong>{{ signStatusText }}</strong>
        </div>
        <div class="summary-item">
          <span>签署进度</span>
          <strong>{{ signedCount }}/{{ requiredSignCount || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span>待签署人</span>
          <strong>{{ pendingTravelerNamesDisplay }}</strong>
        </div>
        <div class="summary-item">
          <span>已签署人</span>
          <strong>{{ signedTravelerNamesDisplay }}</strong>
        </div>
        <div class="summary-item">
          <span>订单出行人</span>
          <strong>{{ travelerNamesDisplay }}</strong>
        </div>
        <div class="summary-item">
          <span>最近签署时间</span>
          <strong>{{ lastSignatureTime ? formatDateTime(lastSignatureTime) : "暂无" }}</strong>
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
        <span>{{ signatureSectionCaption }}</span>
      </div>

      <div class="progress-strip">
        <div class="progress-copy">
          <strong>{{ signStatusText }}</strong>
          <p>{{ progressHint }}</p>
        </div>
        <div class="progress-count">{{ signedCount }}/{{ requiredSignCount || 0 }}</div>
      </div>

      <div v-if="travelers.length" class="traveler-status-list">
        <div
          v-for="(traveler, index) in travelers"
          :key="traveler.id || index"
          class="traveler-status-item"
          :class="{ done: isTravelerSigned(traveler.id) }"
        >
          <div class="traveler-status-main">
            <strong>{{ travelerOptionLabel(traveler, index) }}</strong>
            <p>{{ travelerStatusMeta(traveler) }}</p>
          </div>
          <div class="traveler-status-side">
            <el-tag :type="isTravelerSigned(traveler.id) ? 'success' : 'warning'" effect="light">
              {{ isTravelerSigned(traveler.id) ? "已签署" : "待签署" }}
            </el-tag>
            <span v-if="travelerSignatureTime(traveler.id)">
              {{ formatDateTime(travelerSignatureTime(traveler.id)) }}
            </span>
          </div>
        </div>
      </div>
      <el-empty v-else description="当前订单暂无出行人信息，暂时无法签署合同" />

      <template v-if="canSign">
        <div class="signer-form-row">
          <div class="signer-form">
            <label>本次签署人</label>
            <el-select
              v-model="signForm.travelerId"
              placeholder="请选择待签署的出行人"
            >
              <el-option
                v-for="(traveler, index) in pendingTravelers"
                :key="traveler.id"
                :label="travelerOptionLabel(traveler, index)"
                :value="traveler.id"
              />
            </el-select>
          </div>
          <div class="signer-preview">
            <span>签署姓名</span>
            <strong>{{ currentSignerName || "请选择出行人" }}</strong>
          </div>
        </div>

        <p class="signature-hint">{{ signatureHint }}</p>

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
          <el-button
            type="primary"
            :loading="signing"
            :disabled="!currentSignerName"
            @click="submitSignature"
          >
            确认签署
          </el-button>
        </div>
      </template>

      <div v-else-if="allSigned" class="signed-complete">
        <strong>全部出行人已完成电子签名</strong>
        <p>合同确认已经完成，当前可以下载留存，若订单待支付可继续前往支付。</p>
      </div>

      <div v-if="signatures.length" class="record-section">
        <div class="section-subhead">
          <h4>签署记录</h4>
          <span>{{ signatures.length }} 份</span>
        </div>
        <div class="record-grid">
          <article v-for="(item, index) in signatures" :key="item.id || index" class="record-card">
            <div class="record-head">
              <strong>{{ item.signerName || `签署人 ${index + 1}` }}</strong>
              <span>{{ item.signTime ? formatDateTime(item.signTime) : "已签署" }}</span>
            </div>
            <div class="record-preview">
              <img
                v-if="item.signatureImage"
                :src="item.signatureImage"
                :alt="`${item.signerName || '签署人'}电子签名`"
              />
              <div v-else class="record-fallback">{{ item.signerName || "电子签名" }}</div>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api";

const route = useRoute();
const router = useRouter();
const data = ref({});
const signing = ref(false);
const signatureCanvasRef = ref(null);
const signatureSectionRef = ref(null);
const isDrawing = ref(false);
const hasSignatureStroke = ref(false);
let context2d = null;

const signForm = reactive({
  travelerId: null
});

const travelers = computed(() => sanitizeTravelers(data.value.travelers));
const signatures = computed(() => (
  Array.isArray(data.value.signatures) ? data.value.signatures.filter(Boolean) : []
));
const travelerNameCounts = computed(() => {
  const counts = new Map();
  travelers.value.forEach((traveler) => {
    const key = normalizeName(traveler.travelerName);
    if (!key) return;
    counts.set(key, (counts.get(key) || 0) + 1);
  });
  return counts;
});
const signatureByTravelerKey = computed(() => {
  const map = new Map();
  signatures.value.forEach((item) => {
    const key = normalizeId(item?.travelerId);
    if (!key || map.has(key)) return;
    map.set(key, item);
  });
  return map;
});
const signedTravelerKeys = computed(() => {
  const keys = new Set();
  const rawIds = Array.isArray(data.value.signedTravelerIds) ? data.value.signedTravelerIds : [];
  rawIds.forEach((item) => {
    const key = normalizeId(item);
    if (key) {
      keys.add(key);
    }
  });
  if (keys.size) {
    return keys;
  }
  travelers.value.forEach((traveler) => {
    if (findSignatureForTraveler(traveler)) {
      keys.add(normalizeId(traveler.id));
    }
  });
  return keys;
});
const signedTravelerNames = computed(() => {
  const serverNames = Array.isArray(data.value.signedTravelerNames) ? data.value.signedTravelerNames : [];
  const normalized = [...new Set(serverNames.map(normalizeName).filter(Boolean))];
  if (normalized.length) {
    return normalized;
  }
  return travelers.value
    .filter((traveler) => isTravelerSigned(traveler.id))
    .map((traveler) => normalizeName(traveler.travelerName))
    .filter(Boolean);
});
const requiredSignCount = computed(() => resolveCount(data.value.requiredSignCount, travelers.value.length));
const signedCount = computed(() => resolveCount(data.value.signedCount, signedTravelerKeys.value.size));
const pendingTravelers = computed(() => {
  const serverTravelers = sanitizeTravelers(data.value.pendingTravelers);
  if (serverTravelers.length || requiredSignCount.value === 0) {
    return serverTravelers;
  }
  return travelers.value.filter((traveler) => !isTravelerSigned(traveler.id));
});
const pendingSignCount = computed(() => (
  resolveCount(data.value.pendingSignCount, Math.max(requiredSignCount.value - signedCount.value, 0))
));
const allSigned = computed(() => (
  Boolean(data.value.allSigned) || (requiredSignCount.value > 0 && pendingSignCount.value === 0)
));
const canSign = computed(() => requiredSignCount.value > 0 && pendingTravelers.value.length > 0);
const canPay = computed(() => allSigned.value && Boolean(data.value.contract?.orderId));
const currentTraveler = computed(() => (
  pendingTravelers.value.find((item) => normalizeId(item.id) === normalizeId(signForm.travelerId)) || null
));
const currentSignerName = computed(() => normalizeName(currentTraveler.value?.travelerName));
const contractParagraphs = computed(() => {
  const content = data.value.contract?.contractContent || "";
  return content
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
});
const travelerNamesDisplay = computed(() => {
  const names = travelers.value.map((traveler) => normalizeName(traveler.travelerName)).filter(Boolean);
  return names.length ? names.join("、") : "暂无出行人";
});
const signedTravelerNamesDisplay = computed(() => (
  signedTravelerNames.value.length ? signedTravelerNames.value.join("、") : "暂无"
));
const pendingTravelerNamesDisplay = computed(() => {
  const names = pendingTravelers.value
    .map((traveler) => normalizeName(traveler.travelerName))
    .filter(Boolean);
  if (!requiredSignCount.value) {
    return "暂无出行人";
  }
  return names.length ? names.join("、") : "全部完成";
});
const lastSignatureTime = computed(() => {
  const latestSignature = signatures.value[signatures.value.length - 1];
  return latestSignature?.signTime || data.value.contract?.signTime || null;
});
const signStatusText = computed(() => {
  if (!requiredSignCount.value) {
    return "缺少出行人信息";
  }
  if (allSigned.value) {
    return "已签署";
  }
  if (signedCount.value > 0) {
    return "待补签";
  }
  return "待签署";
});
const heroStatusMessage = computed(() => {
  if (!requiredSignCount.value) {
    return "当前订单缺少有效的出行人信息，暂时无法完成电子签署。";
  }
  if (allSigned.value) {
    return canPay.value
      ? "当前合同已完成全部电子签名，可以继续前往支付。"
      : "当前合同已完成全部电子签名，可下载留存。";
  }
  return `订单共有 ${requiredSignCount.value} 位出行人，需全部完成签名后才算合同确认，当前还差 ${pendingSignCount.value} 个签名。`;
});
const signatureSectionCaption = computed(() => {
  if (!requiredSignCount.value) {
    return "缺少出行人信息";
  }
  if (allSigned.value) {
    return "已完成全部电子签名";
  }
  return `还需 ${pendingSignCount.value} 个签名`;
});
const progressHint = computed(() => {
  if (!requiredSignCount.value) {
    return "请先补全订单中的出行人信息，再进行合同签署。";
  }
  if (allSigned.value) {
    return `共 ${requiredSignCount.value} 位出行人，均已完成电子签名。`;
  }
  return `当前已完成 ${signedCount.value} 个签名，剩余 ${pendingSignCount.value} 位出行人待签署。`;
});
const signatureHint = computed(() => {
  if (!currentSignerName.value) {
    return "请选择本次需要签署的出行人，然后在下方签名板中完成手写电子签名。";
  }
  return `订单中的每位出行人都需要各自签名。本次将以“${currentSignerName.value}”的实际姓名提交签名，提交后会自动更新剩余待签名单。`;
});

async function load() {
  data.value = await api.get(`/contracts/${route.params.id}`);
  syncSelectedTraveler();
  await nextTick();
  if (canSign.value) {
    initCanvas();
    if (route.query.action === "sign") {
      scrollToSignature();
    }
    return;
  }
  resetCanvasState();
}

function syncSelectedTraveler() {
  const currentKey = normalizeId(signForm.travelerId);
  if (currentKey && pendingTravelers.value.some((traveler) => normalizeId(traveler.id) === currentKey)) {
    return;
  }
  signForm.travelerId = pendingTravelers.value[0]?.id ?? null;
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
  hasSignatureStroke.value = false;
  isDrawing.value = false;
}

function resetCanvasState() {
  context2d = null;
  hasSignatureStroke.value = false;
  isDrawing.value = false;
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
  if (!canSign.value) {
    resetCanvasState();
    return;
  }
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
  if (!requiredSignCount.value) {
    ElMessage.warning("当前订单缺少出行人信息，暂时无法签署");
    return;
  }
  if (!currentTraveler.value || !currentSignerName.value) {
    ElMessage.warning("请选择待签署的出行人");
    return;
  }
  if (!hasSignatureStroke.value || !signatureCanvasRef.value) {
    ElMessage.warning("请先完成电子签名");
    return;
  }

  signing.value = true;
  try {
    await api.post(`/contracts/${route.params.id}/sign`, {
      travelerId: currentTraveler.value.id,
      signerName: currentSignerName.value,
      signatureImage: signatureCanvasRef.value.toDataURL("image/png")
    });

    await load();

    if (allSigned.value) {
      ElMessage.success("全部出行人已完成合同签署");
      if (data.value.contract?.orderId) {
        router.replace(`/order/pay/${data.value.contract.orderId}`);
      }
      return;
    }

    ElMessage.success(`已完成签署，当前进度 ${signedCount.value}/${requiredSignCount.value}`);
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

function isTravelerSigned(travelerId) {
  return signedTravelerKeys.value.has(normalizeId(travelerId));
}

function travelerSignatureTime(travelerId) {
  const traveler = travelers.value.find((item) => normalizeId(item.id) === normalizeId(travelerId));
  return findSignatureForTraveler(traveler)?.signTime || null;
}

function travelerOptionLabel(traveler, index) {
  const name = normalizeName(traveler?.travelerName) || `出行人 ${index + 1}`;
  const duplicateCount = travelerNameCounts.value.get(normalizeName(traveler?.travelerName)) || 0;
  if (duplicateCount <= 1) {
    return name;
  }
  if (traveler?.idCard) {
    return `${name}（身份证后4位 ${String(traveler.idCard).slice(-4)}）`;
  }
  return `${name}（出行人 ${index + 1}）`;
}

function travelerStatusMeta(traveler) {
  const parts = [];
  if (traveler?.idCard) {
    parts.push(`身份证号：${traveler.idCard}`);
  }
  if (traveler?.phone) {
    parts.push(`手机号：${traveler.phone}`);
  }
  return parts.length ? parts.join(" · ") : "订单中已登记的出行人";
}

function findSignatureForTraveler(traveler) {
  if (!traveler) return null;
  const directMatch = signatureByTravelerKey.value.get(normalizeId(traveler.id));
  if (directMatch) {
    return directMatch;
  }
  const travelerName = normalizeName(traveler.travelerName);
  if (!travelerName) {
    return null;
  }
  if ((travelerNameCounts.value.get(travelerName) || 0) !== 1) {
    return null;
  }
  return signatures.value.find((item) => (
    !normalizeId(item?.travelerId) && normalizeName(item?.signerName) === travelerName
  )) || null;
}

function sanitizeTravelers(list) {
  return (Array.isArray(list) ? list : []).filter((item) => (
    item && normalizeId(item.id) && normalizeName(item.travelerName)
  ));
}

function normalizeId(value) {
  return value === null || value === undefined ? "" : String(value);
}

function normalizeName(value) {
  return typeof value === "string" ? value.trim() : "";
}

function resolveCount(value, fallback) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric >= 0 ? numeric : fallback;
}

watch(() => route.params.id, () => {
  load();
});

watch(() => route.query.action, (action) => {
  if (action === "sign" && canSign.value) {
    nextTick(() => {
      scrollToSignature();
    });
  }
});

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

.section-head,
.section-subhead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-head {
  margin-bottom: 16px;
}

.section-subhead {
  margin-bottom: 14px;
}

.section-head h3,
.section-subhead h4 {
  margin: 0;
  color: #12233d;
}

.section-head h3 {
  font-size: 24px;
}

.section-subhead h4 {
  font-size: 18px;
}

.section-head span,
.section-subhead span {
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
  line-height: 1.6;
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

.progress-strip {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef4fd 100%);
  border: 1px solid #dfe8f6;
}

.progress-copy strong {
  display: block;
  font-size: 18px;
  color: #13233d;
}

.progress-copy p {
  margin: 8px 0 0;
  line-height: 1.7;
  color: #607089;
}

.progress-count {
  min-width: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  border-radius: 18px;
  background: #fff;
  color: #17324f;
  font-size: 24px;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px #dbe5f3;
}

.traveler-status-list {
  display: grid;
  gap: 12px;
  margin-bottom: 20px;
}

.traveler-status-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #fbfcff;
  border: 1px solid #e3eaf6;
}

.traveler-status-item.done {
  background: #f4fbf5;
  border-color: #d5e9d8;
}

.traveler-status-main strong {
  display: block;
  color: #152847;
  line-height: 1.5;
}

.traveler-status-main p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
  word-break: break-word;
}

.traveler-status-side {
  display: grid;
  justify-items: end;
  align-content: center;
  gap: 8px;
  min-width: 120px;
  color: #7a879b;
  font-size: 12px;
}

.signer-form-row {
  display: grid;
  grid-template-columns: minmax(0, 360px) minmax(220px, 1fr);
  gap: 16px;
  align-items: end;
}

.signer-form {
  display: grid;
  gap: 8px;
}

.signer-form label {
  font-size: 14px;
  color: #54657e;
}

.signer-preview {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid #e2eaf5;
}

.signer-preview span {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #7a879b;
}

.signer-preview strong {
  color: #152847;
  font-size: 18px;
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

.signed-complete {
  padding: 18px 20px;
  margin-top: 16px;
  border-radius: 18px;
  background: #f4fbf5;
  border: 1px solid #d6ead9;
}

.signed-complete strong {
  display: block;
  color: #206836;
  font-size: 18px;
}

.signed-complete p {
  margin: 8px 0 0;
  color: #5c6c84;
  line-height: 1.7;
}

.record-section {
  margin-top: 22px;
}

.record-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.record-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 20px;
  background: #f8fbff;
  border: 1px solid #e2eaf5;
}

.record-head {
  display: grid;
  gap: 6px;
}

.record-head strong {
  color: #162844;
  line-height: 1.5;
}

.record-head span {
  color: #7a879b;
  font-size: 13px;
}

.record-preview {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: linear-gradient(180deg, #fbfcfe 0%, #f4f7fb 100%);
  border: 1px solid #e1e9f4;
  overflow: hidden;
}

.record-preview img {
  max-width: 100%;
  max-height: 180px;
  object-fit: contain;
}

.record-fallback {
  font-family: "KaiTi", "STKaiti", serif;
  font-size: 36px;
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

  .progress-strip,
  .traveler-status-item,
  .signer-form-row {
    grid-template-columns: 1fr;
  }

  .progress-strip,
  .traveler-status-item {
    display: grid;
  }

  .traveler-status-side {
    justify-items: start;
    min-width: 0;
  }

  .progress-count {
    width: fit-content;
  }

  .signature-actions {
    justify-content: stretch;
    flex-direction: column;
  }
}
</style>
