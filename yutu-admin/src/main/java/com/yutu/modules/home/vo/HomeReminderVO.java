package com.yutu.modules.home.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HomeReminderVO {
    private Long orderId;
    private String orderNo;
    private Long routeId;
    private String routeName;
    private String coverImage;
    private LocalDate departDate;
    private Integer travelerCount;
    private String orderStatus;
    private String payStatus;
    private String contractStatus;
    private Integer daysUntil;
    private String reminderLevel;
    private String reminderTitle;
    private String reminderText;
}
