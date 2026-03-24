package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("complaint_order")
public class ComplaintOrder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String complaintNo;
    private Long orderId;
    private Long contractId;
    private Long userId;
    private Long merchantId;
    private String complaintType;
    private String title;
    private String content;
    private String status;
    private String resultType;
    private String resultContent;
    @TableLogic
    private Integer deleted;
}
