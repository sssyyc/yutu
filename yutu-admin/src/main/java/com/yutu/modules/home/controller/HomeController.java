package com.yutu.modules.home.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.home.service.HomeService;
import com.yutu.modules.home.vo.HomeReminderVO;
import com.yutu.modules.model.entity.SysBanner;
import com.yutu.modules.model.entity.TourReview;
import com.yutu.modules.model.entity.TourRoute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/banners")
    public Result<List<SysBanner>> banners() {
        return Result.ok(homeService.banners());
    }

    @GetMapping("/hot-routes")
    public Result<List<TourRoute>> hotRoutes() {
        return Result.ok(homeService.hotRoutes());
    }

    @GetMapping("/recommend-routes")
    public Result<List<TourRoute>> recommendRoutes() {
        return Result.ok(homeService.recommendRoutes());
    }

    @GetMapping("/reviews")
    public Result<List<TourReview>> reviews() {
        return Result.ok(homeService.reviews());
    }

    @GetMapping("/reminders")
    public Result<List<HomeReminderVO>> reminders() {
        return Result.ok(homeService.reminders());
    }
}
