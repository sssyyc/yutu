package com.yutu.modules.user.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.UserFavorite;
import com.yutu.modules.user.dto.FavoriteCreateRequest;
import com.yutu.modules.user.service.UserCenterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final UserCenterService userCenterService;

    public FavoriteController(UserCenterService userCenterService) {
        this.userCenterService = userCenterService;
    }

    @PreAuthorize("hasAuthority('favorite:add')")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody FavoriteCreateRequest request) {
        return Result.ok(userCenterService.addFavorite(request));
    }

    @PreAuthorize("hasAuthority('favorite:list')")
    @GetMapping
    public Result<List<UserFavorite>> list() {
        return Result.ok(userCenterService.favoriteList());
    }

    @PreAuthorize("hasAuthority('favorite:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userCenterService.deleteFavorite(id);
        return Result.ok();
    }
}
