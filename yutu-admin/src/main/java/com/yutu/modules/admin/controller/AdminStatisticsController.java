package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {
    private final AdminService adminService;

    public AdminStatisticsController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:statistics:view')")
    @GetMapping
    public Result<Map<String, Object>> statistics() {
        return Result.ok(adminService.dashboardOverview());
    }

    @PreAuthorize("hasAuthority('admin:statistics:view')")
    @GetMapping("/merchant")
    public Result<Map<String, Object>> merchant(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(adminService.merchantAnalytics(startDate, endDate));
    }

    @PreAuthorize("hasAuthority('admin:statistics:view')")
    @GetMapping("/customer")
    public Result<Map<String, Object>> customer(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(adminService.customerAnalytics(startDate, endDate));
    }

    @PreAuthorize("hasAuthority('admin:statistics:view')")
    @GetMapping("/platform")
    public Result<Map<String, Object>> platform(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(adminService.platformAnalytics(startDate, endDate));
    }

    @PreAuthorize("hasAuthority('admin:statistics:view')")
    @GetMapping("/export")
    public Result<Map<String, String>> export(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(adminService.exportAnalytics(type, startDate, endDate));
    }
}
