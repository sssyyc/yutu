package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.dto.MerchantAuditRequest;
import com.yutu.modules.admin.service.AdminService;
import com.yutu.modules.admin.vo.AdminMerchantVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/merchants")
public class AdminMerchantController {
    private final AdminService adminService;

    public AdminMerchantController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:merchant:list')")
    @GetMapping
    public Result<List<AdminMerchantVO>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(adminService.merchants(keyword));
    }

    @PreAuthorize("hasAuthority('admin:merchant:audit')")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) MerchantAuditRequest request) {
        adminService.merchantAudit(id, true, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:merchant:audit')")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) MerchantAuditRequest request) {
        adminService.merchantAudit(id, false, request);
        return Result.ok();
    }
}
