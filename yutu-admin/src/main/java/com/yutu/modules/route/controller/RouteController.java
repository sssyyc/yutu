package com.yutu.modules.route.controller;

import com.yutu.common.result.Result;
import com.yutu.modules.model.entity.TourCategory;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.entity.TourTag;
import com.yutu.modules.route.service.RouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public Result<List<TourRoute>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Long tagId,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(routeService.list(categoryId, tagId, keyword));
    }

    @GetMapping("/categories")
    public Result<List<TourCategory>> categories() {
        return Result.ok(routeService.categories());
    }

    @GetMapping("/tags")
    public Result<List<TourTag>> tags() {
        return Result.ok(routeService.tags());
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(routeService.detail(id));
    }

    @GetMapping("/{id}/dates")
    public Result<List<TourDepartureDate>> dates(@PathVariable("id") Long routeId) {
        return Result.ok(routeService.dates(routeId));
    }
}
