package com.yutu.modules.merchant.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MerchantRouteDatesSaveRequest {
    @Valid
    @NotEmpty(message = "请至少添加一个出发日期")
    private List<MerchantRouteDepartureDateRequest> departureDates;
}
