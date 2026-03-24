package com.yutu.modules.merchant.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class MerchantRouteSaveRequest {
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "路线名称不能为空")
    private String routeName;

    private String coverImage;

    private String summary;

    private String detailContent;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private List<Long> tagIds = new ArrayList<>();

    @Valid
    @NotEmpty(message = "请至少添加一个出发日期")
    private List<MerchantRouteDepartureDateRequest> departureDates;
}

