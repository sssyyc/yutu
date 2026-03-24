package com.yutu.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.exception.BizException;
import com.yutu.modules.admin.dto.BannerSaveRequest;
import com.yutu.modules.model.entity.SysBanner;
import com.yutu.modules.model.mapper.SysBannerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminBannerService {
    private final SysBannerMapper sysBannerMapper;

    public AdminBannerService(SysBannerMapper sysBannerMapper) {
        this.sysBannerMapper = sysBannerMapper;
    }

    public List<SysBanner> list() {
        return sysBannerMapper.selectList(new LambdaQueryWrapper<SysBanner>()
                .orderByAsc(SysBanner::getSortNum)
                .orderByDesc(SysBanner::getUpdateTime));
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
}
