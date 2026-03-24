package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_contract")
public class TourContract extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private Long orderId;
    private Long templateId;
    private Long userId;
    private Long merchantId;
    private String contractTitle;
    private String contractContent;
    private String signStatus;
    private LocalDateTime signTime;
    @TableLogic
    private Integer deleted;
}
