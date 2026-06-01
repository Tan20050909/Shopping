package com.shopping.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopping.entity.Goods;
import com.shopping.entity.GoodsPic;
import com.shopping.entity.Merchant;
import com.shopping.mapper.GoodsPicMapper;
import com.shopping.service.GoodsFavoriteService;
import com.shopping.service.GoodsService;
import com.shopping.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsPicMapper goodsPicMapper;

    @Autowired
    private GoodsFavoriteService goodsFavoriteService;

    @Autowired
    private MerchantService merchantService;

    @PostMapping
    public Goods create(@RequestBody Goods goods) {
        if (goods == null || goods.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少merchantId");
        }
        merchantService.requireOperating(goods.getMerchantId());
        return goodsService.create(goods);
    }

    @GetMapping("/list")
    public List<Goods> list(@RequestParam Long merchantId) {
        return goodsService.listByMerchantId(merchantId);
    }

    @GetMapping("/{id}")
    public Goods detail(@PathVariable Long id) {
        return goodsService.getById(id);
    }

    @GetMapping("/{id}/pics")
    public List<GoodsPic> listPics(@PathVariable Long id) {
        return goodsPicMapper.selectList(new LambdaQueryWrapper<GoodsPic>()
                .eq(GoodsPic::getGoodsId, id)
                .eq(GoodsPic::getIsDeleted, 0)
                .orderByAsc(GoodsPic::getSort)
                .orderByAsc(GoodsPic::getId));
    }

    @PutMapping("/{id}/pics")
    public boolean replacePics(@PathVariable Long id, @RequestBody List<GoodsPic> pics) {
        return goodsService.replacePics(id, pics);
    }

    @PutMapping
    public boolean update(@RequestBody Goods goods) {
        if (goods == null || goods.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少商品ID");
        }
        Goods existing = goodsService.getById(goods.getId());
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不存在");
        }
        merchantService.requireOperating(existing.getMerchantId());
        return goodsService.updateGoods(goods);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return goodsService.delete(id);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Goods existing = goodsService.getById(id);
        if (existing != null && existing.getMerchantId() != null) {
            merchantService.requireOperating(existing.getMerchantId());
        }
        return goodsService.updateStatus(id, status);
    }

    @PutMapping("/status/batch")
    public boolean batchUpdateStatus(@RequestParam Integer status, @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品ID列表不能为空");
        }
        java.util.Set<Long> merchantIds = new java.util.HashSet<>();
        for (Long id : ids) {
            Goods g = goodsService.getById(id);
            if (g == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不存在：" + id);
            }
            merchantIds.add(g.getMerchantId());
        }
        for (Long merchantId : merchantIds) {
            merchantService.requireOperating(merchantId);
        }
        boolean ok = goodsService.batchUpdateStatus(ids, status);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批量更新失败");
        }
        return true;
    }

    @DeleteMapping("/batch")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        boolean ok = goodsService.batchDelete(ids);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批量删除失败");
        }
        return true;
    }
    
    @GetMapping("/public")
    public List<Goods> getPublicGoods(@RequestParam(required = false) Long userId) {
        return goodsService.getPublicGoods(userId);
    }

    @PostMapping("/{id}/favorite")
    public Map<String, Object> toggleFavorite(@PathVariable Long id, @RequestParam Long userId) {
        boolean favorited = goodsFavoriteService.toggle(userId, id);
        long count = goodsFavoriteService.countByGoodsId(id);
        Map<String, Object> res = new HashMap<>();
        res.put("favorited", favorited);
        res.put("favoriteCount", count);
        return res;
    }

    @GetMapping("/{id}/favorite/count")
    public Map<String, Object> favoriteCount(@PathVariable Long id) {
        long count = goodsFavoriteService.countByGoodsId(id);
        Map<String, Object> res = new HashMap<>();
        res.put("favoriteCount", count);
        return res;
    }

    @GetMapping("/{id}/favorite/status")
    public Map<String, Object> favoriteStatus(@PathVariable Long id, @RequestParam Long userId) {
        boolean favorited = goodsFavoriteService.isFavorited(userId, id);
        Map<String, Object> res = new HashMap<>();
        res.put("favorited", favorited);
        return res;
    }
}
