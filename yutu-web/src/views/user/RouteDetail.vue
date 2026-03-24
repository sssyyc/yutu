<template>
  <div class="route-detail-page">
    <section class="page-card hero-card">
      <div class="hero-left">
        <div class="hero-topline">
          <span class="hero-kicker">ROUTE DETAIL</span>
          <el-tag v-if="routeScore" round type="success" effect="light">{{ routeScore.toFixed(1) }} 分</el-tag>
        </div>

        <h2 class="route-title">{{ routeInfo.routeName || "路线详情" }}</h2>
        <p class="route-sub">{{ routeInfo.summary || "暂无路线简介" }}</p>

        <div class="quick-facts">
          <div class="fact-item">
            <span class="fact-label">最近可约</span>
            <strong class="fact-value">{{ nextDeparture?.departDate || "待更新" }}</strong>
          </div>
          <div class="fact-item">
            <span class="fact-label">出发价</span>
            <strong class="fact-value">¥ {{ displayNextPrice }}</strong>
          </div>
          <div v-if="nextDeparture" class="fact-item">
            <span class="fact-label">余位</span>
            <strong class="fact-value">{{ nextDeparture.remainCount }}</strong>
          </div>
        </div>

        <div class="price-panel">
          <span class="price-label">参考价格</span>
          <div class="price-main">¥ {{ displayPrice }}</div>
        </div>

        <p class="detail-text">{{ detailText || "暂无详细介绍" }}</p>

        <div class="hero-actions">
          <el-button class="book-btn" type="primary" @click="scrollToDates">立即预订</el-button>
        </div>
      </div>

      <div class="hero-right">
        <div class="cover-wrap">
          <el-image
            :src="mainImage"
            fit="cover"
            class="cover-image"
            :preview-src-list="previewImages"
            preview-teleported
          />
          <div class="cover-mask">
            <span>{{ previewImages.length }} 张可预览</span>
          </div>
        </div>
      </div>
    </section>

    <section class="page-card scenic-card">
      <div class="section-head">
        <h3 class="section-title">相关景点图片</h3>
        <span class="section-sub">{{ scenicImages.length }} 张</span>
      </div>

      <div v-if="scenicImages.length" class="scenic-showcase">
        <el-carousel
          class="scenic-carousel"
          :autoplay="scenicImages.length > 1"
          trigger="click"
          indicator-position="outside"
          height="360px"
        >
          <el-carousel-item v-for="(img, idx) in scenicImages" :key="`${img}-${idx}`">
            <div class="carousel-item-wrap">
              <el-image :src="img" fit="cover" class="carousel-image" :preview-src-list="scenicImages" preview-teleported />
              <span class="carousel-index">{{ idx + 1 }} / {{ scenicImages.length }}</span>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <div v-else class="empty-block">
        <el-empty description="暂未上传相关景点图片" />
      </div>
    </section>

    <section ref="datesSectionRef" class="page-card date-card">
      <div class="section-head">
        <h3 class="section-title">可选出发日期</h3>
      </div>

      <el-table v-if="dates.length" :data="dates" border stripe class="date-table">
        <el-table-column prop="departDate" label="出发日期" min-width="180" />
        <el-table-column label="价格" min-width="140">
          <template #default="{ row }">
            <span class="price-cell">¥ {{ formatPrice(row.salePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余量" min-width="120">
          <template #default="{ row }">
            <el-tag :type="Number(row.remainCount) > 0 ? 'success' : 'danger'" effect="light">
              {{ Number(row.remainCount) > 0 ? `${row.remainCount} 位` : "已售罄" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" :disabled="Number(row.remainCount) <= 0" @click="openOrder(row)">下单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="当前只显示今天及之后的可用出发日期" />
    </section>

    <el-dialog v-model="orderDialog" title="创建订单" width="860px" class="order-dialog">
      <div class="order-dialog-body">
        <div class="order-info-row">
          <span class="info-label">出发日期</span>
          <span class="info-value">{{ selectedDate?.departDate || "-" }}</span>
        </div>

        <div class="traveler-select-wrap">
          <div class="traveler-toolbar">
            <span>请选择本次出行人（已选 {{ selectedTravelers.length }} 人）</span>
            <el-button text type="primary" @click="toTravelerPage">去管理出行人</el-button>
          </div>

          <el-table
            ref="travelerTableRef"
            :data="travelers"
            border
            class="traveler-select-table"
            max-height="320"
            @selection-change="handleTravelerSelectionChange"
          >
            <el-table-column type="selection" width="56" />
            <el-table-column prop="travelerName" label="姓名" min-width="130" />
            <el-table-column prop="idCard" label="身份证号" min-width="260" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机号" min-width="150" />
          </el-table>

          <el-empty v-if="!loadingTravelers && travelers.length === 0" description="暂无出行人，请先添加" />
        </div>
      </div>

      <template #footer>
        <el-button @click="orderDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingOrder" @click="createOrder">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api } from "../../api";
import { filterUpcomingDepartures } from "../../utils/departureDate";
import { parseRouteDetailContent } from "../../utils/routeDetailMeta";

const ID_CARD_18_REGEX = /^\d{17}[\dXx]$/;

const route = useRoute();
const router = useRouter();

const detail = ref({});
const dates = ref([]);
const travelers = ref([]);
const selectedTravelers = ref([]);
const loadingTravelers = ref(false);
const creatingOrder = ref(false);
const orderDialog = ref(false);
const selectedDate = ref(null);
const travelerTableRef = ref(null);
const datesSectionRef = ref(null);

const routeInfo = computed(() => detail.value?.route || {});
const parsedDetail = computed(() => parseRouteDetailContent(routeInfo.value.detailContent));
const detailText = computed(() => parsedDetail.value.plainText);
const scenicImages = computed(() => parsedDetail.value.relatedImages || []);
const mainImage = computed(() => routeInfo.value.coverImage || scenicImages.value[0] || "");
const routeScore = computed(() => {
  const score = Number(routeInfo.value.score);
  return Number.isFinite(score) ? score : 0;
});
const nextDeparture = computed(() => {
  return dates.value.find((item) => Number(item.remainCount) > 0) || dates.value[0] || null;
});
const displayPrice = computed(() => formatPrice(routeInfo.value.price));
const displayNextPrice = computed(() => formatPrice(nextDeparture.value?.salePrice ?? routeInfo.value.price));

const previewImages = computed(() => {
  const images = [];
  const append = (url) => {
    if (!url) return;
    if (!images.includes(url)) images.push(url);
  };
  append(mainImage.value);
  scenicImages.value.forEach(append);
  return images;
});

function formatPrice(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return "--";
  return Number.isInteger(num) ? String(num) : num.toFixed(2);
}

async function load() {
  const id = route.params.id;
  detail.value = await api.get(`/routes/${id}`);
  dates.value = filterUpcomingDepartures(await api.get(`/routes/${id}/dates`));
}

async function loadTravelers() {
  loadingTravelers.value = true;
  try {
    travelers.value = await api.get("/travelers");
  } finally {
    loadingTravelers.value = false;
  }
}

function scrollToDates() {
  datesSectionRef.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function openOrder(row) {
  selectedDate.value = row;
  selectedTravelers.value = [];
  await loadTravelers();
  orderDialog.value = true;
  await nextTick();
  travelerTableRef.value?.clearSelection();
}

function handleTravelerSelectionChange(rows) {
  selectedTravelers.value = rows || [];
}

function toTravelerPage() {
  orderDialog.value = false;
  router.push("/user/travelers");
}

async function createOrder() {
  if (!selectedDate.value?.id) {
    ElMessage.warning("请选择出发日期");
    return;
  }
  if (!selectedTravelers.value.length) {
    ElMessage.warning("请勾选至少 1 位出行人");
    return;
  }
  const hasInvalidIdCard = selectedTravelers.value.some((item) => !ID_CARD_18_REGEX.test(String(item.idCard ?? "").trim()));
  if (hasInvalidIdCard) {
    ElMessage.warning("所选出行人存在身份证号不符合 18 位规则，请先到出行人管理页修改");
    return;
  }

  creatingOrder.value = true;
  try {
    const payload = await api.post("/orders", {
      routeId: Number(route.params.id),
      departDateId: selectedDate.value.id,
      travelerCount: selectedTravelers.value.length,
      travelers: selectedTravelers.value.map((item) => ({
        travelerName: item.travelerName,
        idCard: item.idCard,
        phone: item.phone
      }))
    });
    ElMessage.success("下单成功");
    orderDialog.value = false;
    if (payload?.contractId) {
      router.push(`/contract/detail/${payload.contractId}?action=sign`);
      return;
    }
    router.push(`/order/detail/${payload?.orderId}`);
  } finally {
    creatingOrder.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.route-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(420px, 1fr);
  gap: 20px;
  align-items: stretch;
  border: 1px solid #e5eaf3;
}

.hero-left {
  display: flex;
  flex-direction: column;
}

.hero-topline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #425466;
  font-size: 11px;
  letter-spacing: 0.1em;
  font-weight: 700;
}

.route-title {
  margin: 12px 0 0;
  font-size: clamp(28px, 2.2vw, 44px);
  line-height: 1.2;
  color: #0f172a;
}

.route-sub {
  margin: 10px 0 0;
  color: #334155;
  font-size: 18px;
  line-height: 1.55;
}

.quick-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.fact-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 10px;
  border: 1px solid #dbe4ef;
  background: #f8fbff;
}

.fact-label {
  color: #60758a;
  font-size: 13px;
}

.fact-value {
  color: #1f3347;
  font-size: 14px;
  font-weight: 600;
}

.price-panel {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #dbe8ef;
  background: linear-gradient(90deg, #f7fffd 0%, #eef7ff 100%);
}

.price-label {
  display: inline-block;
  color: #60758a;
  font-size: 13px;
}

.price-main {
  margin-top: 4px;
  font-size: clamp(40px, 2.8vw, 52px);
  line-height: 1;
  font-weight: 800;
  color: #0f766e;
}

.detail-text {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e6edf5;
  color: #334155;
  font-size: 16px;
  line-height: 1.7;
}

.hero-actions {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.book-btn {
  min-width: 112px;
  height: 38px;
  border-radius: 10px;
}

.hero-right {
  display: flex;
}

.cover-wrap {
  position: relative;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: #f1f5f9;
}

.cover-image {
  width: 100%;
  height: 100%;
  min-height: 300px;
}

.cover-mask {
  position: absolute;
  right: 14px;
  bottom: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.52);
  backdrop-filter: blur(4px);
}

.scenic-card {
  border: 1px solid #e5eaf3;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: clamp(22px, 1.8vw, 30px);
  line-height: 1.2;
  font-weight: 800;
}

.section-sub {
  color: #64748b;
  font-size: 13px;
}

.scenic-showcase {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.scenic-carousel :deep(.el-carousel__container) {
  background: #eef2f7;
}

.carousel-item-wrap {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-image {
  width: 100%;
  height: 360px;
}

.carousel-index {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.56);
  color: #fff;
  font-size: 12px;
}

.empty-block {
  padding: 14px 0 4px;
}

.date-card {
  border: 1px solid #e5eaf3;
}

.date-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
}

.price-cell {
  color: #0f766e;
  font-weight: 700;
}

.order-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 2px;
}

.order-info-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.info-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}

.info-value {
  font-size: 16px;
  color: #0f172a;
  font-weight: 600;
}

.traveler-select-wrap {
  width: 100%;
}

.traveler-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: #334155;
  font-size: 15px;
  font-weight: 500;
}

.traveler-select-table :deep(.el-table__header th) {
  height: 46px;
}

.traveler-select-table :deep(.el-table__row td) {
  height: 58px;
}

@media (max-width: 1280px) {
  .hero-card {
    grid-template-columns: 1fr;
  }

  .cover-image {
    min-height: 300px;
    height: 320px;
  }
}

@media (max-width: 960px) {
  :deep(.order-dialog .el-dialog) {
    width: min(95vw, 860px) !important;
  }

  .route-title {
    font-size: clamp(26px, 7vw, 36px);
  }

  .route-sub {
    font-size: 16px;
  }

  .detail-text {
    font-size: 15px;
  }
}

@media (max-width: 760px) {
  .section-title {
    font-size: 22px;
  }

  .cover-image,
  .carousel-image {
    height: 230px;
    min-height: 230px;
  }

  .traveler-toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
