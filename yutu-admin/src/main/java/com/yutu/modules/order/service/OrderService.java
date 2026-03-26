package com.yutu.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.model.entity.ContractTemplate;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.PayRecord;
import com.yutu.modules.model.entity.TourContract;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourOrderTraveler;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.PayRecordMapper;
import com.yutu.modules.model.mapper.TourContractMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourOrderTravelerMapper;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final long PAYMENT_QR_REUSE_MINUTES = 10L;
    private static final long PAYMENT_STATUS_CONFIRM_GRACE_MINUTES = 1L;

    private final TourOrderMapper tourOrderMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final TourOrderTravelerMapper tourOrderTravelerMapper;
    private final PayRecordMapper payRecordMapper;
    private final ContractTemplateMapper contractTemplateMapper;
    private final TourContractMapper tourContractMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final AlipaySandboxService alipaySandboxService;
    private final ObjectMapper objectMapper;
    private final long paymentTimeoutMinutes;

    public OrderService(TourOrderMapper tourOrderMapper,
            TourRouteMapper tourRouteMapper,
            TourDepartureDateMapper tourDepartureDateMapper,
            TourOrderTravelerMapper tourOrderTravelerMapper,
            PayRecordMapper payRecordMapper,
            ContractTemplateMapper contractTemplateMapper,
            TourContractMapper tourContractMapper,
            MerchantShopMapper merchantShopMapper,
            AlipaySandboxService alipaySandboxService,
            ObjectMapper objectMapper,
            @Value("${app.order.payment-timeout-minutes:30}") long paymentTimeoutMinutes) {
        this.tourOrderMapper = tourOrderMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourOrderTravelerMapper = tourOrderTravelerMapper;
        this.payRecordMapper = payRecordMapper;
        this.contractTemplateMapper = contractTemplateMapper;
        this.tourContractMapper = tourContractMapper;
        this.merchantShopMapper = merchantShopMapper;
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
            throw new BizException(400, "路线不可预订");
        }

        TourDepartureDate departureDate = tourDepartureDateMapper.selectById(request.getDepartDateId());
        if (departureDate == null
                || !Objects.equals(departureDate.getRouteId(), route.getId())
                || !Objects.equals(departureDate.getStatus(), 1)
                || !Objects.equals(departureDate.getAuditStatus(), 1)) {
            throw new BizException(400, "出发日期无效");
        }
        if (departureDate.getDepartDate() == null || departureDate.getDepartDate().isBefore(LocalDate.now())) {
            throw new BizException(400, "出发日期已过期");
        }
        if (departureDate.getRemainCount() < request.getTravelerCount()) {
            throw new BizException(400, "库存不足");
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
            throw new BizException(404, "订单不存在");
        }
        return orderDetailMap(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TourOrder order = getOwnedOrder(id);
        if (isOverdueCancelled(order)) {
            throw new BizException(400, "订单已超时未支付，系统已自动取消");
        }
        if (!canCancelPendingPaymentOrder(order)) {
            throw new BizException(400, "当前状态不可取消");
        }
        if (!cancelPendingUnpaidOrder(order)) {
            TourOrder latestOrder = refreshOrderPaymentStateById(id);
            if (isOverdueCancelled(latestOrder)) {
                throw new BizException(400, "订单已超时未支付，系统已自动取消");
            }
            throw new BizException(400, "当前状态不可取消");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentPrepareVO pay(Long id) {
        TourOrder order = getOwnedOrder(id);
        if ("PAID".equals(order.getPayStatus())) {
            return buildPaidPaymentPrepare(order, latestPayRecord(order.getId()));
        }
        if (isOverdueCancelled(order)) {
            throw new BizException(400, "订单支付时限已过，系统已自动取消");
        }
        if (Boolean.TRUE.equals(order.getPaymentExpired())) {
            throw new BizException(400, "订单已超过支付时限，正在确认支付结果，请稍后刷新");
        }
        if (!canCancelPendingPaymentOrder(order)) {
            throw new BizException(400, "当前订单状态不可支付");
        }
        if (!"SIGNED".equals(order.getContractStatus())) {
            throw new BizException(400, "请先完成合同签署，再进入支付");
        }

        PayRecord payRecord = latestPayRecord(order.getId());
        if (payRecord != null && "WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            order = refreshOrderPaymentStateById(id);
            if (order != null && "PAID".equals(order.getPayStatus())) {
                return buildPaidPaymentPrepare(order, latestPayRecord(order.getId()));
            }
            if (isOverdueCancelled(order)) {
                throw new BizException(400, "订单支付时限已过，系统已自动取消");
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
                "豫途旅游订单 " + order.getOrderNo(),
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
        ContractTemplate template = contractTemplateMapper.selectOne(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, 1)
                .last("limit 1"));
        if (template == null) {
            throw new BizException(500, "未配置可用合同模板");
        }

        TourContract contract = new TourContract();
        contract.setContractNo(generateNo("CON"));
        contract.setOrderId(order.getId());
        contract.setTemplateId(template.getId());
        contract.setUserId(order.getUserId());
        contract.setMerchantId(order.getMerchantId());
        contract.setContractTitle("豫途旅游服务合同 " + order.getOrderNo());
        contract.setContractContent(template.getTemplateContent());
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
            throw new BizException(404, "订单不存在");
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
        order.setOrderStatus("COMPLETED");
        tourOrderMapper.updateById(order);
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
        return orders;
    }

    private void refreshOrderPaymentState(TourOrder order) {
        if (order == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!canExpirePendingPaymentOrder(order) || !hasPaymentExpired(order, now)) {
            refreshOrderPaymentWindow(order, now);
            return;
        }
        reconcileExpiredPaymentOrder(order);
        refreshOrderPaymentWindow(order, LocalDateTime.now());
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
                && "UNPAID".equals(order.getPayStatus());
    }

    private boolean canExpirePendingPaymentOrder(TourOrder order) {
        return canCancelPendingPaymentOrder(order);
    }

    private boolean isOverdueCancelled(TourOrder order) {
        return order != null
                && "CANCELLED".equals(order.getOrderStatus())
                && "UNPAID".equals(order.getPayStatus())
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
        if (order == null || !"UNPAID".equals(order.getPayStatus())) {
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
            throw new BizException(404, "订单不存在");
        }
        if (!"PAID".equals(order.getPayStatus())) {
            throw new BizException(400, "当前订单不可退款");
        }
        order.setOrderStatus("REFUNDING");
        tourOrderMapper.updateById(order);
        order.setOrderStatus("REFUNDED");
        order.setPayStatus("REFUNDED");
        tourOrderMapper.updateById(order);
    }

    public List<TourOrder> merchantOrders() {
        Long shopId = currentMerchantShopId();
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, shopId)
                .orderByDesc(TourOrder::getCreateTime));
        return decorateOrders(orders);
    }

    public Map<String, Object> merchantOrderDetail(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getMerchantId(), currentMerchantShopId())) {
            throw new BizException(404, "订单不存在");
        }
        return orderDetailMap(order);
    }

    public List<TourOrder> adminOrders() {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .orderByDesc(TourOrder::getCreateTime));
        return decorateOrders(orders);
    }

    public List<PayRecord> adminPayRecords() {
        return payRecordMapper.selectList(new LambdaQueryWrapper<PayRecord>()
                .orderByDesc(PayRecord::getCreateTime));
    }

    public void adminHandleOrderException(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(404, "订单不存在");
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
        Map<String, Object> map = new HashMap<>();
        map.put("order", order);
        map.put("travelers", travelers);
        map.put("contracts", contracts);
        return map;
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ((int) (Math.random() * 9000) + 1000);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "未登录");
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
            throw new BizException(400, "商家店铺不存在");
        }
        return shop.getId();
    }

    public void complete(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'complete'");
    }

    public void review(Long id, OrderReviewRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'review'");
    }
}
