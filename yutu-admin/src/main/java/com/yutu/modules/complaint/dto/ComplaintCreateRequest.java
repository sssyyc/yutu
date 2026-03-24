package com.yutu.modules.complaint.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ComplaintCreateRequest {
    @NotNull(message = "订单不能为空")
    private Long orderId;
    private String complaintType = "SERVICE";
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
}
