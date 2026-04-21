<template>
  <div class="page-card route-list-page">
    <div class="filter-bar">
      <div class="filter-copy">
        <p class="eyebrow">ROUTE EXPLORE</p>
        <h2>按形式和标签找到更合适的路线</h2>
      </div>

      <div class="filter-actions">
        <el-input
          v-model="query.keyword"
          placeholder="输入路线名称、景点、目的地"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div class="filter-panel">
      <div class="filter-group">
        <span class="group-label">路线形式</span>
        <div class="chip-list">
          <button
            type="button"
            class="filter-chip"
            :class="{ active: !query.categoryId }"
            @click="setCategory(null)"
          >
            全部
          </button>
          <button
            v-for="item in categories"
            :key="item.id"
            type="button"
            class="filter-chip"
            :class="{ active: query.categoryId === Number(item.id) }"
            @click="setCategory(item.id)"
          >
            {{ item.categoryName }}
          </button>
        </div>
      </div>

      <div class="filter-group">
        <span class="group-label">路线标签</span>
        <div class="chip-list">
          <button
            type="button"
            class="filter-chip subtle"
            :class="{ active: !query.tagId }"
            @click="setTag(null)"
          >
            不限
          </button>
          <button
            v-for="item in tags"
            :key="item.id"
            type="button"
            class="filter-chip subtle"
            :class="{ active: query.tagId === Number(item.id) }"
            @click="setTag(item.id)"
          >
            {{ item.tagName }}
          </button>
        </div>
      </div>

      <div class="filter-summary">
        <span>当前共找到 {{ routes.length }} 条路线</span>
        <el-button text type="primary" @click="resetFilters">清空筛选</el-button>
      </div>
    </div>

    <el-table v-if="routes.length" :data="routes" border class="route-table" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="路线名称" min-width="240">
        <template #default="{ row }">
          <el-button type="primary" text class="route-name-button" @click="showDetail(row)">
            {{ row.routeName }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="路线形式" width="140">
        <template #default="{ row }">
          {{ resolveCategoryName(row.categoryId) }}
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="score" label="评分" width="120" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="primary" text @click="$router.push(`/route/detail/${row.id}`)">查看详情</el-button>
          <el-button type="warning" text @click="favorite(row.id)">收藏</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-else class="empty-wrap" v-loading="loading">
      <el-empty description="当前筛选条件下暂无路线，换个标签或关键字试试看" />
    </div>

    <el-dialog v-model="detailVisible" width="960px" class="route-preview-dialog" destroy-on-close>
      <template #header>
        <div class="detail-header">
          <div>
            <div class="detail-kicker">ROUTE PREVIEW</div>
            <h3 class="detail-title">{{ currentRoute.routeName || "路线详情" }}</h3>
          </div>
          <div class="detail-header-tags">
            <el-tag effect="light">{{ resolveCategoryName(currentRoute.categoryId) }}</el-tag>
            <el-tag v-if="displayRouteScore !== '--'" type="warning" effect="light">{{ displayRouteScore }} 分</el-tag>
          </div>
        </div>
      </template>

      <div v-loading="detailLoading" class="detail-dialog-body">
        <div class="detail-hero">
          <div class="detail-copy">
            <p class="detail-summary">{{ currentRoute.summary || "暂无路线简介" }}</p>

            <div class="metric-grid">
              <article class="metric-card">
                <div class="metric-label">参考价格</div>
                <div class="metric-value">￥{{ formatPrice(currentRoute.price) }}</div>
              </article>
              <article class="metric-card">
                <div class="metric-label">最近出发</div>
                <div class="metric-value">{{ nextDeparture?.departDate || "待更新" }}</div>
              </article>
              <article class="metric-card">
                <div class="metric-label">可选批次</div>
                <div class="metric-value">{{ currentRouteDates.length || 0 }}</div>
              </article>
              <article class="metric-card">
                <div class="metric-label">评分</div>
                <div class="metric-value">{{ displayRouteScore === "--" ? "暂无" : `${displayRouteScore} 分` }}</div>
              </article>
            </div>
          </div>

          <div class="detail-cover-card">
            <el-image
              v-if="previewMainImage"
              :src="previewMainImage"
              fit="cover"
              class="detail-cover-image"
              :preview-src-list="previewImages"
              preview-teleported
            />
            <div v-else class="detail-cover-empty">暂无路线图片</div>
          </div>
        </div>

        <section class="detail-section">
          <div class="section-heading">详细介绍</div>
          <div class="section-text">{{ currentRouteDetailText || "暂无详细介绍" }}</div>
        </section>

        <section class="detail-section">
          <div class="section-heading">可选出发日期</div>

          <el-table v-if="currentRouteDates.length" :data="currentRouteDates" border max-height="280">
            <el-table-column prop="departDate" label="出发日期" min-width="160" />
            <el-table-column label="价格" width="140">
              <template #default="{ row }">
                ￥{{ formatPrice(row.salePrice) }}
              </template>
            </el-table-column>
            <el-table-column label="余量" width="120">
              <template #default="{ row }">
                <el-tag :type="Number(row.remainCount) > 0 ? 'success' : 'danger'" effect="light">
                  {{ Number(row.remainCount) > 0 ? `${row.remainCount} 位` : "已售罄" }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else description="暂无可预订出发日期" />
        </section>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!currentRoute.id" @click="router.push(`/route/detail/${currentRoute.id}`)">
          查看完整详情
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { api } from "../../api"
import { filterUpcomingDepartures } from "../../utils/departureDate"
import { parseRouteDetailContent } from "../../utils/routeDetailMeta"

const route = useRoute()
const router = useRouter()
const query = reactive({ keyword: "", categoryId: null, tagId: null })
const routes = ref([])
const categories = ref([])
const tags = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentRoute = ref({})
const currentRouteDates = ref([])

const categoryNameMap = computed(() => {
  return categories.value.reduce((acc, item) => {
    acc[Number(item.id)] = item.categoryName
    return acc
  }, {})
})

const currentRouteParsedDetail = computed(() => parseRouteDetailContent(currentRoute.value.detailContent))
const currentRouteDetailText = computed(() => currentRouteParsedDetail.value.plainText)
const currentRouteRelatedImages = computed(() => currentRouteParsedDetail.value.relatedImages || [])
const previewMainImage = computed(() => currentRoute.value.coverImage || currentRouteRelatedImages.value[0] || "")
const nextDeparture = computed(() => {
  return currentRouteDates.value.find((item) => Number(item.remainCount) > 0) || currentRouteDates.value[0] || null
})
const displayRouteScore = computed(() => {
  const score = Number(currentRoute.value.score)
  if (!Number.isFinite(score)) return "--"
  return Number.isInteger(score) ? String(score) : score.toFixed(1)
})
const previewImages = computed(() => {
  const images = []
  const append = (url) => {
    if (!url || images.includes(url)) return
    images.push(url)
  }
  append(currentRoute.value.coverImage)
  currentRouteRelatedImages.value.forEach(append)
  return images
})

function parsePositiveInt(value) {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

function syncQueryFromRoute() {
  query.keyword = typeof route.query.keyword === "string" ? route.query.keyword : ""
  query.categoryId = parsePositiveInt(route.query.categoryId)
  query.tagId = parsePositiveInt(route.query.tagId)
}

function buildRouteQuery() {
  const nextQuery = {}
  const keyword = query.keyword.trim()
  if (keyword) nextQuery.keyword = keyword
  if (query.categoryId) nextQuery.categoryId = String(query.categoryId)
  if (query.tagId) nextQuery.tagId = String(query.tagId)
  return nextQuery
}

async function loadMeta() {
  const [categoryResp, tagResp] = await Promise.all([
    api.get("/routes/categories"),
    api.get("/routes/tags")
  ])
  categories.value = Array.isArray(categoryResp) ? categoryResp : []
  tags.value = Array.isArray(tagResp) ? tagResp : []
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.tagId) params.tagId = query.tagId
    routes.value = await api.get("/routes", params)
  } finally {
    loading.value = false
  }
}

async function pushCurrentQuery() {
  await router.push({
    path: "/route/list",
    query: buildRouteQuery()
  })
}

async function handleSearch() {
  await pushCurrentQuery()
}

async function setCategory(categoryId) {
  query.categoryId = parsePositiveInt(categoryId)
  await pushCurrentQuery()
}

async function setTag(tagId) {
  query.tagId = parsePositiveInt(tagId)
  await pushCurrentQuery()
}

async function resetFilters() {
  query.keyword = ""
  query.categoryId = null
  query.tagId = null
  await pushCurrentQuery()
}

function resolveCategoryName(categoryId) {
  return categoryNameMap.value[Number(categoryId)] || "未分类"
}

function formatPrice(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return "--"
  return Number.isInteger(num) ? String(num) : num.toFixed(2)
}

async function favorite(routeId) {
  await api.post("/favorites", { targetId: routeId, targetType: "ROUTE" })
  ElMessage.success("收藏成功")
}

async function showDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  currentRoute.value = { ...row }
  currentRouteDates.value = []

  try {
    const [routeResp, dateResp] = await Promise.all([
      api.get(`/routes/${row.id}`),
      api.get(`/routes/${row.id}/dates`, null, { silent: true }).catch(() => [])
    ])
    currentRoute.value = {
      ...row,
      ...(routeResp?.route || routeResp || {})
    }
    currentRouteDates.value = filterUpcomingDepartures(dateResp)
  } finally {
    detailLoading.value = false
  }
}

watch(
  () => [route.query.keyword, route.query.categoryId, route.query.tagId],
  async () => {
    syncQueryFromRoute()
    await load()
  }
)

onMounted(async () => {
  syncQueryFromRoute()
  await loadMeta()
  await load()
})
</script>

<style scoped>
.route-list-page {
  padding: 22px;
  border-radius: 24px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, #f6fbfa 0%, #f0fdf7 100%);
  border: 1px solid #d8efe7;
}

.eyebrow {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.16em;
}

.filter-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.keyword-input {
  width: 320px;
}

.filter-panel {
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: 22px;
  background: #f8fbfa;
  border: 1px solid #e2eeea;
}

.filter-group + .filter-group {
  margin-top: 14px;
}

.group-label {
  display: inline-block;
  width: 72px;
  margin-right: 10px;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.chip-list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 10px;
  vertical-align: top;
}

.filter-chip {
  border: 1px solid #d4e8e1;
  background: #ffffff;
  color: #315b50;
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-chip.subtle {
  background: #f4faf7;
}

.filter-chip:hover {
  border-color: #86c9b2;
  color: #0f766e;
}

.filter-chip.active {
  background: linear-gradient(135deg, #0f766e 0%, #2ea37f 100%);
  border-color: transparent;
  color: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.18);
}

.filter-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #d9e7e2;
  color: #64748b;
  font-size: 13px;
}

.route-table :deep(.el-table__header th) {
  background: #f8fafc;
}

.route-name-button {
  padding: 0;
  font-weight: 600;
}

.route-name-button:hover {
  transform: translateY(-1px);
}

.empty-wrap {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dbe7e2;
  border-radius: 20px;
  background: #fbfefd;
}

:deep(.route-preview-dialog .el-dialog) {
  border-radius: 22px;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  color: #475569;
  font-size: 12px;
  letter-spacing: 0.16em;
  font-weight: 700;
}

.detail-title {
  margin: 12px 0 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.2;
}

.detail-header-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.detail-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 240px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.9fr);
  gap: 18px;
}

.detail-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-summary {
  margin: 0;
  color: #334155;
  font-size: 16px;
  line-height: 1.7;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.detail-section {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.metric-label,
.section-heading {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.metric-value {
  margin-top: 8px;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}

.detail-cover-card {
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
}

.detail-cover-image {
  width: 100%;
  height: 100%;
  min-height: 260px;
  display: block;
}

.detail-cover-empty {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 15px;
}

.section-text {
  margin-top: 10px;
  color: #334155;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .filter-bar {
    flex-direction: column;
  }

  .filter-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .keyword-input {
    width: 100%;
  }

  .group-label {
    display: block;
    width: auto;
    margin: 0 0 10px;
  }

  .filter-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-header,
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .detail-header {
    flex-direction: column;
  }

  .detail-title {
    font-size: 24px;
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
