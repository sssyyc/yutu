package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.dto.BannerSaveRequest;
import com.yutu.modules.admin.service.AdminBannerService;
import com.yutu.modules.model.entity.SysBanner;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {
    private final AdminBannerService adminBannerService;

    public AdminBannerController(AdminBannerService adminBannerService) {
        this.adminBannerService = adminBannerService;
    }

    @PreAuthorize("hasAnyAuthority('admin:banner:list','admin:dashboard:view')")
    @GetMapping
    public Result<List<SysBanner>> list() {
        return Result.ok(adminBannerService.list());
    }

    @PreAuthorize("hasAnyAuthority('admin:banner:manage','admin:category:manage')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody BannerSaveRequest request) {
        return Result.ok(adminBannerService.create(request));
    }

    @PreAuthorize("hasAnyAuthority('admin:banner:manage','admin:category:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody BannerSaveRequest request) {
        adminBannerService.update(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAnyAuthority('admin:banner:manage','admin:category:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminBannerService.delete(id);
        return Result.ok();
    }
}
