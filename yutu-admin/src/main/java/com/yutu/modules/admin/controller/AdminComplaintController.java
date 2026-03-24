package com.yutu.modules.admin.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.complaint.dto.ComplaintActionRequest;
import com.yutu.modules.complaint.service.ComplaintService;
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
@RequestMapping("/api/admin/complaints")
public class AdminComplaintController {
    private final ComplaintService complaintService;

    public AdminComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PreAuthorize("hasAuthority('admin:complaint:list')")
    @GetMapping
    public Result<List<ComplaintOrder>> list() {
        return Result.ok(complaintService.adminList());
    }

    @PreAuthorize("hasAuthority('admin:complaint:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(complaintService.adminDetail(id));
    }

    @PreAuthorize("hasAuthority('admin:complaint:handle')")
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id, @Validated @RequestBody ComplaintActionRequest request) {
        complaintService.adminAccept(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:complaint:handle')")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @Validated @RequestBody ComplaintActionRequest request) {
        complaintService.adminAssign(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:complaint:handle')")
    @PostMapping("/{id}/judge")
    public Result<Void> judge(@PathVariable Long id, @Validated @RequestBody ComplaintActionRequest request) {
        complaintService.adminJudge(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('admin:complaint:handle')")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id, @Validated @RequestBody ComplaintActionRequest request) {
        complaintService.adminFinish(id, request);
        return Result.ok();
    }
}
