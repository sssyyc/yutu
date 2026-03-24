package com.yutu.modules.order.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.order.dto.OrderCreateRequest;
import com.yutu.modules.order.dto.OrderReviewRequest;
import com.yutu.modules.order.service.OrderService;
import com.yutu.modules.order.vo.OrderCreateVO;
import com.yutu.modules.order.vo.OrderPaymentPrepareVO;
import com.yutu.modules.order.vo.OrderPaymentStatusVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasAuthority('order:create')")
    @PostMapping
    public Result<OrderCreateVO> create(@Validated @RequestBody OrderCreateRequest request) {
        return Result.ok(orderService.create(request));
    }

    @PreAuthorize("hasAuthority('order:list')")
    @GetMapping
    public Result<List<TourOrder>> list() {
        return Result.ok(orderService.userOrders());
    }

    @PreAuthorize("hasAuthority('order:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(orderService.userOrderDetail(id));
    }

    @PreAuthorize("hasAuthority('order:cancel')")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('order:pay')")
    @PostMapping("/{id}/pay")
    public Result<OrderPaymentPrepareVO> pay(@PathVariable Long id) {
        return Result.ok(orderService.pay(id));
    }

    @PreAuthorize("hasAuthority('order:pay')")
    @GetMapping("/{id}/payment-status")
    public Result<OrderPaymentStatusVO> paymentStatus(@PathVariable Long id) {
        return Result.ok(orderService.queryPaymentStatus(id));
    }

    @PreAuthorize("hasAuthority('order:refund')")
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        orderService.refund(id);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('order:list')")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        orderService.complete(id);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('order:list')")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Validated @RequestBody OrderReviewRequest request) {
        orderService.review(id, request);
        return Result.ok();
    }
}
