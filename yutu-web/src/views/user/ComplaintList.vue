<template>
  <div class="complaint-page">
    <section class="page-card complaint-create-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">COMPLAINT CENTER</p>
          <h3>发起投诉</h3>
          <p class="section-copy">完成出行后，可以在这里选择对应路线提交投诉。</p>
        </div>
      </div>

      <el-empty
        v-if="!routeOptions.length"
        description="当前没有可投诉的已完成路线，完成出行后再来试试。"
      />

      <el-form v-else label-position="top" class="complaint-form">
        <el-form-item label="投诉路线">
          <el-select
            v-model="form.orderId"
            placeholder="请选择要投诉的路线"
            filterable
            clearable
            class="route-select"
          >
            <el-option
              v-for="item in routeOptions"
              :key="item.orderId"
              :label="routeOptionLabel(item)"
              :value="item.orderId"
            >
              <div class="route-option">
                <span class="route-option-name">{{ item.routeName }}</span>
                <span class="route-option-meta">{{ item.orderNo }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <div v-if="selectedRoute" class="selected-route">
          <div class="selected-route-name">{{ selectedRoute.routeName }}</div>
          <div class="selected-route-meta">
            <span>订单号：{{ selectedRoute.orderNo }}</span>
            <span>完成时间：{{ formatDateTime(selectedRoute.completedTime) }}</span>
          </div>
        </div>

        <el-form-item label="投诉标题">
          <el-input
            v-model="form.title"
            maxlength="128"
            show-word-limit
            placeholder="请输入投诉标题"
          />
        </el-form-item>

        <el-form-item label="投诉内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请详细描述投诉内容"
          />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" :disabled="!routeOptions.length" @click="createComplaint">提交投诉</el-button>
        </div>
      </el-form>
    </section>

    <section class="page-card complaint-list-card">
      <div class="section-head">
        <div>
          <p class="eyebrow">MY RECORDS</p>
          <h3>我的投诉</h3>
        </div>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="complaintNo" label="投诉号" min-width="180" />
        <el-table-column prop="routeName" label="投诉路线" min-width="220" />
        <el-table-column prop="orderNo" label="关联订单" min-width="180" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="status" label="状态" width="140" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text type="primary" @click="$router.push(`/complaint/detail/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue"
import { ElMessage } from "element-plus"
import { api } from "../../api"

const list = ref([])
const routeOptions = ref([])
const form = reactive({
  orderId: null,
  title: "",
  content: ""
})

const selectedRoute = computed(() =>
  routeOptions.value.find((item) => Number(item.orderId) === Number(form.orderId)) || null
)

async function load() {
  const [complaints, options] = await Promise.all([
    api.get("/complaints"),
    api.get("/complaints/route-options")
  ])
  list.value = complaints
  routeOptions.value = options
}

function routeOptionLabel(item) {
  return `${item.routeName || "未命名路线"} · ${item.orderNo || "-"}`
}

async function createComplaint() {
  if (!form.orderId) {
    ElMessage.warning("请先选择要投诉的路线")
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning("请输入投诉标题")
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning("请输入投诉内容")
    return
  }

  await api.post("/complaints", {
    orderId: Number(form.orderId),
    title: form.title.trim(),
    content: form.content.trim()
  })
  ElMessage.success("投诉已提交")
  form.orderId = null
  form.title = ""
  form.content = ""
  await load()
}

function formatDateTime(value) {
  if (!value) return "-"
  return String(value).replace("T", " ").slice(0, 19)
}

onMounted(load)
</script>

<style scoped>
.complaint-page {
  display: grid;
  grid-template-columns: minmax(360px, 420px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.complaint-create-card,
.complaint-list-card {
  padding: 24px 26px;
  border-radius: 24px;
}

.complaint-create-card {
  position: sticky;
  top: 16px;
}

.complaint-list-card {
  min-width: 0;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.14em;
}

.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
}

.section-copy {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.complaint-form {
  display: grid;
  gap: 6px;
}

.route-select {
  width: 100%;
}

.route-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-option-name {
  color: #0f172a;
}

.route-option-meta {
  color: #94a3b8;
  font-size: 12px;
}

.selected-route {
  margin-bottom: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #dbeafe;
}

.selected-route-name {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.selected-route-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-start;
}

@media (max-width: 1100px) {
  .complaint-page {
    grid-template-columns: 1fr;
  }

  .complaint-create-card {
    position: static;
  }
}
</style>
