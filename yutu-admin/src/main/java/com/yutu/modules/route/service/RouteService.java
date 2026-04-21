package com.yutu.modules.route.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.exception.BizException;
import com.yutu.modules.model.entity.ContractTemplate;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourCategory;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourReview;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.TourRouteSchedule;
import com.yutu.modules.model.entity.TourRouteTag;
import com.yutu.modules.model.entity.TourTag;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import com.yutu.modules.model.mapper.TourCategoryMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourReviewMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.model.mapper.TourRouteScheduleMapper;
import com.yutu.modules.model.mapper.TourRouteTagMapper;
import com.yutu.modules.model.mapper.TourTagMapper;
import com.yutu.modules.route.vo.RouteReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private final TourReviewMapper tourReviewMapper;
    private final SysUserMapper sysUserMapper;
    private final ContractTemplateMapper contractTemplateMapper;

    public RouteService(TourRouteMapper tourRouteMapper,
                        TourDepartureDateMapper tourDepartureDateMapper,
                        TourRouteScheduleMapper tourRouteScheduleMapper,
                        TourRouteTagMapper tourRouteTagMapper,
                        TourCategoryMapper tourCategoryMapper,
                        TourTagMapper tourTagMapper,
                        TourReviewMapper tourReviewMapper,
                        SysUserMapper sysUserMapper,
                        ContractTemplateMapper contractTemplateMapper) {
        this.tourRouteMapper = tourRouteMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.tourRouteScheduleMapper = tourRouteScheduleMapper;
        this.tourRouteTagMapper = tourRouteTagMapper;
        this.tourCategoryMapper = tourCategoryMapper;
        this.tourTagMapper = tourTagMapper;
        this.tourReviewMapper = tourReviewMapper;
        this.sysUserMapper = sysUserMapper;
        this.contractTemplateMapper = contractTemplateMapper;
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

    public List<ContractTemplate> contractTemplates(Long categoryId) {
        TourCategory category = categoryId == null ? null : tourCategoryMapper.selectById(categoryId);
        String categoryName = category == null ? null : category.getCategoryName();
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, 1)
                .orderByAsc(ContractTemplate::getTemplateType)
                .orderByDesc(ContractTemplate::getUpdateTime);
        if (StringUtils.hasText(categoryName)) {
            wrapper.eq(ContractTemplate::getApplyScope, categoryName.trim());
        }
        return contractTemplateMapper.selectList(wrapper);
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
        List<RouteReviewVO> reviews = listRouteReviews(id);
        Map<String, Object> map = new HashMap<>();
        map.put("route", route);
        map.put("category", category);
        map.put("schedules", schedules);
        map.put("tags", tags);
        map.put("reviews", reviews);
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

    private List<RouteReviewVO> listRouteReviews(Long routeId) {
        List<TourReview> reviews = tourReviewMapper.selectList(new LambdaQueryWrapper<TourReview>()
                .eq(TourReview::getRouteId, routeId)
                .eq(TourReview::getStatus, 1)
                .orderByDesc(TourReview::getCreateTime)
                .orderByDesc(TourReview::getId)
                .last("limit 20"));
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = reviews.stream()
                .map(TourReview::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(SysUser::getId, item -> item, (left, right) -> left, HashMap::new));

        List<RouteReviewVO> result = new ArrayList<>();
        for (TourReview review : reviews) {
            if (review == null) {
                continue;
            }
            SysUser user = userMap.get(review.getUserId());
            RouteReviewVO vo = new RouteReviewVO();
            vo.setId(review.getId());
            vo.setOrderId(review.getOrderId());
            vo.setRouteId(review.getRouteId());
            vo.setUserId(review.getUserId());
            vo.setDisplayName(resolveReviewDisplayName(user));
            vo.setAvatar(user == null ? null : user.getAvatar());
            vo.setScore(review.getScore());
            vo.setContent(review.getContent());
            vo.setCreateTime(review.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private String resolveReviewDisplayName(SysUser user) {
        if (user == null) {
            return "匿名用户";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "匿名用户";
    }
}
