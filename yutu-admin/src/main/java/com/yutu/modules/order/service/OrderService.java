package com.yutu.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public OrderService(TourOrderMapper tourOrderMapper,
            TourRouteMapper tourRouteMapper,
            TourDepartureDateMapper tourDepartureDateMapper,
            TourOrderTravelerMapper tourOrderTravelerMapper,
            PayRecordMapper payRecordMapper,
            ContractTemplateMapper contractTemplateMapper,
            TourContractMapper tourContractMapper,
            MerchantShopMapper merchantShopMapper,
            AlipaySandboxService alipaySandboxService,
            ObjectMapper objectMapper) {
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
            throw new BizException(400, "鍑哄彂鏃ユ湡宸茶繃鏈燂紝璇烽噸鏂伴€夋嫨");
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
        return tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, currentUserId())
                .orderByDesc(TourOrder::getCreateTime));
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
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getUserId(), currentUserId())) {
            throw new BizException(404, "订单不存在");
        }
        if (!"PENDING_PAY".equals(order.getOrderStatus())) {
            throw new BizException(400, "当前状态不可取消");
        }
        order.setOrderStatus("CANCELLED");
        tourOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentPrepareVO pay(Long id) {
        TourOrder order = getOwnedOrder(id);
        if ("PAID".equals(order.getPayStatus())) {
            return buildPaidPaymentPrepare(order, latestPayRecord(order.getId()));
        }
        if (!"SIGNED".equals(order.getContractStatus())) {
            throw new BizException(400, "请先完成合同签署，再进入支付");
        }

        PayRecord payRecord = latestPayRecord(order.getId());
        if (payRecord != null && "WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            order = tourOrderMapper.selectById(id);
            if ("PAID".equals(order.getPayStatus())) {
                return buildPaidPaymentPrepare(order, latestPayRecord(order.getId()));
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
                "豫途旅游订单-" + order.getOrderNo(),
                payNo,
                order.getPayAmount());

        PayRecord newPayRecord = new PayRecord();
        newPayRecord.setOrderId(order.getId());
        newPayRecord.setOrderNo(order.getOrderNo());
        newPayRecord.setPayNo(payNo);
        newPayRecord.setPayType("ALIPAY_SANDBOX");
        newPayRecord.setPayAmount(order.getPayAmount());
        newPayRecord.setPayStatus("WAIT_BUYER_PAY");
        newPayRecord
                .setCallbackContent(writePaymentSnapshot(precreateResult.getQrCode(), precreateResult.getQrCodeImage(),
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
        if (payRecord == null) {
            return buildPaymentStatus(order, null, false);
        }
        if (!"WAIT_BUYER_PAY".equals(payRecord.getPayStatus())) {
            return buildPaymentStatus(order, payRecord, "PAID".equals(order.getPayStatus()));
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
            payRecord.setPayStatus("FAILED");
            payRecord.setCallbackContent(mergePayRecordSnapshot(payRecord.getCallbackContent(), queryResult));
            payRecordMapper.updateById(payRecord);
            return buildPaymentStatus(order, payRecord, false);
        }
        if (queryResult.isSuccess()) {
            payRecord.setCallbackContent(mergePayRecordSnapshot(payRecord.getCallbackContent(), queryResult));
            payRecordMapper.updateById(payRecord);
        }
        return buildPaymentStatus(order, payRecord, false);
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
        contract.setContractTitle("豫途旅游服务合同-" + order.getOrderNo());
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
    }

    private String writePaymentSnapshot(String qrCode, String qrCodeImage, String rawBody) {
        Map<String, Object> payload = new HashMap<String, Object>();
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
            return new HashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return new HashMap<String, Object>();
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
        payRecord.setPayStatus("FAILED");
        payRecordMapper.updateById(payRecord);
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
        return tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, shopId)
                .orderByDesc(TourOrder::getCreateTime));
    }

    public Map<String, Object> merchantOrderDetail(Long id) {
        TourOrder order = tourOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getMerchantId(), currentMerchantShopId())) {
            throw new BizException(404, "订单不存在");
        }
        return orderDetailMap(order);
    }

    public List<TourOrder> adminOrders() {
        return tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .orderByDesc(TourOrder::getCreateTime));
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
        if ("PENDING_PAY".equals(order.getOrderStatus())) {
            order.setOrderStatus("CANCELLED");
            tourOrderMapper.updateById(order);
        }
    }

    private Map<String, Object> orderDetailMap(TourOrder order) {
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'complete'");
    }

    public void review(Long id, OrderReviewRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'review'");
    }
}
