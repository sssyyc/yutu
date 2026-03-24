package com.yutu.modules.complaint.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ComplaintActionRequest {
    @NotBlank(message = "处理内容不能为空")
    private String content;
}
