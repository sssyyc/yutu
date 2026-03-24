package com.yutu.modules.order.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OrderCreateRequest {
    @NotNull(message = "路线不能为空")
    private Long routeId;

    @NotNull(message = "出发日期不能为空")
    private Long departDateId;

    @NotNull(message = "出行人数不能为空")
    @Min(value = 1, message = "出行人数至少1人")
    private Integer travelerCount;

    @Valid
    private List<OrderTravelerItem> travelers;
}
