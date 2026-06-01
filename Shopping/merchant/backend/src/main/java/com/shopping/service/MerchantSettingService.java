package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantSetting;
import com.shopping.mapper.MerchantSettingMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class MerchantSettingService extends ServiceImpl<MerchantSettingMapper, MerchantSetting> {

    public MerchantSetting getByMerchantId(Long merchantId) {
        return getOne(new LambdaQueryWrapper<MerchantSetting>().eq(MerchantSetting::getMerchantId, merchantId));
    }

    public boolean updateSetting(MerchantSetting setting) {
        setting.setUpdateTime(LocalDateTime.now());
        MerchantSetting existing = getByMerchantId(setting.getMerchantId());
        if (existing == null) {
            return save(setting);
        }
        return update(setting, new LambdaQueryWrapper<MerchantSetting>().eq(MerchantSetting::getMerchantId, setting.getMerchantId()));
    }
}
