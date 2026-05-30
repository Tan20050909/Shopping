package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantActivity;
import com.shopping.entity.PlatformActivity;
import com.shopping.mapper.MerchantActivityMapper;
import com.shopping.mapper.PlatformActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MerchantActivityService extends ServiceImpl<MerchantActivityMapper, MerchantActivity> {

    @Autowired
    private PlatformActivityMapper platformActivityMapper;

    public MerchantActivity apply(MerchantActivity activity) {
        activity.setStatus(0);
        activity.setCreateTime(LocalDateTime.now());
        save(activity);
        return activity;
    }

    public List<MerchantActivity> listByMerchantId(Long merchantId) {
        return list(new LambdaQueryWrapper<MerchantActivity>()
                .eq(MerchantActivity::getMerchantId, merchantId)
                .orderByDesc(MerchantActivity::getCreateTime));
    }

    public List<PlatformActivity> listPlatformActivities() {
        return platformActivityMapper.selectList(new LambdaQueryWrapper<PlatformActivity>()
                .orderByDesc(PlatformActivity::getCreateTime));
    }
}
