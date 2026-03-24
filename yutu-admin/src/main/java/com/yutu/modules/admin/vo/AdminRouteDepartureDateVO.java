package com.yutu.modules.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminRouteDepartureDateVO {
    private Long id;
    private Long routeId;
    private String routeName;
    private String routeSummary;
    private Integer routeAuditStatus;
    private Integer routePublishStatus;
    private LocalDate departDate;
    private Integer remainCount;
    private BigDecimal salePrice;
    private Integer auditStatus;
    private String auditRemark;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
