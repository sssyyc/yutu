package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.refund.dto.RefundAdminArbitrateRequest;
import com.yutu.modules.refund.service.RefundService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {
    private final RefundService refundService;

    public AdminRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PreAuthorize("hasAuthority('admin:order:handle')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(refundService.detailForUser(id));
    }

    @PreAuthorize("hasAuthority('admin:order:handle')")
    @PostMapping("/{id}/arbitrate")
    public Result<Void> arbitrate(@PathVariable Long id, @Validated @RequestBody RefundAdminArbitrateRequest request) {
        refundService.adminArbitrate(id, request);
        return Result.ok();
    }
}
