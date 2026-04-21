package com.yutu.modules.merchant.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MerchantRouteDepartureDateRequest {
    private Long id;

    @NotNull(message = "出发日期不能为空")
    private LocalDate departDate;

    @NotNull(message = "出发价格不能为空")
    @DecimalMin(value = "0.0", message = "出发价格不能小于0")
    private BigDecimal salePrice;

    @NotNull(message = "出发余量不能为空")
    @Min(value = 0, message = "出发余量不能小于0")
    private Integer remainCount;
}
