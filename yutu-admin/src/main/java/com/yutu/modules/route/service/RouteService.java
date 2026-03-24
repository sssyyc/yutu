package com.yutu.modules.route.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.exception.BizException;
import com.yutu.modules.model.entity.TourCategory;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.TourRouteSchedule;
import com.yutu.modules.model.entity.TourRouteTag;
import com.yutu.modules.model.entity.TourTag;
import com.yutu.modules.model.mapper.TourCategoryMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.model.mapper.TourRouteScheduleMapper;
import com.yutu.modules.model.mapper.TourRouteTagMapper;
import com.yutu.modules.model.mapper.TourTagMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RouteService {
    private final TourRouteMapper tourRouteMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final TourRouteScheduleMapper tourRouteScheduleMapper;
    private final TourRouteTagMapper tourRouteTagMapper;
    private final TourCategoryMapper tourCategoryMapper;
    private final TourTagMapper tourTagMapper;

    public RouteService(TourRouteMapper tourRouteMapper,
                        TourDepartureDateMapper tourDepartureDateMapper,
                        TourRouteScheduleMapper tourRouteScheduleMapper,
                        TourRouteTagMapper tourRouteTagMapper,
                        TourCategoryMapper tourCategoryMapper,
                        TourTagMapper tourTagMapper) {
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourRouteScheduleMapper = tourRouteScheduleMapper;
        this.tourRouteTagMapper = tourRouteTagMapper;
        this.tourCategoryMapper = tourCategoryMapper;
        this.tourTagMapper = tourTagMapper;
    }

    public List<TourRoute> list(Long categoryId, Long tagId, String keyword) {
        LambdaQueryWrapper<TourRoute> wrapper = new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getStatus, 1)
                .eq(TourRoute::getPublishStatus, 1)
                .eq(TourRoute::getAuditStatus, 1)
                .orderByDesc(TourRoute::getCreateTime);
        if (categoryId != null) {
            wrapper.eq(TourRoute::getCategoryId, categoryId);
        }
        if (tagId != null) {
            List<Long> routeIds = tourRouteTagMapper.selectList(new LambdaQueryWrapper<TourRouteTag>()
                            .eq(TourRouteTag::getTagId, tagId))
                    .stream()
                    .map(TourRouteTag::getRouteId)
                    .distinct()
                    .collect(Collectors.toList());
            if (routeIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(TourRoute::getId, routeIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(TourRoute::getRouteName, keyword)
                    .or()
                    .like(TourRoute::getSummary, keyword));
        }
        return tourRouteMapper.selectList(wrapper);
    }

    public List<TourCategory> categories() {
        return tourCategoryMapper.selectList(new LambdaQueryWrapper<TourCategory>()
                .eq(TourCategory::getStatus, 1)
                .orderByAsc(TourCategory::getSortNum)
                .orderByAsc(TourCategory::getId));
    }

    public List<TourTag> tags() {
        return tourTagMapper.selectList(new LambdaQueryWrapper<TourTag>()
                .eq(TourTag::getStatus, 1)
                .orderByAsc(TourTag::getTagType)
                .orderByDesc(TourTag::getUpdateTime));
    }

    public Map<String, Object> detail(Long id) {
        TourRoute route = tourRouteMapper.selectById(id);
        if (route == null || route.getDeleted() != null && route.getDeleted() == 1) {
            throw new BizException(404, "路线不存在");
        }
        List<TourRouteSchedule> schedules = tourRouteScheduleMapper.selectList(new LambdaQueryWrapper<TourRouteSchedule>()
                .eq(TourRouteSchedule::getRouteId, id)
                .orderByAsc(TourRouteSchedule::getDayNo));
        TourCategory category = route.getCategoryId() == null ? null : tourCategoryMapper.selectById(route.getCategoryId());
        List<TourRouteTag> routeTags = tourRouteTagMapper.selectList(new LambdaQueryWrapper<TourRouteTag>()
                .eq(TourRouteTag::getRouteId, id));
        List<TourTag> tags = resolveTags(routeTags);
        Map<String, Object> map = new HashMap<>();
        map.put("route", route);
        map.put("category", category);
        map.put("schedules", schedules);
        map.put("tags", tags);
        return map;
    }

    public List<TourDepartureDate> dates(Long routeId) {
        LocalDate today = LocalDate.now();
        return tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()
                .eq(TourDepartureDate::getRouteId, routeId)
                .eq(TourDepartureDate::getStatus, 1)
                .eq(TourDepartureDate::getAuditStatus, 1)
                .ge(TourDepartureDate::getDepartDate, today)
                .orderByAsc(TourDepartureDate::getDepartDate));
    }

    private List<TourTag> resolveTags(List<TourRouteTag> routeTags) {
        if (routeTags == null || routeTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = routeTags.stream()
                .map(TourRouteTag::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, TourTag> tagMap = tourTagMapper.selectBatchIds(tagIds).stream()
                .filter(tag -> Objects.equals(tag.getStatus(), 1))
                .collect(Collectors.toMap(TourTag::getId, tag -> tag, (left, right) -> left, HashMap::new));
        return tagIds.stream()
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
