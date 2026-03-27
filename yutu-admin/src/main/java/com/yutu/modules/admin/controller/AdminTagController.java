package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.dto.TagSaveRequest;
import com.yutu.modules.admin.service.AdminService;
import com.yutu.modules.model.entity.TourTag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {
    private final AdminService adminService;

    public AdminTagController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:tag:list')")
    @GetMapping
    public Result<List<TourTag>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(adminService.tags(keyword));
    }

    @PreAuthorize("hasAuthority('admin:tag:manage')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody TagSaveRequest request) {
        return Result.ok(adminService.createTag(request));
    }

    @PreAuthorize("hasAuthority('admin:tag:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody TagSaveRequest request) {
        adminService.updateTag(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:tag:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.deleteTag(id);
        return Result.ok();
    }
}
