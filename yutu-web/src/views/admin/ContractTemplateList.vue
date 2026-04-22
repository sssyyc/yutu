<template>
  <div class="admin-module-page">
    <AdminPageHero
      kicker="CONTRACT TEMPLATE"
      title="合同模板管理"
    />

    <section class="page-card contract-overview">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">模板总数</span>
          <strong class="overview-value">{{ overview.total }}</strong>
          <p class="overview-note">平台当前维护的合同模板总量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">启用中模板</span>
          <strong class="overview-value success">{{ overview.active }}</strong>
          <p class="overview-note">当前允许商户调用生成合同的模板数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">最新版本模板</span>
          <strong class="overview-value primary">{{ overview.latest }}</strong>
          <p class="overview-note">各模板编码下处于最新版本的模板数量。</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">累计调用量</span>
          <strong class="overview-value warning">{{ overview.useCount }}</strong>
          <p class="overview-note">模板被订单实际调用生成合同的累计次数。</p>
        </article>
      </div>
    </section>

    <section class="page-card">
      <div class="toolbar toolbar-search-row">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            class="toolbar-search"
            clearable
            placeholder="请输入模板名称或模板编码"
            @clear="load"
            @keyup.enter="load"
          />
          <el-select v-model="typeFilter" class="toolbar-select" clearable placeholder="模板类型">
            <el-option
              v-for="option in ADMIN_TEMPLATE_TYPE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="scopeFilter" class="toolbar-select" clearable placeholder="适用范围">
            <el-option
              v-for="option in applyScopeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="statusFilter" class="toolbar-select status-select" clearable placeholder="状态">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>

        <div class="toolbar-right">
          <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>

      <div class="toolbar-tip">
        当前共 {{ filteredList.length }} 份模板，已启用 {{ overview.active }} 份
      </div>

      <el-table :data="filteredList" border class="resource-table">
        <el-table-column prop="templateName" label="模板名称" min-width="190" show-overflow-tooltip />
        <el-table-column label="模板类型" width="140">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.templateType)" effect="light" round>
              {{ typeText(row.templateType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="templateCode" label="模板编号" min-width="150" />
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column label="适用范围" width="130">
          <template #default="{ row }">
            {{ scopeText(row.applyScope) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" round>
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载量" width="100" />
        <el-table-column prop="useCount" label="使用量" width="100" />
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="preview(row)">预览</el-button>
            <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button text type="success" @click="copyAsNewVersion(row)">复制新版本</el-button>
            <el-popconfirm
              :title="row.status === 1 ? '确定禁用这个合同模板吗？' : '确定启用这个合同模板吗？'"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="toggleStatus(row)"
            >
              <template #reference>
                <el-button
                  text
                  :type="row.status === 1 ? 'warning' : 'success'"
                >
                  {{ row.status === 1 ? "禁用" : "启用" }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑合同模板' : '新增合同模板'"
      width="920px"
      destroy-on-close
      @closed="reset"
    >
      <div class="dialog-intro">
        管理员端维护国家制式合同和线路附件；商家额外补充的附加合同内容不在此处展示。
      </div>

      <el-form :model="form" label-width="100px" class="template-form">
        <div class="form-section">
          <div class="form-section-title">基础信息</div>
          <div class="form-grid">
            <el-form-item label="模板名称">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
            <el-form-item label="模板编号">
              <el-input v-model="form.templateCode" placeholder="请输入模板编号" />
            </el-form-item>
            <el-form-item label="模板类型">
              <el-select v-model="form.templateType" placeholder="请选择模板类型" @change="handleTypeChange">
                <el-option
                  v-for="option in ADMIN_TEMPLATE_TYPE_OPTIONS"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="版本号">
              <el-input v-model="form.versionNo" placeholder="请输入版本号，如 v1.0" />
            </el-form-item>
            <el-form-item label="适用范围">
              <el-select v-model="form.applyScope" placeholder="请选择适用范围">
                <el-option
                  v-for="option in applyScopeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="备注说明">
              <el-input v-model="form.note" placeholder="请输入模板说明或版本更新摘要" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">内容配置</div>

          <template v-if="form.templateType === 'STANDARD'">
            <div class="content-tip">
              国家制式合同属于标准法律文本，正文仅支持版本维护与预览，不建议在后台直接改动主条款。
            </div>
            <div class="form-grid">
              <el-form-item label="标准来源">
                <el-input v-model="form.standardSource" placeholder="如：文化和旅游行业国家制式合同" />
              </el-form-item>
              <el-form-item label="允许补充">
                <el-switch v-model="form.allowSupplement" />
              </el-form-item>
            </div>
            <el-form-item label="正文预览">
              <el-input
                v-model="form.templateContent"
                type="textarea"
                :rows="12"
                readonly
                placeholder="国家制式合同正文预览"
              />
            </el-form-item>
          </template>

          <template v-else-if="form.templateType === 'ROUTE'">
            <div class="content-tip">
              线路附件用于补充具体业务信息，应围绕行程安排、服务标准和费用说明进行结构化配置。
            </div>
            <div class="form-grid route-grid">
              <el-form-item label="行程安排">
                <el-input
                  v-model="form.routeArrangement"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入行程天数、景点安排、出发返程等信息"
                />
              </el-form-item>
              <el-form-item label="服务标准">
                <el-input
                  v-model="form.serviceStandards"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入交通、住宿、餐饮、导游等服务标准"
                />
              </el-form-item>
              <el-form-item label="费用包含">
                <el-input
                  v-model="form.feeIncludes"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入费用包含内容"
                />
              </el-form-item>
              <el-form-item label="费用不含">
                <el-input
                  v-model="form.feeExcludes"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入费用不包含内容"
                />
              </el-form-item>
              <el-form-item label="退改规则" class="grid-span-2">
                <el-input
                  v-model="form.refundRules"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入退改规则、成团说明及补充约定"
                />
              </el-form-item>
            </div>
          </template>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">{{ form.id ? "保存修改" : "确认新增" }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewVisible"
      title="模板预览"
      width="900px"
      destroy-on-close
    >
      <div v-if="previewTemplate" class="preview-panel">
        <div class="preview-meta">
          <div class="preview-meta-item">
            <span>模板名称</span>
            <strong>{{ previewTemplate.templateName }}</strong>
          </div>
          <div class="preview-meta-item">
            <span>模板类型</span>
            <strong>{{ typeText(previewTemplate.templateType) }}</strong>
          </div>
          <div class="preview-meta-item">
            <span>版本号</span>
            <strong>{{ previewTemplate.versionNo }}</strong>
          </div>
          <div class="preview-meta-item">
            <span>适用范围</span>
            <strong>{{ scopeText(previewTemplate.applyScope) }}</strong>
          </div>
        </div>
        <div class="preview-content">
          <template v-for="(line, index) in previewLines" :key="`${index}-${line}`">
            <div class="preview-line">{{ line || "\u00a0" }}</div>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";
import AdminPageHero from "../../components/admin/AdminPageHero.vue";
import { api } from "../../api";

const TEMPLATE_TYPE_OPTIONS = [
  { label: "国家制式合同", value: "STANDARD" },
  { label: "线路附件", value: "ROUTE" },
  { label: "附加合同", value: "SUPPLEMENT" }
];

const ADMIN_TEMPLATE_TYPE_OPTIONS = TEMPLATE_TYPE_OPTIONS.filter((item) => item.value !== "SUPPLEMENT");

const APPLY_SCOPE_OPTIONS = [
  { label: "国内游", value: "DOMESTIC" },
  { label: "出境游", value: "OUTBOUND" },
  { label: "团队游", value: "GROUP" },
  { label: "自由行", value: "FREE" }
];

const STANDARD_TEMPLATE_CONTENT = `2026年团队国内旅游合同模板

合同编号：______________
甲方（旅游者）：______________
乙方（旅行社）：______________

根据《中华人民共和国合同法》《中华人民共和国旅游法》及相关法律法规的规定，甲乙双方本着平等、自愿、公平、诚信的原则，经充分协商，就甲方参加乙方组织的2026年团队国内旅游事宜达成如下合同：

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

function createDefaultForm() {
  return {
    id: null,
    templateName: "",
    templateCode: "",
    versionNo: "v1.0",
    templateContent: STANDARD_TEMPLATE_CONTENT,
    templateType: "STANDARD",
    applyScope: "",
    note: "",
    standardSource: "文化和旅游行业国家制式合同",
    allowSupplement: true,
    routeArrangement: "",
    serviceStandards: "",
    feeIncludes: "",
    feeExcludes: "",
    refundRules: "",
    supplementTheme: "",
    supplementScenario: "",
    supplementClauses: ""
  };
}

const list = ref([]);
const categoryList = ref([]);
const keyword = ref("");
const typeFilter = ref("");
const scopeFilter = ref("");
const statusFilter = ref("");
const dialogVisible = ref(false);
const previewVisible = ref(false);
const previewTemplate = ref(null);
const form = reactive(createDefaultForm());

const previewLines = computed(() => {
  return String(previewTemplate.value?.previewContent || "").split(/\r?\n/);
});

const applyScopeOptions = computed(() => categoryList.value
  .filter((item) => item.status === 1)
  .map((item) => ({
    label: item.categoryName,
    value: item.categoryName
  })));

function parseTemplateMeta(remark) {
  const fallback = {
    templateType: "STANDARD",
    applyScope: "",
    note: "",
    standardSource: "文化和旅游行业国家制式合同",
    allowSupplement: true,
    routeArrangement: "",
    serviceStandards: "",
    feeIncludes: "",
    feeExcludes: "",
    refundRules: "",
    supplementTheme: "",
    supplementScenario: "",
    supplementClauses: ""
  };

  if (!remark) {
    return fallback;
  }

  try {
    const parsed = JSON.parse(remark);
    return { ...fallback, ...parsed };
  } catch {
    return { ...fallback, note: remark };
  }
}

function enrichTemplate(row) {
  return {
    ...row,
    templateType: row.templateType || "STANDARD",
    applyScope: row.applyScope || "",
    meta: parseTemplateMeta(row.remark)
  };
}

const filteredList = computed(() => {
  return list.value
    .map(enrichTemplate)
    .filter((item) => {
      if (item.templateType === "SUPPLEMENT") {
        return false;
      }
      const matchType = !typeFilter.value || item.templateType === typeFilter.value;
      const matchScope = !scopeFilter.value || item.applyScope === scopeFilter.value;
      const matchStatus = statusFilter.value === "" || item.status === statusFilter.value;
      return matchType && matchScope && matchStatus;
    });
});

const overview = computed(() => {
  const items = filteredList.value;
  const active = items.filter((item) => item.status === 1).length;
  const useCount = items.reduce((sum, item) => sum + Number(item.useCount || 0), 0);
  const latestMap = new Map();

  items.forEach((item) => {
    const current = latestMap.get(item.templateCode);
    if (!current || compareVersion(item.versionNo, current.versionNo) > 0) {
      latestMap.set(item.templateCode, item);
    }
  });

  return {
    total: items.length,
    active,
    latest: latestMap.size,
    useCount
  };
});

async function load() {
  const trimmedKeyword = keyword.value.trim();
  const params = trimmedKeyword ? { keyword: trimmedKeyword } : undefined;
  list.value = await api.get("/admin/contract-templates", params);
}

async function loadCategoryOptions() {
  categoryList.value = await api.get("/admin/categories");
  if (!form.applyScope) {
    form.applyScope = getDefaultApplyScope();
  }
}

function reset() {
  Object.assign(form, createDefaultForm());
  form.applyScope = getDefaultApplyScope();
}

function fillFormFromRow(row) {
  const meta = parseTemplateMeta(row.remark);
  Object.assign(form, {
    id: row.id,
    templateName: row.templateName || "",
    templateCode: row.templateCode || "",
    versionNo: row.versionNo || "v1.0",
    templateContent: row.templateContent || STANDARD_TEMPLATE_CONTENT,
    templateType: row.templateType || meta.templateType,
    applyScope: row.applyScope || meta.applyScope,
    note: meta.note,
    standardSource: meta.standardSource,
    allowSupplement: meta.allowSupplement,
    routeArrangement: meta.routeArrangement,
    serviceStandards: meta.serviceStandards,
    feeIncludes: meta.feeIncludes,
    feeExcludes: meta.feeExcludes,
    refundRules: meta.refundRules,
    supplementTheme: meta.supplementTheme,
    supplementScenario: meta.supplementScenario,
    supplementClauses: meta.supplementClauses
  });
}

function openCreateDialog() {
  reset();
  dialogVisible.value = true;
}

function openEditDialog(row) {
  reset();
  fillFormFromRow(row);
  dialogVisible.value = true;
}

function preview(row) {
  const template = enrichTemplate(row);
  previewTemplate.value = {
    ...template,
    previewContent: template.templateContent || STANDARD_TEMPLATE_CONTENT
  };
  previewVisible.value = true;
}

function copyAsNewVersion(row) {
  reset();
  fillFormFromRow(row);
  form.id = null;
  form.versionNo = nextVersion(form.versionNo);
  form.templateName = `${form.templateName}-新版本`;
  dialogVisible.value = true;
}

function handleTypeChange(type) {
  if (type === "STANDARD") {
    form.templateContent = STANDARD_TEMPLATE_CONTENT;
  }
}

function buildTemplateContent() {
  if (form.templateType === "STANDARD") {
    return form.templateContent || STANDARD_TEMPLATE_CONTENT;
  }

  if (form.templateType === "ROUTE") {
    return `线路附件

适用范围：${scopeText(form.applyScope)}
行程安排：
${form.routeArrangement || "-"}

服务标准：
${form.serviceStandards || "-"}

费用包含：${form.feeIncludes || "-"}

费用不含：${form.feeExcludes || "-"}

退改规则：${form.refundRules || "-"}`;
  }

  return `附加合同（特殊约定补充协议）

甲方（游客）：______________
乙方（商家）：______________
适用线路：______________

一、补充主题
${form.supplementTheme || "______________"}

二、适用场景
${form.supplementScenario || "-"}

三、补充条款
${form.supplementClauses || "-"}

四、效力说明
本附加合同仅补充特殊事项、变更安排或个性化约定，不替代国家制式合同正文；与国家制式合同不一致的，以不违反法律法规和国家制式合同强制性条款为前提执行。`;
}

function buildRemark() {
  return JSON.stringify({
    note: form.note,
    standardSource: form.standardSource,
    allowSupplement: form.allowSupplement,
    routeArrangement: form.routeArrangement,
    serviceStandards: form.serviceStandards,
    feeIncludes: form.feeIncludes,
    feeExcludes: form.feeExcludes,
    refundRules: form.refundRules,
    supplementTheme: form.supplementTheme,
    supplementScenario: form.supplementScenario,
    supplementClauses: form.supplementClauses
  });
}

async function save() {
  const payload = {
    templateName: form.templateName,
    templateCode: form.templateCode,
    templateType: form.templateType,
    applyScope: form.applyScope,
    versionNo: form.versionNo,
    templateContent: buildTemplateContent(),
    remark: buildRemark()
  };

  if (form.id) {
    await api.put(`/admin/contract-templates/${form.id}`, payload);
  } else {
    await api.post("/admin/contract-templates", payload);
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

async function toggleStatus(row) {
  if (row.status === 1) {
    await disable(row.id);
  } else {
    await enable(row.id);
  }
}

function resetSearch() {
  keyword.value = "";
  typeFilter.value = "";
  scopeFilter.value = "";
  statusFilter.value = "";
  load();
}

function getDefaultApplyScope() {
  return applyScopeOptions.value[0]?.value || "";
}

function typeText(type) {
  return TEMPLATE_TYPE_OPTIONS.find((item) => item.value === type)?.label || "国家制式合同";
}

function legacyScopeText(scope) {
  return APPLY_SCOPE_OPTIONS.find((item) => item.value === scope)?.label || "国内游";
}

function typeTagType(type) {
  const map = {
    STANDARD: "danger",
    ROUTE: "primary",
    SUPPLEMENT: "warning"
  };
  return map[type] || "info";
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ").slice(0, 19);
}

function compareVersion(a, b) {
  const toParts = (value) =>
    String(value || "")
      .replace(/[^\d.]/g, "")
      .split(".")
      .map((item) => Number(item || 0));

  const aParts = toParts(a);
  const bParts = toParts(b);
  const maxLength = Math.max(aParts.length, bParts.length);

  for (let index = 0; index < maxLength; index += 1) {
    const left = aParts[index] || 0;
    const right = bParts[index] || 0;
    if (left !== right) {
      return left - right;
    }
  }

  return 0;
}

function nextVersion(versionNo) {
  const match = String(versionNo || "v1.0").match(/(\d+)(?:\.(\d+))?/);
  if (!match) {
    return "v1.1";
  }
  const major = Number(match[1] || 1);
  const minor = Number(match[2] || 0) + 1;
  return `v${major}.${minor}`;
}

function scopeText(scope) {
  return applyScopeOptions.value.find((item) => item.value === scope)?.label || scope || "-";
}

onMounted(async () => {
  await loadCategoryOptions();
  await load();
});
</script>

<style scoped>
.contract-overview {
  margin-bottom: 0;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.overview-card {
  padding: 18px 28px 16px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e3edf8;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.overview-label {
  display: inline-block;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 10px;
}

.overview-value {
  display: block;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
  margin-bottom: 12px;
}

.overview-value.warning {
  color: #f59e0b;
}

.overview-value.success {
  color: #16a34a;
}

.overview-value.primary {
  color: #2563eb;
}

.overview-note {
  margin: 0;
  color: #8ea0bc;
  font-size: 11px;
  line-height: 1.55;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-search-row {
  margin-bottom: 12px;
}

.toolbar-left,
.toolbar-right,
.dialog-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-search {
  flex: 0 0 360px;
  width: 360px;
  max-width: 100%;
}

.toolbar-select {
  width: 140px;
}

.status-select {
  width: 110px;
}

.toolbar-tip {
  display: none;
}

.resource-table {
  border-radius: 16px;
  overflow: hidden;
}

.resource-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.resource-table :deep(td:first-child .cell),
.resource-table :deep(th:first-child .cell) {
  white-space: nowrap;
}

.resource-table :deep(td:last-child .cell),
.resource-table :deep(th:last-child .cell) {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}

.dialog-intro {
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fbff;
  color: #64748b;
  line-height: 1.7;
}

.template-form {
  display: grid;
  gap: 20px;
}

.form-section {
  padding: 18px 18px 6px;
  border: 1px solid #e7eef8;
  border-radius: 18px;
  background: #fbfdff;
}

.form-section-title {
  margin-bottom: 18px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.route-grid :deep(.el-form-item) {
  align-self: start;
}

.grid-span-2 {
  grid-column: 1 / -1;
}

.content-tip {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff7ed;
  color: #9a3412;
  line-height: 1.7;
}

.preview-panel {
  display: grid;
  gap: 18px;
}

.preview-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.preview-meta-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e5edf7;
}

.preview-meta-item span {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
}

.preview-meta-item strong {
  color: #0f172a;
  font-size: 15px;
}

.preview-content {
  padding: 18px;
  min-height: 320px;
  border-radius: 18px;
  border: 1px solid #e5edf7;
  background: #ffffff;
  line-height: 1.8;
  color: #334155;
}

.preview-line {
  min-height: 1.8em;
  white-space: pre-wrap;
}

@media (max-width: 1200px) {
  .overview-grid,
  .preview-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .overview-grid,
  .form-grid,
  .preview-meta {
    grid-template-columns: 1fr;
  }

  .toolbar-search,
  .toolbar-select,
  .status-select {
    width: 100%;
    flex-basis: 100%;
  }

  .overview-card,
  .form-section {
    padding: 18px 16px;
  }
}
</style>
