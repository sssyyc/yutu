package com.yutu.modules.complaint.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.complaint.dto.ComplaintCreateRequest;
import com.yutu.modules.complaint.service.ComplaintService;
import com.yutu.modules.complaint.vo.ComplaintRouteOptionVO;
import com.yutu.modules.model.entity.ComplaintOrder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PreAuthorize("hasAuthority('complaint:create')")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody ComplaintCreateRequest request) {
        return Result.ok(complaintService.create(request));
    }

    @PreAuthorize("hasAuthority('complaint:create')")
    @GetMapping("/route-options")
    public Result<List<ComplaintRouteOptionVO>> routeOptions() {
        return Result.ok(complaintService.userComplaintRouteOptions());
    }

    @PreAuthorize("hasAuthority('complaint:list')")
    @GetMapping
    public Result<List<ComplaintOrder>> list() {
        return Result.ok(complaintService.userList());
    }

    @PreAuthorize("hasAuthority('complaint:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(complaintService.userDetail(id));
    }
}
