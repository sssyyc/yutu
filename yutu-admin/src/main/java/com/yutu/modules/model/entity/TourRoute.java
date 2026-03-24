package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_route")
public class TourRoute extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long categoryId;
    private String routeName;
    private String coverImage;
    private String summary;
    private String detailContent;
    private BigDecimal price;
    private Integer stock;
    private BigDecimal score;
    private Integer auditStatus;
    private String auditRemark;
    private Integer publishStatus;
    private Integer status;
    @TableLogic
    private Integer deleted;
}
