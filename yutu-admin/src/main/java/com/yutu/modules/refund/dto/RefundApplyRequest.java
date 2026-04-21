package com.yutu.modules.refund.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

import java.util.List;

@Data
public class RefundApplyRequest {
    @NotBlank(message = "退款类型不能为空")
    private String refundType;
    @NotBlank(message = "退款原因不能为空")
    private String refundReason;
    private List<String> evidenceUrls;
    @NotBlank(message = "退款账户类型不能为空")
    private String refundAccountType;
    private String refundAccountNo;
}
