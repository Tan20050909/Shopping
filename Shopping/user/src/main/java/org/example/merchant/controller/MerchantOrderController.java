package org.example.merchant.controller;

import jakarta.validation.Valid;
import org.example.common.PageResult;
import org.example.common.Result;
import org.example.context.MerchantContext;
import org.example.dto.ShipOrderRequest;
import org.example.service.ShoppingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantOrderController {
    private final ShoppingService shoppingService;

    public MerchantOrderController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @GetMapping("/orders")
    public Result<PageResult<Map<String, Object>>> orders(@RequestParam(required = false) Integer status,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(shoppingService.merchantOrders(
                MerchantContext.requireCurrentMerchantId(),
                status,
                pageNum,
                pageSize
        ));
    }

    @PostMapping("/orders/{orderId}/ship")
    public Result<Map<String, Object>> shipOrder(@PathVariable long orderId,
                                                 @Valid @RequestBody ShipOrderRequest request) {
        return Result.ok(shoppingService.shipOrder(
                MerchantContext.requireCurrentMerchantId(),
                orderId,
                request
        ));
    }
}
