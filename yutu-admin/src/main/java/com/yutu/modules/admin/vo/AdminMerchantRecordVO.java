package com.yutu.modules.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminMerchantRecordVO {
    private Long merchantId;
    private String shopName;
    private Integer status;
    private Integer auditStatus;
    private String latestRemark;
    private long routeCount;
    private long publishedRouteCount;
    private long orderCount;
    private long paidOrderCount;
    private BigDecimal turnover;
    private long complaintCount;
    private long refundCount;
    private List<RouteItem> recentRoutes;
    private List<OrderItem> recentOrders;
    private List<ComplaintItem> recentComplaints;

    @Data
    public static class RouteItem {
        private Long id;
        private String routeName;
        private Integer auditStatus;
        private Integer publishStatus;
        private LocalDateTime updateTime;
    }

    @Data
    public static class OrderItem {
        private Long id;
        private String orderNo;
        private String orderStatus;
        private String payStatus;
        private BigDecimal payAmount;
        private LocalDateTime createTime;
    }

    @Data
    public static class ComplaintItem {
        private Long id;
        private String complaintNo;
        private String complaintType;
        private String status;
        private String title;
        private LocalDateTime createTime;
    }
}
