package com.yutu.modules.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.merchant.dto.MerchantRouteDepartureDateRequest;
import com.yutu.modules.merchant.dto.MerchantRouteSaveRequest;
import com.yutu.modules.merchant.dto.MerchantShopUpdateRequest;
import com.yutu.modules.merchant.vo.MerchantRouteDepartureDateVO;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourReview;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.TourRouteTag;
import com.yutu.modules.model.entity.TourTag;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourReviewMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.model.mapper.TourRouteTagMapper;
import com.yutu.modules.model.mapper.TourTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MerchantService {
    private final MerchantShopMapper merchantShopMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final TourOrderMapper tourOrderMapper;
    private final SysUserMapper sysUserMapper;
    private final TourReviewMapper tourReviewMapper;
    private final TourRouteTagMapper tourRouteTagMapper;
    private final TourTagMapper tourTagMapper;

    public MerchantService(MerchantShopMapper merchantShopMapper,
            TourRouteMapper tourRouteMapper,
            TourDepartureDateMapper tourDepartureDateMapper,
            TourOrderMapper tourOrderMapper,
            SysUserMapper sysUserMapper,
            TourReviewMapper tourReviewMapper,
            TourRouteTagMapper tourRouteTagMapper,
            TourTagMapper tourTagMapper) {
        this.merchantShopMapper = merchantShopMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.sysUserMapper = sysUserMapper;
        this.tourReviewMapper = tourReviewMapper;
        this.tourRouteTagMapper = tourRouteTagMapper;
        this.tourTagMapper = tourTagMapper;
    }

    public MerchantShop getShop() {
        return currentShop();
    }

    public void updateShop(MerchantShopUpdateRequest request) {
        MerchantShop shop = currentShop();
        if (request.getContactPhone() != null) {
            shop.setContactPhone(request.getContactPhone().trim());
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription().trim());
        }
        merchantShopMapper.updateById(shop);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRoute(MerchantRouteSaveRequest request) {
        MerchantShop shop = currentShop();
        TourRoute route = new TourRoute();
        route.setMerchantId(shop.getId());
        route.setCategoryId(request.getCategoryId());
        route.setRouteName(request.getRouteName());
        route.setCoverImage(request.getCoverImage());
        route.setSummary(request.getSummary());
        route.setDetailContent(request.getDetailContent());
        route.setPrice(request.getPrice());
        route.setStock(request.getStock());
        route.setScore(new BigDecimal("5.0"));
        route.setAuditStatus(0);
        route.setAuditRemark(null);
        route.setPublishStatus(0);
        route.setStatus(1);
        route.setDeleted(0);
        tourRouteMapper.insert(route);
        syncDepartureDates(route.getId(), request.getDepartureDates());
        return route.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRoute(Long id, MerchantRouteSaveRequest request) {
        TourRoute route = ownedRoute(id);
        boolean routeBaseChanged = hasRouteBaseChanged(route, request);
        route.setCategoryId(request.getCategoryId());
        route.setRouteName(request.getRouteName());
        route.setCoverImage(request.getCoverImage());
        route.setSummary(request.getSummary());
        route.setDetailContent(request.getDetailContent());
        route.setPrice(request.getPrice());
        route.setStock(request.getStock());
        if (routeBaseChanged) {
            route.setAuditStatus(0);
            route.setAuditRemark(null);
        }
        tourRouteMapper.updateById(route);
        syncDepartureDates(route.getId(), request.getDepartureDates());
    }

    public List<TourRoute> routeList() {
        return tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getMerchantId, currentShop().getId())
                .orderByDesc(TourRoute::getUpdateTime));
    }

    public List<TourDepartureDate> routeDates(Long routeId) {
        TourRoute route = ownedRoute(routeId);
        return tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()
                .eq(TourDepartureDate::getRouteId, route.getId())
                .eq(TourDepartureDate::getStatus, 1)
                .orderByAsc(TourDepartureDate::getDepartDate));
    }

    public List<MerchantRouteDepartureDateVO> routeDepartureDates() {
        List<TourRoute> routes = routeList();
        if (routes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, TourRoute> routeMap = routes.stream()
                .collect(Collectors.toMap(TourRoute::getId, item -> item, (left, right) -> left, HashMap::new));
        List<Long> routeIds = new ArrayList<>(routeMap.keySet());

        return tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()
                        .in(TourDepartureDate::getRouteId, routeIds)
                        .eq(TourDepartureDate::getStatus, 1)
                        .orderByAsc(TourDepartureDate::getId))
                .stream()
                .map(item -> {
                    TourRoute route = routeMap.get(item.getRouteId());
                    MerchantRouteDepartureDateVO vo = new MerchantRouteDepartureDateVO();
                    vo.setId(item.getId());
                    vo.setRouteId(item.getRouteId());
                    vo.setRouteName(route == null ? null : route.getRouteName());
                    vo.setRouteAuditStatus(route == null ? null : route.getAuditStatus());
                    vo.setRoutePublishStatus(route == null ? null : route.getPublishStatus());
                    vo.setDepartDate(item.getDepartDate());
                    vo.setRemainCount(item.getRemainCount());
                    vo.setSalePrice(item.getSalePrice());
                    vo.setAuditStatus(item.getAuditStatus());
                    vo.setAuditRemark(item.getAuditRemark());
                    vo.setStatus(item.getStatus());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRouteDates(Long routeId, List<MerchantRouteDepartureDateRequest> requests) {
        ownedRoute(routeId);
        syncDepartureDates(routeId, requests);
    }

    public void publish(Long routeId, boolean publish) {
        TourRoute route = ownedRoute(routeId);
        if (publish && !Objects.equals(route.getAuditStatus(), 1)) {
            throw new BizException(400, "路线未审核通过，不能上架");
        }
        if (publish && countApprovedActiveDepartureDates(routeId) == 0L) {
            throw new BizException(400, "请至少保留一个审核通过的出发日期后再上架");
        }
        route.setPublishStatus(publish ? 1 : 0);
        tourRouteMapper.updateById(route);
    }

    public List<Map<String, Object>> customers(String keyword) {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, currentShop().getId()));
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = orders.stream().map(TourOrder::getUserId).collect(Collectors.toSet());
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
        Map<Long, Long> countMap = orders.stream()
                .collect(Collectors.groupingBy(TourOrder::getUserId, Collectors.counting()));
        List<Map<String, Object>> customers = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("phone", user.getPhone());
            map.put("orderCount", countMap.getOrDefault(user.getId(), 0L));
            return map;
        }).collect(Collectors.toList());
        if (!StringUtils.hasText(keyword)) {
            return customers;
        }
        String trimmedKeyword = keyword.trim();
        return customers.stream()
                .filter(item -> containsKeyword(item.get("username"), trimmedKeyword)
                        || containsKeyword(item.get("nickname"), trimmedKeyword)
                        || containsKeyword(item.get("phone"), trimmedKeyword))
                .collect(Collectors.toList());
    }

    public Map<String, Object> statsOverview() {
        Long shopId = currentShop().getId();
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, shopId));
        long totalOrders = orders.size();
        BigDecimal turnover = orders.stream()
                .filter(order -> "PAID".equals(order.getPayStatus()) || "REFUNDED".equals(order.getPayStatus()))
                .map(TourOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidOrders = orders.stream().filter(order -> "PAID".equals(order.getPayStatus())).count();
        Map<String, Object> map = new HashMap<>();
        map.put("totalOrders", totalOrders);
        map.put("paidOrders", paidOrders);
        map.put("turnover", turnover);
        map.put("refundOrders", orders.stream().filter(order -> "REFUNDED".equals(order.getPayStatus())).count());
        return map;
    }

    public List<Map<String, Object>> statsOrders() {
        Long shopId = currentShop().getId();
        List<TourRoute> routes = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getMerchantId, shopId));
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, shopId));
        Map<Long, Long> countByRoute = orders.stream()
                .collect(Collectors.groupingBy(TourOrder::getRouteId, Collectors.counting()));
        return routes.stream().map(route -> {
            Map<String, Object> row = new HashMap<>();
            row.put("routeId", route.getId());
            row.put("routeName", route.getRouteName());
            row.put("orderCount", countByRoute.getOrDefault(route.getId(), 0L));
            return row;
        }).collect(Collectors.toList());
    }

    public List<TourReview> statsReviews() {
        List<TourRoute> routes = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getMerchantId, currentShop().getId()));
        if (routes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> routeIds = routes.stream().map(TourRoute::getId).collect(Collectors.toList());
        return tourReviewMapper.selectList(new LambdaQueryWrapper<TourReview>()
                .in(TourReview::getRouteId, routeIds)
                .orderByDesc(TourReview::getCreateTime));
    }

    private void syncDepartureDates(Long routeId, List<MerchantRouteDepartureDateRequest> requests) {
        validateDepartureDates(requests);

        List<TourDepartureDate> existingDates = tourDepartureDateMapper
                .selectList(new LambdaQueryWrapper<TourDepartureDate>()
                        .eq(TourDepartureDate::getRouteId, routeId)
                        .orderByAsc(TourDepartureDate::getDepartDate));
        Map<Long, TourDepartureDate> existingDateMap = new HashMap<>();
        for (TourDepartureDate existingDate : existingDates) {
            if (existingDate.getId() != null) {
                existingDateMap.put(existingDate.getId(), existingDate);
            }
        }

        Set<Long> activeIds = new HashSet<>();
        for (MerchantRouteDepartureDateRequest item : requests) {
            TourDepartureDate entity;
            boolean changed = true;
            if (item.getId() != null) {
                entity = existingDateMap.get(item.getId());
                if (entity == null) {
                    throw new BizException(400, "出发日期记录不存在");
                }
                changed = hasDepartureDateChanged(entity, item);
            } else {
                entity = new TourDepartureDate();
                entity.setRouteId(routeId);
            }

            entity.setRouteId(routeId);
            entity.setDepartDate(item.getDepartDate());
            entity.setRemainCount(item.getRemainCount());
            entity.setSalePrice(item.getSalePrice());
            entity.setStatus(1);
            if (changed) {
                entity.setAuditStatus(0);
                entity.setAuditRemark(null);
            } else if (entity.getAuditStatus() == null) {
                entity.setAuditStatus(1);
            }

            if (entity.getId() == null) {
                tourDepartureDateMapper.insert(entity);
            } else {
                tourDepartureDateMapper.updateById(entity);
            }
            activeIds.add(entity.getId());
        }

        for (TourDepartureDate existingDate : existingDates) {
            if (existingDate.getId() == null || activeIds.contains(existingDate.getId())) {
                continue;
            }
            if (!Objects.equals(existingDate.getStatus(), 0)) {
                existingDate.setStatus(0);
                tourDepartureDateMapper.updateById(existingDate);
            }
        }
    }

    private void syncRouteTags(Long routeId, List<Long> requestTagIds) {
        List<Long> normalizedTagIds = normalizeTagIds(requestTagIds);
        if (!normalizedTagIds.isEmpty()) {
            List<TourTag> enabledTags = tourTagMapper.selectBatchIds(normalizedTagIds).stream()
                    .filter(tag -> Objects.equals(tag.getStatus(), 1))
                    .collect(Collectors.toList());
            if (enabledTags.size() != normalizedTagIds.size()) {
                throw new BizException(400, "请选择有效且已启用的路线标签");
            }
        }

        List<TourRouteTag> existingTags = tourRouteTagMapper.selectList(new LambdaQueryWrapper<TourRouteTag>()
                .eq(TourRouteTag::getRouteId, routeId));
        Set<Long> existingTagIds = existingTags.stream()
                .map(TourRouteTag::getTagId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long tagId : normalizedTagIds) {
            if (existingTagIds.contains(tagId)) {
                continue;
            }
            TourRouteTag relation = new TourRouteTag();
            relation.setRouteId(routeId);
            relation.setTagId(tagId);
            tourRouteTagMapper.insert(relation);
        }

        for (TourRouteTag relation : existingTags) {
            if (normalizedTagIds.contains(relation.getTagId())) {
                continue;
            }
            tourRouteTagMapper.deleteById(relation.getId());
        }
    }

    private List<Long> normalizeTagIds(List<Long> requestTagIds) {
        if (requestTagIds == null || requestTagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return requestTagIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateDepartureDates(List<MerchantRouteDepartureDateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException(400, "请至少添加一个出发日期");
        }

        Set<LocalDate> departDates = new HashSet<>();
        for (MerchantRouteDepartureDateRequest item : requests) {
            if (item == null) {
                throw new BizException(400, "出发日期配置不能为空");
            }
            if (item.getDepartDate() == null || item.getSalePrice() == null || item.getRemainCount() == null) {
                throw new BizException(400, "请完整填写出发日期、价格和余量");
            }
            if (item.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "出发价格不能小于0");
            }
            if (item.getRemainCount() < 0) {
                throw new BizException(400, "出发余量不能小于0");
            }
            if (!departDates.add(item.getDepartDate())) {
                throw new BizException(400, "出发日期不能重复");
            }
        }
    }

    private Long countApprovedActiveDepartureDates(Long routeId) {
        return tourDepartureDateMapper.selectCount(new LambdaQueryWrapper<TourDepartureDate>()
                .eq(TourDepartureDate::getRouteId, routeId)
                .eq(TourDepartureDate::getStatus, 1)
                .eq(TourDepartureDate::getAuditStatus, 1));
    }

    private boolean hasRouteBaseChanged(TourRoute route, MerchantRouteSaveRequest request) {
        return !Objects.equals(route.getCategoryId(), request.getCategoryId())
                || !Objects.equals(route.getRouteName(), request.getRouteName())
                || !Objects.equals(route.getCoverImage(), request.getCoverImage())
                || !Objects.equals(route.getSummary(), request.getSummary())
                || !Objects.equals(route.getDetailContent(), request.getDetailContent())
                || compareDecimal(route.getPrice(), request.getPrice()) != 0
                || !Objects.equals(route.getStock(), request.getStock());
    }

    private boolean hasDepartureDateChanged(TourDepartureDate entity, MerchantRouteDepartureDateRequest item) {
        return !Objects.equals(entity.getDepartDate(), item.getDepartDate())
                || !Objects.equals(entity.getRemainCount(), item.getRemainCount())
                || compareDecimal(entity.getSalePrice(), item.getSalePrice()) != 0;
    }

    private int compareDecimal(BigDecimal left, BigDecimal right) {
        BigDecimal leftValue = left == null ? BigDecimal.ZERO : left;
        BigDecimal rightValue = right == null ? BigDecimal.ZERO : right;
        return leftValue.compareTo(rightValue);
    }

    private boolean containsKeyword(Object source, String keyword) {
        return source != null && StringUtils.hasText(String.valueOf(source))
                && String.valueOf(source).contains(keyword);
    }

    private TourRoute ownedRoute(Long routeId) {
        TourRoute route = tourRouteMapper.selectById(routeId);
        if (route == null || !Objects.equals(route.getMerchantId(), currentShop().getId())) {
            throw new BizException(404, "路线不存在");
        }
        return route;
    }

    private MerchantShop currentShop() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        MerchantShop shop = merchantShopMapper.selectOne(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getUserId, userId)
                .eq(MerchantShop::getStatus, 1)
                .last("limit 1"));
        if (shop == null) {
            throw new BizException(400, "商家店铺不存在");
        }
        return shop;
    }
}
