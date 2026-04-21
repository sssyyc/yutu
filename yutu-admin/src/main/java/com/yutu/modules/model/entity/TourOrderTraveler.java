package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tour_order_traveler")
public class TourOrderTraveler {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String travelerName;
    private String idCard;
    private String phone;
    @TableField("create_time")
    private LocalDateTime createTime;
}
