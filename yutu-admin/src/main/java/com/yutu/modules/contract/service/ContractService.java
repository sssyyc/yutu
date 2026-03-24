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
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourContractAppendixMapper;
import com.yutu.modules.model.mapper.TourContractMapper;
import com.yutu.modules.model.mapper.TourContractSignatureMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public ContractService(TourContractMapper tourContractMapper,
                           TourContractAppendixMapper tourContractAppendixMapper,
                           TourContractSignatureMapper tourContractSignatureMapper,
                           MerchantShopMapper merchantShopMapper,
                           ContractTemplateMapper contractTemplateMapper,
                           SysUserMapper sysUserMapper,
                           TourOrderMapper tourOrderMapper) {
        this.tourContractMapper = tourContractMapper;
        this.tourContractAppendixMapper = tourContractAppendixMapper;
        this.tourContractSignatureMapper = tourContractSignatureMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.contractTemplateMapper = contractTemplateMapper;
        this.sysUserMapper = sysUserMapper;
        this.tourOrderMapper = tourOrderMapper;
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

        StringBuilder content = new StringBuilder();
        content.append(contract.getContractTitle() == null ? "Contract" : contract.getContractTitle()).append("\n\n");
        content.append(contract.getContractContent() == null ? "" : contract.getContractContent());

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
        if ("SIGNED".equals(contract.getSignStatus())) {
            throw new BizException(400, "Contract already signed");
        }

        TourContractSignature existed = tourContractSignatureMapper.selectOne(
                new LambdaQueryWrapper<TourContractSignature>()
                        .eq(TourContractSignature::getContractId, contract.getId())
                        .last("limit 1")
        );
        if (existed != null) {
            throw new BizException(400, "Signature already exists");
        }

        LocalDateTime signTime = LocalDateTime.now();

        TourContractSignature signature = new TourContractSignature();
        signature.setContractId(contract.getId());
        signature.setUserId(contract.getUserId());
        signature.setSignerName(request.getSignerName());
        signature.setSignatureImage(request.getSignatureImage());
        signature.setSignTime(signTime);
        tourContractSignatureMapper.insert(signature);

        contract.setSignStatus("SIGNED");
        contract.setSignTime(signTime);
        tourContractMapper.updateById(contract);

        if (contract.getOrderId() != null) {
            com.yutu.modules.model.entity.TourOrder order = tourOrderMapper.selectById(contract.getOrderId());
            if (order != null) {
                order.setContractStatus("SIGNED");
                tourOrderMapper.updateById(order);
            }
        }
    }

    public List<TourContract> merchantContracts() {
        Long shopId = currentMerchantShopId();
        return tourContractMapper.selectList(new LambdaQueryWrapper<TourContract>()
                .eq(TourContract::getMerchantId, shopId)
                .orderByDesc(TourContract::getCreateTime));
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

    public List<ContractTemplate> adminTemplateList() {
        return contractTemplateMapper.selectList(new LambdaQueryWrapper<ContractTemplate>()
                .orderByDesc(ContractTemplate::getUpdateTime));
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
        TourContractSignature signature = tourContractSignatureMapper.selectOne(
                new LambdaQueryWrapper<TourContractSignature>()
                        .eq(TourContractSignature::getContractId, contract.getId())
                        .last("limit 1")
        );
        SysUser signer = sysUserMapper.selectById(contract.getUserId());

        Map<String, Object> map = new HashMap<>();
        map.put("contract", contract);
        map.put("appendices", appendices);
        map.put("signature", signature);
        map.put("signerDisplayName", buildSignerDisplayName(signer));
        return map;
    }

    private String buildSignerDisplayName(SysUser user) {
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
