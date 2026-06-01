package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.ShopFollow;
import com.shopping.mapper.ShopFollowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShopFollowService extends ServiceImpl<ShopFollowMapper, ShopFollow> {

    @Transactional
    public boolean toggle(Long userId, Long merchantId) {
        ShopFollow existed = getOne(new LambdaQueryWrapper<ShopFollow>()
                .eq(ShopFollow::getUserId, userId)
                .eq(ShopFollow::getMerchantId, merchantId));
        if (existed != null) {
            removeById(existed.getId());
            return false;
        }
        ShopFollow created = new ShopFollow();
        created.setUserId(userId);
        created.setMerchantId(merchantId);
        created.setCreateTime(LocalDateTime.now());
        save(created);
        return true;
    }

    public long countByMerchantId(Long merchantId) {
        try {
            return count(new LambdaQueryWrapper<ShopFollow>().eq(ShopFollow::getMerchantId, merchantId));
        } catch (Exception e) {
            return 0L;
        }
    }

    public boolean isFollowed(Long userId, Long merchantId) {
        try {
            return count(new LambdaQueryWrapper<ShopFollow>()
                    .eq(ShopFollow::getUserId, userId)
                    .eq(ShopFollow::getMerchantId, merchantId)) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<Long, Long> countByMerchantIds(List<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) return new HashMap<>();
        try {
            List<ShopFollow> list = list(new LambdaQueryWrapper<ShopFollow>().in(ShopFollow::getMerchantId, merchantIds));
            return list.stream().collect(Collectors.groupingBy(ShopFollow::getMerchantId, Collectors.counting()));
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public Map<Long, Boolean> followedMap(Long userId, List<Long> merchantIds) {
        Map<Long, Boolean> map = new HashMap<>();
        if (userId == null || merchantIds == null || merchantIds.isEmpty()) return map;
        List<ShopFollow> list;
        try {
            list = list(new LambdaQueryWrapper<ShopFollow>()
                    .eq(ShopFollow::getUserId, userId)
                    .in(ShopFollow::getMerchantId, merchantIds));
        } catch (Exception e) {
            return map;
        }
        for (ShopFollow f : list) {
            if (f == null || f.getMerchantId() == null) continue;
            map.put(f.getMerchantId(), true);
        }
        return map;
    }
}
