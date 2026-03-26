<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CONTRACT TEMPLATE"
      title="合同模板管理"
    />

    <section class="page-card">
      <div class="toolbar">
        <div class="toolbar-tip">当前共 {{ list.length }} 份模板</div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="templateCode" label="模板编码" min-width="160" />
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column prop="useCount" label="使用量" width="120" />
        <el-table-column prop="downloadCount" label="下载量" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="success" @click="enable(row.id)">启用</el-button>
            <el-button text type="warning" @click="disable(row.id)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑合同模板' : '新增合同模板'"
      width="760px"
      destroy-on-close
      @closed="reset"
    >
      <el-form :model="form" label-width="92px">
        <el-form-item label="模板名称">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.versionNo" placeholder="请输入版本号" />
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.templateContent" type="textarea" :rows="8" placeholder="请输入模板内容" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">{{ form.id ? "保存修改" : "确认新增" }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const list = ref([]);
const dialogVisible = ref(false);
const defaultTemplateContent = `2024年团队国内旅游合同模板

合同编号：______________
甲方（旅游者）：______________
乙方（旅行社）：______________

根据《中华人民共和国合同法》《中华人民共和国旅游法》及相关法律法规的规定，甲乙双方本着平等、自愿、公平、诚信的原则，经充分协商，就甲方参加乙方组织的2024年团队国内旅游事宜达成如下合同：

一、旅游产品及服务内容
（一）旅游线路：______________（具体线路名称）
（二）出发日期：______年______月______日
（三）结束日期：______年______月______日
（四）旅游目的地：______________
（五）交通：负责甲方在旅游期间的交通安排，包括但不限于飞机、火车、汽车等。
（六）住宿：负责甲方在旅游期间的住宿安排，保证住宿设施安全、卫生。
（七）餐饮：负责甲方在旅游期间的餐饮安排，保证食品安全、卫生。
（八）景点门票：负责甲方在旅游期间所需景点门票的购买。
（九）导游服务：为甲方提供专业导游服务。

二、合同价格及支付方式
（一）旅游费用总额：人民币（大写）______________元整（￥______________元）。
（二）费用包含：交通费、住宿费、餐费、景点首道门票费、导游服务费及行程单中明确列明的其他费用。
（三）费用不包含：旅游者个人消费、单房差、自费项目、行程外活动费用及因不可抗力导致增加的合理费用。
（四）支付方式：甲方应于______年______月______日前支付定金人民币￥______________元；余款应于出发前______日一次性付清，或按双方约定的其他方式支付。
（五）乙方收款后应向甲方出具合法有效的收费凭证。

三、双方权利与义务
（一）甲方权利与义务
1. 有权要求乙方按照约定提供旅游服务。
2. 应如实提供参加旅游人员信息和有效身份证件。
3. 应遵守旅游目的地法律法规、社会公德和团队纪律，不得从事违法活动。
4. 应按约支付旅游费用，并配合乙方完成出行所需手续。
（二）乙方权利与义务
1. 有权按合同约定收取旅游费用。
2. 应按照约定安排交通、住宿、餐饮、导游及其他服务，不得擅自降低服务标准。
3. 应提前向甲方说明旅游行程、注意事项、风险提示及另行付费项目。
4. 在旅游过程中应对可能危及甲方人身、财产安全的情况及时采取必要措施。

四、合同变更、转让与解除
（一）经双方协商一致，可以书面变更本合同。
（二）甲方在出发前解除合同的，乙方可按实际已发生且不可退还的费用扣除后退还剩余款项；双方另有约定的，从其约定。
（三）乙方因未达到成团人数、资源无法落实或其他非不可抗力原因不能成行的，应及时通知甲方，并全额退还已收费用；给甲方造成损失的，应依法承担相应责任。
（四）旅游行程开始后，因不可抗力、公共交通延误、自然灾害、政府行为等客观原因导致行程变更的，双方应本着减少损失原则协商处理，增加的合理费用按法律规定承担。

五、违约责任
（一）一方违反本合同约定给对方造成损失的，应承担赔偿责任。
（二）乙方未经甲方同意擅自变更行程、减少服务项目或者降低服务标准的，应退还相应费用，并依法承担违约责任。
（三）甲方因自身原因未能按照约定参加旅游、迟到、脱团或中途退团的，相应损失由甲方承担；乙方未实际发生的费用应依法退还。
（四）因第三方原因造成旅游服务不能履行的，乙方应积极协助甲方维权并提供必要证明材料。

六、安全保障与保险
（一）乙方应依法履行安全保障义务，对旅游活动中可能存在的风险进行提示。
（二）甲方应根据自身身体状况判断是否适合参加本次旅游，如实说明健康信息；因隐瞒病史或不听劝阻导致的后果，由甲方自行承担。
（三）乙方已建议甲方购买旅游意外保险，甲方可自行购买或委托乙方代为办理，保险责任以保险合同约定为准。

七、争议解决
本合同履行过程中发生争议的，双方应先协商解决；协商不成的，可向消费者协会、文化和旅游主管部门申请调解，或依法向有管辖权的人民法院提起诉讼。

八、其他约定
（一）本合同未尽事宜，由双方另行协商签订补充协议；补充协议与本合同具有同等法律效力。
（二）本合同自双方签字或盖章之日起生效。
（三）本合同一式______份，甲乙双方各执______份，具有同等法律效力。

甲方（签字）：______________
联系电话：______________
签署日期：______年______月______日

乙方（盖章）：______________
法定代表人/委托代理人：______________
联系电话：______________
签署日期：______年______月______日`;
const form = reactive({
  id: null,
  templateName: "",
  templateCode: "",
  versionNo: "v1.0",
  templateContent: defaultTemplateContent,
  remark: ""
});

async function load() {
  list.value = await api.get("/admin/contract-templates");
}

function openCreateDialog() {
  reset();
  dialogVisible.value = true;
}

function openEditDialog(row) {
  reset();
  Object.assign(form, row);
  dialogVisible.value = true;
}

function reset() {
  Object.assign(form, {
    id: null,
    templateName: "",
    templateCode: "",
    versionNo: "v1.0",
    templateContent: defaultTemplateContent,
    remark: ""
  });
}

async function save() {
  if (form.id) {
    await api.put(`/admin/contract-templates/${form.id}`, form);
  } else {
    await api.post("/admin/contract-templates", form);
  }
  dialogVisible.value = false;
  reset();
  await load();
}

async function enable(id) {
  await api.post(`/admin/contract-templates/${id}/enable`);
  await load();
}

async function disable(id) {
  await api.post(`/admin/contract-templates/${id}/disable`);
  await load();
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.toolbar-tip {
  color: #64748b;
  font-size: 14px;
}

.toolbar-actions,
.dialog-actions {
  display: flex;
  gap: 12px;
}
</style>
