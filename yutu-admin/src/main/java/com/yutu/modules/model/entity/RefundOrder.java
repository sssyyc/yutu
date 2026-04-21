package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_order")
public class RefundOrder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long routeId;
    private Long departDateId;
    private String refundType;
    private String refundReason;
    private String evidenceUrls;
    private String refundAccountType;
    private String refundAccountNo;
    private String originalOrderStatus;
    private String originalPayStatus;
    private BigDecimal expectedRefundAmount;
    private BigDecimal proposedRefundAmount;
    private BigDecimal finalRefundAmount;
    private BigDecimal deductAmount;
    private String feeBreakdownJson;
    private String policyNote;
    private String merchantNote;
    private String adminNote;
    private String executionNote;
    private String status;
    private LocalDateTime merchantDeadlineTime;
    private LocalDateTime merchantProcessedTime;
    private LocalDateTime adminDeadlineTime;
    private LocalDateTime refundProcessedTime;
    private LocalDateTime completedTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String routeName;

    @TableField(exist = false)
    private LocalDate departDate;

    @TableField(exist = false)
    private List<String> evidenceUrlList;
}
