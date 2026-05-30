package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.GoodsFavorite;
import com.shopping.mapper.GoodsFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsFavoriteService extends ServiceImpl<GoodsFavoriteMapper, GoodsFavorite> {

    @Transactional
    public boolean toggle(Long userId, Long goodsId) {
        GoodsFavorite existed = getOne(new LambdaQueryWrapper<GoodsFavorite>()
                .eq(GoodsFavorite::getUserId, userId)
                .eq(GoodsFavorite::getGoodsId, goodsId));
        if (existed != null) {
            removeById(existed.getId());
            return false;
        }
        GoodsFavorite created = new GoodsFavorite();
        created.setUserId(userId);
        created.setGoodsId(goodsId);
        created.setCreateTime(LocalDateTime.now());
        save(created);
        return true;
    }

    public long countByGoodsId(Long goodsId) {
        return count(new LambdaQueryWrapper<GoodsFavorite>().eq(GoodsFavorite::getGoodsId, goodsId));
    }

    public boolean isFavorited(Long userId, Long goodsId) {
        return count(new LambdaQueryWrapper<GoodsFavorite>()
                .eq(GoodsFavorite::getUserId, userId)
                .eq(GoodsFavorite::getGoodsId, goodsId)) > 0;
    }

    public Map<Long, Long> countByGoodsIds(List<Long> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) return new HashMap<>();
        List<GoodsFavorite> list = list(new LambdaQueryWrapper<GoodsFavorite>().in(GoodsFavorite::getGoodsId, goodsIds));
        return list.stream().collect(Collectors.groupingBy(GoodsFavorite::getGoodsId, Collectors.counting()));
    }

    public Map<Long, Boolean> favoritedMap(Long userId, List<Long> goodsIds) {
        Map<Long, Boolean> map = new HashMap<>();
        if (userId == null || goodsIds == null || goodsIds.isEmpty()) return map;
        List<GoodsFavorite> list = list(new LambdaQueryWrapper<GoodsFavorite>()
                .eq(GoodsFavorite::getUserId, userId)
                .in(GoodsFavorite::getGoodsId, goodsIds));
        for (GoodsFavorite f : list) {
            if (f == null || f.getGoodsId() == null) continue;
            map.put(f.getGoodsId(), true);
        }
        return map;
    }
}

