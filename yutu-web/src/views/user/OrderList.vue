<template>
  <div class="page-card">
    <el-table :data="orders" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="200" />
      <el-table-column label="订单状态" width="130">
        <template #default="{ row }">
          <el-tag :type="orderStatusTag(row.orderStatus).type">{{ orderStatusTag(row.orderStatus).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="130">
        <template #default="{ row }">
          <el-tag :type="payStatusTag(row.payStatus).type">{{ payStatusTag(row.payStatus).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payAmount" label="金额" width="120" />
      <el-table-column label="评价" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.hasReviewed" type="success" effect="light">已评价</el-tag>
          <span v-else class="muted">未评价</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="420">
        <template #default="{ row }">
          <el-button type="primary" text @click="$router.push(`/order/detail/${row.id}`)">详情</el-button>
          <el-button
            v-if="row.contractStatus !== 'SIGNED'"
            type="warning"
            text
            @click="goContract(row.id, true)"
            :disabled="row.payStatus !== 'UNPAID'"
          >
            签合同
          </el-button>
          <el-button
            type="success"
            text
            @click="pay(row.id)"
            :disabled="row.payStatus !== 'UNPAID' || row.contractStatus !== 'SIGNED'"
          >
            支付
          </el-button>
          <el-button type="warning" text @click="cancel(row.id)" :disabled="row.orderStatus !== 'PENDING_PAY'">取消</el-button>
          <el-button type="danger" text @click="refund(row.id)" :disabled="row.payStatus !== 'PAID' || row.orderStatus === 'COMPLETED'">
            退款
          </el-button>
          <el-button type="info" text @click="complete(row.id)" :disabled="row.orderStatus !== 'PENDING_TRAVEL'">确认完成</el-button>
          <el-button type="primary" text @click="openReviewDialog(row)" :disabled="row.orderStatus !== 'COMPLETED' || row.hasReviewed">
            去评价
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="reviewDialogVisible" title="订单评价" width="520px">
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" :max="5" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请输入您对本次行程的评价"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { api } from "../../api";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

const orders = ref([]);
const router = useRouter();
const reviewDialogVisible = ref(false);
const submittingReview = ref(false);
const reviewForm = reactive({
  orderId: null,
  score: 5,
  content: ""
});

async function load() {
  orders.value = await api.get("/orders");
}

function orderStatusTag(status) {
  const map = {
    PENDING_PAY: { text: "待支付", type: "warning" },
    PENDING_TRAVEL: { text: "待出行", type: "primary" },
    COMPLETED: { text: "已完成", type: "success" },
    CANCELLED: { text: "已取消", type: "info" },
    REFUNDING: { text: "退款中", type: "warning" },
    REFUNDED: { text: "已退款", type: "info" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

function payStatusTag(status) {
  const map = {
    UNPAID: { text: "未支付", type: "warning" },
    PAID: { text: "已支付", type: "success" },
    REFUNDED: { text: "已退款", type: "info" }
  };
  return map[status] || { text: status || "-", type: "info" };
}

async function goContract(orderId, withSignAction = false) {
  const detail = await api.get(`/orders/${orderId}`, null, { silent: true });
  const contract = (detail.contracts || [])[0];
  if (!contract?.id) {
    ElMessage.warning("当前订单还没有可签署合同");
    return;
  }
  router.push(`/contract/detail/${contract.id}${withSignAction ? "?action=sign" : ""}`);
}

function pay(id) {
  router.push(`/order/pay/${id}`);
}

async function cancel(id) {
  await api.post(`/orders/${id}/cancel`);
  ElMessage.success("订单已取消");
  await load();
}

async function refund(id) {
  await api.post(`/orders/${id}/refund`);
  ElMessage.success("退款已提交");
  await load();
}

async function complete(id) {
  await api.post(`/orders/${id}/complete`);
  ElMessage.success("订单已完成，现在可以评价");
  await load();
}

function openReviewDialog(row) {
  reviewForm.orderId = row.id;
  reviewForm.score = 5;
  reviewForm.content = "";
  reviewDialogVisible.value = true;
}

async function submitReview() {
  if (!reviewForm.orderId) return;
  submittingReview.value = true;
  try {
    await api.post(`/orders/${reviewForm.orderId}/review`, {
      score: reviewForm.score,
      content: reviewForm.content
    });
    ElMessage.success("评价提交成功");
    reviewDialogVisible.value = false;
    await load();
  } finally {
    submittingReview.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.muted {
  color: #94a3b8;
  font-size: 13px;
}
</style>
