UPDATE contract_template
SET template_name='周边游标准旅游合同',
    template_code='LOCAL_STANDARD_TRAVEL',
    template_type='STANDARD',
    apply_scope='周边游',
    version_no='v2.0',
    template_content='豫途周边游旅游服务合同\n\n合同编号：{{contractNo}}\n订单编号：{{orderNo}}\n甲方（游客）：{{partyA}}\n乙方（商家）：{{partyB}}\n\n根据订单确认信息、行程安排和双方约定，甲乙双方就本次旅游服务达成如下协议：\n\n一、旅游基本信息\n1. 路线名称：{{routeName}}\n2. 出发日期：{{departDate}}\n3. 行程结束：{{endDate}}\n4. 出行目的地：{{destination}}\n5. 出行人数：{{travelerCount}} 人（{{travelerNames}}）\n\n二、费用与支付\n1. 合同金额：人民币{{amountUpper}}（￥{{amount}}）\n2. 费用包含内容以订单展示、线路说明和已选择资源为准。\n3. 游客应按照平台订单约定完成支付，商家在确认订单后履行出行服务。\n\n三、双方权利义务\n1. 乙方应按照页面公示内容提供交通、景区、导览、客服等约定服务。\n2. 甲方应如实提供出行人身份信息、联系方式及其他必要资料。\n3. 如遇景区预约、交通调度、天气变化等情形，双方应及时沟通并按照规则处理。\n\n四、特别说明\n1. 路线简介：{{routeSummary}}\n2. 集合时间、集合地点、入园预约、证件要求等，以商家出团通知和订单确认信息为准。\n3. 未尽事宜可由双方另行签署补充协议，补充协议与本合同具有同等效力。\n\n甲方（游客）：{{partyA}}\n乙方（商家）：{{partyB}}\n商家联系人：{{merchantContact}}    联系电话：{{merchantPhone}}\n签署日期：{{currentDate}}',
    remark='系统演示模板：下单后自动拉取订单、游客、商家与路线信息生成正式合同。'
WHERE id=6;

UPDATE contract_template
SET template_name='周边游行程确认单',
    template_code='LOCAL_ROUTE_APPENDIX',
    template_type='ROUTE',
    apply_scope='周边游',
    version_no='v2.0',
    template_content='线路附件\n\n适用范围：周边游\n适用线路：{{routeName}}\n出发日期：{{departDate}}\n预计结束：{{endDate}}\n目的地：{{destination}}\n\n线路说明：\n{{routeSummary}}\n\n服务安排：\n1. 接待商家：{{merchantName}}\n2. 对接联系人：{{merchantContact}}（{{merchantPhone}}）\n3. 本次订单关联游客：{{travelerNames}}\n4. 具体班次、发车点、入园时间、导览顺序等，以出行前通知为准。\n\n费用说明：\n1. 本线路订单成交金额为 ￥{{amount}}。\n2. 费用包含项目以用户下单时已勾选资源和订单详情展示为准。\n3. 费用不含个人消费、景区二消及未在订单中明确列示的其他项目。',
    remark='系统演示模板：作为线路附件与标准合同合并展示。'
WHERE id=3;

UPDATE contract_template
SET template_name='周边游补充协议',
    template_code='LOCAL_SUPPLEMENT_AGREEMENT',
    template_type='SUPPLEMENT',
    apply_scope='周边游',
    version_no='v2.0',
    template_content='补充协议\n\n甲方（游客）：{{partyA}}\n乙方（商家）：{{partyB}}\n适用线路：{{routeName}}\n\n如本次订单涉及儿童占位、特殊集合点、临时天气调整、票务改签或其他个性化约定，\n双方可依据实际服务情况补充确认，并以平台留痕信息作为履约依据。',
    remark='系统演示模板：用于补充特殊约定，不强制绑定。'
WHERE id=7;
