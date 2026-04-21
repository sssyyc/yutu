package com.yutu.modules.merchant.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MerchantRouteDepartureDateVO {
    private Long id;
    private Long routeId;
    private String routeName;
    private Integer routeAuditStatus;
    private Integer routePublishStatus;
    private LocalDate departDate;
    private Integer remainCount;
    private BigDecimal salePrice;
    private Integer auditStatus;
    private String auditRemark;
    private Integer status;
}
