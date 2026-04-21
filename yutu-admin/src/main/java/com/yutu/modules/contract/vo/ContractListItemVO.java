package com.yutu.modules.contract.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContractListItemVO {
    private Long id;
    private String contractNo;
    private String contractTitle;
    private String routeName;
    private String merchantName;
    private String orderNo;
    private BigDecimal payAmount;
    private LocalDateTime signTime;
    private String lifecycleStatus;
    private String signStatus;
}
