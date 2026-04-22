package com.yutu.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.modules.model.entity.ContractTemplate;
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Order(10)
public class ContractTemplateDemoContentRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ContractTemplateDemoContentRunner.class);

    private final ContractTemplateMapper contractTemplateMapper;

    public ContractTemplateDemoContentRunner(ContractTemplateMapper contractTemplateMapper) {
        this.contractTemplateMapper = contractTemplateMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            syncDemoTemplates();
        } catch (Exception ex) {
            log.warn("Failed to sync demo contract template content", ex);
        }
    }

    private void syncDemoTemplates() {
        List<ContractTemplate> templates = contractTemplateMapper.selectList(
                new LambdaQueryWrapper<ContractTemplate>().orderByAsc(ContractTemplate::getId)
        );
        if (templates.isEmpty()) {
            return;
        }

        Map<Long, ContractTemplate> byId = templates.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ContractTemplate::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        int updatedCount = 0;
        updatedCount += updateTemplate(byId.get(6), template -> {
            template.setTemplateName("周边游标准旅游合同");
            template.setTemplateCode("LOCAL_STANDARD_TRAVEL");
            template.setTemplateType("STANDARD");
            template.setApplyScope("周边游");
            template.setVersionNo("v2.0");
            template.setTemplateContent(buildLocalStandardTemplate());
            template.setRemark("系统演示模板：下单后自动拉取订单、游客、商家与路线信息生成正式合同。");
        });
        updatedCount += updateTemplate(byId.get(3), template -> {
            template.setTemplateName("周边游行程确认单");
            template.setTemplateCode("LOCAL_ROUTE_APPENDIX");
            template.setTemplateType("ROUTE");
            template.setApplyScope("周边游");
            template.setVersionNo("v2.0");
            template.setTemplateContent(buildLocalRouteAppendixTemplate());
            template.setRemark("系统演示模板：作为线路附件与标准合同合并展示。");
        });
        updatedCount += updateTemplate(byId.get(7), template -> {
            template.setTemplateName("周边游补充协议");
            template.setTemplateCode("LOCAL_SUPPLEMENT_AGREEMENT");
            template.setTemplateType("SUPPLEMENT");
            template.setApplyScope("周边游");
            template.setVersionNo("v2.0");
            template.setTemplateContent(buildLocalSupplementTemplate());
            template.setRemark("系统演示模板：用于补充特殊约定，不强制绑定。");
        });

        if (updatedCount > 0) {
            log.info("Synchronized {} demo contract templates", updatedCount);
        }
    }

    private int updateTemplate(ContractTemplate template, Consumer<ContractTemplate> updater) {
        if (template == null) {
            return 0;
        }
        ContractTemplate snapshot = copyOf(template);
        updater.accept(template);
        template.setStatus(1);
        template.setDeleted(0);
        if (sameTemplate(snapshot, template)) {
            return 0;
        }
        contractTemplateMapper.updateById(template);
        return 1;
    }

    private ContractTemplate copyOf(ContractTemplate template) {
        ContractTemplate copy = new ContractTemplate();
        copy.setId(template.getId());
        copy.setTemplateName(template.getTemplateName());
        copy.setTemplateCode(template.getTemplateCode());
        copy.setTemplateType(template.getTemplateType());
        copy.setApplyScope(template.getApplyScope());
        copy.setVersionNo(template.getVersionNo());
        copy.setTemplateContent(template.getTemplateContent());
        copy.setRemark(template.getRemark());
        copy.setStatus(template.getStatus());
        copy.setDeleted(template.getDeleted());
        return copy;
    }

    private boolean sameTemplate(ContractTemplate left, ContractTemplate right) {
        return Objects.equals(left.getTemplateName(), right.getTemplateName())
                && Objects.equals(left.getTemplateCode(), right.getTemplateCode())
                && Objects.equals(left.getTemplateType(), right.getTemplateType())
                && Objects.equals(left.getApplyScope(), right.getApplyScope())
                && Objects.equals(left.getVersionNo(), right.getVersionNo())
                && Objects.equals(left.getTemplateContent(), right.getTemplateContent())
                && Objects.equals(left.getRemark(), right.getRemark())
                && Objects.equals(left.getStatus(), right.getStatus())
                && Objects.equals(left.getDeleted(), right.getDeleted());
    }

    private String buildLocalStandardTemplate() {
        return String.join("\n",
                "2026年团队国内旅游合同模板",
                "",
                "合同编号：______________",
                "甲方（旅游者）：______________",
                "乙方（旅行社）：______________",
                "",
                "根据《中华人民共和国合同法》《中华人民共和国旅游法》及相关法律法规的规定，甲乙双方本着平等、自愿、公平、诚信的原则，经充分协商，就甲方参加乙方组织的2026年团队国内旅游事宜达成如下合同：",
                "",
                "一、旅游产品及服务内容",
                "（一）旅游线路：______________（具体线路名称）",
                "（二）出发日期：______年______月______日",
                "（三）结束日期：______年______月______日",
                "（四）旅游目的地：______________",
                "（五）交通：负责甲方在旅游期间的交通安排，包括但不限于飞机、火车、汽车等。",
                "（六）住宿：负责甲方在旅游期间的住宿安排，保证住宿设施安全、卫生。",
                "（七）餐饮：负责甲方在旅游期间的餐饮安排，保证食品安全、卫生。",
                "（八）景点门票：负责甲方在旅游期间所需景点门票的购买。",
                "（九）导游服务：为甲方提供专业导游服务。",
                "",
                "二、合同价格及支付方式",
                "（一）旅游费用总额：人民币（大写）______________元整（￥______________元）。",
                "（二）费用包含：交通费、住宿费、餐费、景点首道门票费、导游服务费及行程单中明确列明的其他费用。",
                "（三）费用不包含：旅游者个人消费、单房差、自费项目、行程外活动费用及因不可抗力导致增加的合理费用。",
                "（四）支付方式：甲方应于______年______月______日前支付定金人民币￥______________元；余款应于出发前______日一次性付清，或按双方约定的其他方式支付。",
                "（五）乙方收款后应向甲方出具合法有效的收费凭证。",
                "",
                "三、双方权利与义务",
                "（一）甲方权利与义务",
                "1. 有权要求乙方按照约定提供旅游服务。",
                "2. 应如实提供参加旅游人员信息和有效身份证件。",
                "3. 应遵守旅游目的地法律法规、社会公德和团队纪律，不得从事违法活动。",
                "4. 应按约支付旅游费用，并配合乙方完成出行所需手续。",
                "（二）乙方权利与义务",
                "1. 有权按合同约定收取旅游费用。",
                "2. 应按照约定安排交通、住宿、餐饮、导游及其他服务，不得擅自降低服务标准。",
                "3. 应提前向甲方说明旅游行程、注意事项、风险提示及另行付费项目。",
                "4. 在旅游过程中应对可能危及甲方人身、财产安全的情况及时采取必要措施。",
                "",
                "四、合同变更、转让与解除",
                "（一）经双方协商一致，可以书面变更本合同。",
                "（二）甲方在出发前解除合同的，乙方可按实际已发生且不可退还的费用扣除后退还剩余款项；双方另有约定的，从其约定。",
                "（三）乙方因未达到成团人数、资源无法落实或其他非不可抗力原因不能成行的，应及时通知甲方，并全额退还已收费用；给甲方造成损失的，应依法承担相应责任。",
                "（四）旅游行程开始后，因不可抗力、公共交通延误、自然灾害、政府行为等客观原因导致行程变更的，双方应本着减少损失原则协商处理，增加的合理费用按法律规定承担。",
                "",
                "五、违约责任",
                "（一）一方违反本合同约定给对方造成损失的，应承担赔偿责任。",
                "（二）乙方未经甲方同意擅自变更行程、减少服务项目或者降低服务标准的，应退还相应费用，并依法承担违约责任。",
                "（三）甲方因自身原因未能按照约定参加旅游、迟到、脱团或中途退团的，相应损失由甲方承担；乙方未实际发生的费用应依法退还。",
                "（四）因第三方原因造成旅游服务不能履行的，乙方应积极协助甲方维权并提供必要证明材料。",
                "",
                "六、安全保障与保险",
                "（一）乙方应依法履行安全保障义务，对旅游活动中可能存在的风险进行提示。",
                "（二）甲方应根据自身身体状况判断是否适合参加本次旅游，如实说明健康信息；因隐瞒病史或不听劝阻导致的后果，由甲方自行承担。",
                "（三）乙方已建议甲方购买旅游意外保险，甲方可自行购买或委托乙方代为办理，保险责任以保险合同约定为准。",
                "",
                "七、争议解决",
                "本合同履行过程中发生争议的，双方应先协商解决；协商不成的，可向消费者协会、文化和旅游主管部门申请调解，或依法向有管辖权的人民法院提起诉讼。",
                "",
                "八、其他约定",
                "（一）本合同未尽事宜，由双方另行协商签订补充协议；补充协议与本合同具有同等法律效力。",
                "（二）本合同自双方签字或盖章之日起生效。",
                "（三）本合同一式______份，甲乙双方各执______份，具有同等法律效力。",
                "",
                "甲方（签字）：______________",
                "联系电话：______________",
                "签署日期：______年______月______日",
                "",
                "乙方（盖章）：______________",
                "法定代表人/委托代理人：______________",
                "联系电话：______________",
                "签署日期：______年______月______日"
        );
    }

    private String buildLocalRouteAppendixTemplate() {
        return String.join("\n",
                "线路附件",
                "",
                "适用范围：周边游",
                "行程安排：",
                "D1 集合出发，导游统一组织出行并讲解注意事项。",
                "D2 按行程游览核心景点，统一安排交通、住宿与用餐。",
                "D3 团队返程，完成行程回访与服务确认。",
                "",
                "服务标准：",
                "1. 按合同约定统一安排往返交通及当地接驳。",
                "2. 提供标准酒店住宿与行程所列餐食。",
                "3. 配备持证导游提供随团服务。",
                "4. 统一安排景点首道门票及基础保险服务。",
                "",
                "费用包含：交通、住宿、门票、导游服务。",
                "费用不含：单房差、个人消费、行程外自费项目及未列明费用。",
                "退改规则：出发前7日外可免费申请变更，临近出发按团队资源损失收取相应费用。"
        );
    }

    private String buildLocalSupplementTemplate() {
        return String.join("\n",
                "附加合同（特殊约定补充协议）",
                "",
                "甲方（游客）：______________",
                "乙方（商家）：______________",
                "适用线路：______________",
                "",
                "一、适用场景",
                "如本次订单涉及儿童占位、特殊集合点、临时天气调整、票务改签、资源替换或其他个性化约定，双方可通过本附加合同补充确认。",
                "",
                "二、补充条款",
                "1. 补充事项应以平台订单、商家确认记录、双方在线沟通留痕或书面附件为准。",
                "2. 因资源调整产生的费用增减、履约时间变化或服务替代方案，应由双方确认后执行。",
                "3. 本附加合同未约定的权利义务、违约责任和争议解决方式，继续适用国家制式合同。",
                "",
                "三、效力说明",
                "本附加合同不替代国家制式合同正文；与国家制式合同不一致的，以不违反法律法规和国家制式合同强制性条款为前提执行。"
        );
    }
}
