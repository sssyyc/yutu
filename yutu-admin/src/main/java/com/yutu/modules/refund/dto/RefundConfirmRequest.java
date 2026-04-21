package com.yutu.modules.refund.dto;

import lombok.Data;

@Data
public class RefundConfirmRequest {
    private Boolean accepted;
    private String note;
}
