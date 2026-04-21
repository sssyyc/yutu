package com.yutu.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.ComplaintOrder;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.UserFavorite;
import com.yutu.modules.model.entity.UserTraveler;
import com.yutu.modules.model.mapper.ComplaintOrderMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import com.yutu.modules.model.mapper.UserFavoriteMapper;
import com.yutu.modules.model.mapper.UserTravelerMapper;
import com.yutu.modules.user.dto.FavoriteCreateRequest;
import com.yutu.modules.user.dto.MerchantApplicationSaveRequest;
import com.yutu.modules.user.dto.PasswordUpdateRequest;
import com.yutu.modules.user.dto.TravelerSaveRequest;
import com.yutu.modules.user.dto.UserProfileUpdateRequest;
import com.yutu.modules.user.vo.MerchantApplicationVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class UserCenterService {
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserTravelerMapper userTravelerMapper;
    private final SysUserMapper sysUserMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final TourOrderMapper tourOrderMapper;
    private final ComplaintOrderMapper complaintOrderMapper;
    private final TourRouteMapper tourRouteMapper;
    private final PasswordEncoder passwordEncoder;

    public UserCenterService(UserFavoriteMapper userFavoriteMapper,
                             UserTravelerMapper userTravelerMapper,
                             SysUserMapper sysUserMapper,
                             MerchantShopMapper merchantShopMapper,
                             TourOrderMapper tourOrderMapper,
                             ComplaintOrderMapper complaintOrderMapper,
                             TourRouteMapper tourRouteMapper,
                             PasswordEncoder passwordEncoder) {
        this.userFavoriteMapper = userFavoriteMapper;
        this.userTravelerMapper = userTravelerMapper;
        this.sysUserMapper = sysUserMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.complaintOrderMapper = complaintOrderMapper;
        this.tourRouteMapper = tourRouteMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Long addFavorite(FavoriteCreateRequest request) {
        Long userId = currentUserId();
        Long count = userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetId, request.getTargetId())
                .eq(UserFavorite::getTargetType, request.getTargetType()));
        if (count != null && count > 0) {
            throw new BizException(400, "该内容已收藏");
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setTargetId(request.getTargetId());
        favorite.setTargetType(request.getTargetType());
        userFavoriteMapper.insert(favorite);
        return favorite.getId();
    }

    public List<UserFavorite> favoriteList() {
        return userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, currentUserId())
                .orderByDesc(UserFavorite::getCreateTime));
    }

    public void deleteFavorite(Long id) {
        UserFavorite favorite = userFavoriteMapper.selectById(id);
        if (favorite == null || !favorite.getUserId().equals(currentUserId())) {
            throw new BizException(404, "收藏不存在");
        }
        userFavoriteMapper.deleteById(id);
    }

    public List<UserTraveler> travelerList() {
        return userTravelerMapper.selectList(new LambdaQueryWrapper<UserTraveler>()
                .eq(UserTraveler::getUserId, currentUserId())
                .orderByDesc(UserTraveler::getUpdateTime));
    }

    public Long addTraveler(TravelerSaveRequest request) {
        UserTraveler traveler = new UserTraveler();
        traveler.setUserId(currentUserId());
        traveler.setTravelerName(request.getTravelerName());
        traveler.setIdCard(request.getIdCard());
        traveler.setPhone(request.getPhone());
        userTravelerMapper.insert(traveler);
        return traveler.getId();
    }

    public void updateTraveler(Long id, TravelerSaveRequest request) {
        UserTraveler traveler = userTravelerMapper.selectById(id);
        if (traveler == null || !traveler.getUserId().equals(currentUserId())) {
            throw new BizException(404, "出行人不存在");
        }
        traveler.setTravelerName(request.getTravelerName());
        traveler.setIdCard(request.getIdCard());
        traveler.setPhone(request.getPhone());
        userTravelerMapper.updateById(traveler);
    }

    public void deleteTraveler(Long id) {
        UserTraveler traveler = userTravelerMapper.selectById(id);
        if (traveler == null || !traveler.getUserId().equals(currentUserId())) {
            throw new BizException(404, "出行人不存在");
        }
        userTravelerMapper.deleteById(id);
    }

    public SysUser updateProfile(UserProfileUpdateRequest request) {
        SysUser user = getCurrentUser();
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        sysUserMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public MerchantApplicationVO merchantApplication() {
        SysUser user = getCurrentUser();
        MerchantShop merchantShop = findMerchantShop(user.getId());
        return buildMerchantApplication(user, merchantShop);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submitMerchantApplication(MerchantApplicationSaveRequest request) {
        SysUser user = getCurrentUser();
        MerchantShop merchantShop = findMerchantShop(user.getId());

        if (merchantShop != null && Integer.valueOf(1).equals(merchantShop.getAuditStatus())) {
            throw new BizException(400, "当前账号已通过商户审核");
        }

        if (merchantShop == null) {
            merchantShop = new MerchantShop();
            merchantShop.setUserId(user.getId());
            merchantShop.setDeleted(0);
        }

        merchantShop.setShopName(request.getShopName());
        merchantShop.setContactName(request.getContactName());
        merchantShop.setContactPhone(request.getContactPhone());
        merchantShop.setDescription(request.getDescription());
        merchantShop.setLicenseNo(request.getLicenseNo());
        merchantShop.setLicenseImage(request.getLicenseImage());
        merchantShop.setIdCardFrontImage(request.getIdCardFrontImage());
        merchantShop.setIdCardBackImage(request.getIdCardBackImage());
        merchantShop.setAuditStatus(0);
        merchantShop.setAuditRemark(null);
        merchantShop.setAuditTime(null);
        merchantShop.setStatus(1);

        if (merchantShop.getId() == null) {
            merchantShopMapper.insert(merchantShop);
        } else {
            merchantShopMapper.updateById(merchantShop);
        }

        if (StringUtils.hasText(request.getContactPhone())) {
            user.setPhone(request.getContactPhone());
            sysUserMapper.updateById(user);
        }
        return merchantShop.getId();
    }

    public void updatePassword(PasswordUpdateRequest request) {
        SysUser user = getCurrentUser();
        boolean matched;
        if (user.getPassword() != null && user.getPassword().startsWith("$2a$")) {
            matched = passwordEncoder.matches(request.getOldPassword(), user.getPassword());
        } else {
            matched = request.getOldPassword().equals(user.getPassword());
        }
        if (!matched) {
            throw new BizException(400, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelMerchant() {
        SysUser user = getCurrentUser();
        if (!Integer.valueOf(2).equals(user.getRoleType())) {
            throw new BizException(400, "当前账号不是商户，无需注销");
        }
        MerchantShop shop = findMerchantShop(user.getId());
        if (shop == null || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw new BizException(400, "商户店铺不存在或已停用");
        }

        Long unfinishedOrderCount = tourOrderMapper.selectCount(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getMerchantId, shop.getId())
                .notIn(TourOrder::getOrderStatus, Arrays.asList("CANCELLED", "REFUNDED", "COMPLETED")));
        if (unfinishedOrderCount != null && unfinishedOrderCount > 0) {
            throw new BizException(400, "存在未完成订单，无法注销商户");
        }

        Long unfinishedComplaintCount = complaintOrderMapper.selectCount(new LambdaQueryWrapper<ComplaintOrder>()
                .eq(ComplaintOrder::getMerchantId, shop.getId())
                .ne(ComplaintOrder::getStatus, "FINISHED"));
        if (unfinishedComplaintCount != null && unfinishedComplaintCount > 0) {
            throw new BizException(400, "存在未处理完成的投诉，无法注销商户");
        }

        List<TourRoute> routes = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()
                .eq(TourRoute::getMerchantId, shop.getId()));
        for (TourRoute route : routes) {
            route.setPublishStatus(0);
            route.setStatus(0);
            tourRouteMapper.updateById(route);
        }

        shop.setStatus(0);
        shop.setAuditStatus(2);
        shop.setAuditRemark("商户主动注销");
        shop.setAuditTime(LocalDateTime.now());
        merchantShopMapper.updateById(shop);

        user.setRoleType(1);
        sysUserMapper.updateById(user);
    }

    private MerchantShop findMerchantShop(Long userId) {
        return merchantShopMapper.selectOne(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getUserId, userId)
                .orderByDesc(MerchantShop::getId)
                .last("limit 1"));
    }

    private MerchantApplicationVO buildMerchantApplication(SysUser user, MerchantShop merchantShop) {
        MerchantApplicationVO vo = new MerchantApplicationVO();
        vo.setUserId(user.getId());
        vo.setRoleType(user.getRoleType());
        if (merchantShop == null) {
            return vo;
        }
        vo.setId(merchantShop.getId());
        vo.setShopName(merchantShop.getShopName());
        vo.setContactName(merchantShop.getContactName());
        vo.setContactPhone(merchantShop.getContactPhone());
        vo.setDescription(merchantShop.getDescription());
        vo.setLicenseNo(merchantShop.getLicenseNo());
        vo.setLicenseImage(merchantShop.getLicenseImage());
        vo.setIdCardFrontImage(merchantShop.getIdCardFrontImage());
        vo.setIdCardBackImage(merchantShop.getIdCardBackImage());
        vo.setAuditStatus(merchantShop.getAuditStatus());
        vo.setAuditRemark(merchantShop.getAuditRemark());
        vo.setCreateTime(merchantShop.getCreateTime());
        vo.setUpdateTime(merchantShop.getUpdateTime());
        vo.setAuditTime(merchantShop.getAuditTime());
        return vo;
    }

    private SysUser getCurrentUser() {
        SysUser user = sysUserMapper.selectById(currentUserId());
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        return userId;
    }
}
