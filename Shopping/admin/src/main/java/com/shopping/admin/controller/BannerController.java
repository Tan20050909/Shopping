package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Banner;
import com.shopping.admin.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Integer displayPosition) {
        return Result.success(bannerService.listBanners(current, size, status, displayPosition));
    }

    /** 获取指定位置已启用的轮播图（供前台展示，无需鉴权） */
    @GetMapping("/active")
    public Result<?> activeBanners(@RequestParam(required = false) Integer displayPosition) {
        return Result.success(bannerService.getActiveBanners(displayPosition));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Banner banner) {
        bannerService.addBanner(banner);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Banner banner) {
        bannerService.updateBanner(banner);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        bannerService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@RequestBody List<Long> ids, @RequestParam Integer status) {
        bannerService.batchUpdateStatus(ids, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.removeById(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        bannerService.batchDelete(ids);
        return Result.success();
    }
}
