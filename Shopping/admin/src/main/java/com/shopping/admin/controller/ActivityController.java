package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Activity;
import com.shopping.admin.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer activityType,
                          @RequestParam(required = false) Integer status) {
        return Result.success(activityService.listActivities(current, size, keyword, activityType, status));
    }

    @GetMapping("/{id}")
    public Result<Activity> detail(@PathVariable Long id) {
        return Result.success(activityService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Activity activity) {
        activityService.save(activity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Activity activity) {
        activityService.updateById(activity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.removeById(id);
        return Result.success();
    }
}
