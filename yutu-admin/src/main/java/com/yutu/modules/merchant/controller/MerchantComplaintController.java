package com.yutu.modules.merchant.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/complaints")
public class MerchantComplaintController {
    private final ComplaintService complaintService;

    public MerchantComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PreAuthorize("hasAuthority('merchant:complaint:list')")
    @GetMapping
    public Result<List<ComplaintOrder>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(complaintService.merchantList(keyword));
    }

    @PreAuthorize("hasAuthority('merchant:complaint:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(complaintService.merchantDetail(id));
    }

    @PreAuthorize("hasAuthority('merchant:complaint:reply')")
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Validated @RequestBody ComplaintActionRequest request) {
        complaintService.merchantReply(id, request);
        return Result.ok();
    }
}
