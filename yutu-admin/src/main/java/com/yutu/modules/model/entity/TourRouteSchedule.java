package com.yutu.modules.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tour_route_schedule")
public class TourRouteSchedule extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Integer dayNo;
    private String title;
    private String content;
}
