package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Goods;
import com.shopping.entity.Order;
import com.shopping.entity.OrderItem;
import com.shopping.mapper.GoodsMapper;
import com.shopping.mapper.OrderMapper;
import com.shopping.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderItemService extends ServiceImpl<OrderItemMapper, OrderItem> {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<OrderItem> listByOrderId(Long orderId) {
        List<OrderItem> items = list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items == null || items.isEmpty()) {
            Order order = orderMapper.selectById(orderId);
            if (order != null && order.getGroupId() != null) {
                items = list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getGroupId, order.getGroupId()));
            }
        }
        fillMissingPics(items);
        fillImageMeta(items);
        return items;
    }

    /** 为所有 OrderItem 填充 currentGoodsPic 和 firstGalleryPic */
    public void fillImageMeta(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return;
        List<Long> goodsIds = items.stream()
                .filter(it -> it != null && it.getGoodsId() != null)
                .map(OrderItem::getGoodsId)
                .distinct()
                .collect(Collectors.toList());
        if (goodsIds.isEmpty()) return;

        // 批量查询当前商品主图
        List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .select(Goods::getId, Goods::getGoodsPic)
                .in(Goods::getId, goodsIds));
        Map<Long, String> currentPicMap = new HashMap<>();
        for (Goods g : goodsList) {
            if (g == null || g.getId() == null) continue;
            String pic = g.getGoodsPic();
            if (pic == null || pic.isBlank()) continue;
            currentPicMap.put(g.getId(), pic);
        }

        // 批量查询 tb_goods_pic 第一张
        Map<Long, String> firstGalleryMap = new HashMap<>();
        for (Long gid : goodsIds) {
            try {
                Map<String, Object> row = jdbcTemplate.queryForMap(
                        "SELECT pic_url FROM tb_goods_pic WHERE goods_id = ? AND is_deleted = 0 ORDER BY pic_sort, pic_id LIMIT 1",
                        gid);
                if (row != null) {
                    String url = (String) row.get("pic_url");
                    if (url != null && !url.isBlank()) firstGalleryMap.put(gid, url);
                }
            } catch (Exception ignored) { /* 表或数据不存在 */ }
        }

        for (OrderItem it : items) {
            if (it == null) continue;
            Long goodsId = it.getGoodsId();
            if (goodsId == null) continue;
            String cur = currentPicMap.get(goodsId);
            String gal = firstGalleryMap.get(goodsId);
            String snap = it.getGoodsPic();
            it.setCurrentGoodsPic(cur);
            it.setFirstGalleryPic(gal);
            // 覆写 goodsPic 为 displayPic：当前主图 > 图集第一张 > 订单快照
            String display = (cur != null && !cur.isBlank() ? cur :
                            (gal != null && !gal.isBlank() ? gal :
                            (snap != null && !snap.isBlank() ? snap : null)));
            if (display != null) it.setGoodsPic(display);
        }
    }

    private void fillMissingPics(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return;
        List<Long> goodsIds = items.stream()
                .filter(it -> {
                    if (it == null || it.getGoodsId() == null) return false;
                    String pic = it.getGoodsPic();
                    if (pic == null || pic.isBlank()) return true;
                    String v = pic.trim();
                    return v.startsWith("/goods/") || v.startsWith("goods/") || v.startsWith("/images/") || v.startsWith("images/");
                })
                .map(OrderItem::getGoodsId)
                .distinct()
                .collect(Collectors.toList());
        if (goodsIds.isEmpty()) return;

        List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .select(Goods::getId, Goods::getGoodsPic)
                .in(Goods::getId, goodsIds));
        Map<Long, String> picMap = new HashMap<>();
        for (Goods g : goodsList) {
            if (g == null || g.getId() == null) continue;
            String pic = g.getGoodsPic();
            if (pic == null || pic.isBlank()) continue;
            picMap.put(g.getId(), pic);
        }

        for (OrderItem it : items) {
            if (it == null) continue;
            String existedPic = it.getGoodsPic();
            boolean invalidLocal = false;
            if (existedPic != null && !existedPic.isBlank()) {
                String v = existedPic.trim();
                invalidLocal = v.startsWith("/goods/") || v.startsWith("goods/") || v.startsWith("/images/") || v.startsWith("images/");
            }
            if (existedPic != null && !existedPic.isBlank() && !invalidLocal) continue;
            String pic = picMap.get(it.getGoodsId());
            if (pic == null || pic.isBlank()) continue;
            it.setGoodsPic(pic);
        }
    }
}
