package com.yutu.modules.contract.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.contract.dto.ContractSignRequest;
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
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PreAuthorize("hasAuthority('contract:list')")
    @GetMapping
    public Result<List<TourContract>> list() {
        return Result.ok(contractService.userContracts());
    }

    @PreAuthorize("hasAuthority('contract:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(contractService.userContractDetail(id));
    }

    @PreAuthorize("hasAuthority('contract:list')")
    @GetMapping("/{id}/download")
    public Result<Map<String, String>> download(@PathVariable Long id) {
        return Result.ok(contractService.userContractDownload(id));
    }

    @PreAuthorize("hasAuthority('contract:sign')")
    @PostMapping("/{id}/sign")
    public Result<Void> sign(@PathVariable Long id, @Validated @RequestBody ContractSignRequest request) {
        contractService.sign(id, request);
        return Result.ok();
    }
}
