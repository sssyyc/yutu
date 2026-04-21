package com.yutu.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.exception.BizException;
import com.yutu.modules.admin.dto.BannerSaveRequest;
import com.yutu.modules.model.entity.SysBanner;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.SysBannerMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminBannerService {
    private final SysBannerMapper sysBannerMapper;
    private final TourRouteMapper tourRouteMapper;

    public AdminBannerService(SysBannerMapper sysBannerMapper, TourRouteMapper tourRouteMapper) {
        this.sysBannerMapper = sysBannerMapper;
        this.tourRouteMapper = tourRouteMapper;
    }

    public List<SysBanner> list(String keyword) {
        List<SysBanner> banners = sysBannerMapper.selectList(new LambdaQueryWrapper<SysBanner>()
                .orderByAsc(SysBanner::getSortNum)
                .orderByDesc(SysBanner::getUpdateTime));
        if (!StringUtils.hasText(keyword)) {
            return banners;
        }
        String trimmedKeyword = keyword.trim();
        List<SysBanner> filteredBanners = new java.util.ArrayList<>();
        for (SysBanner banner : banners) {
            if (banner == null) {
                continue;
            }
            if (containsKeyword(banner.getTitle(), trimmedKeyword) || matchRouteKeyword(banner.getLinkUrl(), trimmedKeyword)) {
                filteredBanners.add(banner);
            }
        }
        return filteredBanners;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BannerSaveRequest request) {
        SysBanner banner = new SysBanner();
        applyRequest(banner, request);
        sysBannerMapper.insert(banner);
        return banner.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BannerSaveRequest request) {
        SysBanner banner = sysBannerMapper.selectById(id);
        if (banner == null) {
            throw new BizException(404, "轮播图不存在");
        }
        applyRequest(banner, request);
        sysBannerMapper.updateById(banner);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysBanner banner = sysBannerMapper.selectById(id);
        if (banner == null) {
            throw new BizException(404, "轮播图不存在");
        }
        sysBannerMapper.deleteById(id);
    }

    private void applyRequest(SysBanner banner, BannerSaveRequest request) {
        banner.setTitle(request.getTitle().trim());
        banner.setImageUrl(request.getImageUrl().trim());
        banner.setLinkUrl(StringUtils.hasText(request.getLinkUrl()) ? request.getLinkUrl().trim() : null);
        banner.setSortNum(request.getSortNum() == null ? 0 : request.getSortNum());
        banner.setStatus(request.getStatus() != null && request.getStatus() == 0 ? 0 : 1);
    }

    private boolean matchRouteKeyword(String linkUrl, String keyword) {
        Long routeId = parseRouteId(linkUrl);
        if (routeId == null) {
            return false;
        }
        TourRoute route = tourRouteMapper.selectById(routeId);
        return route != null && containsKeyword(route.getRouteName(), keyword);
    }

    private Long parseRouteId(String linkUrl) {
        if (!StringUtils.hasText(linkUrl)) {
            return null;
        }
        String trimmedLinkUrl = linkUrl.trim();
        String prefix = "/route/detail/";
        if (!trimmedLinkUrl.startsWith(prefix)) {
            return null;
        }
        try {
            return Long.parseLong(trimmedLinkUrl.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean containsKeyword(String source, String keyword) {
        return StringUtils.hasText(source) && source.contains(keyword);
    }
}
