package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_order")
public class TourOrder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long routeId;
    private Long departDateId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer travelerCount;
    private String orderStatus;
    private String payStatus;
    private String contractStatus;
    private String source;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Boolean hasReviewed;

    @TableField(exist = false)
    private LocalDateTime paymentExpireTime;

    @TableField(exist = false)
    private Long paymentRemainingSeconds;

    @TableField(exist = false)
    private Boolean paymentExpired;

    @TableField(exist = false)
    private Long paymentTimeoutMinutes;
}
