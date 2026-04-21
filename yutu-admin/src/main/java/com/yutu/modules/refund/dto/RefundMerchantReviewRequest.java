package com.yutu.modules.refund.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Data
public class RefundMerchantReviewRequest {
    @NotBlank(message = "处理动作不能为空")
    private String action;
    private BigDecimal tourFeeAmount;
    private BigDecimal insuranceFeeAmount;
    private BigDecimal visaFeeAmount;
    private BigDecimal lossFeeAmount;
    private BigDecimal proposedRefundAmount;
    private String note;
}
