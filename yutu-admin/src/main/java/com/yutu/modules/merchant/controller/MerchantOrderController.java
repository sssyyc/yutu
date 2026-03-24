package com.yutu.modules.merchant.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.order.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/orders")
public class MerchantOrderController {
    private final OrderService orderService;

    public MerchantOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasAuthority('merchant:order:list')")
    @GetMapping
    public Result<List<TourOrder>> list() {
        return Result.ok(orderService.merchantOrders());
    }

    @PreAuthorize("hasAuthority('merchant:order:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(orderService.merchantOrderDetail(id));
    }
}
