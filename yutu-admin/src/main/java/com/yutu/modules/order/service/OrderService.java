package com.yutu.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.contract.service.ContractContentRenderer;
import com.yutu.modules.model.entity.ContractTemplate;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.PayRecord;
import com.yutu.modules.model.entity.RefundFlow;
import com.yutu.modules.model.entity.RefundOrder;
import com.yutu.modules.model.entity.TourContract;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourOrderTraveler;
import com.yutu.modules.model.entity.TourReview;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.PayRecordMapper;
import com.yutu.modules.model.mapper.RefundFlowMapper;
import com.yutu.modules.model.mapper.RefundOrderMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourContractMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourOrderTravelerMapper;
import com.yutu.modules.model.mapper.TourReviewMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.order.dto.OrderCreateRequest;
import com.yutu.modules.order.dto.OrderReviewRequest;
import com.yutu.modules.order.dto.OrderTravelerItem;
import com.yutu.modules.order.vo.OrderCreateVO;
import com.yutu.modules.order.vo.OrderPaymentPrepareVO;
import com.yutu.modules.order.vo.OrderPaymentStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final long PAYMENT_QR_REUSE_MINUTES = 10L;
    private static final long PAYMENT_STATUS_CONFIRM_GRACE_MINUTES = 1L;
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String ORDER_STATUS_PENDING_TRAVEL = "PENDING_TRAVEL";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    private static final String ORDER_STATUS_EXPIRED = "EXPIRED";
    private static final String PAY_STATUS_UNPAID = "UNPAID";
    private static final String PAY_STATUS_PAID = "PAID";

    private final TourOrderMapper tourOrderMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final TourOrderTravelerMapper tourOrderTravelerMapper;
    private final PayRecordMapper payRecordMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final RefundFlowMapper refundFlowMapper;
    private final ContractTemplateMapper contractTemplateMapper;
    private final TourContractMapper tourContractMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final SysUserMapper sysUserMapper;
    private final TourReviewMapper tourReviewMapper;
    private final AlipaySandboxService alipaySandboxService;
    private final ObjectMapper objectMapper;
    private final long paymentTimeoutMinutes;

    public OrderService(TourOrderMapper tourOrderMapper,
            TourRouteMapper tourRouteMapper,
            TourDepartureDateMapper tourDepartureDateMapper,
            TourOrderTravelerMapper tourOrderTravelerMapper,
            PayRecordMapper payRecordMapper,
            RefundOrderMapper refundOrderMapper,
            RefundFlowMapper refundFlowMapper,
            ContractTemplateMapper contractTemplateMapper,
            TourContractMapper tourContractMapper,
            MerchantShopMapper merchantShopMapper,
            SysUserMapper sysUserMapper,
            TourReviewMapper tourReviewMapper,
            AlipaySandboxService alipaySandboxService,
            ObjectMapper objectMapper,
            @Value("${app.order.payment-timeout-minutes:30}") long paymentTimeoutMinutes) {
        this.tourOrderMapper = tourOrderMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourOrderTravelerMapper = tourOrderTravelerMapper;
        this.payRecordMapper = payRecordMapper;
        this.refundOrderMapper = refundOrderMapper;
        this.refundFlowMapper = refundFlowMapper;
        this.contractTemplateMapper = contractTemplateMapper;
        this.tourContractMapper = tourContractMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.sysUserMapper = sysUserMapper;
        this.tourReviewMapper = tourReviewMapper;
        this.alipaySandboxService = alipaySandboxService;
        this.objectMapper = objectMapper;
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO create(OrderCreateRequest request) {
        Long userId = currentUserId();
        TourRoute route = tourRouteMapper.selectById(request.getRouteId());
        if (route == null || !Objects.equals(route.getPublishStatus(), 1)
                || !Objects.equals(route.getAuditStatus(), 1)) {
            throw new BizException(400, "璺嚎涓嶅彲棰勮");
        }

        TourDepartureDate departureDate = tourDepartureDateMapper.selectById(request.getDepartDateId());
        if (departureDate == null
                || !Objects.equals(departureDate.getRouteId(), route.getId())
                || !Objects.equals(departureDate.getStatus(), 1)
                || !Objects.equals(departureDate.getAuditStatus(), 1)) {
            throw new BizException(400, "鍑哄彂鏃ユ湡鏃犳晥");
        }
        if (departureDate.getDepartDate() == null || departureDate.getDepartDate().isBefore(LocalDate.now())) {
            throw new BizException(400, "departure date expired");
        }
        if (departureDate.getRemainCount() < request.getTravelerCount()) {
            throw new BizException(400, "搴撳瓨涓嶈冻");
        }

        departureDate.setRemainCount(departureDate.getRemainCount() - request.getTravelerCount());
        tourDepartureDateMapper.updateById(departureDate);

        BigDecimal amount = departureDate.getSalePrice().multiply(new BigDecimal(request.getTravelerCount()));
        TourOrder order = new TourOrder();
        order.setOrderNo(generateNo("ORD"));
        order.setUserId(userId);
        order.setMerchantId(route.getMerchantId());
        order.setRouteId(route.getId());
        order.setDepartDateId(departureDate.getId());
        order.setTravelerCount(request.getTravelerCount());
        order.setTotalAmount(amount);
        order.setPayAmount(amount);
        order.setOrderStatus("PENDING_PAY");
        order.setPayStatus("UNPAID");
        order.setContractStatus("GENERATED");
        order.setSource("WEB");
        order.setDeleted(0);
        tourOrderMapper.insert(order);

        if (request.getTravelers() != null) {
            for (OrderTravelerItem item : request.getTravelers()) {
                TourOrderTraveler traveler = new TourOrderTraveler();
                traveler.setOrderId(order.getId());
                traveler.setTravelerName(item.getTravelerName());
                traveler.setIdCard(item.getIdCard());
                traveler.setPhone(item.getPhone());
                tourOrderTravelerMapper.insert(traveler);
            }
        }

        TourContract contract = createContractForOrder(order);

        OrderCreateVO result = new OrderCreateVO();
        result.setOrderId(order.getId());
        result.setContractId(contract.getId());
        return result;
    }

    public List<TourOrder> userOrders() {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, currentUserId())
                .orderByDesc(TourOrder::getCreateTime));
        return decorateOrders(orders);
    }

    public Map<String, Object> userOrderDetail(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "order not found");
        }
        return orderDetailMap(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TourOrder order = getOwnedOrder(id);
        if (isOverdueCancelled(order)) {
            throw new BizException(400, "璁㈠崟宸茶秴鏃舵湭鏀粯锛岀郴缁熷凡鑷姩鍙栨秷");
        }
        if (!canCancelPendingPaymentOrder(order)) {
            throw new BizException(400, "order cannot be cancelled");
        }
        if (!cancelPendingUnpaidOrder(order)) {
            TourOrder latestOrder = refreshOrderPaymentStateById(id);
            if (isOverdueCancelled(latestOrder)) {
                throw new BizException(400, "璁㈠崟宸茶秴鏃舵湭鏀粯锛岀郴缁熷凡鑷姩鍙栨秷");
            }
            throw new BizException(400, "order cannot be cancelled");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentPrepareVO pay(Long id) {
        TourOrder order = getOwnedOrder(id);
        if ("PAID".equals(order.getPayStatus())) {
            return buildPaidPaymentPrepare(order, latestPayRecord(order.getId()));
        }
        if (isOverdueCancelled(order)) {
            throw new BizException(400, "璁㈠崟鏀粯鏃堕檺宸茶繃锛岀郴缁熷凡鑷姩鍙栨秷");
        }
        if (Boolean.TRUE.equals(order.getPaymentExpired())) {
            throw new BizException(400, "璁㈠崟宸茶秴杩囨敮浠樻椂闄愶紝姝ｅ湪纭鏀粯缁撴灉锛岃绋嶅悗鍒锋柊");
        }
        if (!canCancelPendingPaymentOrder(order)) {
            throw new BizException(400, "order cannot be paid");
        }
        if (!"SIGNED".equals(order.getContractStatus())) {
            throw new BizException(400, "璇峰厛瀹屾垚鍚堝悓绛剧讲锛屽啀杩涘叆鏀粯");
        }

        PayRecord payRecord = latestPayRecord(order.getId());
        if (payRecord != null && "WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            order = refreshOrderPaymentStateById(id);
            if (order != null && "PAID".equals(order.getPayStatus())) {
                return buildPaidPaymentPrepare(order, latestPayRecord(order.getId()));
            }
            if (isOverdueCancelled(order)) {
                throw new BizException(400, "璁㈠崟鏀粯鏃堕檺宸茶繃锛岀郴缁熷凡鑷姩鍙栨秷");
            }
            String cachedQrCode = getPayRecordExtra(payRecord, "qrCode");
            String cachedQrCodeImage = getPayRecordExtra(payRecord, "qrCodeImage");
            if (hasText(cachedQrCode) && hasText(cachedQrCodeImage) && canReusePaymentQr(payRecord)) {
                return buildPendingPaymentPrepare(order, payRecord, cachedQrCode, cachedQrCodeImage);
            }
            expirePendingPayRecord(payRecord);
        }

        String payNo = generateNo("PAY");
        AlipaySandboxService.PrecreateResult precreateResult = alipaySandboxService.preCreate(
                "璞€旀梾娓歌鍗?" + order.getOrderNo(),
                payNo,
                order.getPayAmount());

        PayRecord newPayRecord = new PayRecord();
        newPayRecord.setOrderId(order.getId());
        newPayRecord.setOrderNo(order.getOrderNo());
        newPayRecord.setPayNo(payNo);
        newPayRecord.setPayType("ALIPAY_SANDBOX");
        newPayRecord.setPayAmount(order.getPayAmount());
        newPayRecord.setPayStatus("WAIT_BUYER_PAY");
        newPayRecord.setCallbackContent(writePaymentSnapshot(
                precreateResult.getQrCode(),
                precreateResult.getQrCodeImage(),
                precreateResult.getRawBody()));
        payRecordMapper.insert(newPayRecord);

        return buildPendingPaymentPrepare(order, newPayRecord, precreateResult.getQrCode(),
                precreateResult.getQrCodeImage());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentStatusVO queryPaymentStatus(Long id) {
        TourOrder order = getOwnedOrder(id);
        PayRecord payRecord = latestPayRecord(order.getId());

        if ("PAID".equals(order.getPayStatus())) {
            return buildPaymentStatus(order, payRecord, true);
        }
        if (isOverdueCancelled(order) || !"PENDING_PAY".equals(order.getOrderStatus())) {
            return buildPaymentStatus(order, payRecord, false);
        }
        if (payRecord == null) {
            return buildPaymentStatus(order, null, false);
        }
        if (!"WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            return buildPaymentStatus(order, payRecord, false);
        }

        AlipaySandboxService.QueryResult queryResult;
        try {
            queryResult = alipaySandboxService.query(payRecord.getPayNo());
        } catch (Exception ex) {
            log.warn("failed to sync payment status, orderId={}, payNo={}", order.getId(), payRecord.getPayNo(), ex);
            return buildPaymentStatus(order, payRecord, false);
        }

        String tradeStatus = queryResult.getTradeStatus();
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            syncPaidOrder(order, payRecord, queryResult);
            return buildPaymentStatus(order, payRecord, true);
        }
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            failPendingPayRecord(payRecord, queryResult);
            return buildPaymentStatus(order, payRecord, false);
        }
        if (queryResult.isSuccess()) {
            payRecord.setCallbackContent(mergePayRecordSnapshot(payRecord.getCallbackContent(), queryResult));
            payRecordMapper.updateById(payRecord);
        }
        refreshOrderPaymentWindow(order);
        return buildPaymentStatus(order, payRecord, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireOverdueOrders() {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getOrderStatus, "PENDING_PAY")
                .eq(TourOrder::getPayStatus, "UNPAID")
                .le(TourOrder::getCreateTime, LocalDateTime.now().minusMinutes(paymentTimeoutMinutes)));
        int cancelledCount = 0;
        for (TourOrder order : orders) {
            if (order == null) {
                continue;
            }
            String previousStatus = order.getOrderStatus();
            refreshOrderPaymentState(order);
            if (!Objects.equals(previousStatus, order.getOrderStatus()) && "CANCELLED".equals(order.getOrderStatus())) {
                cancelledCount++;
            }
        }
        return cancelledCount;
    }

    public TourOrder refreshOrderPaymentStateById(Long orderId) {
        TourOrder order = tourOrderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        refreshOrderPaymentState(order);
        return order;
    }

    private TourContract createContractForOrder(TourOrder order) {
        TourRoute route = tourRouteMapper.selectById(order.getRouteId());
        if (route == null) {
            throw new BizException(500, "route not found");
        }

        ContractTemplate standardTemplate = route.getStandardTemplateId() == null
                ? null
                : contractTemplateMapper.selectById(route.getStandardTemplateId());
        ContractTemplate routeTemplate = route.getRouteTemplateId() == null
                ? null
                : contractTemplateMapper.selectById(route.getRouteTemplateId());
        if (standardTemplate == null || routeTemplate == null) {
            throw new BizException(500, "褰撳墠璺嚎鏈厤缃畬鏁寸殑鍚堝悓妯℃澘");
        }

        TourContract contract = new TourContract();
        contract.setContractNo(generateNo("CON"));
        contract.setOrderId(order.getId());
        contract.setTemplateId(standardTemplate.getId());
        contract.setUserId(order.getUserId());
        contract.setMerchantId(order.getMerchantId());
        contract.setContractTitle(buildContractTitle(order));
        List<TourOrderTraveler> travelers = tourOrderTravelerMapper.selectList(new LambdaQueryWrapper<TourOrderTraveler>()
                .eq(TourOrderTraveler::getOrderId, order.getId())
                .orderByAsc(TourOrderTraveler::getCreateTime)
                .orderByAsc(TourOrderTraveler::getId));
        SysUser user = sysUserMapper.selectById(order.getUserId());
        MerchantShop merchantShop = merchantShopMapper.selectById(order.getMerchantId());
        TourDepartureDate departureDate = tourDepartureDateMapper.selectById(order.getDepartDateId());
        contract.setContractContent(buildContractContent(contract, order, route, departureDate, user, merchantShop,
                travelers, standardTemplate, routeTemplate));
        contract.setSignStatus("UNSIGNED");
        contract.setDeleted(0);
        tourContractMapper.insert(contract);
        try {
            contractTemplateMapper.incrementUseCount(standardTemplate.getId());
            contractTemplateMapper.incrementUseCount(routeTemplate.getId());
        } catch (Exception ex) {
            log.warn("failed to increase contract template use count for routeId={}", route.getId(), ex);
        }
        return contract;
    }

    private String buildContractContent(TourContract contract,
                                        TourOrder order,
                                        TourRoute route,
                                        TourDepartureDate departureDate,
                                        SysUser user,
                                        MerchantShop merchantShop,
                                        List<TourOrderTraveler> travelers,
                                        ContractTemplate standardTemplate,
                                        ContractTemplate routeTemplate) {
        List<String> sections = new ArrayList<>();
        sections.add(ContractContentRenderer.render(
                standardTemplate.getTemplateContent(),
                contract,
                order,
                route,
                departureDate,
                user,
                merchantShop,
                travelers
        ));
        sections.add(ContractContentRenderer.render(
                routeTemplate.getTemplateContent(),
                contract,
                order,
                route,
                departureDate,
                user,
                merchantShop,
                travelers
        ));
        return sections.stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.trim().isEmpty())
                .collect(Collectors.joining("\n\n------------------------------\n\n"));
    }

    private String buildContractTitle(TourOrder order) {
        String orderNo = order == null ? "" : order.getOrderNo();
        return "豫途旅游服务合同-" + (orderNo == null ? "" : orderNo);
    }

    private TourContract createContractForOrderLegacy(TourOrder order) {
        ContractTemplate template = contractTemplateMapper.selectOne(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, 1)
                .last("limit 1"));
        if (template == null) {
            throw new BizException(500, "contract template not configured");
        }

        TourContract contract = new TourContract();
        contract.setContractNo(generateNo("CON"));
        contract.setOrderId(order.getId());
        contract.setTemplateId(template.getId());
        contract.setUserId(order.getUserId());
        contract.setMerchantId(order.getMerchantId());
        contract.setContractTitle(buildContractTitle(order));
        List<TourOrderTraveler> travelers = tourOrderTravelerMapper.selectList(new LambdaQueryWrapper<TourOrderTraveler>()
                .eq(TourOrderTraveler::getOrderId, order.getId())
                .orderByAsc(TourOrderTraveler::getCreateTime)
                .orderByAsc(TourOrderTraveler::getId));
        SysUser user = sysUserMapper.selectById(order.getUserId());
        MerchantShop merchantShop = merchantShopMapper.selectById(order.getMerchantId());
        TourRoute route = tourRouteMapper.selectById(order.getRouteId());
        TourDepartureDate departureDate = tourDepartureDateMapper.selectById(order.getDepartDateId());
        contract.setContractContent(ContractContentRenderer.render(
                template.getTemplateContent(),
                contract,
                order,
                route,
                departureDate,
                user,
                merchantShop,
                travelers
        ));
        contract.setSignStatus("UNSIGNED");
        contract.setDeleted(0);
        tourContractMapper.insert(contract);
        try {
            contractTemplateMapper.incrementUseCount(template.getId());
        } catch (Exception ex) {
            log.warn("failed to increase contract template use count, templateId={}", template.getId(), ex);
        }
        return contract;
    }

    private TourOrder getOwnedOrder(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "order not found");
        }
        refreshOrderPaymentState(order);
        return order;
    }

    private PayRecord latestPayRecord(Long orderId) {
        return payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, orderId)
                .orderByDesc(PayRecord::getCreateTime)
                .last("limit 1"));
    }

    private OrderPaymentPrepareVO buildPendingPaymentPrepare(TourOrder order, PayRecord payRecord, String qrCode,
            String qrCodeImage) {
        refreshOrderPaymentWindow(order);
        OrderPaymentPrepareVO result = new OrderPaymentPrepareVO();
        result.setOrderId(order.getId());
        result.setOrderNo(order.getOrderNo());
        result.setPayNo(payRecord.getPayNo());
        result.setPayStatus(order.getPayStatus());
        result.setOrderStatus(order.getOrderStatus());
        result.setPayAmount(order.getPayAmount());
        result.setQrCode(qrCode);
        result.setQrCodeImage(qrCodeImage);
        return result;
    }

    private OrderPaymentPrepareVO buildPaidPaymentPrepare(TourOrder order, PayRecord payRecord) {
        refreshOrderPaymentWindow(order);
        OrderPaymentPrepareVO result = new OrderPaymentPrepareVO();
        result.setOrderId(order.getId());
        result.setOrderNo(order.getOrderNo());
        result.setPayNo(payRecord == null ? null : payRecord.getPayNo());
        result.setPayStatus(order.getPayStatus());
        result.setOrderStatus(order.getOrderStatus());
        result.setPayAmount(order.getPayAmount());
        return result;
    }

    private OrderPaymentStatusVO buildPaymentStatus(TourOrder order, PayRecord payRecord, boolean paid) {
        refreshOrderPaymentWindow(order);
        OrderPaymentStatusVO result = new OrderPaymentStatusVO();
        result.setOrderId(order.getId());
        result.setOrderNo(order.getOrderNo());
        result.setPayNo(payRecord == null ? null : payRecord.getPayNo());
        result.setPayStatus(order.getPayStatus());
        result.setOrderStatus(order.getOrderStatus());
        result.setPaid(paid);
        return result;
    }

    private void syncPaidOrder(TourOrder order, PayRecord payRecord, AlipaySandboxService.QueryResult queryResult) {
        payRecord.setPayStatus("SUCCESS");
        payRecord.setPayTime(LocalDateTime.now());
        payRecord.setCallbackContent(mergePayRecordSnapshot(payRecord.getCallbackContent(), queryResult));
        payRecordMapper.updateById(payRecord);

        order.setPayStatus("PAID");
        order.setOrderStatus(ORDER_STATUS_PENDING_TRAVEL);
        tourOrderMapper.updateById(order);
        refreshOrderLifecycleState(order);
        refreshOrderPaymentWindow(order);
    }

    private String writePaymentSnapshot(String qrCode, String qrCodeImage, String rawBody) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("qrCode", qrCode);
        payload.put("qrCodeImage", qrCodeImage);
        payload.put("rawBody", rawBody);
        return writeAsJson(payload);
    }

    private String mergePayRecordSnapshot(String original, AlipaySandboxService.QueryResult queryResult) {
        Map<String, Object> payload = readJsonMap(original);
        payload.put("tradeStatus", queryResult.getTradeStatus());
        payload.put("tradeNo", queryResult.getTradeNo());
        payload.put("buyerLogonId", queryResult.getBuyerLogonId());
        payload.put("rawBody", queryResult.getRawBody());
        return writeAsJson(payload);
    }

    private String getPayRecordExtra(PayRecord payRecord, String key) {
        Map<String, Object> payload = readJsonMap(payRecord == null ? null : payRecord.getCallbackContent());
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Object> readJsonMap(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private String writeAsJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("failed to serialize payment payload", ex);
            return "{}";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean canReusePaymentQr(PayRecord payRecord) {
        if (payRecord == null || payRecord.getCreateTime() == null) {
            return false;
        }
        return payRecord.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(PAYMENT_QR_REUSE_MINUTES));
    }

    private void expirePendingPayRecord(PayRecord payRecord) {
        if (payRecord == null || !"WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            return;
        }
        payRecord.setPayStatus("FAILED");
        payRecordMapper.updateById(payRecord);
    }

    private void failPendingPayRecord(PayRecord payRecord, AlipaySandboxService.QueryResult queryResult) {
        if (payRecord == null || !"WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            return;
        }
        payRecord.setPayStatus("FAILED");
        payRecord.setCallbackContent(mergePayRecordSnapshot(payRecord.getCallbackContent(), queryResult));
        payRecordMapper.updateById(payRecord);
    }

    private List<TourOrder> decorateOrders(List<TourOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return orders;
        }
        for (TourOrder order : orders) {
            refreshOrderPaymentState(order);
        }
        applyReviewFlags(orders);
        applyRefundFlags(orders);
        return orders;
    }

    private void applyReviewFlags(List<TourOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream()
                .map(TourOrder::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return;
        }
        Set<Long> reviewedOrderIds = tourReviewMapper.selectList(new LambdaQueryWrapper<TourReview>()
                        .in(TourReview::getOrderId, orderIds)
                        .eq(TourReview::getStatus, 1))
                .stream()
                .map(TourReview::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (TourOrder order : orders) {
            order.setHasReviewed(reviewedOrderIds.contains(order.getId()));
        }
    }

    private void applyRefundFlags(List<TourOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream()
                .map(TourOrder::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return;
        }
        List<RefundOrder> refunds = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .in(RefundOrder::getOrderId, orderIds)
                .orderByDesc(RefundOrder::getCreateTime)
                .orderByDesc(RefundOrder::getId));
        Map<Long, RefundOrder> latestRefundByOrderId = new HashMap<>();
        for (RefundOrder refund : refunds) {
            if (refund == null || refund.getOrderId() == null || latestRefundByOrderId.containsKey(refund.getOrderId())) {
                continue;
            }
            latestRefundByOrderId.put(refund.getOrderId(), refund);
        }
        for (TourOrder order : orders) {
            RefundOrder refund = latestRefundByOrderId.get(order.getId());
            if (refund == null) {
                if (isRefundedOrder(order)) {
                    order.setRefundStatus("REFUND_COMPLETED");
                }
                continue;
            }
            order.setRefundId(refund.getId());
            order.setRefundNo(refund.getRefundNo());
            order.setRefundStatus(refund.getStatus());
        }
    }

    private boolean isRefundedOrder(TourOrder order) {
        return order != null
                && ("REFUNDED".equals(order.getOrderStatus()) || "REFUNDED".equals(order.getPayStatus()));
    }

    private void refreshOrderPaymentState(TourOrder order) {
        if (order == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!canExpirePendingPaymentOrder(order) || !hasPaymentExpired(order, now)) {
            refreshOrderLifecycleState(order);
            refreshOrderPaymentWindow(order, now);
            return;
        }
        reconcileExpiredPaymentOrder(order);
        refreshOrderLifecycleState(order);
        refreshOrderPaymentWindow(order, LocalDateTime.now());
    }

    private void refreshOrderLifecycleState(TourOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (!PAY_STATUS_PAID.equals(order.getPayStatus())) {
            return;
        }
        if (isRefundOrderStatus(order)) {
            return;
        }

        TourDepartureDate departureDate = order.getDepartDateId() == null
                ? null
                : tourDepartureDateMapper.selectById(order.getDepartDateId());
        if (departureDate == null || departureDate.getDepartDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (departureDate.getDepartDate().isBefore(today)) {
            completeFinishedTravelOrder(order);
            return;
        }

        if (ORDER_STATUS_COMPLETED.equals(order.getOrderStatus())) {
            order.setOrderStatus(ORDER_STATUS_PENDING_TRAVEL);
            tourOrderMapper.updateById(order);
        }
    }

    private void completeFinishedTravelOrder(TourOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (ORDER_STATUS_COMPLETED.equals(order.getOrderStatus())) {
            return;
        }
        order.setOrderStatus(ORDER_STATUS_COMPLETED);
        tourOrderMapper.updateById(order);
        log.info("completed finished travel order, orderId={}, orderNo={}", order.getId(), order.getOrderNo());
    }

    private boolean isRefundOrderStatus(TourOrder order) {
        if (order == null) {
            return false;
        }
        return "REFUNDING".equals(order.getOrderStatus())
                || "REFUNDED".equals(order.getOrderStatus())
                || "REFUNDED".equals(order.getPayStatus());
    }

    private boolean isPaidFinishedTravelOrder(TourOrder order) {
        if (order == null || !PAY_STATUS_PAID.equals(order.getPayStatus()) || isRefundOrderStatus(order)) {
            return false;
        }
        TourDepartureDate departureDate = order.getDepartDateId() == null
                ? null
                : tourDepartureDateMapper.selectById(order.getDepartDateId());
        return departureDate != null
                && departureDate.getDepartDate() != null
                && departureDate.getDepartDate().isBefore(LocalDate.now());
    }

    private void reconcileExpiredPaymentOrder(TourOrder order) {
        if (!canExpirePendingPaymentOrder(order)) {
            return;
        }

        PayRecord payRecord = latestPayRecord(order.getId());
        if (payRecord != null && "WAIT_BUYER_PAY".equals(payRecord.getPayStatus()) && hasText(payRecord.getPayNo())
                && alipaySandboxService.isConfigured()) {
            try {
                AlipaySandboxService.QueryResult queryResult = alipaySandboxService.query(payRecord.getPayNo());
                String tradeStatus = queryResult.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    syncPaidOrder(order, payRecord, queryResult);
                    return;
                }
                if ("TRADE_CLOSED".equals(tradeStatus)) {
                    failPendingPayRecord(payRecord, queryResult);
                } else if (queryResult.isSuccess()) {
                    payRecord.setCallbackContent(mergePayRecordSnapshot(payRecord.getCallbackContent(), queryResult));
                    payRecordMapper.updateById(payRecord);
                }
            } catch (Exception ex) {
                log.warn("failed to reconcile overdue payment before auto cancel, orderId={}, payNo={}",
                        order.getId(), payRecord.getPayNo(), ex);
                if (!hasExceededPaymentConfirmGrace(order, LocalDateTime.now())) {
                    return;
                }
            }
        }

        if (cancelPendingUnpaidOrder(order)) {
            log.info("cancelled overdue unpaid order, orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        }
    }

    private boolean cancelPendingUnpaidOrder(TourOrder order) {
        if (order == null || order.getId() == null) {
            return false;
        }

        int updated = tourOrderMapper.update(null, new LambdaUpdateWrapper<TourOrder>()
                .eq(TourOrder::getId, order.getId())
                .eq(TourOrder::getOrderStatus, "PENDING_PAY")
                .eq(TourOrder::getPayStatus, "UNPAID")
                .set(TourOrder::getOrderStatus, "CANCELLED"));
        if (updated <= 0) {
            return false;
        }

        order.setOrderStatus("CANCELLED");
        restoreDepartureInventory(order);
        expireOpenPayRecords(order.getId());
        refreshOrderPaymentWindow(order);
        return true;
    }

    private void restoreDepartureInventory(TourOrder order) {
        if (order == null || order.getDepartDateId() == null || order.getTravelerCount() == null
                || order.getTravelerCount() <= 0) {
            return;
        }
        TourDepartureDate departureDate = tourDepartureDateMapper.selectById(order.getDepartDateId());
        if (departureDate == null) {
            return;
        }
        int remainCount = departureDate.getRemainCount() == null ? 0 : departureDate.getRemainCount();
        departureDate.setRemainCount(remainCount + order.getTravelerCount());
        tourDepartureDateMapper.updateById(departureDate);
    }

    private void expireOpenPayRecords(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<PayRecord> payRecords = payRecordMapper.selectList(new LambdaQueryWrapper<PayRecord>()
                .eq(PayRecord::getOrderId, orderId)
                .eq(PayRecord::getPayStatus, "WAIT_BUYER_PAY"));
        for (PayRecord payRecord : payRecords) {
            expirePendingPayRecord(payRecord);
        }
    }

    private boolean canCancelPendingPaymentOrder(TourOrder order) {
        return order != null
                && "PENDING_PAY".equals(order.getOrderStatus())
                && PAY_STATUS_UNPAID.equals(order.getPayStatus());
    }

    private boolean canExpirePendingPaymentOrder(TourOrder order) {
        return canCancelPendingPaymentOrder(order);
    }

    private boolean isOverdueCancelled(TourOrder order) {
        return order != null
                && ORDER_STATUS_CANCELLED.equals(order.getOrderStatus())
                && PAY_STATUS_UNPAID.equals(order.getPayStatus())
                && Boolean.TRUE.equals(order.getPaymentExpired());
    }

    private void refreshOrderPaymentWindow(TourOrder order) {
        refreshOrderPaymentWindow(order, LocalDateTime.now());
    }

    private void refreshOrderPaymentWindow(TourOrder order, LocalDateTime now) {
        if (order == null) {
            return;
        }
        order.setPaymentTimeoutMinutes(paymentTimeoutMinutes);
        order.setPaymentExpireTime(resolvePaymentExpireTime(order));
        order.setPaymentExpired(isUnpaidOrderPastDeadline(order, now));
        order.setPaymentRemainingSeconds(resolveRemainingPaymentSeconds(order, now));
    }

    private LocalDateTime resolvePaymentExpireTime(TourOrder order) {
        if (order == null || order.getCreateTime() == null) {
            return null;
        }
        return order.getCreateTime().plusMinutes(paymentTimeoutMinutes);
    }

    private long resolveRemainingPaymentSeconds(TourOrder order, LocalDateTime now) {
        if (!canCancelPendingPaymentOrder(order)) {
            return 0L;
        }
        LocalDateTime expireTime = resolvePaymentExpireTime(order);
        if (expireTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(now, expireTime).getSeconds(), 0L);
    }

    private boolean hasPaymentExpired(TourOrder order, LocalDateTime now) {
        if (!canExpirePendingPaymentOrder(order)) {
            return false;
        }
        LocalDateTime expireTime = resolvePaymentExpireTime(order);
        return expireTime != null && !now.isBefore(expireTime);
    }

    private boolean isUnpaidOrderPastDeadline(TourOrder order, LocalDateTime now) {
        if (order == null || !PAY_STATUS_UNPAID.equals(order.getPayStatus())) {
            return false;
        }
        LocalDateTime expireTime = resolvePaymentExpireTime(order);
        return expireTime != null && !now.isBefore(expireTime);
    }

    private boolean hasExceededPaymentConfirmGrace(TourOrder order, LocalDateTime now) {
        LocalDateTime expireTime = resolvePaymentExpireTime(order);
        if (expireTime == null) {
            return true;
        }
        return !now.isBefore(expireTime.plusMinutes(PAYMENT_STATUS_CONFIRM_GRACE_MINUTES));
    }

    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "order not found");
        }
        throw new BizException(400, "please use the new refund flow");
    }

    public List<TourOrder> merchantOrders(String keyword) {
        Long shopId = currentMerchantShopId();
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, shopId)
                .orderByDesc(TourOrder::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(TourOrder::getOrderNo, keyword.trim());
        }
        List<TourOrder> orders = decorateOrders(tourOrderMapper.selectList(wrapper));
        orders.sort(Comparator
                .comparing((TourOrder order) -> !isMerchantPendingRefund(order))
                .thenComparing(TourOrder::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return orders;
    }

    public Map<String, Object> merchantOrderDetail(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getMerchantId(), currentMerchantShopId())) {
            throw new BizException(404, "order not found");
        }
        return orderDetailMap(order);
    }

    public List<TourOrder> adminOrders(String keyword) {
        LambdaQueryWrapper<TourOrder> wrapper = new LambdaQueryWrapper<TourOrder>()
                .orderByDesc(TourOrder::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(TourOrder::getOrderNo, keyword.trim());
        }
        List<TourOrder> orders = tourOrderMapper.selectList(wrapper);
        return decorateOrders(orders);
    }

    public Map<String, Object> adminOrderDetail(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "order not found");
        }
        return orderDetailMap(order);
    }

    public List<PayRecord> adminPayRecords(String keyword) {
        LambdaQueryWrapper<PayRecord> wrapper = new LambdaQueryWrapper<PayRecord>()
                .orderByDesc(PayRecord::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(q -> q.like(PayRecord::getPayNo, trimmedKeyword)
                    .or()
                    .like(PayRecord::getOrderNo, trimmedKeyword));
        }
        return payRecordMapper.selectList(wrapper);
    }

    public void adminHandleOrderException(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "order not found");
        }
        refreshOrderPaymentState(order);
        if (canCancelPendingPaymentOrder(order)) {
            cancelPendingUnpaidOrder(order);
        }
    }

    private Map<String, Object> orderDetailMap(TourOrder order) {
        refreshOrderPaymentState(order);
        List<TourOrderTraveler> travelers = tourOrderTravelerMapper
                .selectList(new LambdaQueryWrapper<TourOrderTraveler>()
                        .eq(TourOrderTraveler::getOrderId, order.getId()));
        List<TourContract> contracts = tourContractMapper.selectList(new LambdaQueryWrapper<TourContract>()
                .eq(TourContract::getOrderId, order.getId())
                .orderByDesc(TourContract::getCreateTime));
        TourRoute route = order.getRouteId() == null ? null : tourRouteMapper.selectById(order.getRouteId());
        TourDepartureDate departureDate = order.getDepartDateId() == null ? null : tourDepartureDateMapper.selectById(order.getDepartDateId());
        RefundOrder refund = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, order.getId())
                .orderByDesc(RefundOrder::getCreateTime)
                .orderByDesc(RefundOrder::getId)
                .last("limit 1"));
        List<RefundFlow> refundFlows = refund == null
                ? Collections.emptyList()
                : refundFlowMapper.selectList(new LambdaQueryWrapper<RefundFlow>()
                        .eq(RefundFlow::getRefundId, refund.getId())
                        .orderByAsc(RefundFlow::getCreateTime)
                        .orderByAsc(RefundFlow::getId));
        Map<String, Object> map = new HashMap<>();
        map.put("order", order);
        map.put("travelers", travelers);
        map.put("contracts", contracts);
        map.put("route", route);
        map.put("departureDate", departureDate);
        map.put("refund", refund);
        map.put("refundFlows", refundFlows);
        return map;
    }

    private boolean isMerchantPendingRefund(TourOrder order) {
        if (order == null || order.getRefundId() == null || !StringUtils.hasText(order.getRefundStatus())) {
            return false;
        }
        return Arrays.asList(
                "WAITING_MERCHANT_REVIEW",
                "WAITING_REFUND_EXECUTION",
                "REFUND_PROCESSING").contains(order.getRefundStatus());
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ((int) (Math.random() * 9000) + 1000);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "not logged in");
        }
        return userId;
    }

    private Long currentMerchantShopId() {
        Long userId = currentUserId();
        MerchantShop shop = merchantShopMapper.selectOne(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getUserId, userId)
                .eq(MerchantShop::getStatus, 1)
                .last("limit 1"));
        if (shop == null) {
            throw new BizException(400, "merchant shop not found");
        }
        return shop.getId();
    }

    public void complete(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "order not found");
        }
        refreshOrderPaymentState(order);
        if (!PAY_STATUS_PAID.equals(order.getPayStatus())) {
            throw new BizException(400, "订单支付完成后才能确认完成");
        }
        if (ORDER_STATUS_COMPLETED.equals(order.getOrderStatus())) {
            return;
        }
        if (!ORDER_STATUS_PENDING_TRAVEL.equals(order.getOrderStatus())
                && !ORDER_STATUS_EXPIRED.equals(order.getOrderStatus())
                && !ORDER_STATUS_CANCELLED.equals(order.getOrderStatus())) {
            throw new BizException(400, "当前订单状态不能确认完成");
        }

        if (!isPaidFinishedTravelOrder(order)) {
            throw new BizException(400, "出行结束后才能确认完成并评价");
        }
        completeFinishedTravelOrder(order);
    }

    public void review(Long id, OrderReviewRequest request) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "order not found");
        }
        refreshOrderPaymentState(order);
        if (!ORDER_STATUS_COMPLETED.equals(order.getOrderStatus()) && !isPaidFinishedTravelOrder(order)) {
            throw new BizException(400, "只有已支付且已出行完成的订单才能评价");
        }
        if (!ORDER_STATUS_COMPLETED.equals(order.getOrderStatus())) {
            completeFinishedTravelOrder(order);
        }
        TourReview existingReview = tourReviewMapper.selectOne(new LambdaQueryWrapper<TourReview>()
                .eq(TourReview::getOrderId, id)
                .eq(TourReview::getStatus, 1)
                .last("limit 1"));
        if (existingReview != null) {
            throw new BizException(400, "璇ヨ鍗曞凡鎻愪氦璇勪环");
        }

        TourReview review = new TourReview();
        review.setRouteId(order.getRouteId());
        review.setOrderId(order.getId());
        review.setUserId(order.getUserId());
        review.setScore(request.getScore());
        review.setContent(StringUtils.hasText(request.getContent()) ? request.getContent().trim() : "");
        review.setStatus(1);
        tourReviewMapper.insert(review);
        refreshRouteScore(order.getRouteId());
    }

    private void refreshRouteScore(Long routeId) {
        if (routeId == null) {
            return;
        }
        TourRoute route = tourRouteMapper.selectById(routeId);
        if (route == null) {
            return;
        }
        List<TourReview> reviews = tourReviewMapper.selectList(new LambdaQueryWrapper<TourReview>()
                .eq(TourReview::getRouteId, routeId)
                .eq(TourReview::getStatus, 1));
        if (reviews.isEmpty()) {
            route.setScore(BigDecimal.ZERO);
            tourRouteMapper.updateById(route);
            return;
        }
        BigDecimal totalScore = reviews.stream()
                .map(TourReview::getScore)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageScore = totalScore.divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
        route.setScore(averageScore);
        tourRouteMapper.updateById(route);
    }
}

