package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.refund.dto.RefundExecuteRequest;
import com.yutu.modules.refund.dto.RefundMerchantReviewRequest;
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
@RequestMapping("/api/merchant/refunds")
public class MerchantRefundController {
    private final RefundService refundService;

    public MerchantRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PreAuthorize("hasAuthority('merchant:order:list') or principal.roleType == 2")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(refundService.detailForUser(id));
    }

    @PreAuthorize("hasAuthority('merchant:order:list') or principal.roleType == 2")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Validated @RequestBody RefundMerchantReviewRequest request) {
        refundService.merchantReview(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('merchant:order:list') or principal.roleType == 2")
    @PostMapping("/{id}/execute")
    public Result<Void> execute(@PathVariable Long id, @RequestBody RefundExecuteRequest request) {
        refundService.merchantExecute(id, request);
        return Result.ok();
    }
}
