package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.merchant.dto.MerchantShopUpdateRequest;
import com.yutu.modules.merchant.service.MerchantService;
import com.yutu.modules.model.entity.MerchantShop;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/shop")
public class MerchantShopController {
    private final MerchantService merchantService;

    public MerchantShopController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PreAuthorize("hasAuthority('merchant:shop:view')")
    @GetMapping
    public Result<MerchantShop> get() {
        return Result.ok(merchantService.getShop());
    }

    @PreAuthorize("hasAuthority('merchant:shop:update')")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody MerchantShopUpdateRequest request) {
        merchantService.updateShop(request);
        return Result.ok();
    }
}
