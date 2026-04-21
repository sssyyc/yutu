package com.yutu.modules.refund.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Data
public class RefundAdminArbitrateRequest {
    @NotBlank(message = "仲裁动作不能为空")
    private String action;
    private BigDecimal finalRefundAmount;
    private String note;
}
