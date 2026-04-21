package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.dto.CategorySaveRequest;
import com.yutu.modules.admin.service.AdminService;
import com.yutu.modules.model.entity.TourCategory;
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
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final AdminService adminService;

    public AdminCategoryController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:category:list')")
    @GetMapping
    public Result<List<TourCategory>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(adminService.categories(keyword));
    }

    @PreAuthorize("hasAuthority('admin:category:manage')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CategorySaveRequest request) {
        return Result.ok(adminService.createCategory(request));
    }

    @PreAuthorize("hasAuthority('admin:category:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody CategorySaveRequest request) {
        adminService.updateCategory(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:category:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.ok();
    }
}
