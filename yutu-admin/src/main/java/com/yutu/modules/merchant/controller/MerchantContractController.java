package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.contract.dto.ContractAppendixRequest;
import com.yutu.modules.contract.service.ContractService;
import com.yutu.modules.model.entity.TourContract;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/contracts")
public class MerchantContractController {
    private final ContractService contractService;

    public MerchantContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PreAuthorize("hasAuthority('merchant:contract:list')")
    @GetMapping
    public Result<List<TourContract>> list() {
        return Result.ok(contractService.merchantContracts());
    }

    @PreAuthorize("hasAuthority('merchant:contract:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(contractService.merchantContractDetail(id));
    }

    @PreAuthorize("hasAuthority('merchant:contract:appendix')")
    @PostMapping("/{id}/appendix")
    public Result<Long> append(@PathVariable Long id, @Validated @RequestBody ContractAppendixRequest request) {
        return Result.ok(contractService.appendContract(id, request));
    }
}
