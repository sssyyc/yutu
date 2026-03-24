package com.yutu.modules.user.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.UserTraveler;
import com.yutu.modules.user.dto.TravelerSaveRequest;
import com.yutu.modules.user.service.UserCenterService;
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
@RequestMapping("/api/travelers")
public class TravelerController {
    private final UserCenterService userCenterService;

    public TravelerController(UserCenterService userCenterService) {
        this.userCenterService = userCenterService;
    }

    @PreAuthorize("hasAuthority('traveler:manage')")
    @GetMapping
    public Result<List<UserTraveler>> list() {
        return Result.ok(userCenterService.travelerList());
    }

    @PreAuthorize("hasAuthority('traveler:manage')")
    @PostMapping
    public Result<Long> add(@Validated @RequestBody TravelerSaveRequest request) {
        return Result.ok(userCenterService.addTraveler(request));
    }

    @PreAuthorize("hasAuthority('traveler:manage')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody TravelerSaveRequest request) {
        userCenterService.updateTraveler(id, request);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('traveler:manage')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userCenterService.deleteTraveler(id);
        return Result.ok();
    }
}
