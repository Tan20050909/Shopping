package org.example.controller;

import org.example.common.Result;
import org.example.service.ShoppingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev/after-sales")
@ConditionalOnProperty(name = "dev.api.enabled", havingValue = "true")
public class DevAfterSaleController {
    private final ShoppingService shoppingService;

    public DevAfterSaleController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @PostMapping("/{afterSaleId}/approve")
    public Result<Map<String, Object>> approve(@PathVariable long afterSaleId) {
        return Result.ok(shoppingService.devApproveAfterSale(afterSaleId));
    }

    @PostMapping("/{afterSaleId}/reject")
    public Result<Map<String, Object>> reject(@PathVariable long afterSaleId) {
        return Result.ok(shoppingService.devRejectAfterSale(afterSaleId));
    }

    @PostMapping("/{afterSaleId}/refund-success")
    public Result<Map<String, Object>> refundSuccess(@PathVariable long afterSaleId) {
        return Result.ok(shoppingService.devRefundSuccess(afterSaleId));
    }
}
