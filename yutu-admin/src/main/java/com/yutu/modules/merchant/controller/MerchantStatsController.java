package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.merchant.service.MerchantService;
import com.yutu.modules.model.entity.TourReview;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/stats")
public class MerchantStatsController {
    private final MerchantService merchantService;

    public MerchantStatsController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PreAuthorize("hasAuthority('merchant:stats:view')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(merchantService.statsOverview());
    }

    @PreAuthorize("hasAuthority('merchant:stats:view')")
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders() {
        return Result.ok(merchantService.statsOrders());
    }

    @PreAuthorize("hasAuthority('merchant:stats:view')")
    @GetMapping("/reviews")
    public Result<List<TourReview>> reviews() {
        return Result.ok(merchantService.statsReviews());
    }
}
