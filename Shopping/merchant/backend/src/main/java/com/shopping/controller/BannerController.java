package com.shopping.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopping.entity.Banner;
import com.shopping.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/banner")
public class BannerController {
    
    @Autowired
    private BannerService bannerService;
    
    @GetMapping("/list")
    public List<Banner> list() {
        return bannerService.list(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort));
    }
    
    @GetMapping("/active")
    public List<Banner> getActiveBanners() {
        return bannerService.getActiveBanners();
    }
    
    @PostMapping("/add")
    public boolean add(@RequestBody Banner banner) {
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        return bannerService.save(banner);
    }
    
    @PutMapping("/update")
    public boolean update(@RequestBody Banner banner) {
        banner.setUpdateTime(LocalDateTime.now());
        return bannerService.updateById(banner);
    }
    
    @DeleteMapping("/delete/{id}")
    public boolean delete(@PathVariable Long id) {
        return bannerService.removeById(id);
    }
}
