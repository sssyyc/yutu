package com.yutu.modules.complaint.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintRouteOptionVO {
    private Long orderId;
    private String orderNo;
    private Long routeId;
    private String routeName;
    private String coverImage;
    private LocalDateTime completedTime;
}
