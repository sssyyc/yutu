package com.yutu.modules.user.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.user.dto.MerchantApplicationSaveRequest;
import com.yutu.modules.user.dto.PasswordUpdateRequest;
import com.yutu.modules.user.dto.UserProfileUpdateRequest;
import com.yutu.modules.user.service.UserCenterService;
import com.yutu.modules.user.vo.MerchantApplicationVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserCenterService userCenterService;

    public UserController(UserCenterService userCenterService) {
        this.userCenterService = userCenterService;
    }

    @PutMapping("/profile")
    public Result<SysUser> updateProfile(@Validated @RequestBody UserProfileUpdateRequest request) {
        return Result.ok(userCenterService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody PasswordUpdateRequest request) {
        userCenterService.updatePassword(request);
        return Result.ok();
    }

    @GetMapping("/merchant-application")
    public Result<MerchantApplicationVO> merchantApplication() {
        return Result.ok(userCenterService.merchantApplication());
    }

    @PostMapping("/merchant-application")
    public Result<Long> submitMerchantApplication(@Validated @RequestBody MerchantApplicationSaveRequest request) {
        return Result.ok(userCenterService.submitMerchantApplication(request));
    }

    @PostMapping("/merchant-cancel")
    public Result<Void> cancelMerchant() {
        userCenterService.cancelMerchant();
        return Result.ok();
    }
}
