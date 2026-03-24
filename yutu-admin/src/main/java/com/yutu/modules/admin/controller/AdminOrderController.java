package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.PayRecord;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.order.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasAuthority('admin:order:list')")
    @GetMapping("/orders")
    public Result<List<TourOrder>> orders() {
        return Result.ok(orderService.adminOrders());
    }

    @PreAuthorize("hasAuthority('admin:pay:list')")
    @GetMapping("/pay-records")
    public Result<List<PayRecord>> payRecords() {
        return Result.ok(orderService.adminPayRecords());
    }

    @PreAuthorize("hasAuthority('admin:order:handle')")
    @PostMapping("/orders/{id}/handle-exception")
    public Result<Void> handle(@PathVariable Long id) {
        orderService.adminHandleOrderException(id);
        return Result.ok();
    }
}
