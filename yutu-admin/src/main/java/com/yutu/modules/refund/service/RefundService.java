package com.yutu.modules.refund.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.PayRecord;
import com.yutu.modules.model.entity.RefundFlow;
import com.yutu.modules.model.entity.RefundOrder;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.PayRecordMapper;
import com.yutu.modules.model.mapper.RefundFlowMapper;
import com.yutu.modules.model.mapper.RefundOrderMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.refund.dto.RefundAdminArbitrateRequest;
import com.yutu.modules.refund.dto.RefundApplyRequest;
import com.yutu.modules.refund.dto.RefundConfirmRequest;
import com.yutu.modules.refund.dto.RefundExecuteRequest;
import com.yutu.modules.refund.dto.RefundMerchantReviewRequest;
import com.yutu.modules.refund.dto.RefundSupplementRequest;
import com.yutu.modules.refund.vo.RefundEstimateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RefundService {
    public static final String STATUS_WAITING_MERCHANT_REVIEW = "WAITING_MERCHANT_REVIEW";
    public static final String STATUS_WAITING_USER_SUPPLEMENT = "WAITING_USER_SUPPLEMENT";
    public static final String STATUS_WAITING_USER_CONFIRM = "WAITING_USER_CONFIRM";
    public static final String STATUS_WAITING_ADMIN_ARBITRATION = "WAITING_ADMIN_ARBITRATION";
    public static final String STATUS_WAITING_REFUND_EXECUTION = "WAITING_REFUND_EXECUTION";
    public static final String STATUS_REFUND_PROCESSING = "REFUND_PROCESSING";
    public static final String STATUS_REFUND_COMPLETED = "REFUND_COMPLETED";
    public static final String STATUS_REFUND_REJECTED = "REFUND_REJECTED";

    private final RefundOrderMapper refundOrderMapper;
    private final RefundFlowMapper refundFlowMapper;
    private final TourOrderMapper tourOrderMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final PayRecordMapper payRecordMapper;
    private final ObjectMapper objectMapper;

    public RefundService(RefundOrderMapper refundOrderMapper,
                         RefundFlowMapper refundFlowMapper,
                         TourOrderMapper tourOrderMapper,
                         TourRouteMapper tourRouteMapper,
                         TourDepartureDateMapper tourDepartureDateMapper,
                         MerchantShopMapper merchantShopMapper,
                         PayRecordMapper payRecordMapper,
                         ObjectMapper objectMapper) {
        this.refundOrderMapper = refundOrderMapper;
        this.refundFlowMapper = refundFlowMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.payRecordMapper = payRecordMapper;
        this.objectMapper = objectMapper;
    }

    public RefundEstimateVO estimateForUser(Long orderId, String refundType) {
        TourOrder order = getOwnedOrder(orderId);
        TourRoute route = tourRouteMapper.selectById(order.getRouteId());
        TourDepartureDate departureDate = order.getDepartDateId() == null ? null : tourDepartureDateMapper.selectById(order.getDepartDateId());
        RefundEstimateVO estimate = calculateEstimate(order, departureDate, refundType);
        estimate.setOrderId(order.getId());
        estimate.setOrderNo(order.getOrderNo());
        estimate.setRouteName(route == null ? "-" : route.getRouteName());
        estimate.setRefundType(refundType);
        estimate.setPayAmount(order.getPayAmount());
        return estimate;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long orderId, RefundApplyRequest request) {
        TourOrder order = getOwnedOrder(orderId);
        validateRefundableOrder(order);
        ensureNoOpenRefund(order.getId());

        TourDepartureDate departureDate = order.getDepartDateId() == null ? null : tourDepartureDateMapper.selectById(order.getDepartDateId());
        RefundEstimateVO estimate = calculateEstimate(order, departureDate, request.getRefundType());

        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(generateNo("RFD"));
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setMerchantId(order.getMerchantId());
        refund.setRouteId(order.getRouteId());
        refund.setDepartDateId(order.getDepartDateId());
        refund.setRefundType(request.getRefundType());
        refund.setRefundReason(trim(request.getRefundReason()));
        refund.setEvidenceUrls(writeJson(request.getEvidenceUrls()));
        refund.setRefundAccountType(trim(request.getRefundAccountType()));
        refund.setRefundAccountNo(trim(request.getRefundAccountNo()));
        refund.setOriginalOrderStatus(order.getOrderStatus());
        refund.setOriginalPayStatus(order.getPayStatus());
        refund.setExpectedRefundAmount(estimate.getEstimatedRefundAmount());
        refund.setDeductAmount(estimate.getDeductAmount());
        refund.setPolicyNote(estimate.getPolicyNote());
        refund.setStatus(STATUS_WAITING_MERCHANT_REVIEW);
        refund.setMerchantDeadlineTime(LocalDateTime.now().plusHours(48));
        refund.setDeleted(0);
        refundOrderMapper.insert(refund);

        order.setOrderStatus("REFUNDING");
        tourOrderMapper.updateById(order);

        addFlow(refund.getId(), currentUserId(), "USER", "APPLY",
                "用户提交退款申请：" + safeText(refund.getRefundReason()));
        return refund.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void supplement(Long refundId, RefundSupplementRequest request) {
        RefundOrder refund = getOwnedRefund(refundId);
        syncTimeoutStatus(refund);
        if (!STATUS_WAITING_USER_SUPPLEMENT.equals(refund.getStatus())) {
            throw new BizException(400, "当前退款单不在补充材料阶段");
        }

        List<String> mergedUrls = mergeEvidenceUrls(readStringList(refund.getEvidenceUrls()), request.getEvidenceUrls());
        refund.setEvidenceUrls(writeJson(mergedUrls));
        refund.setStatus(STATUS_WAITING_MERCHANT_REVIEW);
        refund.setMerchantDeadlineTime(LocalDateTime.now().plusHours(24));
        refundOrderMapper.updateById(refund);

        addFlow(refund.getId(), currentUserId(), "USER", "SUPPLEMENT",
                buildSupplementContent(request.getContent(), request.getEvidenceUrls()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmProposal(Long refundId, RefundConfirmRequest request) {
        RefundOrder refund = getOwnedRefund(refundId);
        syncTimeoutStatus(refund);
        if (!STATUS_WAITING_USER_CONFIRM.equals(refund.getStatus())) {
            throw new BizException(400, "当前退款单不需要用户确认");
        }

        boolean accepted = Boolean.TRUE.equals(request.getAccepted());
        if (accepted) {
            refund.setStatus(STATUS_WAITING_REFUND_EXECUTION);
            addFlow(refund.getId(), currentUserId(), "USER", "CONFIRM",
                    "用户接受商家退款方案" + withSuffix(request.getNote()));
        } else {
            refund.setStatus(STATUS_WAITING_ADMIN_ARBITRATION);
            refund.setAdminDeadlineTime(LocalDateTime.now().plusHours(24));
            addFlow(refund.getId(), currentUserId(), "USER", "REJECT_PROPOSAL",
                    "用户不同意商家退款方案，申请管理员仲裁" + withSuffix(request.getNote()));
        }
        refundOrderMapper.updateById(refund);
    }

    public RefundOrder latestByOrder(Long orderId) {
        RefundOrder refund = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, orderId)
                .orderByDesc(RefundOrder::getCreateTime)
                .orderByDesc(RefundOrder::getId)
                .last("limit 1"));
        if (refund != null) {
            syncTimeoutStatus(refund);
            decorateRefund(refund);
        }
        return refund;
    }

    public Map<String, Object> detailForUser(Long refundId) {
        RefundOrder refund = getOwnedRefund(refundId);
        syncTimeoutStatus(refund);
        decorateRefund(refund);
        return buildRefundDetail(refund);
    }

    @Transactional(rollbackFor = Exception.class)
    public void merchantReview(Long refundId, RefundMerchantReviewRequest request) {
        RefundOrder refund = getMerchantRefund(refundId);
        syncTimeoutStatus(refund);
        String action = trim(request.getAction()).toUpperCase();
        switch (action) {
            case "APPROVE":
                applyMerchantDecision(refund, request, STATUS_WAITING_REFUND_EXECUTION, "商家同意退款");
                break;
            case "PARTIAL":
                applyMerchantDecision(refund, request, STATUS_WAITING_USER_CONFIRM, "商家提出部分退款方案");
                break;
            case "SUPPLEMENT":
                refund.setStatus(STATUS_WAITING_USER_SUPPLEMENT);
                refund.setMerchantNote(trim(request.getNote()));
                refund.setMerchantProcessedTime(LocalDateTime.now());
                refundOrderMapper.updateById(refund);
                addFlow(refund.getId(), currentUserId(), "MERCHANT", "REQUEST_SUPPLEMENT",
                        "商家要求补充材料" + withSuffix(request.getNote()));
                break;
            case "ESCALATE":
                refund.setStatus(STATUS_WAITING_ADMIN_ARBITRATION);
                refund.setMerchantNote(trim(request.getNote()));
                refund.setMerchantProcessedTime(LocalDateTime.now());
                refund.setAdminDeadlineTime(LocalDateTime.now().plusHours(24));
                refundOrderMapper.updateById(refund);
                addFlow(refund.getId(), currentUserId(), "MERCHANT", "ESCALATE",
                        "商家申请平台仲裁" + withSuffix(request.getNote()));
                break;
            case "REJECT":
                refund.setStatus(STATUS_REFUND_REJECTED);
                refund.setMerchantNote(trim(request.getNote()));
                refund.setMerchantProcessedTime(LocalDateTime.now());
                refund.setCompletedTime(LocalDateTime.now());
                refundOrderMapper.updateById(refund);
                restoreOrderOnRejected(refund);
                addFlow(refund.getId(), currentUserId(), "MERCHANT", "REJECT",
                        "商家驳回退款申请" + withSuffix(request.getNote()));
                break;
            default:
                throw new BizException(400, "不支持的商家处理动作");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void merchantExecute(Long refundId, RefundExecuteRequest request) {
        RefundOrder refund = getMerchantRefund(refundId);
        syncTimeoutStatus(refund);
        if (!STATUS_WAITING_REFUND_EXECUTION.equals(refund.getStatus())
                && !STATUS_REFUND_PROCESSING.equals(refund.getStatus())) {
            throw new BizException(400, "当前退款单还不能执行退款");
        }

        refund.setStatus(STATUS_REFUND_PROCESSING);
        refund.setRefundProcessedTime(LocalDateTime.now());
        refund.setExecutionNote(trim(request.getNote()));
        refundOrderMapper.updateById(refund);
        addFlow(refund.getId(), currentUserId(), "MERCHANT", "EXECUTE",
                "商家已发起退款执行" + withSuffix(request.getNote()));

        refund.setStatus(STATUS_REFUND_COMPLETED);
        refund.setCompletedTime(LocalDateTime.now());
        if (refund.getFinalRefundAmount() == null) {
            refund.setFinalRefundAmount(resolveRefundAmount(refund));
        }
        refundOrderMapper.updateById(refund);
        finishOrderAsRefunded(refund);
        addFlow(refund.getId(), currentUserId(), "MERCHANT", "COMPLETE",
                "退款执行完成" + withSuffix(request.getNote()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminArbitrate(Long refundId, RefundAdminArbitrateRequest request) {
        RefundOrder refund = getAdminRefund(refundId);
        syncTimeoutStatus(refund);
        if (!STATUS_WAITING_ADMIN_ARBITRATION.equals(refund.getStatus())) {
            throw new BizException(400, "当前退款单不在管理员仲裁阶段");
        }

        String action = trim(request.getAction()).toUpperCase();
        switch (action) {
            case "APPROVE":
                refund.setFinalRefundAmount(resolveAmount(request.getFinalRefundAmount(), resolveRefundAmount(refund)));
                refund.setStatus(STATUS_WAITING_REFUND_EXECUTION);
                refund.setAdminNote(trim(request.getNote()));
                refundOrderMapper.updateById(refund);
                addFlow(refund.getId(), currentUserId(), "ADMIN", "ARBITRATE_APPROVE",
                        "管理员裁定退款通过" + withSuffix(request.getNote()));
                break;
            case "PARTIAL":
                refund.setFinalRefundAmount(resolveAmount(request.getFinalRefundAmount(), refund.getProposedRefundAmount()));
                refund.setStatus(STATUS_WAITING_REFUND_EXECUTION);
                refund.setAdminNote(trim(request.getNote()));
                refundOrderMapper.updateById(refund);
                addFlow(refund.getId(), currentUserId(), "ADMIN", "ARBITRATE_PARTIAL",
                        "管理员裁定部分退款" + withSuffix(request.getNote()));
                break;
            case "SUPPLEMENT":
                refund.setStatus(STATUS_WAITING_USER_SUPPLEMENT);
                refund.setAdminNote(trim(request.getNote()));
                refundOrderMapper.updateById(refund);
                addFlow(refund.getId(), currentUserId(), "ADMIN", "REQUEST_SUPPLEMENT",
                        "管理员要求补充材料" + withSuffix(request.getNote()));
                break;
            case "REJECT":
                refund.setStatus(STATUS_REFUND_REJECTED);
                refund.setAdminNote(trim(request.getNote()));
                refund.setCompletedTime(LocalDateTime.now());
                refundOrderMapper.updateById(refund);
                restoreOrderOnRejected(refund);
                addFlow(refund.getId(), currentUserId(), "ADMIN", "ARBITRATE_REJECT",
                        "管理员裁定驳回退款" + withSuffix(request.getNote()));
                break;
            default:
                throw new BizException(400, "不支持的管理员仲裁动作");
        }
    }

    public List<RefundFlow> flows(Long refundId) {
        return refundFlowMapper.selectList(new LambdaQueryWrapper<RefundFlow>()
                .eq(RefundFlow::getRefundId, refundId)
                .orderByAsc(RefundFlow::getCreateTime)
                .orderByAsc(RefundFlow::getId));
    }

    public void decorateRefunds(List<RefundOrder> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return;
        }
        Map<Long, TourRoute> routeMap = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                        .in(TourRoute::getId, refunds.stream().map(RefundOrder::getRouteId).filter(Objects::nonNull).distinct().collect(Collectors.toList())))
                .stream()
                .collect(Collectors.toMap(TourRoute::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, TourDepartureDate> departureDateMap = tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()
                        .in(TourDepartureDate::getId, refunds.stream().map(RefundOrder::getDepartDateId).filter(Objects::nonNull).distinct().collect(Collectors.toList())))
                .stream()
                .collect(Collectors.toMap(TourDepartureDate::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (RefundOrder refund : refunds) {
            syncTimeoutStatus(refund);
            TourRoute route = routeMap.get(refund.getRouteId());
            TourDepartureDate departureDate = departureDateMap.get(refund.getDepartDateId());
            refund.setRouteName(route == null ? "-" : route.getRouteName());
            refund.setDepartDate(departureDate == null ? null : departureDate.getDepartDate());
            refund.setEvidenceUrlList(readStringList(refund.getEvidenceUrls()));
        }
    }

    private void decorateRefund(RefundOrder refund) {
        if (refund == null) {
            return;
        }
        List<RefundOrder> list = new ArrayList<>();
        list.add(refund);
        decorateRefunds(list);
    }

    private Map<String, Object> buildRefundDetail(RefundOrder refund) {
        Map<String, Object> map = new HashMap<>();
        map.put("refund", refund);
        map.put("flows", flows(refund.getId()));
        return map;
    }

    private void applyMerchantDecision(RefundOrder refund,
                                       RefundMerchantReviewRequest request,
                                       String nextStatus,
                                       String actionText) {
        BigDecimal proposedAmount = resolveAmount(request.getProposedRefundAmount(), refund.getExpectedRefundAmount());
        Map<String, Object> feeBreakdown = new LinkedHashMap<>();
        feeBreakdown.put("tourFeeAmount", normalizeMoney(request.getTourFeeAmount()));
        feeBreakdown.put("insuranceFeeAmount", normalizeMoney(request.getInsuranceFeeAmount()));
        feeBreakdown.put("visaFeeAmount", normalizeMoney(request.getVisaFeeAmount()));
        feeBreakdown.put("lossFeeAmount", normalizeMoney(request.getLossFeeAmount()));

        BigDecimal deductAmount = safeMoney(request.getTourFeeAmount())
                .add(safeMoney(request.getInsuranceFeeAmount()))
                .add(safeMoney(request.getVisaFeeAmount()))
                .add(safeMoney(request.getLossFeeAmount()));
        if (deductAmount.compareTo(BigDecimal.ZERO) == 0) {
            deductAmount = safeMoney(refund.getExpectedRefundAmount()).subtract(proposedAmount).max(BigDecimal.ZERO);
        }

        refund.setProposedRefundAmount(proposedAmount);
        refund.setFinalRefundAmount(STATUS_WAITING_REFUND_EXECUTION.equals(nextStatus) ? proposedAmount : refund.getFinalRefundAmount());
        refund.setDeductAmount(deductAmount);
        refund.setFeeBreakdownJson(writeJson(feeBreakdown));
        refund.setMerchantNote(trim(request.getNote()));
        refund.setMerchantProcessedTime(LocalDateTime.now());
        refund.setStatus(nextStatus);
        refundOrderMapper.updateById(refund);

        addFlow(refund.getId(), currentUserId(), "MERCHANT",
                STATUS_WAITING_USER_CONFIRM.equals(nextStatus) ? "PROPOSE_PARTIAL" : "APPROVE",
                actionText + "，应退金额 " + proposedAmount + withSuffix(request.getNote()));
    }

    private void finishOrderAsRefunded(RefundOrder refund) {
        TourOrder order = tourOrderMapper.selectById(refund.getOrderId());
        if (order == null) {
            return;
        }
        order.setOrderStatus("REFUNDED");
        order.setPayStatus("REFUNDED");
        tourOrderMapper.updateById(order);

        PayRecord payRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, order.getId())
                .orderByDesc(PayRecord::getCreateTime)
                .last("limit 1"));
        if (payRecord != null) {
            payRecord.setPayStatus("REFUNDED");
            payRecordMapper.updateById(payRecord);
        }
    }

    private void restoreOrderOnRejected(RefundOrder refund) {
        TourOrder order = tourOrderMapper.selectById(refund.getOrderId());
        if (order == null) {
            return;
        }
        order.setOrderStatus(StringUtils.hasText(refund.getOriginalOrderStatus()) ? refund.getOriginalOrderStatus() : "PENDING_TRAVEL");
        order.setPayStatus(StringUtils.hasText(refund.getOriginalPayStatus()) ? refund.getOriginalPayStatus() : "PAID");
        tourOrderMapper.updateById(order);
    }

    private void validateRefundableOrder(TourOrder order) {
        if (!"PAID".equals(order.getPayStatus())) {
            throw new BizException(400, "当前订单还不能发起退款");
        }
        if ("CANCELLED".equals(order.getOrderStatus()) || "REFUNDED".equals(order.getOrderStatus())) {
            throw new BizException(400, "当前订单状态不支持退款");
        }
    }

    private void ensureNoOpenRefund(Long orderId) {
        List<RefundOrder> refunds = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, orderId)
                .orderByDesc(RefundOrder::getCreateTime));
        for (RefundOrder refund : refunds) {
            if (refund == null) {
                continue;
            }
            syncTimeoutStatus(refund);
            if (!isFinalStatus(refund.getStatus())) {
                throw new BizException(400, "当前订单已有退款申请正在处理中");
            }
        }
    }

    private boolean isFinalStatus(String status) {
        return STATUS_REFUND_COMPLETED.equals(status) || STATUS_REFUND_REJECTED.equals(status);
    }

    private TourOrder getOwnedOrder(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "订单不存在");
        }
        return order;
    }

    private RefundOrder getOwnedRefund(Long refundId) {
        RefundOrder refund = refundOrderMapper.selectById(refundId);
        if (refund == null || !Objects.equals(refund.getUserId(), currentUserId())) {
            throw new BizException(404, "退款申请不存在");
        }
        return refund;
    }

    private RefundOrder getMerchantRefund(Long refundId) {
        RefundOrder refund = refundOrderMapper.selectById(refundId);
        Long merchantId = currentMerchantShopId();
        if (refund == null || !Objects.equals(refund.getMerchantId(), merchantId)) {
            throw new BizException(404, "退款申请不存在");
        }
        return refund;
    }

    private RefundOrder getAdminRefund(Long refundId) {
        RefundOrder refund = refundOrderMapper.selectById(refundId);
        if (refund == null) {
            throw new BizException(404, "退款申请不存在");
        }
        return refund;
    }

    private Long currentMerchantShopId() {
        Long userId = currentUserId();
        MerchantShop shop = merchantShopMapper.selectOne(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getUserId, userId)
                .eq(MerchantShop::getStatus, 1)
                .last("limit 1"));
        if (shop == null) {
            throw new BizException(400, "商家店铺不存在");
        }
        return shop.getId();
    }

    private RefundEstimateVO calculateEstimate(TourOrder order, TourDepartureDate departureDate, String refundType) {
        if (!StringUtils.hasText(refundType)) {
            throw new BizException(400, "退款类型不能为空");
        }
        String type = refundType.trim().toUpperCase();
        BigDecimal payAmount = safeMoney(order.getPayAmount());
        BigDecimal ratio = BigDecimal.ONE;
        String note;

        long daysBeforeDeparture = departureDate == null || departureDate.getDepartDate() == null
                ? 999
                : ChronoUnit.DAYS.between(LocalDate.now(), departureDate.getDepartDate());

        switch (type) {
            case "PRE_DEPARTURE":
                if (daysBeforeDeparture >= 7) {
                    ratio = BigDecimal.ONE;
                    note = "出发前 7 天以上申请，系统按标准规则预估为全额退款。";
                } else if (daysBeforeDeparture >= 1) {
                    ratio = new BigDecimal("0.80");
                    note = "出发前 1 至 7 天申请，系统按预估 80% 退款，实际以商家损失核算为准。";
                } else {
                    ratio = new BigDecimal("0.50");
                    note = "临近出发申请退团，系统按预估 50% 退款，实际以已发生损失核算为准。";
                }
                break;
            case "MERCHANT_REASON":
                ratio = BigDecimal.ONE;
                note = "因商家原因退款，系统预估全额退款。";
                break;
            case "FORCE_MAJEURE":
                ratio = BigDecimal.ONE;
                note = "因不可抗力退款，系统预估按合同与政策优先全额处理，最终以平台方案为准。";
                break;
            case "PARTIAL":
                ratio = new BigDecimal(daysBeforeDeparture < 0 ? "0.30" : "0.60");
                note = "部分退款将按未履约部分核算，当前仅给出预估金额，最终以商家或平台核定为准。";
                break;
            default:
                throw new BizException(400, "不支持的退款类型");
        }

        BigDecimal estimatedRefund = payAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deductAmount = payAmount.subtract(estimatedRefund).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        RefundEstimateVO result = new RefundEstimateVO();
        result.setEstimatedRefundAmount(estimatedRefund);
        result.setDeductAmount(deductAmount);
        result.setPolicyNote(note);
        return result;
    }

    private void syncTimeoutStatus(RefundOrder refund) {
        if (refund == null || refund.getId() == null) {
            return;
        }
        if (STATUS_WAITING_MERCHANT_REVIEW.equals(refund.getStatus())
                && refund.getMerchantDeadlineTime() != null
                && !LocalDateTime.now().isBefore(refund.getMerchantDeadlineTime())) {
            refund.setStatus(STATUS_WAITING_ADMIN_ARBITRATION);
            refund.setAdminDeadlineTime(LocalDateTime.now().plusHours(24));
            refundOrderMapper.updateById(refund);
            addFlow(refund.getId(), 0L, "SYSTEM", "TIMEOUT_ESCALATE",
                    "商家超时未处理，系统已转交管理员仲裁");
        }
    }

    private void addFlow(Long refundId, Long operatorId, String role, String action, String content) {
        RefundFlow flow = new RefundFlow();
        flow.setRefundId(refundId);
        flow.setOperatorId(operatorId == null ? 0L : operatorId);
        flow.setOperatorRole(role);
        flow.setActionType(action);
        flow.setActionContent(content);
        refundFlowMapper.insert(flow);
    }

    private List<String> mergeEvidenceUrls(List<String> original, List<String> incoming) {
        List<String> merged = new ArrayList<>();
        if (original != null) {
            merged.addAll(original.stream().filter(StringUtils::hasText).collect(Collectors.toList()));
        }
        if (incoming != null) {
            for (String item : incoming) {
                if (!StringUtils.hasText(item) || merged.contains(item.trim())) {
                    continue;
                }
                merged.add(item.trim());
            }
        }
        return merged;
    }

    private String buildSupplementContent(String content, List<String> evidenceUrls) {
        StringBuilder builder = new StringBuilder("用户补充材料");
        if (StringUtils.hasText(content)) {
            builder.append("：").append(content.trim());
        }
        if (evidenceUrls != null && !evidenceUrls.isEmpty()) {
            builder.append("（新增凭证 ").append(evidenceUrls.size()).append(" 项）");
        }
        return builder.toString();
    }

    private BigDecimal resolveRefundAmount(RefundOrder refund) {
        if (refund.getFinalRefundAmount() != null) {
            return refund.getFinalRefundAmount();
        }
        if (refund.getProposedRefundAmount() != null) {
            return refund.getProposedRefundAmount();
        }
        return safeMoney(refund.getExpectedRefundAmount());
    }

    private BigDecimal resolveAmount(BigDecimal input, BigDecimal fallback) {
        return normalizeMoney(input != null ? input : fallback);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return safeMoney(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> readStringList(String content) {
        if (!StringUtils.hasText(content)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String withSuffix(String note) {
        return StringUtils.hasText(note) ? "，说明：" + note.trim() : "";
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ((int) (Math.random() * 9000) + 1000);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        return userId;
    }
}
