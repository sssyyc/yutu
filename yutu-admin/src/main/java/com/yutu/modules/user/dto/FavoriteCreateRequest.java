package com.yutu.modules.user.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class FavoriteCreateRequest {
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
    private String targetType = "ROUTE";
}
