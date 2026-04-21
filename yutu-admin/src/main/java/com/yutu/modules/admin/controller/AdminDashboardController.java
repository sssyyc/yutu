package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminService adminService;

    public AdminDashboardController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:dashboard:view')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(adminService.dashboardOverview());
    }

    @PreAuthorize("hasAuthority('admin:dashboard:view')")
    @GetMapping("/order-trend")
    public Result<List<Map<String, Object>>> orderTrend() {
        return Result.ok(adminService.orderTrend());
    }

    @PreAuthorize("hasAuthority('admin:dashboard:view')")
    @GetMapping("/complaint-trend")
    public Result<List<Map<String, Object>>> complaintTrend() {
        return Result.ok(adminService.complaintTrend());
    }

    @PreAuthorize("hasAuthority('admin:dashboard:view')")
    @GetMapping("/top-routes")
    public Result<List<Map<String, Object>>> topRoutes() {
        return Result.ok(adminService.topRoutes());
    }
}
