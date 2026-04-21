package com.yutu.modules.refund.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundEstimateVO {
    private Long orderId;
    private String orderNo;
    private String routeName;
    private String refundType;
    private BigDecimal payAmount;
    private BigDecimal estimatedRefundAmount;
    private BigDecimal deductAmount;
    private String policyNote;
}
