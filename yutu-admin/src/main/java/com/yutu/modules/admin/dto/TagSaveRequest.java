package com.yutu.modules.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TagSaveRequest {
    @NotBlank(message = "标签名称不能为空")
    private String tagName;
    private String tagType;
    private Integer status = 1;
}
