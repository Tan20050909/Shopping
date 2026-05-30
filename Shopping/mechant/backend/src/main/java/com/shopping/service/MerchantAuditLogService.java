package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantAuditLog;
import com.shopping.mapper.MerchantAuditLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantAuditLogService extends ServiceImpl<MerchantAuditLogMapper, MerchantAuditLog> {

    public List<MerchantAuditLog> listByMerchantId(Long merchantId) {
        return list(new LambdaQueryWrapper<MerchantAuditLog>()
                .eq(MerchantAuditLog::getMerchantId, merchantId)
                .orderByDesc(MerchantAuditLog::getCreateTime)
                .orderByDesc(MerchantAuditLog::getId));
    }
}

