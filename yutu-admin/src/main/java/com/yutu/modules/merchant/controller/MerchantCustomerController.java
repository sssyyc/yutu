package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.merchant.service.MerchantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/customers")
public class MerchantCustomerController {
    private final MerchantService merchantService;

    public MerchantCustomerController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PreAuthorize("hasAuthority('merchant:customer:list')")
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(merchantService.customers(keyword));
    }
}
