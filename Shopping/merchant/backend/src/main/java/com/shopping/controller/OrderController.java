package com.shopping.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopping.entity.Logistics;
import com.shopping.entity.LogisticsTrace;
import com.shopping.entity.Order;
import com.shopping.entity.OrderDetail;
import com.shopping.entity.OrderItem;
import com.shopping.service.LogisticsService;
import com.shopping.service.LogisticsTraceService;
import com.shopping.service.MerchantService;
import com.shopping.service.OrderItemService;
import com.shopping.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private LogisticsTraceService logisticsTraceService;

    @Autowired
    private MerchantService merchantService;

    @GetMapping("/list")
    public List<Order> list(
            @RequestParam Long merchantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId
    ) {
        return orderService.listByMerchantId(merchantId, status, userId);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Order order = orderService.getById(id);
        if (order != null && order.getMerchantId() != null) {
            merchantService.requireOperating(order.getMerchantId());
        }
        boolean ok = orderService.updateStatus(id, status);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单状态更新失败");
        }
        return true;
    }

    @PutMapping("/{id}/freight")
    public Order updateFreight(@PathVariable Long id, @RequestParam BigDecimal freight) {
        Order order = orderService.getById(id);
        if (order != null && order.getMerchantId() != null) {
            merchantService.requireOperating(order.getMerchantId());
        }
        boolean ok = orderService.updateFreight(id, freight);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮费更新失败");
        }
        Order updated = orderService.getById(id);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "邮费更新后查询失败");
        }
        return updated;
    }

    @PutMapping("/{id}/pay-amount")
    public Order updatePayAmount(@PathVariable Long id, @RequestParam BigDecimal payAmount) {
        Order existed = orderService.getById(id);
        if (existed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        if (existed.getMerchantId() != null) {
            merchantService.requireOperating(existed.getMerchantId());
        }
        if (existed.getStatus() == null || existed.getStatus() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "改价失败（仅待付款订单可改价）");
        }
        BigDecimal total = existed.getTotalAmount() == null ? BigDecimal.ZERO : existed.getTotalAmount();
        BigDecimal freight = existed.getFreight() == null ? BigDecimal.ZERO : existed.getFreight();
        BigDecimal max = total.add(freight);
        BigDecimal req = payAmount == null ? BigDecimal.ZERO : payAmount;
        if (req.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "应付金额不能小于0");
        }
        if (req.compareTo(max) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "应付金额不能大于订单总额（商品金额+运费）");
        }

        boolean ok = orderService.updatePayAmount(id, req);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "改价失败");
        }
        Order updated = orderService.getById(id);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "改价后查询失败");
        }
        BigDecimal actual = updated.getPayAmount() == null ? BigDecimal.ZERO : updated.getPayAmount();
        if (actual.compareTo(req) != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "改价未生效，请检查数据库字段(pay_amount/discount_amount)或触发器");
        }
        return updated;
    }

    @GetMapping("/{id}")
    public OrderDetail detail(@PathVariable Long id) {
        OrderDetail detail = new OrderDetail();
        Order order = orderService.getById(id);
        List<OrderItem> items = orderItemService.listByOrderId(id);
        Logistics logistics = logisticsService.getOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, id)
                .eq(Logistics::getBizType, LogisticsService.BIZ_TYPE_ORDER));
        List<LogisticsTrace> traces = logistics == null ? List.of() : logisticsTraceService.listByLogisticsId(logistics.getId());
        detail.setOrder(order);
        detail.setItems(items);
        detail.setLogistics(logistics);
        detail.setLogisticsTraces(traces);
        return detail;
    }

    @PostMapping("/{id}/ship")
    public Logistics ship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String expressCompany = body.getOrDefault("expressCompany", "");
        String expressNo = body.getOrDefault("expressNo", "");
        Order order = orderService.getById(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        Long orderMerchantId = order.getMerchantId();
        if (orderMerchantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单缺少商家信息");
        }
        // 校验请求体 merchantId 与订单一致
        Long reqMerchantId = body.get("merchantId") == null ? null : Long.valueOf(body.get("merchantId"));
        if (reqMerchantId != null && !reqMerchantId.equals(orderMerchantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId与订单不一致");
        }
        merchantService.requireOperating(orderMerchantId);
        if (expressCompany == null || expressCompany.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "快递公司不能为空");
        }
        if (expressNo == null || expressNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "快递单号不能为空");
        }
        return logisticsService.shipOrder(id, orderMerchantId, expressCompany, expressNo);
    }
}
