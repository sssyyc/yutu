package com.yutu.modules.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CategorySaveRequest {
    private Long parentId = 0L;
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    private Integer sortNum = 0;
    private Integer status = 1;
}
