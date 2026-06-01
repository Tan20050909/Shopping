package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Merchant;
import com.shopping.mapper.MerchantMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

    /** 校验商家处于可经营状态：审核通过 + 正常营业 + 未删除 */
    public void requireOperating(Long merchantId) {
        Merchant m = getById(merchantId);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商家不存在");
        }
        if (m.getIsDeleted() != null && m.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "商家账号已被删除");
        }
        if (m.getAuditStatus() == null || m.getAuditStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "店铺未审核通过，暂不可经营");
        }
        if (m.getStatus() == null || m.getStatus() != 1) {
            if (m.getStatus() != null && m.getStatus() == 3) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "店铺已被平台冻结，暂不可操作");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "店铺未营业，暂不可操作");
        }
    }
}
