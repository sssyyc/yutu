package com.yutu.modules.order.schedule;

import com.yutu.modules.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderPaymentExpiryScheduler.class);

    private final OrderService orderService;

    public OrderPaymentExpiryScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(initialDelay = 15000L, fixedDelay = 60000L)
    public void cancelExpiredOrders() {
        int cancelledCount = orderService.expireOverdueOrders();
        if (cancelledCount > 0) {
            log.info("auto cancelled {} overdue unpaid orders", cancelledCount);
        }
    }
}
