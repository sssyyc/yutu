package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_departure_date")
public class TourDepartureDate extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private LocalDate departDate;
    private Integer remainCount;
    private BigDecimal salePrice;
    private Integer status;
    private Integer auditStatus;
    private String auditRemark;
}
