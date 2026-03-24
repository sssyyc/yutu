<template>
  <div class="user-home-page">
    <section class="page-card banner-card">
      <el-carousel class="home-carousel">
        <el-carousel-item v-for="item in banners" :key="item.id">
          <button
            v-if="item.linkUrl"
            type="button"
            class="banner-trigger"
            @click="openBanner(item)"
          >
            <img :src="item.imageUrl" class="banner-image" />
          </button>
          <div v-else class="banner-static">
            <img :src="item.imageUrl" class="banner-image" />
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="route-grid">
      <div class="page-card route-card">
        <div class="section-title">{{ TEXT.hotRoutes }}</div>
        <el-row :gutter="14">
          <el-col :span="12" v-for="item in hotRoutes.slice(0, 6)" :key="`hot-${item.id}`">
            <div class="route-item">
              <div class="route-cover">
                <img v-if="item.coverImage" :src="item.coverImage" :alt="item.routeName" class="route-cover-image" />
                <div v-else class="route-cover-fallback">
                  <span>{{ routeFallbackText(item.routeName) }}</span>
                </div>
                <div class="route-cover-mask"></div>
                <div class="route-cover-copy">
                  <div class="route-tag">{{ TEXT.hotTag }}</div>
                  <div class="route-cover-title">{{ item.routeName }}</div>
                </div>
              </div>

              <div class="route-body">
                <div class="route-summary">{{ item.summary || TEXT.hotFallbackSummary }}</div>
                <div class="route-footer">
                  <span class="route-price">{{ `${TEXT.yuan}${formatPrice(item.price)}` }}</span>
                  <el-button text type="primary" @click="$router.push(`/route/detail/${item.id}`)">
                    {{ TEXT.viewDetail }}
                  </el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="page-card route-card">
        <div class="section-title">{{ TEXT.recommendRoutes }}</div>
        <el-row :gutter="14">
          <el-col :span="12" v-for="item in recommendRoutes.slice(0, 6)" :key="`rec-${item.id}`">
            <div class="route-item">
              <div class="route-cover">
                <img v-if="item.coverImage" :src="item.coverImage" :alt="item.routeName" class="route-cover-image" />
                <div v-else class="route-cover-fallback">
                  <span>{{ routeFallbackText(item.routeName) }}</span>
                </div>
                <div class="route-cover-mask"></div>
                <div class="route-cover-copy">
                  <div class="route-tag">{{ TEXT.recommendTag }}</div>
                  <div class="route-cover-title">{{ item.routeName }}</div>
                </div>
              </div>

              <div class="route-body">
                <div class="route-summary">{{ item.summary || TEXT.recommendFallbackSummary }}</div>
                <div class="route-footer">
                  <span class="route-price">{{ `${TEXT.yuan}${formatPrice(item.price)}` }}</span>
                  <el-button text type="success" @click="$router.push(`/route/detail/${item.id}`)">
                    {{ TEXT.viewNow }}
                  </el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>

    <section class="info-grid">
      <div class="page-card info-card">
        <div class="info-head">
          <div>
            <div class="section-title">{{ TEXT.latestReviews }}</div>
            <div class="section-subtitle">{{ TEXT.latestReviewsSub }}</div>
          </div>
        </div>

        <div v-if="reviews.length" class="feedback-list">
          <article v-for="item in reviews.slice(0, 5)" :key="item.id" class="feedback-item">
            <div class="feedback-top">
              <div class="feedback-route">{{ resolveReviewRouteName(item.routeId) }}</div>
              <el-tag type="warning" effect="light">{{ `${formatScore(item.score)} ${TEXT.points}` }}</el-tag>
            </div>
            <p class="feedback-content">{{ item.content || TEXT.reviewPlaceholder }}</p>
            <div class="feedback-meta">
              <span>{{ `${TEXT.routeIdLabel} ${item.routeId}` }}</span>
              <span>{{ formatDateTime(item.createTime) }}</span>
            </div>
          </article>
        </div>

        <el-empty v-else :description="TEXT.noReviews" />
      </div>

      <div class="page-card info-card">
        <div class="info-head">
          <div>
            <div class="section-title">{{ TEXT.latestReminders }}</div>
            <div class="section-subtitle">{{ TEXT.latestRemindersSub }}</div>
          </div>
        </div>

        <div v-if="reminders.length" class="reminder-list">
          <article
            v-for="item in reminders.slice(0, 5)"
            :key="`${item.orderId}-${item.departDate}`"
            class="reminder-item"
            :class="`reminder-${item.reminderLevel || 'plan'}`"
          >
            <div class="reminder-top">
              <div>
                <div class="reminder-route">{{ item.routeName }}</div>
                <div class="reminder-date">{{ item.departDate }}</div>
              </div>
              <el-tag :type="reminderTagType(item.reminderLevel)" effect="light">
                {{ item.reminderTitle || reminderCountdownText(item.daysUntil) }}
              </el-tag>
            </div>

            <p class="reminder-text">{{ item.reminderText }}</p>

            <div class="reminder-meta">
              <span>{{ reminderCountdownText(item.daysUntil) }}</span>
              <span>{{ `${TEXT.orderNoLabel}${item.orderNo || '--'}` }}</span>
              <span>{{ `${TEXT.travelersLabel}${item.travelerCount ?? 0}${TEXT.peopleSuffix}` }}</span>
            </div>

            <div class="reminder-badges">
              <el-tag size="small" effect="light" type="success">{{ payStatusLabel(item.payStatus) }}</el-tag>
              <el-tag size="small" effect="light" :type="contractStatusType(item.contractStatus)">
                {{ contractStatusLabel(item.contractStatus) }}
              </el-tag>
            </div>

            <el-button
              text
              type="primary"
              class="reminder-action"
              @click="$router.push(`/order/detail/${item.orderId}`)"
            >
              {{ TEXT.viewOrder }}
            </el-button>
          </article>
        </div>

        <el-empty v-else :description="TEXT.noReminders" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { api } from "../../api"

const TEXT = {
  hotRoutes: "\u70ed\u95e8\u8def\u7ebf",
  recommendRoutes: "\u63a8\u8350\u8def\u7ebf",
  hotTag: "\u70ed\u95e8\u63a8\u8350",
  recommendTag: "\u7cbe\u9009\u8def\u7ebf",
  hotFallbackSummary: "\u8fd9\u6761\u8def\u7ebf\u6b63\u5728\u7b49\u5f85\u5546\u5bb6\u8865\u5145\u66f4\u591a\u4eae\u70b9\u4ecb\u7ecd\u3002",
  recommendFallbackSummary: "\u9002\u5408\u5148\u70b9\u5f00\u8be6\u60c5\u9875\u770b\u770b\u5177\u4f53\u884c\u7a0b\u5b89\u6392\u548c\u51fa\u53d1\u65e5\u671f\u3002",
  viewDetail: "\u67e5\u770b\u8be6\u60c5",
  viewNow: "\u7acb\u5373\u67e5\u770b",
  latestReviews: "\u6700\u65b0\u8bc4\u4ef7",
  latestReviewsSub: "\u6700\u8fd1\u63d0\u4ea4\u7684\u7528\u6237\u53cd\u9988\u4f1a\u663e\u793a\u5728\u8fd9\u91cc",
  latestReminders: "\u6700\u65b0\u63d0\u9192",
  latestRemindersSub: "\u53ea\u63d0\u9192\u4f60\u5df2\u8d2d\u4e70\u4e14\u5c1a\u672a\u8fc7\u671f\u7684\u884c\u7a0b",
  reviewPlaceholder: "\u7528\u6237\u7ed9\u51fa\u4e86\u7b80\u77ed\u8bc4\u4ef7\u3002",
  noReviews: "\u6682\u65e0\u6700\u65b0\u8bc4\u4ef7",
  noReminders: "\u5f53\u524d\u6ca1\u6709\u5df2\u8d2d\u4e70\u7684\u5f85\u51fa\u53d1\u884c\u7a0b",
  routeIdLabel: "\u8def\u7ebf ID",
  orderNoLabel: "\u8ba2\u5355\u53f7 ",
  travelersLabel: "\u51fa\u884c\u4eba ",
  peopleSuffix: " \u4eba",
  viewOrder: "\u67e5\u770b\u8ba2\u5355",
  justUpdated: "\u521a\u521a\u66f4\u65b0",
  waitSchedule: "\u7b49\u5f85\u6392\u671f\u66f4\u65b0",
  departToday: "\u4eca\u5929\u51fa\u53d1",
  departTomorrow: "\u660e\u5929\u51fa\u53d1",
  daysLeftPrefix: "\u8fd8\u6709 ",
  daysLeftSuffix: " \u5929",
  routePrefix: "\u8def\u7ebf #",
  routeFallback: "\u65c5\u6e38",
  yuan: "\uffe5",
  points: "\u5206"
}

const router = useRouter()
const banners = ref([])
const hotRoutes = ref([])
const recommendRoutes = ref([])
const reviews = ref([])
const reminders = ref([])

const routeNameMap = computed(() => {
  const map = {}
  ;[...hotRoutes.value, ...recommendRoutes.value].forEach((item) => {
    if (!item?.id || !item?.routeName || map[item.id]) return
    map[item.id] = item.routeName
  })
  reminders.value.forEach((item) => {
    if (!item?.routeId || !item?.routeName || map[item.routeId]) return
    map[item.routeId] = item.routeName
  })
  return map
})

function routeFallbackText(name) {
  return String(name || TEXT.routeFallback).slice(0, 2)
}

function openBanner(item) {
  const link = String(item?.linkUrl || "").trim()
  if (!link) {
    return
  }
  if (/^https?:\/\//i.test(link)) {
    window.open(link, "_blank", "noopener,noreferrer")
    return
  }
  router.push(link)
}

function formatPrice(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return "--"
  return Number.isInteger(num) ? String(num) : num.toFixed(2)
}

function formatScore(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return "--"
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function resolveReviewRouteName(routeId) {
  return routeNameMap.value[routeId] || `${TEXT.routePrefix}${routeId}`
}

function formatDateTime(value) {
  const text = String(value || "").trim()
  if (!text) return TEXT.justUpdated
  return text.replace("T", " ").slice(0, 16)
}

function reminderTagType(level) {
  return {
    urgent: "danger",
    warning: "warning",
    notice: "success",
    plan: "info"
  }[level] || "info"
}

function reminderCountdownText(daysUntil) {
  const days = Number(daysUntil)
  if (!Number.isFinite(days)) return TEXT.waitSchedule
  if (days <= 0) return TEXT.departToday
  if (days === 1) return TEXT.departTomorrow
  return `${TEXT.daysLeftPrefix}${days}${TEXT.daysLeftSuffix}`
}

function payStatusLabel(status) {
  return {
    PAID: "\u5df2\u652f\u4ed8",
    UNPAID: "\u5f85\u652f\u4ed8",
    REFUNDED: "\u5df2\u9000\u6b3e"
  }[status] || "\u652f\u4ed8\u72b6\u6001\u5f85\u786e\u8ba4"
}

function contractStatusLabel(status) {
  return {
    SIGNED: "\u5df2\u7b7e\u7ea6",
    UNSIGNED: "\u5f85\u7b7e\u7ea6",
    GENERATED: "\u5408\u540c\u5df2\u751f\u6210"
  }[status] || "\u5408\u540c\u72b6\u6001\u5f85\u786e\u8ba4"
}

function contractStatusType(status) {
  return {
    SIGNED: "success",
    UNSIGNED: "warning",
    GENERATED: "info"
  }[status] || "info"
}

onMounted(async () => {
  const [bannerRes, hotRes, recRes, reviewRes, reminderRes] = await Promise.all([
    api.get("/home/banners"),
    api.get("/home/hot-routes"),
    api.get("/home/recommend-routes"),
    api.get("/home/reviews"),
    api.get("/home/reminders")
  ])

  banners.value = bannerRes || []
  hotRoutes.value = hotRes || []
  recommendRoutes.value = recRes || []
  reviews.value = reviewRes || []
  reminders.value = reminderRes || []
})
</script>

<style scoped>
.user-home-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.user-home-page :deep(.page-card) {
  border-radius: 24px;
  border: 1px solid var(--user-card-border, rgba(20, 64, 89, 0.08));
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 250, 0.94));
  box-shadow: 0 14px 34px rgba(20, 43, 60, 0.05);
}

.banner-card {
  padding: 0;
  overflow: hidden;
}

.section-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--user-text, #142235);
  letter-spacing: 0.01em;
}

.section-subtitle {
  margin-top: 8px;
  color: var(--user-muted, #6d7b88);
  font-size: 13px;
  line-height: 1.6;
}

.home-carousel {
  --banner-height: 540px;
  border-radius: inherit;
  overflow: hidden;
  box-shadow: none;
}

.home-carousel :deep(.el-carousel__container) {
  height: var(--banner-height) !important;
}

.home-carousel :deep(.el-carousel__item) {
  height: 100%;
}

.banner-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-trigger,
.banner-static {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  border: none;
  padding: 0;
  background: none;
  text-align: left;
  cursor: pointer;
}

.banner-static {
  cursor: default;
}

.route-grid,
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.route-card,
.info-card {
  padding: 18px;
}

.route-card .section-title,
.info-head {
  margin-bottom: 14px;
}

.route-item {
  margin-bottom: 12px;
  border-radius: 18px;
  background: linear-gradient(180deg, var(--user-primary-surface, #f8fcfb), #ffffff);
  border: 1px solid var(--user-primary-line, #d9ebe6);
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.route-item:hover {
  transform: translateY(-2px);
  border-color: rgba(75, 157, 144, 0.28);
  box-shadow: 0 12px 24px rgba(20, 43, 60, 0.08);
}

.route-cover {
  position: relative;
  height: 164px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(75, 157, 144, 0.18), rgba(231, 179, 90, 0.18));
}

.route-cover-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.route-item:hover .route-cover-image {
  transform: scale(1.04);
}

.route-cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top right, rgba(231, 179, 90, 0.35), transparent 30%),
    linear-gradient(135deg, rgba(75, 157, 144, 0.82), rgba(47, 127, 116, 0.96));
  color: rgba(255, 255, 255, 0.95);
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.route-cover-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(16, 24, 40, 0.08), rgba(16, 24, 40, 0.58));
}

.route-cover-copy {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 14px;
  z-index: 1;
  color: #ffffff;
}

.route-tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  font-size: 12px;
  font-weight: 700;
}

.route-cover-title {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

.route-body {
  padding: 16px;
}

.route-summary {
  min-height: 52px;
  line-height: 1.6;
  color: var(--user-muted, #6d7b88);
}

.route-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-price {
  color: var(--user-accent, #e7b35a);
  font-weight: 800;
  font-size: 24px;
}

.route-footer :deep(.el-button),
.reminder-action {
  padding: 0;
  color: var(--user-primary-strong, #2f7f74);
  font-weight: 700;
}

.route-footer :deep(.el-button:hover),
.reminder-action:hover {
  color: var(--user-primary, #4b9d90);
}

.info-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.feedback-list,
.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-item,
.reminder-item {
  border-radius: 18px;
  padding: 16px 18px;
  border: 1px solid #d9ebe6;
  background: linear-gradient(180deg, #fbfefd, #ffffff);
}

.feedback-top,
.reminder-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.feedback-route,
.reminder-route {
  color: var(--user-text, #142235);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.5;
}

.reminder-date {
  margin-top: 6px;
  color: var(--user-muted, #6d7b88);
  font-size: 13px;
}

.feedback-content,
.reminder-text {
  margin: 10px 0 0;
  color: #425468;
  line-height: 1.75;
}

.feedback-meta,
.reminder-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: #6d7b88;
  font-size: 13px;
}

.reminder-action {
  margin-top: 10px;
}

.reminder-urgent {
  border-color: rgba(239, 68, 68, 0.16);
  background: linear-gradient(180deg, rgba(254, 242, 242, 0.92), #ffffff);
}

.reminder-warning {
  border-color: rgba(245, 158, 11, 0.18);
  background: linear-gradient(180deg, rgba(255, 247, 237, 0.96), #ffffff);
}

.reminder-notice {
  border-color: rgba(34, 197, 94, 0.18);
  background: linear-gradient(180deg, rgba(240, 253, 244, 0.96), #ffffff);
}

.reminder-plan {
  border-color: rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), #ffffff);
}

@media (max-width: 1200px) {
  .home-carousel {
    --banner-height: 440px;
  }

  .route-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .home-carousel {
    --banner-height: 320px;
  }

  .route-cover {
    height: 144px;
  }

  .route-cover-title {
    font-size: 18px;
  }

  .feedback-top,
  .reminder-top {
    flex-direction: column;
  }
}
</style>
