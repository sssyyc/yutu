package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("refund_flow")
public class RefundFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long refundId;
    private Long operatorId;
    private String operatorRole;
    private String actionType;
    private String actionContent;
    @TableField("create_time")
    private LocalDateTime createTime;
}
