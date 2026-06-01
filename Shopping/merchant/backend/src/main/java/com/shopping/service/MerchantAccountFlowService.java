package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantAccountFlow;
import com.shopping.mapper.MerchantAccountFlowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantAccountFlowService extends ServiceImpl<MerchantAccountFlowMapper, MerchantAccountFlow> {

    public List<MerchantAccountFlow> listByMerchantId(Long merchantId, Integer limit, Integer flowType) {
        int l = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        LambdaQueryWrapper<MerchantAccountFlow> qw = new LambdaQueryWrapper<MerchantAccountFlow>()
                .eq(MerchantAccountFlow::getMerchantId, merchantId)
                .orderByDesc(MerchantAccountFlow::getCreateTime)
                .orderByDesc(MerchantAccountFlow::getId)
                .last("limit " + l);
        if (flowType != null) {
            qw.eq(MerchantAccountFlow::getFlowType, flowType);
        }
        return list(qw);
    }
}

