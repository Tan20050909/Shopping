package com.shopping.controller;

import com.shopping.entity.Logistics;
import com.shopping.entity.LogisticsTrace;
import com.shopping.entity.Order;
import com.shopping.service.LogisticsService;
import com.shopping.service.MerchantService;
import com.shopping.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MerchantService merchantService;

    @PostMapping
    public Logistics create(@RequestBody Logistics logistics) {
        if (logistics.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId不能为空");
        }
        Order order = orderService.getById(logistics.getOrderId());
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        if (order.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单缺少商家信息");
        }
        Long orderMerchantId = order.getMerchantId();
        Long reqMerchantId = logistics.getMerchantId();
        if (reqMerchantId != null && !reqMerchantId.equals(orderMerchantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId与订单不一致");
        }
        merchantService.requireOperating(orderMerchantId);
        logistics.setMerchantId(orderMerchantId);
        return logisticsService.create(logistics);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Logistics existing = logisticsService.getById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "物流记录不存在");
        }
        if (existing.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "物流记录缺少订单信息");
        }
        Order order = orderService.getById(existing.getOrderId());
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关联订单不存在");
        }
        if (order.getMerchantId() != null) {
            merchantService.requireOperating(order.getMerchantId());
        }
        return logisticsService.updateStatus(id, status);
    }

    @GetMapping("/{id}/traces")
    public List<LogisticsTrace> listTraces(@PathVariable Long id) {
        return logisticsService.listTracesByLogisticsId(id);
    }

    @GetMapping("/order/{orderId}/traces")
    public List<LogisticsTrace> listTracesByOrderId(@PathVariable Long orderId, @RequestParam(required = false) Integer bizType) {
        return logisticsService.listTracesByOrderId(orderId, bizType);
    }
}
