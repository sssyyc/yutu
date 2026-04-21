package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.contract.dto.ContractTemplateSaveRequest;
import com.yutu.modules.contract.service.ContractService;
import com.yutu.modules.model.entity.ContractTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/contract-templates")
public class AdminContractTemplateController {
    private final ContractService contractService;

    public AdminContractTemplateController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PreAuthorize("hasAuthority('admin:contract-template:list')")
    @GetMapping
    public Result<List<ContractTemplate>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(contractService.adminTemplateList(keyword));
    }

    @PreAuthorize("hasAuthority('admin:contract-template:manage')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody ContractTemplateSaveRequest request) {
        return Result.ok(contractService.adminCreateTemplate(request));
    }

    @PreAuthorize("hasAuthority('admin:contract-template:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody ContractTemplateSaveRequest request) {
        contractService.adminUpdateTemplate(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:contract-template:manage')")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        contractService.adminEnableTemplate(id, true);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:contract-template:manage')")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        contractService.adminEnableTemplate(id, false);
        return Result.ok();
    }
}
