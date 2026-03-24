package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.merchant.dto.MerchantRouteDatesSaveRequest;
import com.yutu.modules.merchant.dto.MerchantRouteSaveRequest;
import com.yutu.modules.merchant.service.MerchantService;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourRoute;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/routes")
public class MerchantRouteController {
    private final MerchantService merchantService;

    public MerchantRouteController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PreAuthorize("hasAuthority('merchant:route:create')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody MerchantRouteSaveRequest request) {
        return Result.ok(merchantService.createRoute(request));
    }

    @PreAuthorize("hasAuthority('merchant:route:update')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody MerchantRouteSaveRequest request) {
        merchantService.updateRoute(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('merchant:route:list')")
    @GetMapping
    public Result<List<TourRoute>> list() {
        return Result.ok(merchantService.routeList());
    }

    @PreAuthorize("hasAuthority('merchant:route:list')")
    @GetMapping("/{id}/dates")
    public Result<List<TourDepartureDate>> dates(@PathVariable Long id) {
        return Result.ok(merchantService.routeDates(id));
    }

    @PreAuthorize("hasAuthority('merchant:route:update')")
    @PutMapping("/{id}/dates")
    public Result<Void> updateDates(@PathVariable Long id, @Validated @RequestBody MerchantRouteDatesSaveRequest request) {
        merchantService.updateRouteDates(id, request.getDepartureDates());
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('merchant:route:publish')")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        merchantService.publish(id, true);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('merchant:route:publish')")
    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        merchantService.publish(id, false);
        return Result.ok();
    }
}
