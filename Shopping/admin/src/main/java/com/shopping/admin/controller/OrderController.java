package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Order;
import com.shopping.admin.entity.OrderItem;
import com.shopping.admin.service.OrderService;
import com.shopping.admin.service.OrderItemService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer orderStatus,
                          @RequestParam(required = false) Integer payStatus) {
        return Result.success(orderService.listOrders(current, size, keyword, orderStatus, payStatus));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) return Result.error(404, "订单不存在");
        List<OrderItem> items = orderItemService.getByOrderId(id);
        // 为订单项补充图片元数据
        List<Long> goodsIds = items.stream()
                .filter(it -> it != null && it.getGoodsId() != null)
                .map(OrderItem::getGoodsId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Map<String, String>> imageMeta = orderItemService.buildImageMetaMap(goodsIds);
        for (OrderItem it : items) {
            if (it == null || it.getGoodsId() == null) continue;
            Map<String, String> meta = imageMeta.get(it.getGoodsId());
            if (meta != null) {
                it.setGoodsPic(
                    meta.getOrDefault("displayPic", meta.getOrDefault("currentGoodsPic", it.getGoodsPic()))
                );
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("items", items);
        return Result.success(data);
    }

    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id, @RequestBody ShipRequest request) {
        orderService.shipOrder(id, request.trackingNo(), request.shippingCompany());
        return Result.success();
    }

    @PostMapping("/{id}/remind-ship")
    public Result<Void> remindShip(@PathVariable Long id,
                                   @RequestBody(required = false) RemindShipRequest request,
                                   HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        orderService.remindMerchantShip(id, request != null ? request.remark() : null, adminId);
        return Result.success();
    }


    @PutMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id, @RequestBody RefundRequest request) {
        orderService.refundOrder(id, request.refundAmount(), request.refundReason());
        return Result.success();
    }

    @PutMapping("/{id}/note")
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody NoteRequest request) {
        orderService.updateNote(id, request.adminNote());
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success();
    }

    public record ShipRequest(String trackingNo, String shippingCompany) {}
    public record RemindShipRequest(String remark) {}
    public record RefundRequest(java.math.BigDecimal refundAmount, String refundReason) {}
    public record NoteRequest(String adminNote) {}
}
