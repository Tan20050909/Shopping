package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Category;
import com.shopping.entity.Goods;
import com.shopping.entity.GoodsPic;
import com.shopping.entity.Merchant;
import com.shopping.mapper.CategoryMapper;
import com.shopping.mapper.GoodsMapper;
import com.shopping.mapper.GoodsPicMapper;
import com.shopping.mapper.MerchantMapper;
import com.shopping.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsService extends ServiceImpl<GoodsMapper, Goods> {

    @Autowired
    private GoodsPicMapper goodsPicMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private GoodsFavoriteService goodsFavoriteService;

    @Autowired
    private ShopFollowService shopFollowService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    public Goods create(Goods goods) {
        goods.setStatus(0);
        goods.setAuditStatus(0);
        if (goods.getCategoryId() == null) {
            goods.setCategoryId(1L);
        }
        if (goods.getGoodsPic() == null || goods.getGoodsPic().isBlank()) {
            goods.setGoodsPic("");
        }
        goods.setCreateTime(LocalDateTime.now());
        goods.setUpdateTime(LocalDateTime.now());
        save(goods);
        return goods;
    }

    public List<Goods> listByMerchantId(Long merchantId) {
        List<Goods> goods = list(new LambdaQueryWrapper<Goods>().eq(Goods::getMerchantId, merchantId));
        fillCategoryNames(goods);
        if (goods != null && !goods.isEmpty()) {
            List<Long> goodsIds = goods.stream().map(Goods::getId).filter(v -> v != null).collect(Collectors.toList());
            Map<Long, Long> buyCountMap = countBuyByGoodsIds(goodsIds);
            Map<Long, Long> favCountMap = goodsFavoriteService.countByGoodsIds(goodsIds);
            for (Goods g : goods) {
                if (g == null || g.getId() == null) continue;
                g.setBuyCount(buyCountMap.getOrDefault(g.getId(), 0L));
                g.setFavoriteCount(favCountMap.getOrDefault(g.getId(), 0L));
            }
        }
        return goods;
    }

    public boolean updateGoods(Goods goods) {
        goods.setUpdateTime(LocalDateTime.now());
        return updateById(goods);
    }

    public boolean delete(Long id) {
        return removeById(id);
    }

    public boolean updateStatus(Long id, Integer status) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setStatus(status);
        goods.setUpdateTime(LocalDateTime.now());
        return updateById(goods);
    }

    @Transactional
    public boolean batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return true;
        if (status == null) return false;
        for (Long id : ids) {
            if (id == null) continue;
            boolean ok = updateStatus(id, status);
            if (!ok) return false;
        }
        return true;
    }

    @Transactional
    public boolean batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return true;
        for (Long id : ids) {
            if (id == null) continue;
            boolean ok = delete(id);
            if (!ok) return false;
        }
        return true;
    }
    
    public List<Goods> getPublicGoods() {
        return getPublicGoods(null);
    }

    public List<Goods> getPublicGoods(Long userId) {
        List<Goods> goods = list(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1)
                .eq(Goods::getAuditStatus, 1));
        fillPublicMeta(goods, userId);
        return goods;
    }

    private void fillPublicMeta(List<Goods> goods, Long userId) {
        if (goods == null || goods.isEmpty()) return;
        fillCategoryNames(goods);
        List<Long> goodsIds = goods.stream().map(Goods::getId).filter(v -> v != null).collect(Collectors.toList());
        List<Long> merchantIds = goods.stream().map(Goods::getMerchantId).filter(v -> v != null).distinct().collect(Collectors.toList());

        Map<Long, Long> favCountMap = goodsFavoriteService.countByGoodsIds(goodsIds);
        Map<Long, Boolean> favMap = goodsFavoriteService.favoritedMap(userId, goodsIds);
        Map<Long, Long> followCountMap = shopFollowService.countByMerchantIds(merchantIds);
        Map<Long, Boolean> followMap = shopFollowService.followedMap(userId, merchantIds);
        Map<Long, Long> buyCountMap = countBuyByGoodsIds(goodsIds);

        Map<Long, Merchant> merchantMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            List<Merchant> merchants = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>().in(Merchant::getMerchantId, merchantIds));
            for (Merchant m : merchants) {
                if (m == null || m.getMerchantId() == null) continue;
                merchantMap.put(m.getMerchantId(), m);
            }
        }

        for (Goods g : goods) {
            if (g == null) continue;
            Long gid = g.getId();
            Long mid = g.getMerchantId();
            g.setFavoriteCount(favCountMap.getOrDefault(gid, 0L));
            g.setFavorited(Boolean.TRUE.equals(favMap.get(gid)));
            g.setFollowerCount(followCountMap.getOrDefault(mid, 0L));
            g.setFollowed(Boolean.TRUE.equals(followMap.get(mid)));
            g.setBuyCount(buyCountMap.getOrDefault(gid, 0L));
            Merchant m = merchantMap.get(mid);
            if (m != null) {
                g.setMerchantName(m.getMerchantName());
            }
        }
    }

    private Map<Long, Long> countBuyByGoodsIds(List<Long> goodsIds) {
        Map<Long, Long> map = new HashMap<>();
        if (goodsIds == null || goodsIds.isEmpty()) return map;
        try {
            List<Map<String, Object>> rows = orderItemMapper.sumBuyCountByGoodsIds(goodsIds);
            if (rows == null) return map;
            for (Map<String, Object> r : rows) {
                if (r == null) continue;
                Object gid = r.get("goodsId");
                Object cnt = r.get("buyCount");
                Long id = gid instanceof Number ? ((Number) gid).longValue() : null;
                Long c = cnt instanceof Number ? ((Number) cnt).longValue() : null;
                if (id == null) continue;
                map.put(id, c == null ? 0L : c);
            }
        } catch (Exception e) {
            return map;
        }
        return map;
    }

    private void fillCategoryNames(List<Goods> goods) {
        if (goods == null || goods.isEmpty()) return;
        List<Long> cateIds = goods.stream()
                .map(Goods::getCategoryId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        if (cateIds.isEmpty()) return;

        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>().in(Category::getId, cateIds));
        Map<Long, String> map = new HashMap<>();
        for (Category c : categories) {
            if (c == null || c.getId() == null) continue;
            if (c.getName() == null || c.getName().isBlank()) continue;
            map.put(c.getId(), c.getName());
        }
        for (Goods g : goods) {
            if (g == null) continue;
            String name = map.get(g.getCategoryId());
            if (name != null && !name.isBlank()) {
                g.setCategoryName(name);
            }
        }
    }

    @Transactional
    public boolean replacePics(Long goodsId, List<GoodsPic> pics) {
        GoodsPic patch = new GoodsPic();
        patch.setIsDeleted(1);
        goodsPicMapper.update(patch, new LambdaQueryWrapper<GoodsPic>()
                .eq(GoodsPic::getGoodsId, goodsId)
                .eq(GoodsPic::getIsDeleted, 0));

        if (pics == null || pics.isEmpty()) {
            Goods goodsPatch = new Goods();
            goodsPatch.setId(goodsId);
            goodsPatch.setGoodsPic("");
            goodsPatch.setUpdateTime(LocalDateTime.now());
            updateById(goodsPatch);
            return true;
        }

        int sort = 0;
        String firstUrl = "";
        for (GoodsPic p : pics) {
            if (p == null) continue;
            if (p.getUrl() == null || p.getUrl().isBlank()) continue;
            GoodsPic row = new GoodsPic();
            row.setGoodsId(goodsId);
            row.setUrl(p.getUrl());
            row.setSort(p.getSort() == null ? sort : p.getSort());
            row.setIsDeleted(0);
            row.setCreateTime(LocalDateTime.now());
            goodsPicMapper.insert(row);
            if (firstUrl.isEmpty()) {
                firstUrl = p.getUrl();
            }
            sort++;
        }
        if (!firstUrl.isEmpty()) {
            Goods goodsPatch = new Goods();
            goodsPatch.setId(goodsId);
            goodsPatch.setGoodsPic(firstUrl);
            goodsPatch.setUpdateTime(LocalDateTime.now());
            updateById(goodsPatch);
        }
        return true;
    }
}
