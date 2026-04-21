USE `yutu_travel`;

START TRANSACTION;

INSERT INTO `complaint_order`
(`id`, `complaint_no`, `order_id`, `contract_id`, `user_id`, `merchant_id`, `complaint_type`, `title`, `content`, `status`, `result_type`, `result_content`, `deleted`, `create_time`, `update_time`)
VALUES
(2, 'CMP202604121015300201', 17, NULL, 8, 1, 'SERVICE', '导游服务与宣传不符', '用户反馈当天讲解时长不足，部分承诺服务未兑现，希望平台核查并督促整改。', 'FINISHED', 'ADMIN_JUDGE', '平台核查后确认服务存在瑕疵，要求商家整改并补偿优惠券。', 0, '2026-04-12 10:15:30', '2026-04-14 18:06:22'),
(3, 'CMP202604151420450301', 18, NULL, 8, 1, 'SCHEDULE', '行程安排临时变更', '用户反馈出发前一天收到集合时间变更通知，影响个人安排，希望平台说明并给出处理方案。', 'ASSIGNED', NULL, NULL, 0, '2026-04-15 14:20:45', '2026-04-15 15:03:18'),
(4, 'CMP202604161135200401', 13, NULL, 1, 2, 'SERVICE', '景点讲解质量一般', '游客认为景点讲解与宣传文案存在差异，申请平台介入核实。', 'FINISHED', 'ADMIN_JUDGE', '平台判定商家服务存在轻微偏差，已向用户致歉并记录经营预警。', 0, '2026-04-16 11:35:20', '2026-04-17 09:48:02')
ON DUPLICATE KEY UPDATE
`order_id` = VALUES(`order_id`),
`contract_id` = VALUES(`contract_id`),
`user_id` = VALUES(`user_id`),
`merchant_id` = VALUES(`merchant_id`),
`complaint_type` = VALUES(`complaint_type`),
`title` = VALUES(`title`),
`content` = VALUES(`content`),
`status` = VALUES(`status`),
`result_type` = VALUES(`result_type`),
`result_content` = VALUES(`result_content`),
`deleted` = VALUES(`deleted`),
`create_time` = VALUES(`create_time`),
`update_time` = VALUES(`update_time`);

INSERT INTO `complaint_flow`
(`id`, `complaint_id`, `operator_id`, `operator_role`, `action_type`, `action_content`, `create_time`)
VALUES
(7, 2, 8, 'USER', 'CREATE', '导游服务与宣传不符，希望平台协助处理。', '2026-04-12 10:15:30'),
(8, 2, 3, 'ADMIN', 'ACCEPT', '平台已受理投诉并开始核查。', '2026-04-12 10:23:18'),
(9, 2, 3, 'ADMIN', 'ASSIGN', '已转交商家补充说明和整改方案。', '2026-04-12 10:40:52'),
(10, 2, 2, 'MERCHANT', 'REPLY', '商家已致歉，并承诺优化后续讲解服务。', '2026-04-12 12:08:33'),
(11, 2, 3, 'ADMIN', 'FINISH', '平台完成核查，投诉办结。', '2026-04-14 18:06:22'),
(12, 3, 8, 'USER', 'CREATE', '集合时间临时调整影响出行安排。', '2026-04-15 14:20:45'),
(13, 3, 3, 'ADMIN', 'ACCEPT', '平台已接单，准备联系商家核实。', '2026-04-15 14:33:17'),
(14, 3, 3, 'ADMIN', 'ASSIGN', '已分派商家尽快反馈调整原因。', '2026-04-15 15:03:18'),
(15, 4, 1, 'USER', 'CREATE', '景点讲解与宣传文案存在差异。', '2026-04-16 11:35:20'),
(16, 4, 3, 'ADMIN', 'ACCEPT', '平台已受理并调取订单与服务记录。', '2026-04-16 11:46:03'),
(17, 4, 5, 'MERCHANT', 'REPLY', '商家补充说明当天存在讲解人员轮换。', '2026-04-16 16:10:12'),
(18, 4, 3, 'ADMIN', 'FINISH', '平台核实后已完成裁定并归档。', '2026-04-17 09:48:02')
ON DUPLICATE KEY UPDATE
`complaint_id` = VALUES(`complaint_id`),
`operator_id` = VALUES(`operator_id`),
`operator_role` = VALUES(`operator_role`),
`action_type` = VALUES(`action_type`),
`action_content` = VALUES(`action_content`),
`create_time` = VALUES(`create_time`);

INSERT INTO `refund_order`
(`id`, `refund_no`, `order_id`, `order_no`, `user_id`, `merchant_id`, `route_id`, `depart_date_id`, `refund_type`, `refund_reason`, `evidence_urls`, `refund_account_type`, `refund_account_no`, `original_order_status`, `original_pay_status`, `expected_refund_amount`, `proposed_refund_amount`, `final_refund_amount`, `deduct_amount`, `fee_breakdown_json`, `policy_note`, `merchant_note`, `admin_note`, `execution_note`, `status`, `merchant_deadline_time`, `merchant_processed_time`, `admin_deadline_time`, `refund_processed_time`, `completed_time`, `deleted`, `create_time`, `update_time`)
VALUES
(3, 'RFD202604171530220301', 14, 'ORD202603191844211675', 1, 2, 3, 6, 'POST_CONFIRM', '用户反馈档期调整，协商后申请部分退款', '[]', 'ORIGINAL', NULL, 'COMPLETED', 'PAID', 499.00, 399.00, 399.00, 100.00, '{"tourFeeAmount":499.00,"insuranceFeeAmount":0.00,"visaFeeAmount":0.00,"lossFeeAmount":100.00}', '已出行前确认改期失败，按商家规则扣除部分资源损失费。', '商家已与用户协商，同意部分退款。', '平台复核后同意退款 399 元。', '财务已原路退回用户支付账户。', 'REFUND_COMPLETED', '2026-04-18 15:30:22', '2026-04-17 16:10:05', '2026-04-18 12:00:00', '2026-04-18 18:26:44', '2026-04-18 18:26:44', 0, '2026-04-17 15:30:22', '2026-04-18 18:26:44')
ON DUPLICATE KEY UPDATE
`order_id` = VALUES(`order_id`),
`order_no` = VALUES(`order_no`),
`user_id` = VALUES(`user_id`),
`merchant_id` = VALUES(`merchant_id`),
`route_id` = VALUES(`route_id`),
`depart_date_id` = VALUES(`depart_date_id`),
`refund_type` = VALUES(`refund_type`),
`refund_reason` = VALUES(`refund_reason`),
`evidence_urls` = VALUES(`evidence_urls`),
`refund_account_type` = VALUES(`refund_account_type`),
`refund_account_no` = VALUES(`refund_account_no`),
`original_order_status` = VALUES(`original_order_status`),
`original_pay_status` = VALUES(`original_pay_status`),
`expected_refund_amount` = VALUES(`expected_refund_amount`),
`proposed_refund_amount` = VALUES(`proposed_refund_amount`),
`final_refund_amount` = VALUES(`final_refund_amount`),
`deduct_amount` = VALUES(`deduct_amount`),
`fee_breakdown_json` = VALUES(`fee_breakdown_json`),
`policy_note` = VALUES(`policy_note`),
`merchant_note` = VALUES(`merchant_note`),
`admin_note` = VALUES(`admin_note`),
`execution_note` = VALUES(`execution_note`),
`status` = VALUES(`status`),
`merchant_deadline_time` = VALUES(`merchant_deadline_time`),
`merchant_processed_time` = VALUES(`merchant_processed_time`),
`admin_deadline_time` = VALUES(`admin_deadline_time`),
`refund_processed_time` = VALUES(`refund_processed_time`),
`completed_time` = VALUES(`completed_time`),
`deleted` = VALUES(`deleted`),
`create_time` = VALUES(`create_time`),
`update_time` = VALUES(`update_time`);

COMMIT;
