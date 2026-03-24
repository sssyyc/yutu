<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="travelerName" label="姓名" min-width="140" />
      <el-table-column prop="idCard" label="身份证" min-width="220" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑出行人' : '新增出行人'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="姓名" prop="travelerName">
          <el-input v-model.trim="form.travelerName" maxlength="20" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="身份证" prop="idCard">
          <el-input v-model.trim="form.idCard" maxlength="18" placeholder="请输入18位身份证号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" maxlength="11" placeholder="请输入11位手机号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { api } from "../../api";
import { isValidPhone } from "../../utils/phone";

const ID_CARD_18_REGEX = /^\d{17}[\dXx]$/;

const list = ref([]);
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref(null);

const form = reactive({
  id: null,
  travelerName: "",
  idCard: "",
  phone: ""
});

const rules = {
  travelerName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idCard: [
    { required: true, message: "请输入身份证号", trigger: "blur" },
    {
      validator: (_, value, callback) => {
        const text = String(value ?? "").trim();
        if (!ID_CARD_18_REGEX.test(text)) {
          callback(new Error("身份证号必须18位（最后一位可为X）"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    {
      validator: (_, value, callback) => {
        if (!isValidPhone(value)) {
          callback(new Error("手机号必须为11位数字"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ]
};

async function load() {
  list.value = await api.get("/travelers");
}

function resetForm() {
  form.id = null;
  form.travelerName = "";
  form.idCard = "";
  form.phone = "";
}

function openCreate() {
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row) {
  form.id = row.id;
  form.travelerName = row.travelerName;
  form.idCard = row.idCard;
  form.phone = row.phone;
  dialogVisible.value = true;
}

async function save() {
  try {
    await formRef.value?.validate();
    saving.value = true;
    const payload = {
      travelerName: form.travelerName,
      idCard: form.idCard,
      phone: form.phone
    };
    if (form.id) {
      await api.put(`/travelers/${form.id}`, payload);
      ElMessage.success("更新成功");
    } else {
      await api.post("/travelers", payload);
      ElMessage.success("新增成功");
    }
    dialogVisible.value = false;
    resetForm();
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm("确认删除该出行人吗？", "删除确认", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      type: "warning"
    });
    await api.del(`/travelers/${id}`);
    ElMessage.success("删除成功");
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
  margin-bottom: 12px;
  display: flex;
  gap: 10px;
}
</style>
