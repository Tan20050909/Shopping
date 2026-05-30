package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Merchant;
import com.shopping.mapper.MerchantMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MerchantService extends ServiceImpl<MerchantMapper, Merchant> {

    public Merchant apply(Merchant merchant) {
        merchant.setStatus(0);
        if (merchant.getAuditStatus() == null) {
            merchant.setAuditStatus(0);
        }
        if (merchant.getIsDeleted() == null) {
            merchant.setIsDeleted(0);
        }
        merchant.setRegisterTime(LocalDateTime.now());
        merchant.setUpdateTime(LocalDateTime.now());
        save(merchant);
        return merchant;
    }

    public List<Merchant> listByMerchantId(Long merchantId) {
        return list(new LambdaQueryWrapper<Merchant>().eq(Merchant::getMerchantId, merchantId));
    }
}
