package com.yutu.modules.home.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.context.UserContext;
import com.yutu.modules.home.vo.HomeReminderVO;
import com.yutu.modules.model.entity.SysBanner;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourReview;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.SysBannerMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourReviewMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HomeService {
    private final SysBannerMapper sysBannerMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final TourOrderMapper tourOrderMapper;
    private final TourReviewMapper tourReviewMapper;

    public HomeService(SysBannerMapper sysBannerMapper,
                       TourRouteMapper tourRouteMapper,
                       TourDepartureDateMapper tourDepartureDateMapper,
                       TourOrderMapper tourOrderMapper,
                       TourReviewMapper tourReviewMapper) {
        this.sysBannerMapper = sysBannerMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.tourReviewMapper = tourReviewMapper;
    }

    public List<SysBanner> banners() {
        return sysBannerMapper.selectList(new LambdaQueryWrapper<SysBanner>()
                .eq(SysBanner::getStatus, 1)
                .orderByAsc(SysBanner::getSortNum));
    }

    public List<TourRoute> hotRoutes() {
        return tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getStatus, 1)
                .eq(TourRoute::getPublishStatus, 1)
                .eq(TourRoute::getAuditStatus, 1)
                .orderByDesc(TourRoute::getScore)
                .last("limit 10"));
    }

    public List<TourRoute> recommendRoutes() {
        return tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getStatus, 1)
                .eq(TourRoute::getPublishStatus, 1)
                .eq(TourRoute::getAuditStatus, 1)
                .orderByDesc(TourRoute::getCreateTime)
                .last("limit 10"));
    }

    public List<TourReview> reviews() {
        return tourReviewMapper.selectList(new LambdaQueryWrapper<TourReview>()
                .eq(TourReview::getStatus, 1)
                .orderByDesc(TourReview::getCreateTime)
                .last("limit 20"));
    }

    public List<HomeReminderVO> reminders() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, userId)
                .eq(TourOrder::getPayStatus, "PAID")
                .notIn(TourOrder::getOrderStatus, Arrays.asList("CANCELLED", "REFUNDED"))
                .orderByDesc(TourOrder::getCreateTime));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TourDepartureDate> departureDateMap = loadDepartureDateMap(orders, today);
        if (departureDateMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TourRoute> routeMap = loadRouteMap(orders);
        if (routeMap.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(order -> buildReminder(order, routeMap.get(order.getRouteId()), departureDateMap.get(order.getDepartDateId()), today))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HomeReminderVO::getDepartDate).thenComparing(HomeReminderVO::getOrderId))
                .limit(6)
                .collect(Collectors.toList());
    }

    private Map<Long, TourDepartureDate> loadDepartureDateMap(List<TourOrder> orders, LocalDate today) {
        List<Long> departDateIds = orders.stream()
                .map(TourOrder::getDepartDateId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (departDateIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return tourDepartureDateMapper.selectBatchIds(departDateIds).stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .filter(item -> Objects.equals(item.getAuditStatus(), 1))
                .filter(item -> item.getDepartDate() != null && !item.getDepartDate().isBefore(today))
                .collect(Collectors.toMap(TourDepartureDate::getId, item -> item, (left, right) -> left, HashMap::new));
    }

    private Map<Long, TourRoute> loadRouteMap(List<TourOrder> orders) {
        List<Long> routeIds = orders.stream()
                .map(TourOrder::getRouteId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (routeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return tourRouteMapper.selectBatchIds(routeIds).stream()
                .filter(Objects::nonNull)
                .filter(route -> Objects.equals(route.getStatus(), 1))
                .filter(route -> Objects.equals(route.getPublishStatus(), 1))
                .filter(route -> Objects.equals(route.getAuditStatus(), 1))
                .collect(Collectors.toMap(TourRoute::getId, route -> route, (left, right) -> left, HashMap::new));
    }

    private HomeReminderVO buildReminder(TourOrder order, TourRoute route, TourDepartureDate departureDate, LocalDate today) {
        if (order == null || route == null || departureDate == null || departureDate.getDepartDate() == null) {
            return null;
        }

        long daysUntil = ChronoUnit.DAYS.between(today, departureDate.getDepartDate());

        HomeReminderVO vo = new HomeReminderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setRouteId(route.getId());
        vo.setRouteName(route.getRouteName());
        vo.setCoverImage(route.getCoverImage());
        vo.setDepartDate(departureDate.getDepartDate());
        vo.setTravelerCount(order.getTravelerCount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setContractStatus(order.getContractStatus());
        vo.setDaysUntil((int) daysUntil);
        vo.setReminderLevel(resolveReminderLevel(daysUntil));
        vo.setReminderTitle(resolveReminderTitle(daysUntil));
        vo.setReminderText(resolveReminderText(daysUntil));
        return vo;
    }

    private String resolveReminderLevel(long daysUntil) {
        if (daysUntil <= 1) {
            return "urgent";
        }
        if (daysUntil <= 3) {
            return "warning";
        }
        if (daysUntil <= 7) {
            return "notice";
        }
        return "plan";
    }

    private String resolveReminderTitle(long daysUntil) {
        if (daysUntil <= 0) {
            return "\u4eca\u65e5\u51fa\u53d1";
        }
        if (daysUntil == 1) {
            return "\u660e\u65e5\u51fa\u53d1";
        }
        if (daysUntil <= 3) {
            return "\u4e34\u8fd1\u51fa\u53d1";
        }
        if (daysUntil <= 7) {
            return "\u672c\u5468\u51fa\u53d1";
        }
        return "\u884c\u7a0b\u63d0\u9192";
    }

    private String resolveReminderText(long daysUntil) {
        if (daysUntil <= 0) {
            return "\u4f60\u5df2\u8d2d\u4e70\u7684\u884c\u7a0b\u4eca\u5929\u51fa\u53d1\uff0c\u8bf7\u7559\u610f\u96c6\u5408\u65f6\u95f4\u5e76\u63d0\u524d\u51c6\u5907\u597d\u8bc1\u4ef6\u3002";
        }
        if (daysUntil == 1) {
            return "\u4f60\u8d2d\u4e70\u7684\u884c\u7a0b\u660e\u5929\u51fa\u53d1\uff0c\u5efa\u8bae\u4eca\u665a\u518d\u6838\u5bf9\u51fa\u884c\u4eba\u3001\u8bc1\u4ef6\u548c\u884c\u674e\u3002";
        }
        if (daysUntil <= 3) {
            return "\u8ddd\u79bb\u51fa\u53d1\u8fd8\u6709 " + daysUntil + " \u5929\uff0c\u53ef\u4ee5\u63d0\u524d\u67e5\u770b\u8ba2\u5355\u3001\u5408\u540c\u548c\u51fa\u884c\u4eba\u4fe1\u606f\u3002";
        }
        if (daysUntil <= 7) {
            return "\u4f60\u5df2\u6709\u4e00\u8d9f\u4e00\u5468\u5185\u51fa\u53d1\u7684\u884c\u7a0b\uff0c\u53ef\u4ee5\u5f00\u59cb\u5b89\u6392\u4ea4\u901a\u4e0e\u884c\u524d\u7269\u54c1\u3002";
        }
        return "\u8fd9\u662f\u4f60\u5df2\u8d2d\u4e70\u7684\u540e\u7eed\u884c\u7a0b\uff0c\u53ef\u4ee5\u63d0\u524d\u89c4\u5212\u51fa\u53d1\u65f6\u95f4\u3002";
    }
}
