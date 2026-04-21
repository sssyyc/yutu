package com.yutu.modules.route.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RouteReviewVO {
    private Long id;
    private Long orderId;
    private Long routeId;
    private Long userId;
    private String displayName;
    private String avatar;
    private Integer score;
    private String content;
    private LocalDateTime createTime;
}
