package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.entity.OrderItem;
import com.shopping.admin.mapper.OrderItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemService extends ServiceImpl<OrderItemMapper, OrderItem> {

    public List<OrderItem> getByOrderId(Long orderId) {
        return list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }
}
