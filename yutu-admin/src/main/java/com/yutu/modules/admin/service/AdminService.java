package com.yutu.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yutu.common.exception.BizException;
import com.yutu.modules.admin.dto.CategorySaveRequest;
import com.yutu.modules.admin.dto.MerchantAuditRequest;
import com.yutu.modules.admin.dto.TagSaveRequest;
import com.yutu.modules.admin.vo.AdminRouteDepartureDateVO;
import com.yutu.modules.admin.vo.AdminMerchantVO;
import com.yutu.modules.model.entity.ComplaintOrder;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourCategory;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.TourTag;
import com.yutu.modules.model.mapper.ComplaintOrderMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourCategoryMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.model.mapper.TourTagMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private static final Set<String> FIXED_HOLIDAYS = new HashSet<>(Arrays.asList(
            "01-01", "05-01", "05-02", "05-03", "10-01", "10-02", "10-03"
    ));

    private final SysUserMapper sysUserMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final TourCategoryMapper tourCategoryMapper;
    private final TourTagMapper tourTagMapper;
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final TourOrderMapper tourOrderMapper;
    private final ComplaintOrderMapper complaintOrderMapper;

    public AdminService(SysUserMapper sysUserMapper,
                        MerchantShopMapper merchantShopMapper,
                        TourCategoryMapper tourCategoryMapper,
                        TourTagMapper tourTagMapper,
                        TourRouteMapper tourRouteMapper,
                        TourDepartureDateMapper tourDepartureDateMapper,
                        TourOrderMapper tourOrderMapper,
                        ComplaintOrderMapper complaintOrderMapper) {
        this.sysUserMapper = sysUserMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.tourCategoryMapper = tourCategoryMapper;
        this.tourTagMapper = tourTagMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.complaintOrderMapper = complaintOrderMapper;
    }

    public List<SysUser> users(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getId);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(q -> q.like(SysUser::getUsername, trimmedKeyword)
                    .or()
                    .like(SysUser::getNickname, trimmedKeyword));
        }
        return sysUserMapper.selectList(wrapper);
    }

    public void updateUserStatus(Long id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        user.setDeleted(1);
        sysUserMapper.updateById(user);
    }

    public List<AdminMerchantVO> merchants(String keyword) {
        List<MerchantShop> merchants = merchantShopMapper.selectList(new LambdaQueryWrapper<MerchantShop>()
                .orderByAsc(MerchantShop::getId));
        List<Long> userIds = merchants.stream()
                .map(MerchantShop::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));

        List<AdminMerchantVO> result = merchants.stream().map(shop -> {
            AdminMerchantVO vo = new AdminMerchantVO();
            BeanUtils.copyProperties(shop, vo);
            SysUser user = userMap.get(shop.getUserId());
            if (user != null) {
                vo.setApplicantUsername(user.getUsername());
                vo.setApplicantNickname(user.getNickname());
                vo.setApplicantPhone(user.getPhone());
                vo.setRoleType(user.getRoleType());
            }
            return vo;
        }).collect(Collectors.toList());

        if (!StringUtils.hasText(keyword)) {
            return result;
        }
        String trimmedKeyword = keyword.trim();
        return result.stream()
                .filter(item -> containsKeyword(item.getApplicantUsername(), trimmedKeyword)
                        || containsKeyword(item.getApplicantNickname(), trimmedKeyword)
                        || containsKeyword(item.getShopName(), trimmedKeyword))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void merchantAudit(Long id, boolean approve, MerchantAuditRequest request) {
        MerchantShop merchantShop = merchantShopMapper.selectById(id);
        if (merchantShop == null) {
            throw new BizException(404, "商户申请不存在");
        }
        merchantShop.setAuditStatus(approve ? 1 : 2);
        merchantShop.setAuditRemark(request == null ? null : request.getAuditRemark());
        merchantShop.setAuditTime(LocalDateTime.now());
        merchantShopMapper.updateById(merchantShop);

        SysUser user = sysUserMapper.selectById(merchantShop.getUserId());
        if (user != null) {
            user.setRoleType(approve ? 2 : 1);
            sysUserMapper.updateById(user);
        }
    }

    public List<TourCategory> categories(String keyword) {
        LambdaQueryWrapper<TourCategory> wrapper = new LambdaQueryWrapper<TourCategory>()
                .orderByAsc(TourCategory::getSortNum);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(TourCategory::getCategoryName, keyword.trim());
        }
        return tourCategoryMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategorySaveRequest request) {
        TourCategory category = new TourCategory();
        category.setParentId(request.getParentId());
        category.setCategoryName(request.getCategoryName());
        category.setSortNum(request.getSortNum());
        category.setStatus(request.getStatus());
        category.setDeleted(0);
        tourCategoryMapper.insert(category);
        return category.getId();
    }

    public void updateCategory(Long id, CategorySaveRequest request) {
        TourCategory category = tourCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(404, "分类不存在");
        }
        category.setParentId(request.getParentId());
        category.setCategoryName(request.getCategoryName());
        category.setSortNum(request.getSortNum());
        category.setStatus(request.getStatus());
        tourCategoryMapper.updateById(category);
    }

    public void deleteCategory(Long id) {
        tourCategoryMapper.deleteById(id);
    }

    public List<TourTag> tags(String keyword) {
        LambdaQueryWrapper<TourTag> wrapper = new LambdaQueryWrapper<TourTag>()
                .orderByDesc(TourTag::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(q -> q.like(TourTag::getTagName, trimmedKeyword)
                    .or()
                    .like(TourTag::getTagType, trimmedKeyword));
        }
        return tourTagMapper.selectList(wrapper);
    }

    public Long createTag(TagSaveRequest request) {
        TourTag tag = new TourTag();
        tag.setTagName(request.getTagName());
        tag.setTagType(request.getTagType());
        tag.setStatus(request.getStatus());
        tag.setDeleted(0);
        tourTagMapper.insert(tag);
        return tag.getId();
    }

    public void updateTag(Long id, TagSaveRequest request) {
        TourTag tag = tourTagMapper.selectById(id);
        if (tag == null) {
            throw new BizException(404, "标签不存在");
        }
        tag.setTagName(request.getTagName());
        tag.setTagType(request.getTagType());
        tag.setStatus(request.getStatus());
        tourTagMapper.updateById(tag);
    }

    public void deleteTag(Long id) {
        tourTagMapper.deleteById(id);
    }

    public List<TourRoute> routes(String keyword) {
        LambdaQueryWrapper<TourRoute> wrapper = new LambdaQueryWrapper<TourRoute>()
                .orderByAsc(TourRoute::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(TourRoute::getRouteName, keyword.trim());
        }
        return tourRouteMapper.selectList(wrapper);
    }

    public List<TourDepartureDate> routeDepartureDatesByRoute(Long routeId) {
        TourRoute route = tourRouteMapper.selectById(routeId);
        if (route == null) {
            throw new BizException(404, "路线不存在");
        }
        return tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()
                .eq(TourDepartureDate::getRouteId, routeId)
                .eq(TourDepartureDate::getStatus, 1)
                .orderByAsc(TourDepartureDate::getDepartDate));
    }

    public List<AdminRouteDepartureDateVO> routeDepartureDates() {
        List<TourDepartureDate> departureDates = tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()
                .eq(TourDepartureDate::getStatus, 1)
                .orderByAsc(TourDepartureDate::getId));
        if (departureDates.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> routeIds = departureDates.stream()
                .map(TourDepartureDate::getRouteId)
                .collect(Collectors.toSet());
        Map<Long, TourRoute> routeMap = tourRouteMapper.selectBatchIds(routeIds).stream()
                .collect(Collectors.toMap(TourRoute::getId, route -> route, (a, b) -> a));
        return departureDates.stream().map(item -> {
            AdminRouteDepartureDateVO vo = new AdminRouteDepartureDateVO();
            vo.setId(item.getId());
            vo.setRouteId(item.getRouteId());
            vo.setDepartDate(item.getDepartDate());
            vo.setRemainCount(item.getRemainCount());
            vo.setSalePrice(item.getSalePrice());
            vo.setAuditStatus(item.getAuditStatus());
            vo.setAuditRemark(item.getAuditRemark());
            vo.setStatus(item.getStatus());
            vo.setCreateTime(item.getCreateTime());
            vo.setUpdateTime(item.getUpdateTime());
            TourRoute route = routeMap.get(item.getRouteId());
            if (route != null) {
                vo.setRouteName(route.getRouteName());
                vo.setRouteSummary(route.getSummary());
                vo.setRouteAuditStatus(route.getAuditStatus());
                vo.setRoutePublishStatus(route.getPublishStatus());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    public void routeAudit(Long id, String action, String auditRemark) {
        TourRoute route = tourRouteMapper.selectById(id);
        if (route == null) {
            throw new BizException(404, "路线不存在");
        }
        if ("approve".equals(action)) {
            tourRouteMapper.update(null, new LambdaUpdateWrapper<TourRoute>()
                    .eq(TourRoute::getId, id)
                    .set(TourRoute::getAuditStatus, 1)
                    .set(TourRoute::getAuditRemark, null));
        } else if ("reject".equals(action)) {
            if (!StringUtils.hasText(auditRemark)) {
                throw new BizException(400, "请填写驳回修改意见");
            }
            tourRouteMapper.update(null, new LambdaUpdateWrapper<TourRoute>()
                    .eq(TourRoute::getId, id)
                    .set(TourRoute::getAuditStatus, 2)
                    .set(TourRoute::getPublishStatus, 0)
                    .set(TourRoute::getAuditRemark, auditRemark.trim()));
        } else if ("offline".equals(action)) {
            tourRouteMapper.update(null, new LambdaUpdateWrapper<TourRoute>()
                    .eq(TourRoute::getId, id)
                    .set(TourRoute::getPublishStatus, 0));
        } else {
            throw new BizException(400, "不支持的审核操作");
        }
    }

    public void routeDepartureDateAudit(Long id, String action, String auditRemark) {
        TourDepartureDate departureDate = tourDepartureDateMapper.selectById(id);
        if (departureDate == null || !Objects.equals(departureDate.getStatus(), 1)) {
            throw new BizException(404, "出发日期不存在");
        }
        if ("approve".equals(action)) {
            tourDepartureDateMapper.update(null, new LambdaUpdateWrapper<TourDepartureDate>()
                    .eq(TourDepartureDate::getId, id)
                    .set(TourDepartureDate::getAuditStatus, 1)
                    .set(TourDepartureDate::getAuditRemark, null));
        } else if ("reject".equals(action)) {
            if (!StringUtils.hasText(auditRemark)) {
                throw new BizException(400, "请填写驳回修改意见");
            }
            tourDepartureDateMapper.update(null, new LambdaUpdateWrapper<TourDepartureDate>()
                    .eq(TourDepartureDate::getId, id)
                    .set(TourDepartureDate::getAuditStatus, 2)
                    .set(TourDepartureDate::getAuditRemark, auditRemark.trim()));
        } else {
            throw new BizException(400, "不支持的出发日期审核操作");
        }
    }

    public Map<String, Object> dashboardOverview() {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>());
        List<MerchantShop> merchants = merchantShopMapper.selectList(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getAuditStatus, 1));
        List<TourRoute> routes = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getPublishStatus, 1)
                .eq(TourRoute::getAuditStatus, 1));
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>());
        List<ComplaintOrder> complaints = complaintOrderMapper.selectList(new LambdaQueryWrapper<ComplaintOrder>());

        BigDecimal turnover = orders.stream()
                .filter(order -> Objects.equals("PAID", order.getPayStatus()) || Objects.equals("REFUNDED", order.getPayStatus()))
                .map(TourOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> map = new HashMap<>();
        map.put("userCount", users.size());
        map.put("merchantCount", merchants.size());
        map.put("routeCount", routes.size());
        map.put("orderCount", orders.size());
        map.put("turnover", turnover);
        map.put("complaintCount", complaints.size());
        map.put("warning", complaints.stream().filter(c -> !"FINISHED".equals(c.getStatus())).count());
        return map;
    }

    public List<Map<String, Object>> orderTrend() {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>());
        Map<LocalDate, Long> map = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreateTime().toLocalDate(), Collectors.counting()));
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", entry.getKey().toString());
                    row.put("count", entry.getValue());
                    return row;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> complaintTrend() {
        List<ComplaintOrder> complaints = complaintOrderMapper.selectList(new LambdaQueryWrapper<ComplaintOrder>());
        Map<LocalDate, Long> map = complaints.stream()
                .collect(Collectors.groupingBy(complaint -> complaint.getCreateTime().toLocalDate(), Collectors.counting()));
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", entry.getKey().toString());
                    row.put("count", entry.getValue());
                    return row;
                }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> topRoutes() {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>());
        List<TourRoute> routes = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>());
        Map<Long, String> routeNameMap = routes.stream()
                .collect(Collectors.toMap(TourRoute::getId, TourRoute::getRouteName, (a, b) -> a));
        Map<Long, Long> countMap = orders.stream()
                .collect(Collectors.groupingBy(TourOrder::getRouteId, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((routeId, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("routeId", routeId);
            row.put("routeName", routeNameMap.getOrDefault(routeId, "未知路线"));
            row.put("orderCount", count);
            result.add(row);
        });
        return result.stream()
                .sorted(Comparator.comparing((Map<String, Object> row) -> (Long) row.get("orderCount")).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    public Map<String, Object> merchantAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<MerchantShop> merchants = merchantShopMapper.selectList(new LambdaQueryWrapper<MerchantShop>());
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>());
        List<ComplaintOrder> complaints = complaintOrderMapper.selectList(new LambdaQueryWrapper<ComplaintOrder>());

        Set<Long> approvedMerchantIds = merchants.stream()
                .filter(shop -> Objects.equals(shop.getAuditStatus(), 1))
                .map(MerchantShop::getId)
                .collect(Collectors.toSet());

        List<TourOrder> paidOrders = orders.stream()
                .filter(this::isPaidOrder)
                .filter(order -> inRange(order.getCreateTime(), start, end))
                .collect(Collectors.toList());
        Set<Long> activeMerchantIds = paidOrders.stream()
                .map(TourOrder::getMerchantId)
                .collect(Collectors.toSet());
        long activeApprovedMerchantCount = approvedMerchantIds.stream()
                .filter(activeMerchantIds::contains)
                .count();

        long newMerchantCount = merchants.stream()
                .filter(shop -> inRange(shop.getCreateTime(), start, end))
                .count();

        Map<Long, String> merchantNameMap = merchants.stream()
                .collect(Collectors.toMap(MerchantShop::getId, shop -> defaultIfBlank(shop.getShopName(), "未知商户"), (a, b) -> a));

        Map<Long, BigDecimal> amountByMerchant = new HashMap<>();
        for (TourOrder order : paidOrders) {
            amountByMerchant.merge(order.getMerchantId(), nullSafeAmount(order.getPayAmount()), BigDecimal::add);
        }
        List<Map<String, Object>> transactionRank = amountByMerchant.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("merchantId", entry.getKey());
                    row.put("merchantName", merchantNameMap.getOrDefault(entry.getKey(), "未知商户"));
                    row.put("amount", entry.getValue());
                    return row;
                })
                .sorted((a, b) -> toBigDecimal(b.get("amount")).compareTo(toBigDecimal(a.get("amount"))))
                .limit(10)
                .collect(Collectors.toList());

        Map<Long, Long> complaintByMerchant = complaints.stream()
                .filter(complaint -> inRange(complaint.getCreateTime(), start, end))
                .collect(Collectors.groupingBy(ComplaintOrder::getMerchantId, Collectors.counting()));
        List<Map<String, Object>> complaintRank = complaintByMerchant.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("merchantId", entry.getKey());
                    row.put("merchantName", merchantNameMap.getOrDefault(entry.getKey(), "未知商户"));
                    row.put("complaintCount", entry.getValue());
                    return row;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("complaintCount"), (Long) a.get("complaintCount")))
                .limit(10)
                .collect(Collectors.toList());

        List<LocalDateTime> settlementTimeList = merchants.stream()
                .map(MerchantShop::getCreateTime)
                .collect(Collectors.toList());
        List<Map<String, Object>> settlementTrend = buildDailyCountTrend(start, end, settlementTimeList);

        Map<LocalDate, Set<Long>> activeMerchantByDate = new HashMap<>();
        for (TourOrder order : paidOrders) {
            LocalDate date = order.getCreateTime().toLocalDate();
            Set<Long> merchantIdSet = activeMerchantByDate.computeIfAbsent(date, key -> new HashSet<Long>());
            merchantIdSet.add(order.getMerchantId());
        }
        List<Map<String, Object>> activeTrend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", date.toString());
            row.put("count", activeMerchantByDate.containsKey(date) ? activeMerchantByDate.get(date).size() : 0);
            activeTrend.add(row);
        }

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalMerchantCount", merchants.size());
        overview.put("approvedMerchantCount", approvedMerchantIds.size());
        overview.put("newMerchantCount", newMerchantCount);
        overview.put("activeMerchantCount", activeApprovedMerchantCount);
        overview.put("activeRatio", toPercent(activeApprovedMerchantCount, approvedMerchantIds.size()));

        Map<String, Object> result = new HashMap<>();
        result.put("rangeStart", start.toString());
        result.put("rangeEnd", end.toString());
        result.put("overview", overview);
        result.put("settlementTrend", settlementTrend);
        result.put("activeTrend", activeTrend);
        result.put("transactionRank", transactionRank);
        result.put("complaintRank", complaintRank);
        return result;
    }

    public Map<String, Object> customerAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<SysUser> userList = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleType, 1));
        List<TourOrder> orderList = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>());
        List<TourRoute> routeList = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>());
        List<TourCategory> categoryList = tourCategoryMapper.selectList(new LambdaQueryWrapper<TourCategory>());

        Set<Long> userIdSet = userList.stream().map(SysUser::getId).collect(Collectors.toSet());
        List<TourOrder> userOrdersInRange = orderList.stream()
                .filter(order -> userIdSet.contains(order.getUserId()))
                .filter(order -> inRange(order.getCreateTime(), start, end))
                .collect(Collectors.toList());
        List<TourOrder> paidOrdersInRange = userOrdersInRange.stream()
                .filter(this::isPaidOrder)
                .collect(Collectors.toList());

        Set<Long> activeUserIds = userOrdersInRange.stream()
                .map(TourOrder::getUserId)
                .collect(Collectors.toSet());

        List<LocalDateTime> registerTimeList = userList.stream()
                .map(SysUser::getCreateTime)
                .collect(Collectors.toList());
        List<Map<String, Object>> registerTrend = buildDailyCountTrend(start, end, registerTimeList);

        Map<String, Long> regionCountMap = userList.stream()
                .collect(Collectors.groupingBy(user -> resolveRegionByPhone(user.getPhone()), Collectors.counting()));
        List<Map<String, Object>> regionDistribution = regionCountMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("region", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .limit(10)
                .collect(Collectors.toList());

        Map<Long, TourRoute> routeMap = routeList.stream()
                .collect(Collectors.toMap(TourRoute::getId, route -> route, (a, b) -> a));
        Map<Long, String> categoryNameMap = categoryList.stream()
                .collect(Collectors.toMap(TourCategory::getId, category -> defaultIfBlank(category.getCategoryName(), "未知品类"), (a, b) -> a));

        Map<Long, Long> preferenceCountMap = new HashMap<>();
        Map<Long, BigDecimal> preferenceAmountMap = new HashMap<>();
        for (TourOrder order : paidOrdersInRange) {
            TourRoute route = routeMap.get(order.getRouteId());
            if (route == null) {
                continue;
            }
            Long categoryId = route.getCategoryId();
            preferenceCountMap.merge(categoryId, 1L, Long::sum);
            preferenceAmountMap.merge(categoryId, nullSafeAmount(order.getPayAmount()), BigDecimal::add);
        }
        List<Map<String, Object>> preferenceRank = preferenceCountMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("categoryId", entry.getKey());
                    row.put("categoryName", categoryNameMap.getOrDefault(entry.getKey(), "未知品类"));
                    row.put("orderCount", entry.getValue());
                    row.put("amount", preferenceAmountMap.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                    return row;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("orderCount"), (Long) a.get("orderCount")))
                .limit(10)
                .collect(Collectors.toList());

        Map<Long, Long> paidCountByUser = paidOrdersInRange.stream()
                .collect(Collectors.groupingBy(TourOrder::getUserId, Collectors.counting()));
        long paidUserCount = paidCountByUser.size();
        long repeatUserCount = paidCountByUser.values().stream().filter(count -> count >= 2).count();
        Map<Long, BigDecimal> paidAmountByUser = new HashMap<>();
        for (TourOrder order : paidOrdersInRange) {
            paidAmountByUser.merge(order.getUserId(), nullSafeAmount(order.getPayAmount()), BigDecimal::add);
        }
        long highValueUserCount = paidAmountByUser.values().stream()
                .filter(amount -> amount.compareTo(new BigDecimal("2000")) >= 0)
                .count();

        BigDecimal totalAmount = paidOrdersInRange.stream()
                .map(TourOrder::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOrderAmount = paidOrdersInRange.isEmpty()
                ? BigDecimal.ZERO
                : totalAmount.divide(new BigDecimal(paidOrdersInRange.size()), 2, BigDecimal.ROUND_HALF_UP);

        String topRegion = regionDistribution.isEmpty() ? "暂无" : String.valueOf(regionDistribution.get(0).get("region"));
        String topPreference = preferenceRank.isEmpty() ? "暂无" : String.valueOf(preferenceRank.get(0).get("categoryName"));

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUserCount", userList.size());
        overview.put("newRegisterCount", userList.stream().filter(user -> inRange(user.getCreateTime(), start, end)).count());
        overview.put("activeUserCount", activeUserIds.size());
        overview.put("activeRate", toPercent(activeUserIds.size(), userList.size()));
        overview.put("repeatPurchaseRate", toPercent(repeatUserCount, paidUserCount));
        overview.put("avgOrderAmount", avgOrderAmount);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("topRegion", topRegion);
        profile.put("topPreference", topPreference);
        profile.put("highValueUserCount", highValueUserCount);
        profile.put("repeatPurchaseUserCount", repeatUserCount);
        profile.put("paidUserCount", paidUserCount);

        Map<String, Object> result = new HashMap<>();
        result.put("rangeStart", start.toString());
        result.put("rangeEnd", end.toString());
        result.put("overview", overview);
        result.put("profile", profile);
        result.put("registerTrend", registerTrend);
        result.put("regionDistribution", regionDistribution);
        result.put("preferenceRank", preferenceRank);
        return result;
    }

    public Map<String, Object> platformAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = normalizeRange(startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        List<TourOrder> orderList = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>());
        List<TourRoute> routeList = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>());
        List<TourCategory> categoryList = tourCategoryMapper.selectList(new LambdaQueryWrapper<TourCategory>());

        List<TourOrder> ordersInRange = orderList.stream()
                .filter(order -> inRange(order.getCreateTime(), start, end))
                .collect(Collectors.toList());
        List<TourOrder> paidOrdersInRange = ordersInRange.stream()
                .filter(this::isPaidOrder)
                .collect(Collectors.toList());

        List<LocalDateTime> orderTimeList = ordersInRange.stream()
                .map(TourOrder::getCreateTime)
                .collect(Collectors.toList());
        List<Map<String, Object>> orderTrend = buildDailyCountTrend(start, end, orderTimeList);

        Map<LocalDate, BigDecimal> amountByDate = new HashMap<>();
        for (TourOrder order : paidOrdersInRange) {
            LocalDate date = order.getCreateTime().toLocalDate();
            amountByDate.merge(date, nullSafeAmount(order.getPayAmount()), BigDecimal::add);
        }
        List<Map<String, Object>> turnoverTrend = buildDailyAmountTrend(start, end, amountByDate);

        Map<Long, TourRoute> routeMap = routeList.stream()
                .collect(Collectors.toMap(TourRoute::getId, route -> route, (a, b) -> a));
        Map<Long, String> categoryNameMap = categoryList.stream()
                .collect(Collectors.toMap(TourCategory::getId, category -> defaultIfBlank(category.getCategoryName(), "未知品类"), (a, b) -> a));

        Map<Long, BigDecimal> amountByCategory = new HashMap<>();
        for (TourOrder order : paidOrdersInRange) {
            TourRoute route = routeMap.get(order.getRouteId());
            if (route == null) {
                continue;
            }
            amountByCategory.merge(route.getCategoryId(), nullSafeAmount(order.getPayAmount()), BigDecimal::add);
        }
        BigDecimal categoryAmountTotal = amountByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> categoryShare = amountByCategory.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("categoryId", entry.getKey());
                    row.put("categoryName", categoryNameMap.getOrDefault(entry.getKey(), "未知品类"));
                    row.put("amount", entry.getValue());
                    row.put("ratio", categoryAmountTotal.compareTo(BigDecimal.ZERO) == 0
                            ? 0D
                            : entry.getValue().multiply(new BigDecimal("100"))
                            .divide(categoryAmountTotal, 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    return row;
                })
                .sorted((a, b) -> toBigDecimal(b.get("amount")).compareTo(toBigDecimal(a.get("amount"))))
                .collect(Collectors.toList());

        List<Map<String, Object>> holidayRevenueTrend = new ArrayList<>();
        BigDecimal holidayTurnover = BigDecimal.ZERO;
        for (Map<String, Object> row : turnoverTrend) {
            LocalDate date = LocalDate.parse(String.valueOf(row.get("date")));
            if (!isHoliday(date)) {
                continue;
            }
            holidayRevenueTrend.add(row);
            holidayTurnover = holidayTurnover.add(toBigDecimal(row.get("amount")));
        }

        BigDecimal turnover = paidOrdersInRange.stream()
                .map(TourOrder::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOrderAmount = paidOrdersInRange.isEmpty()
                ? BigDecimal.ZERO
                : turnover.divide(new BigDecimal(paidOrdersInRange.size()), 2, BigDecimal.ROUND_HALF_UP);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("orderCount", ordersInRange.size());
        overview.put("paidOrderCount", paidOrdersInRange.size());
        overview.put("turnover", turnover);
        overview.put("avgOrderAmount", avgOrderAmount);
        overview.put("holidayTurnover", holidayTurnover);

        Map<String, Object> result = new HashMap<>();
        result.put("rangeStart", start.toString());
        result.put("rangeEnd", end.toString());
        result.put("overview", overview);
        result.put("orderTrend", orderTrend);
        result.put("turnoverTrend", turnoverTrend);
        result.put("categoryShare", categoryShare);
        result.put("holidayRevenueTrend", holidayRevenueTrend);
        return result;
    }

    public Map<String, String> exportAnalytics(String type, LocalDate startDate, LocalDate endDate) {
        if (!StringUtils.hasText(type)) {
            throw new BizException(400, "导出类型不能为空");
        }
        String lowerType = type.toLowerCase();

        Map<String, Object> data;
        if ("merchant".equals(lowerType)) {
            data = merchantAnalytics(startDate, endDate);
        } else if ("customer".equals(lowerType)) {
            data = customerAnalytics(startDate, endDate);
        } else if ("platform".equals(lowerType)) {
            data = platformAnalytics(startDate, endDate);
        } else {
            throw new BizException(400, "不支持的导出类型");
        }

        StringBuilder csv = new StringBuilder();
        if ("merchant".equals(lowerType)) {
            buildMerchantCsv(csv, data);
        } else if ("customer".equals(lowerType)) {
            buildCustomerCsv(csv, data);
        } else {
            buildPlatformCsv(csv, data);
        }

        Map<String, String> result = new HashMap<>();
        result.put("fileName", lowerType + "-statistics-" + LocalDate.now() + ".csv");
        result.put("content", csv.toString());
        return result;
    }

    private LocalDate[] normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(29) : startDate;
        if (start.isAfter(end)) {
            throw new BizException(400, "开始日期不能晚于结束日期");
        }
        return new LocalDate[]{start, end};
    }

    private boolean inRange(LocalDateTime time, LocalDate start, LocalDate end) {
        if (time == null) {
            return false;
        }
        LocalDate date = time.toLocalDate();
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean isPaidOrder(TourOrder order) {
        return order != null && "PAID".equals(order.getPayStatus());
    }

    private List<Map<String, Object>> buildDailyCountTrend(LocalDate start, LocalDate end, List<LocalDateTime> timeList) {
        Map<LocalDate, Long> countMap = timeList.stream()
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .filter(date -> !date.isBefore(start) && !date.isAfter(end))
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", date.toString());
            row.put("count", countMap.getOrDefault(date, 0L));
            trend.add(row);
        }
        return trend;
    }

    private List<Map<String, Object>> buildDailyAmountTrend(LocalDate start, LocalDate end, Map<LocalDate, BigDecimal> amountMap) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", date.toString());
            row.put("amount", amountMap.getOrDefault(date, BigDecimal.ZERO));
            trend.add(row);
        }
        return trend;
    }

    private String resolveRegionByPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 3) {
            return "未知";
        }
        String prefix = phone.substring(0, 3);
        switch (prefix) {
            case "138":
                return "河南";
            case "137":
                return "山东";
            case "136":
                return "陕西";
            case "135":
                return "广东";
            case "139":
                return "北京";
            default:
                return "其他";
        }
    }

    private boolean isHoliday(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            return true;
        }
        String monthDay = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
        return FIXED_HOLIDAYS.contains(monthDay);
    }

    private BigDecimal nullSafeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private double toPercent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return new BigDecimal(numerator * 100D)
                .divide(new BigDecimal(denominator), 2, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private boolean containsKeyword(String source, String keyword) {
        return StringUtils.hasText(source) && source.contains(keyword);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<Map<String, Object>>();
    }

    private void buildMerchantCsv(StringBuilder csv, Map<String, Object> data) {
        Map<String, Object> overview = asMap(data.get("overview"));
        csv.append("分析类型,商家数据分析\n");
        csv.append("时间范围,").append(escapeCsv(String.valueOf(data.get("rangeStart")))).append("~")
                .append(escapeCsv(String.valueOf(data.get("rangeEnd")))).append("\n\n");
        csv.append("指标,值\n");
        csv.append("商户总量,").append(escapeCsv(String.valueOf(overview.get("totalMerchantCount")))).append("\n");
        csv.append("已通过商户,").append(escapeCsv(String.valueOf(overview.get("approvedMerchantCount")))).append("\n");
        csv.append("区间入驻量,").append(escapeCsv(String.valueOf(overview.get("newMerchantCount")))).append("\n");
        csv.append("活跃商户数,").append(escapeCsv(String.valueOf(overview.get("activeMerchantCount")))).append("\n");
        csv.append("活跃占比(%),").append(escapeCsv(String.valueOf(overview.get("activeRatio")))).append("\n\n");

        csv.append("商户交易额排行\n");
        csv.append("商户ID,商户名称,交易额\n");
        for (Map<String, Object> row : asList(data.get("transactionRank"))) {
            csv.append(escapeCsv(String.valueOf(row.get("merchantId")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("merchantName")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("amount")))).append("\n");
        }
        csv.append("\n商户投诉量排行\n");
        csv.append("商户ID,商户名称,投诉量\n");
        for (Map<String, Object> row : asList(data.get("complaintRank"))) {
            csv.append(escapeCsv(String.valueOf(row.get("merchantId")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("merchantName")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("complaintCount")))).append("\n");
        }
    }

    private void buildCustomerCsv(StringBuilder csv, Map<String, Object> data) {
        Map<String, Object> overview = asMap(data.get("overview"));
        Map<String, Object> profile = asMap(data.get("profile"));
        csv.append("分析类型,客户数据分析\n");
        csv.append("时间范围,").append(escapeCsv(String.valueOf(data.get("rangeStart")))).append("~")
                .append(escapeCsv(String.valueOf(data.get("rangeEnd")))).append("\n\n");
        csv.append("指标,值\n");
        csv.append("用户总量,").append(escapeCsv(String.valueOf(overview.get("totalUserCount")))).append("\n");
        csv.append("区间注册量,").append(escapeCsv(String.valueOf(overview.get("newRegisterCount")))).append("\n");
        csv.append("活跃用户数,").append(escapeCsv(String.valueOf(overview.get("activeUserCount")))).append("\n");
        csv.append("活跃度(%),").append(escapeCsv(String.valueOf(overview.get("activeRate")))).append("\n");
        csv.append("复购率(%),").append(escapeCsv(String.valueOf(overview.get("repeatPurchaseRate")))).append("\n");
        csv.append("平均客单价,").append(escapeCsv(String.valueOf(overview.get("avgOrderAmount")))).append("\n");
        csv.append("用户画像-主要地域,").append(escapeCsv(String.valueOf(profile.get("topRegion")))).append("\n");
        csv.append("用户画像-偏好品类,").append(escapeCsv(String.valueOf(profile.get("topPreference")))).append("\n\n");

        csv.append("地域分布\n");
        csv.append("地域,用户数\n");
        for (Map<String, Object> row : asList(data.get("regionDistribution"))) {
            csv.append(escapeCsv(String.valueOf(row.get("region")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("count")))).append("\n");
        }
        csv.append("\n消费偏好排行\n");
        csv.append("品类ID,品类名称,订单量,交易额\n");
        for (Map<String, Object> row : asList(data.get("preferenceRank"))) {
            csv.append(escapeCsv(String.valueOf(row.get("categoryId")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("categoryName")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("orderCount")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("amount")))).append("\n");
        }
    }

    private void buildPlatformCsv(StringBuilder csv, Map<String, Object> data) {
        Map<String, Object> overview = asMap(data.get("overview"));
        csv.append("分析类型,平台运营数据分析\n");
        csv.append("时间范围,").append(escapeCsv(String.valueOf(data.get("rangeStart")))).append("~")
                .append(escapeCsv(String.valueOf(data.get("rangeEnd")))).append("\n\n");
        csv.append("指标,值\n");
        csv.append("订单量,").append(escapeCsv(String.valueOf(overview.get("orderCount")))).append("\n");
        csv.append("支付订单量,").append(escapeCsv(String.valueOf(overview.get("paidOrderCount")))).append("\n");
        csv.append("交易额,").append(escapeCsv(String.valueOf(overview.get("turnover")))).append("\n");
        csv.append("平均客单价,").append(escapeCsv(String.valueOf(overview.get("avgOrderAmount")))).append("\n");
        csv.append("节假日营收,").append(escapeCsv(String.valueOf(overview.get("holidayTurnover")))).append("\n\n");

        csv.append("品类销售占比\n");
        csv.append("品类ID,品类名称,交易额,占比(%)\n");
        for (Map<String, Object> row : asList(data.get("categoryShare"))) {
            csv.append(escapeCsv(String.valueOf(row.get("categoryId")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("categoryName")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("amount")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("ratio")))).append("\n");
        }
        csv.append("\n节假日营收趋势\n");
        csv.append("日期,营收\n");
        for (Map<String, Object> row : asList(data.get("holidayRevenueTrend"))) {
            csv.append(escapeCsv(String.valueOf(row.get("date")))).append(",")
                    .append(escapeCsv(String.valueOf(row.get("amount")))).append("\n");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
