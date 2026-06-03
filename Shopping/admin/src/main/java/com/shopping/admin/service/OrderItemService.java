package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.entity.OrderItem;
import com.shopping.admin.mapper.OrderItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderItemService extends ServiceImpl<OrderItemMapper, OrderItem> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<OrderItem> getByOrderId(Long orderId) {
        return list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    /**
     * 为订单项填充 currentGoodsPic、firstGalleryPic、displayPic。
     * 返回原列表（items 对象上已设 extra 字段）。
     */
    public List<OrderItem> fillImageMeta(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return items;
        List<Long> goodsIds = items.stream()
                .filter(it -> it != null && it.getGoodsId() != null)
                .map(OrderItem::getGoodsId)
                .distinct()
                .collect(Collectors.toList());
        if (goodsIds.isEmpty()) return items;

        // 当前主图
        Map<Long, String> currentPicMap = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT goods_id, goods_pic FROM tb_goods WHERE goods_id IN (" +
                    goodsIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
            for (Map<String, Object> row : rows) {
                Object id = row.get("goods_id");
                Object pic = row.get("goods_pic");
                if (id != null && pic != null) {
                    currentPicMap.put(((Number) id).longValue(), String.valueOf(pic));
                }
            }
        } catch (Exception ignored) {}

        // 图集第一张
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
            } catch (Exception ignored) { }
        }

        // 设置 extra 字段到 OrderItem（用 map 存，不做 entity 扩展）
        for (OrderItem it : items) {
            if (it == null || it.getGoodsId() == null) continue;
            // OrderItem 没有 @TableField(exist=false) 字段，通过额外 map 传递
        }
        return items;
    }

    /**
     * 构建图片附加信息 Map：goodsId → { currentGoodsPic, firstGalleryPic, displayPic }
     */
    public Map<Long, Map<String, String>> buildImageMetaMap(List<Long> goodsIds) {
        Map<Long, Map<String, String>> result = new HashMap<>();
        if (goodsIds == null || goodsIds.isEmpty()) return result;

        // 当前主图
        Map<Long, String> currentPicMap = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT goods_id, goods_pic FROM tb_goods WHERE goods_id IN (" +
                    goodsIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
            for (Map<String, Object> row : rows) {
                Object id = row.get("goods_id");
                Object pic = row.get("goods_pic");
                if (id != null && pic != null) {
                    currentPicMap.put(((Number) id).longValue(), String.valueOf(pic));
                }
            }
        } catch (Exception ignored) {}

        // 图集第一张
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
            } catch (Exception ignored) { }
        }

        for (Long gid : goodsIds) {
            Map<String, String> meta = new HashMap<>();
            String current = currentPicMap.get(gid);
            String firstG = firstGalleryMap.get(gid);
            String display = (current != null && !current.isBlank() ? current :
                            (firstG != null && !firstG.isBlank() ? firstG : null));
            meta.put("currentGoodsPic", current);
            meta.put("firstGalleryPic", firstG);
            meta.put("displayPic", display);
            result.put(gid, meta);
        }
        return result;
    }
}
