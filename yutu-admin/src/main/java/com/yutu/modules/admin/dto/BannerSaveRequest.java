package com.yutu.modules.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BannerSaveRequest {
    @NotBlank(message = "轮播标题不能为空")
    private String title;

    @NotBlank(message = "轮播图片不能为空")
    private String imageUrl;

    private String linkUrl;

    private Integer sortNum = 0;

    private Integer status = 1;
}
