package com.shopping.controller;

import com.shopping.entity.MerchantActivity;
import com.shopping.entity.PlatformActivity;
import com.shopping.service.MerchantActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class MerchantActivityController {

    @Autowired
    private MerchantActivityService activityService;

    @GetMapping("/platform/list")
    public List<PlatformActivity> listPlatformActivities() {
        return activityService.listPlatformActivities();
    }

    @PostMapping("/apply")
    public MerchantActivity apply(@RequestBody MerchantActivity activity) {
        return activityService.apply(activity);
    }

    @GetMapping("/list")
    public List<MerchantActivity> list(@RequestParam Long merchantId) {
        return activityService.listByMerchantId(merchantId);
    }
}
