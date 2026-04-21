package com.yutu.modules.order.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.refund.dto.RefundApplyRequest;
import com.yutu.modules.refund.dto.RefundConfirmRequest;
import com.yutu.modules.refund.dto.RefundSupplementRequest;
import com.yutu.modules.refund.service.RefundService;
import com.yutu.modules.refund.vo.RefundEstimateVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderRefundController {
    private final RefundService refundService;

    public OrderRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PreAuthorize("hasAuthority('order:refund')")
    @GetMapping("/{orderId}/refund-estimate")
    public Result<RefundEstimateVO> estimate(@PathVariable Long orderId, @RequestParam String refundType) {
        return Result.ok(refundService.estimateForUser(orderId, refundType));
    }

    @PreAuthorize("hasAuthority('order:refund')")
    @PostMapping("/{orderId}/refunds")
    public Result<Long> apply(@PathVariable Long orderId, @Validated @RequestBody RefundApplyRequest request) {
        return Result.ok(refundService.apply(orderId, request));
    }

    @PreAuthorize("hasAuthority('order:list')")
    @GetMapping("/refunds/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(refundService.detailForUser(id));
    }

    @PreAuthorize("hasAuthority('order:refund')")
    @PostMapping("/refunds/{id}/supplement")
    public Result<Void> supplement(@PathVariable Long id, @RequestBody RefundSupplementRequest request) {
        refundService.supplement(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('order:refund')")
    @PostMapping("/refunds/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody RefundConfirmRequest request) {
        refundService.confirmProposal(id, request);
        return Result.ok();
    }
}
