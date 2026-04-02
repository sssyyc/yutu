package com.yutu.modules.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.contract.dto.ContractAppendixRequest;
import com.yutu.modules.contract.dto.ContractSignRequest;
import com.yutu.modules.contract.dto.ContractTemplateSaveRequest;
import com.yutu.modules.model.entity.ContractTemplate;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourContract;
import com.yutu.modules.model.entity.TourContractAppendix;
import com.yutu.modules.model.entity.TourContractSignature;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourOrderTraveler;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourContractAppendixMapper;
import com.yutu.modules.model.mapper.TourContractMapper;
import com.yutu.modules.model.mapper.TourContractSignatureMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourOrderTravelerMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContractService {
    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final TourContractMapper tourContractMapper;
    private final TourContractAppendixMapper tourContractAppendixMapper;
    private final TourContractSignatureMapper tourContractSignatureMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final ContractTemplateMapper contractTemplateMapper;
    private final SysUserMapper sysUserMapper;
    private final TourOrderMapper tourOrderMapper;
    private final TourOrderTravelerMapper tourOrderTravelerMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final com.yutu.modules.order.service.OrderService orderService;

    public ContractService(TourContractMapper tourContractMapper,
                           TourContractAppendixMapper tourContractAppendixMapper,
                           TourContractSignatureMapper tourContractSignatureMapper,
                           MerchantShopMapper merchantShopMapper,
                           ContractTemplateMapper contractTemplateMapper,
                           SysUserMapper sysUserMapper,
                           TourOrderMapper tourOrderMapper,
                           TourOrderTravelerMapper tourOrderTravelerMapper,
                           TourRouteMapper tourRouteMapper,
                           TourDepartureDateMapper tourDepartureDateMapper,
                           com.yutu.modules.order.service.OrderService orderService) {
        this.tourContractMapper = tourContractMapper;
        this.tourContractAppendixMapper = tourContractAppendixMapper;
        this.tourContractSignatureMapper = tourContractSignatureMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.contractTemplateMapper = contractTemplateMapper;
        this.sysUserMapper = sysUserMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.tourOrderTravelerMapper = tourOrderTravelerMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.orderService = orderService;
    }

    public List<TourContract> userContracts() {
        return tourContractMapper.selectList(new LambdaQueryWrapper<TourContract>()
                .eq(TourContract::getUserId, currentUserId())
                .orderByDesc(TourContract::getCreateTime));
    }

    public Map<String, Object> userContractDetail(Long id) {
        TourContract contract = tourContractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getUserId(), currentUserId())) {
            throw new BizException(404, "Contract not found");
        }
        return contractMap(contract);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> userContractDownload(Long id) {
        TourContract contract = tourContractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getUserId(), currentUserId())) {
            throw new BizException(404, "Contract not found");
        }

        List<TourContractAppendix> appendices = tourContractAppendixMapper.selectList(
                new LambdaQueryWrapper<TourContractAppendix>()
                        .eq(TourContractAppendix::getContractId, contract.getId())
                        .orderByAsc(TourContractAppendix::getCreateTime)
        );

        ContractTemplate template = contractTemplateMapper.selectById(contract.getTemplateId());
        if (template != null) {
            try {
                contractTemplateMapper.incrementDownloadCount(template.getId());
            } catch (Exception ex) {
                log.warn("failed to increase contract template download count, templateId={}", template.getId(), ex);
            }
        }

        List<TourOrderTraveler> travelers = loadOrderTravelers(contract.getOrderId());
        List<TourContractSignature> signatures = loadContractSignatures(contract.getId());
        SignProgress progress = buildSignProgress(travelers, signatures);
        TourOrder order = contract.getOrderId() == null ? null : tourOrderMapper.selectById(contract.getOrderId());
        TourRoute route = order == null ? null : tourRouteMapper.selectById(order.getRouteId());
        TourDepartureDate departureDate = order == null ? null : tourDepartureDateMapper.selectById(order.getDepartDateId());
        SysUser user = sysUserMapper.selectById(contract.getUserId());
        MerchantShop merchantShop = merchantShopMapper.selectById(contract.getMerchantId());
        String renderedContent = ContractContentRenderer.render(
                contract.getContractContent(),
                contract,
                order,
                route,
                departureDate,
                user,
                merchantShop,
                travelers
        );

        StringBuilder content = new StringBuilder();
        content.append(contract.getContractTitle() == null ? "Contract" : contract.getContractTitle()).append("\n\n");
        content.append(renderedContent == null ? "" : renderedContent);

        if (progress.requiredSignCount > 0) {
            content.append("\n\n签署进度\n");
            content.append("已签署：").append(progress.signedCount).append("/").append(progress.requiredSignCount).append("\n");
            if (!progress.signedTravelerNames.isEmpty()) {
                content.append("已签署人：").append(String.join("、", progress.signedTravelerNames)).append("\n");
            }
            if (!progress.pendingTravelerNames.isEmpty()) {
                content.append("待签署人：").append(String.join("、", progress.pendingTravelerNames)).append("\n");
            }
        }

        if (!signatures.isEmpty()) {
            content.append("\n签署记录\n");
            for (int i = 0; i < signatures.size(); i++) {
                TourContractSignature signature = signatures.get(i);
                content.append(i + 1).append(". ");
                content.append(signature.getSignerName() == null ? "未命名签署人" : signature.getSignerName());
                content.append(" / ");
                content.append(signature.getSignTime() == null ? "-" : signature.getSignTime());
                content.append("\n");
            }
        }

        if (!appendices.isEmpty()) {
            content.append("\n\nAppendices\n");
            for (int i = 0; i < appendices.size(); i++) {
                TourContractAppendix appendix = appendices.get(i);
                content.append(i + 1).append(". ");
                content.append(appendix.getAppendixTitle() == null ? "Untitled appendix" : appendix.getAppendixTitle()).append("\n");
                if (appendix.getAppendixContent() != null) {
                    content.append(appendix.getAppendixContent());
                }
                content.append("\n");
            }
        }

        Map<String, String> data = new HashMap<>();
        data.put("fileName", (contract.getContractNo() == null ? "contract" : contract.getContractNo()) + ".txt");
        data.put("content", content.toString());
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sign(Long id, ContractSignRequest request) {
        TourContract contract = tourContractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getUserId(), currentUserId())) {
            throw new BizException(404, "Contract not found");
        }

        if (contract.getOrderId() != null) {
            TourOrder order = orderService.refreshOrderPaymentStateById(contract.getOrderId());
            if (order == null) {
                throw new BizException(404, "Order not found");
            }
            if ("CANCELLED".equals(order.getOrderStatus())) {
                if (Boolean.TRUE.equals(order.getPaymentExpired())) {
                    throw new BizException(400, "订单已超时未支付，不能继续签署合同");
                }
                throw new BizException(400, "订单已取消，不能继续签署合同");
            }
        }

        List<TourOrderTraveler> travelers = loadOrderTravelers(contract.getOrderId());
        List<TourContractSignature> signatures = loadContractSignatures(contract.getId());
        SignProgress progress = buildSignProgress(travelers, signatures);
        if (progress.requiredSignCount <= 0) {
            throw new BizException(400, "当前订单缺少出行人信息，无法签署合同");
        }
        if (progress.allSigned) {
            throw new BizException(400, "当前合同已完成全部签署");
        }

        TourOrderTraveler traveler = resolveSigningTraveler(request, travelers);
        if (progress.signedTravelerIds.contains(traveler.getId())) {
            throw new BizException(400, "该出行人已完成签署");
        }
        LocalDateTime signTime = LocalDateTime.now();

        TourContractSignature signature = new TourContractSignature();
        signature.setContractId(contract.getId());
        signature.setUserId(contract.getUserId());
        signature.setTravelerId(traveler.getId());
        signature.setSignerName(traveler.getTravelerName().trim());
        signature.setSignatureImage(request.getSignatureImage());
        signature.setSignTime(signTime);
        tourContractSignatureMapper.insert(signature);

        List<TourContractSignature> updatedSignatures = loadContractSignatures(contract.getId());
        SignProgress updatedProgress = buildSignProgress(travelers, updatedSignatures);
        contract.setSignStatus(updatedProgress.allSigned ? "SIGNED" : "UNSIGNED");
        contract.setSignTime(updatedProgress.allSigned ? signTime : null);
        tourContractMapper.updateById(contract);

        if (contract.getOrderId() != null) {
            com.yutu.modules.model.entity.TourOrder order = tourOrderMapper.selectById(contract.getOrderId());
            if (order != null) {
                order.setContractStatus(updatedProgress.allSigned ? "SIGNED" : "GENERATED");
                tourOrderMapper.updateById(order);
            }
        }
    }

    public List<TourContract> merchantContracts(String keyword) {
        Long shopId = currentMerchantShopId();
        LambdaQueryWrapper<TourContract> wrapper = new LambdaQueryWrapper<TourContract>()
                .eq(TourContract::getMerchantId, shopId)
                .orderByDesc(TourContract::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(q -> q.like(TourContract::getContractNo, trimmedKeyword)
                    .or()
                    .like(TourContract::getContractTitle, trimmedKeyword));
        }
        return tourContractMapper.selectList(wrapper);
    }

    public Map<String, Object> merchantContractDetail(Long id) {
        TourContract contract = tourContractMapper.selectById(id);
        if (contract == null || !Objects.equals(contract.getMerchantId(), currentMerchantShopId())) {
            throw new BizException(404, "Contract not found");
        }
        return contractMap(contract);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long appendContract(Long contractId, ContractAppendixRequest request) {
        TourContract contract = tourContractMapper.selectById(contractId);
        if (contract == null || !Objects.equals(contract.getMerchantId(), currentMerchantShopId())) {
            throw new BizException(404, "Contract not found");
        }
        TourContractAppendix appendix = new TourContractAppendix();
        appendix.setContractId(contractId);
        appendix.setAppendixTitle(request.getAppendixTitle());
        appendix.setAppendixContent(request.getAppendixContent());
        tourContractAppendixMapper.insert(appendix);
        return appendix.getId();
    }

    public List<ContractTemplate> adminTemplateList(String keyword) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<ContractTemplate>()
                .orderByDesc(ContractTemplate::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(q -> q.like(ContractTemplate::getTemplateName, trimmedKeyword)
                    .or()
                    .like(ContractTemplate::getTemplateCode, trimmedKeyword));
        }
        return contractTemplateMapper.selectList(wrapper);
    }

    public Long adminCreateTemplate(ContractTemplateSaveRequest request) {
        ContractTemplate template = new ContractTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateCode(request.getTemplateCode());
        template.setVersionNo(request.getVersionNo());
        template.setTemplateContent(request.getTemplateContent());
        template.setStatus(1);
        template.setRemark(request.getRemark());
        template.setDeleted(0);
        contractTemplateMapper.insert(template);
        return template.getId();
    }

    public void adminUpdateTemplate(Long id, ContractTemplateSaveRequest request) {
        ContractTemplate template = contractTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException(404, "Template not found");
        }
        template.setTemplateName(request.getTemplateName());
        template.setTemplateCode(request.getTemplateCode());
        template.setVersionNo(request.getVersionNo());
        template.setTemplateContent(request.getTemplateContent());
        template.setRemark(request.getRemark());
        contractTemplateMapper.updateById(template);
    }

    public void adminEnableTemplate(Long id, boolean enable) {
        ContractTemplate template = contractTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException(404, "Template not found");
        }
        template.setStatus(enable ? 1 : 0);
        contractTemplateMapper.updateById(template);
    }

    private Map<String, Object> contractMap(TourContract contract) {
        List<TourContractAppendix> appendices = tourContractAppendixMapper.selectList(
                new LambdaQueryWrapper<TourContractAppendix>()
                        .eq(TourContractAppendix::getContractId, contract.getId())
                        .orderByDesc(TourContractAppendix::getCreateTime)
        );
        List<TourOrderTraveler> travelers = loadOrderTravelers(contract.getOrderId());
        List<TourContractSignature> signatures = loadContractSignatures(contract.getId());
        List<String> travelerNames = extractTravelerNames(travelers);
        SignProgress progress = buildSignProgress(travelers, signatures);
        SysUser signer = sysUserMapper.selectById(contract.getUserId());
        TourOrder order = contract.getOrderId() == null ? null : tourOrderMapper.selectById(contract.getOrderId());
        TourRoute route = order == null ? null : tourRouteMapper.selectById(order.getRouteId());
        TourDepartureDate departureDate = order == null ? null : tourDepartureDateMapper.selectById(order.getDepartDateId());
        MerchantShop merchantShop = merchantShopMapper.selectById(contract.getMerchantId());
        contract.setContractContent(ContractContentRenderer.render(
                contract.getContractContent(),
                contract,
                order,
                route,
                departureDate,
                signer,
                merchantShop,
                travelers
        ));

        Map<String, Object> map = new HashMap<>();
        map.put("contract", contract);
        map.put("appendices", appendices);
        map.put("signature", signatures.isEmpty() ? null : signatures.get(signatures.size() - 1));
        map.put("signatures", signatures);
        map.put("travelers", travelers);
        map.put("travelerNames", travelerNames);
        map.put("signedTravelerIds", progress.signedTravelerIds);
        map.put("signedTravelerNames", progress.signedTravelerNames);
        map.put("pendingTravelers", progress.pendingTravelers);
        map.put("pendingTravelerNames", progress.pendingTravelerNames);
        map.put("requiredSignCount", progress.requiredSignCount);
        map.put("signedCount", progress.signedCount);
        map.put("pendingSignCount", progress.pendingSignCount);
        map.put("allSigned", progress.allSigned);
        map.put("signerDisplayName", buildSignerDisplayName(signer, travelerNames));
        return map;
    }

    private List<TourOrderTraveler> loadOrderTravelers(Long orderId) {
        if (orderId == null) {
            return new ArrayList<>();
        }
        return tourOrderTravelerMapper.selectList(new LambdaQueryWrapper<TourOrderTraveler>()
                .eq(TourOrderTraveler::getOrderId, orderId)
                .orderByAsc(TourOrderTraveler::getCreateTime)
                .orderByAsc(TourOrderTraveler::getId));
    }

    private List<TourContractSignature> loadContractSignatures(Long contractId) {
        if (contractId == null) {
            return new ArrayList<>();
        }
        return tourContractSignatureMapper.selectList(new LambdaQueryWrapper<TourContractSignature>()
                .eq(TourContractSignature::getContractId, contractId)
                .orderByAsc(TourContractSignature::getSignTime)
                .orderByAsc(TourContractSignature::getId));
    }

    private List<String> extractTravelerNames(List<TourOrderTraveler> travelers) {
        if (travelers == null || travelers.isEmpty()) {
            return new ArrayList<>();
        }
        return travelers.stream()
                .map(TourOrderTraveler::getTravelerName)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private TourOrderTraveler resolveSigningTraveler(ContractSignRequest request, List<TourOrderTraveler> travelers) {
        if (travelers == null || travelers.isEmpty()) {
            throw new BizException(400, "当前订单缺少出行人信息，无法签署合同");
        }
        TourOrderTraveler matchedTraveler = null;
        for (TourOrderTraveler traveler : travelers) {
            if (Objects.equals(traveler.getId(), request.getTravelerId())) {
                matchedTraveler = traveler;
                break;
            }
        }
        if (matchedTraveler == null || !hasText(matchedTraveler.getTravelerName())) {
            throw new BizException(400, "签署人必须为订单出行人的实际姓名");
        }
        String normalizedSignerName = request.getSignerName() == null ? "" : request.getSignerName().trim();
        String actualTravelerName = matchedTraveler.getTravelerName().trim();
        if (!actualTravelerName.equals(normalizedSignerName)) {
            throw new BizException(400, "签署人必须为订单出行人的实际姓名");
        }
        return matchedTraveler;
    }

    private SignProgress buildSignProgress(List<TourOrderTraveler> travelers, List<TourContractSignature> signatures) {
        List<TourOrderTraveler> safeTravelers = travelers == null ? new ArrayList<>() : travelers;
        List<TourContractSignature> safeSignatures = signatures == null ? new ArrayList<>() : signatures;

        Map<Long, TourOrderTraveler> travelerById = new LinkedHashMap<>();
        Map<String, List<TourOrderTraveler>> travelersByName = new LinkedHashMap<>();
        for (TourOrderTraveler traveler : safeTravelers) {
            if (traveler == null || traveler.getId() == null || !hasText(traveler.getTravelerName())) {
                continue;
            }
            travelerById.put(traveler.getId(), traveler);
            String travelerName = traveler.getTravelerName().trim();
            travelersByName.computeIfAbsent(travelerName, key -> new ArrayList<>()).add(traveler);
        }

        Set<Long> signedTravelerIds = new LinkedHashSet<>();
        List<String> signedTravelerNames = new ArrayList<>();
        for (TourContractSignature signature : safeSignatures) {
            if (signature == null) {
                continue;
            }
            Long travelerId = signature.getTravelerId();
            if (travelerId != null && travelerById.containsKey(travelerId)) {
                if (signedTravelerIds.add(travelerId)) {
                    signedTravelerNames.add(travelerById.get(travelerId).getTravelerName().trim());
                }
                continue;
            }

            if (!hasText(signature.getSignerName())) {
                continue;
            }
            List<TourOrderTraveler> sameNameTravelers = travelersByName.get(signature.getSignerName().trim());
            if (sameNameTravelers == null || sameNameTravelers.size() != 1) {
                continue;
            }
            TourOrderTraveler matchedTraveler = sameNameTravelers.get(0);
            if (matchedTraveler.getId() != null && signedTravelerIds.add(matchedTraveler.getId())) {
                signedTravelerNames.add(matchedTraveler.getTravelerName().trim());
            }
        }

        List<TourOrderTraveler> pendingTravelers = new ArrayList<>();
        for (TourOrderTraveler traveler : safeTravelers) {
            if (traveler == null || traveler.getId() == null || !hasText(traveler.getTravelerName())) {
                continue;
            }
            if (!signedTravelerIds.contains(traveler.getId())) {
                pendingTravelers.add(traveler);
            }
        }

        SignProgress progress = new SignProgress();
        progress.signedTravelerIds = new ArrayList<>(signedTravelerIds);
        progress.signedTravelerNames = signedTravelerNames;
        progress.pendingTravelers = pendingTravelers;
        progress.pendingTravelerNames = pendingTravelers.stream()
                .map(TourOrderTraveler::getTravelerName)
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        progress.requiredSignCount = travelerById.size();
        progress.signedCount = progress.signedTravelerIds.size();
        progress.pendingSignCount = Math.max(progress.requiredSignCount - progress.signedCount, 0);
        progress.allSigned = progress.requiredSignCount > 0 && progress.pendingSignCount == 0;
        return progress;
    }

    private String buildSignerDisplayName(SysUser user, List<String> travelerNames) {
        if (travelerNames != null && !travelerNames.isEmpty()) {
            return String.join("、", travelerNames);
        }
        if (user == null) {
            return "Tourist";
        }
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername();
        }
        return "Tourist";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class SignProgress {
        private List<Long> signedTravelerIds = new ArrayList<>();
        private List<String> signedTravelerNames = new ArrayList<>();
        private List<TourOrderTraveler> pendingTravelers = new ArrayList<>();
        private List<String> pendingTravelerNames = new ArrayList<>();
        private int requiredSignCount;
        private int signedCount;
        private int pendingSignCount;
        private boolean allSigned;
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "Unauthorized");
        }
        return userId;
    }

    private Long currentMerchantShopId() {
        MerchantShop shop = merchantShopMapper.selectOne(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getUserId, currentUserId())
                .eq(MerchantShop::getStatus, 1)
                .last("limit 1"));
        if (shop == null) {
            throw new BizException(400, "Merchant shop not found");
        }
        return shop.getId();
    }
}
