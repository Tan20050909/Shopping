package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.CouponScope;
import com.shopping.mapper.CouponScopeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CouponScopeService extends ServiceImpl<CouponScopeMapper, CouponScope> {

    public List<CouponScope> listByCouponIds(List<Long> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) return List.of();
        return list(new LambdaQueryWrapper<CouponScope>().in(CouponScope::getCouponId, couponIds));
    }

    @Transactional
    public void replaceScopes(Long couponId, Integer scopeType, List<Long> targetIds) {
        remove(new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, couponId));
        List<CouponScope> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int st = scopeType == null ? 1 : scopeType;
        if (st == 1) {
            CouponScope scope = new CouponScope();
            scope.setCouponId(couponId);
            scope.setScopeType(1);
            scope.setTargetId(0L);
            scope.setCreateTime(now);
            records.add(scope);
        } else {
            List<Long> targets = targetIds == null ? List.of() : targetIds.stream().filter(v -> v != null && v > 0).distinct().toList();
            for (Long tid : targets) {
                CouponScope scope = new CouponScope();
                scope.setCouponId(couponId);
                scope.setScopeType(st);
                scope.setTargetId(tid);
                scope.setCreateTime(now);
                records.add(scope);
            }
        }
        if (!records.isEmpty()) {
            saveBatch(records);
        }
    }
}
