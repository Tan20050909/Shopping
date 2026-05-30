package com.shopping.controller;

import com.shopping.entity.MerchantSetting;
import com.shopping.service.MerchantSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant-setting")
public class MerchantSettingController {

    @Autowired
    private MerchantSettingService settingService;

    @GetMapping
    public MerchantSetting get(@RequestParam Long merchantId) {
        return settingService.getByMerchantId(merchantId);
    }

    @PutMapping
    public boolean update(@RequestBody MerchantSetting setting) {
        return settingService.updateSetting(setting);
    }
}
