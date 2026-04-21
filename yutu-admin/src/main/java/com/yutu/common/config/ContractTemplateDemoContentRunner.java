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
                "豫途周边游旅游服务合同",
                "",
                "合同编号：{{contractNo}}",
                "订单编号：{{orderNo}}",
                "甲方（游客）：{{partyA}}",
                "乙方（商家）：{{partyB}}",
                "",
                "根据订单确认信息、行程安排和双方约定，甲乙双方就本次旅游服务达成如下协议：",
                "",
                "一、旅游基本信息",
                "1. 路线名称：{{routeName}}",
                "2. 出发日期：{{departDate}}",
                "3. 行程结束：{{endDate}}",
                "4. 出行目的地：{{destination}}",
                "5. 出行人数：{{travelerCount}} 人（{{travelerNames}}）",
                "",
                "二、费用与支付",
                "1. 合同金额：人民币{{amountUpper}}（￥{{amount}}）",
                "2. 费用包含内容以订单展示、线路说明和已选择资源为准。",
                "3. 游客应按照平台订单约定完成支付，商家在确认订单后履行出行服务。",
                "",
                "三、双方权利义务",
                "1. 乙方应按照页面公示内容提供交通、景区、导览、客服等约定服务。",
                "2. 甲方应如实提供出行人身份信息、联系方式及其他必要资料。",
                "3. 如遇景区预约、交通调度、天气变化等情形，双方应及时沟通并按照规则处理。",
                "",
                "四、特别说明",
                "1. 路线简介：{{routeSummary}}",
                "2. 集合时间、集合地点、入园预约、证件要求等，以商家出团通知和订单确认信息为准。",
                "3. 未尽事宜可由双方另行签署补充协议，补充协议与本合同具有同等效力。",
                "",
                "甲方（游客）：{{partyA}}",
                "乙方（商家）：{{partyB}}",
                "商家联系人：{{merchantContact}}    联系电话：{{merchantPhone}}",
                "签署日期：{{currentDate}}"
        );
    }

    private String buildLocalRouteAppendixTemplate() {
        return String.join("\n",
                "线路附件",
                "",
                "适用范围：周边游",
                "适用线路：{{routeName}}",
                "出发日期：{{departDate}}",
                "预计结束：{{endDate}}",
                "目的地：{{destination}}",
                "",
                "线路说明：",
                "{{routeSummary}}",
                "",
                "服务安排：",
                "1. 接待商家：{{merchantName}}",
                "2. 对接联系人：{{merchantContact}}（{{merchantPhone}}）",
                "3. 本次订单关联游客：{{travelerNames}}",
                "4. 具体班次、发车点、入园时间、导览顺序等，以出行前通知为准。",
                "",
                "费用说明：",
                "1. 本线路订单成交金额为 ￥{{amount}}。",
                "2. 费用包含项目以用户下单时已勾选资源和订单详情展示为准。",
                "3. 费用不含个人消费、景区二消及未在订单中明确列示的其他项目。"
        );
    }

    private String buildLocalSupplementTemplate() {
        return String.join("\n",
                "补充协议",
                "",
                "甲方（游客）：{{partyA}}",
                "乙方（商家）：{{partyB}}",
                "适用线路：{{routeName}}",
                "",
                "如本次订单涉及儿童占位、特殊集合点、临时天气调整、票务改签或其他个性化约定，",
                "双方可依据实际服务情况补充确认，并以平台留痕信息作为履约依据。"
        );
    }
}
