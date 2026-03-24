package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.dto.StatusUpdateRequest;
import com.yutu.modules.admin.service.AdminService;
import com.yutu.modules.model.entity.SysUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:user:list')")
    @GetMapping
    public Result<List<SysUser>> list() {
        return Result.ok(adminService.users());
    }

    @PreAuthorize("hasAuthority('admin:user:status')")
    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @Validated @RequestBody StatusUpdateRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:user:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.ok();
    }
}
