package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.admin.dto.RouteAuditRequest;
import com.yutu.modules.admin.service.AdminService;
import com.yutu.modules.admin.vo.AdminRouteDepartureDateVO;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourRoute;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/routes")
public class AdminRouteController {
    private final AdminService adminService;

    public AdminRouteController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('admin:route:list')")
    @GetMapping
    public Result<List<TourRoute>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(adminService.routes(keyword));
    }

    @PreAuthorize("hasAuthority('admin:route:list')")
    @GetMapping("/departure-dates")
    public Result<List<AdminRouteDepartureDateVO>> departureDates() {
        return Result.ok(adminService.routeDepartureDates());
    }

    @PreAuthorize("hasAuthority('admin:route:list')")
    @GetMapping("/{id}/departure-dates")
    public Result<List<TourDepartureDate>> departureDatesByRoute(@PathVariable Long id) {
        return Result.ok(adminService.routeDepartureDatesByRoute(id));
    }

    @PreAuthorize("hasAuthority('admin:route:audit')")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        adminService.routeAudit(id, "approve", null);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:route:audit')")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody RouteAuditRequest request) {
        adminService.routeAudit(id, "reject", request == null ? null : request.getAuditRemark());
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:route:audit')")
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        adminService.routeAudit(id, "offline", null);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:route:audit')")
    @PostMapping("/departure-dates/{id}/approve")
    public Result<Void> approveDepartureDate(@PathVariable Long id) {
        adminService.routeDepartureDateAudit(id, "approve", null);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:route:audit')")
    @PostMapping("/departure-dates/{id}/reject")
    public Result<Void> rejectDepartureDate(@PathVariable Long id, @RequestBody RouteAuditRequest request) {
        adminService.routeDepartureDateAudit(id, "reject", request == null ? null : request.getAuditRemark());
        return Result.ok();
    }
}
