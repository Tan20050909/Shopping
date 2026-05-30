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

    public List<OrderItem> listByOrderId(Long orderId) {
        List<OrderItem> items = list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items == null || items.isEmpty()) {
            Order order = orderMapper.selectById(orderId);
            if (order != null && order.getGroupId() != null) {
                items = list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getGroupId, order.getGroupId()));
            }
        }
        fillMissingPics(items);
        return items;
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
