package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_review")
public class TourReview extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Long orderId;
    private Long userId;
    private Integer score;
    private String content;
    private Integer status;
}
