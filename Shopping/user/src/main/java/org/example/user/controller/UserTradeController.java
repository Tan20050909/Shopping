package org.example.user.controller;

import jakarta.validation.Valid;
import org.example.common.PageResult;
import org.example.common.Result;
import org.example.context.UserContext;
import org.example.dto.AfterSaleRequest;
import org.example.dto.AddressRequest;
import org.example.dto.CreateOrderRequest;
import org.example.dto.PayRequest;
import org.example.dto.CartItemRequest;
import org.example.service.ShoppingService;
import org.example.user.dto.CreateAfterSaleCommand;
import org.example.user.dto.CreateReviewCommand;
import org.example.user.dto.UpdateCartItemRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserTradeController {
    private final ShoppingService shoppingService;

    public UserTradeController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    @GetMapping("/cart")
    public Result<List<Map<String, Object>>> cart() {
        return Result.ok(shoppingService.cart(UserContext.requireCurrentUserId()));
    }

    @PostMapping("/cart/items")
    public Result<Void> addCartItem(@Valid @RequestBody CartItemRequest request) {
        shoppingService.addCart(UserContext.requireCurrentUserId(), request);
        return Result.ok();
    }

    @PutMapping("/cart/items/{cartItemId}")
    public Result<Void> updateCartItem(@PathVariable long cartItemId, @Valid @RequestBody UpdateCartItemRequest request) {
        long userId = UserContext.requireCurrentUserId();
        if (request.num() != null) {
            shoppingService.updateCartNum(userId, cartItemId, request.num());
        }
        if (request.selected() != null) {
            shoppingService.updateCartSelected(userId, cartItemId, request.selected());
        }
        return Result.ok();
    }

    @DeleteMapping("/cart/items/{cartItemId}")
    public Result<Void> deleteCartItem(@PathVariable long cartItemId) {
        shoppingService.deleteCart(UserContext.requireCurrentUserId(), cartItemId);
        return Result.ok();
    }

    @DeleteMapping("/cart/checked")
    public Result<Void> deleteCheckedCartItems() {
        shoppingService.deleteCheckedCart(UserContext.requireCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/cart/selected")
    public Result<Void> updateAllCartSelected(@RequestParam boolean selected) {
        shoppingService.updateAllCartSelected(UserContext.requireCurrentUserId(), selected);
        return Result.ok();
    }

    @GetMapping("/addresses")
    public Result<List<Map<String, Object>>> addresses() {
        return Result.ok(shoppingService.addresses(UserContext.requireCurrentUserId()));
    }

    @PostMapping("/addresses")
    public Result<Map<String, Object>> addAddress(@Valid @RequestBody AddressRequest request) {
        long addrId = shoppingService.saveAddress(UserContext.requireCurrentUserId(), request, null);
        return Result.ok(Map.of("addrId", addrId));
    }

    @PutMapping("/addresses/{addrId}")
    public Result<Void> updateAddress(@PathVariable long addrId, @Valid @RequestBody AddressRequest request) {
        shoppingService.saveAddress(UserContext.requireCurrentUserId(), request, addrId);
        return Result.ok();
    }

    @DeleteMapping("/addresses/{addrId}")
    public Result<Void> deleteAddress(@PathVariable long addrId) {
        shoppingService.deleteAddress(UserContext.requireCurrentUserId(), addrId);
        return Result.ok();
    }

    @PutMapping("/addresses/{addrId}/default")
    public Result<Void> setDefaultAddress(@PathVariable long addrId) {
        shoppingService.setDefaultAddress(UserContext.requireCurrentUserId(), addrId);
        return Result.ok();
    }

    @PostMapping("/orders/preview")
    public Result<Map<String, Object>> previewOrder(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(shoppingService.previewOrder(UserContext.requireCurrentUserId(), request));
    }

    @PostMapping("/orders")
    public Result<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(shoppingService.createOrder(UserContext.requireCurrentUserId(), request));
    }

    @PutMapping("/orders/{orderId}/address")
    public Result<Void> updateOrderAddress(@PathVariable long orderId, @RequestParam long addrId) {
        shoppingService.updateOrderAddress(UserContext.requireCurrentUserId(), orderId, addrId);
        return Result.ok();
    }

    @PostMapping("/payments/{groupId}/pay")
    public Result<Map<String, Object>> pay(@PathVariable long groupId, @RequestBody(required = false) PayRequest request) {
        return Result.ok(shoppingService.pay(
                UserContext.requireCurrentUserId(),
                groupId,
                request == null ? new PayRequest(9) : request
        ));
    }

    @GetMapping("/orders")
    public Result<PageResult<Map<String, Object>>> orders(@RequestParam(required = false) Integer status,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(shoppingService.orders(UserContext.requireCurrentUserId(), status, pageNum, pageSize));
    }

    @GetMapping("/orders/{orderId}")
    public Result<Map<String, Object>> orderDetail(@PathVariable long orderId) {
        return Result.ok(shoppingService.orderDetail(UserContext.requireCurrentUserId(), orderId));
    }

    @PutMapping("/orders/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable long orderId) {
        shoppingService.cancelOrder(UserContext.requireCurrentUserId(), orderId);
        return Result.ok();
    }

    @PutMapping("/orders/{orderId}/receive")
    public Result<Void> confirmReceive(@PathVariable long orderId) {
        shoppingService.confirmReceive(UserContext.requireCurrentUserId(), orderId);
        return Result.ok();
    }

    @PostMapping("/orders/{orderId}/rebuy")
    public Result<Map<String, Object>> rebuy(@PathVariable long orderId) {
        return Result.ok(shoppingService.rebuy(UserContext.requireCurrentUserId(), orderId));
    }

    @GetMapping("/orders/{orderId}/logistics")
    public Result<Map<String, Object>> logistics(@PathVariable long orderId) {
        return Result.ok(shoppingService.logistics(UserContext.requireCurrentUserId(), orderId));
    }

    @PostMapping("/after-sales")
    public Result<Map<String, Object>> createAfterSale(@Valid @RequestBody CreateAfterSaleCommand request) {
        return Result.ok(shoppingService.createAfterSale(
                UserContext.requireCurrentUserId(),
                request.orderId(),
                new AfterSaleRequest(
                        request.orderItemId(),
                        request.afterSaleType(),
                        request.applyReason(),
                        request.applyEvidence(),
                        request.applyAmount()
                )
        ));
    }

    @GetMapping("/after-sales")
    public Result<List<Map<String, Object>>> afterSales() {
        return Result.ok(shoppingService.afterSales(UserContext.requireCurrentUserId()));
    }

    @GetMapping("/after-sales/{afterSaleId}")
    public Result<Map<String, Object>> afterSaleDetail(@PathVariable long afterSaleId) {
        return Result.ok(shoppingService.afterSaleDetail(UserContext.requireCurrentUserId(), afterSaleId));
    }

    @PutMapping("/after-sales/{afterSaleId}/cancel")
    public Result<Map<String, Object>> cancelAfterSale(@PathVariable long afterSaleId) {
        return Result.ok(shoppingService.cancelAfterSale(UserContext.requireCurrentUserId(), afterSaleId));
    }

    @PostMapping("/after-sales/{afterSaleId}/return-logistics")
    public Result<Void> submitReturnLogistics(@PathVariable long afterSaleId, @RequestBody Map<String, String> body) {
        String expressCompany = body.getOrDefault("expressCompany", "").trim();
        String expressNo = body.getOrDefault("expressNo", "").trim();
        shoppingService.submitReturnLogistics(UserContext.requireCurrentUserId(), afterSaleId, expressCompany, expressNo);
        return Result.ok();
    }

    @PostMapping("/disputes")
    public Result<Map<String, Object>> createDispute(@RequestBody Map<String, Object> body) {
        long afterSaleId = Long.parseLong(String.valueOf(body.get("afterSaleId")));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        String desc = String.valueOf(body.getOrDefault("desc", ""));
        return Result.ok(shoppingService.createDispute(UserContext.requireCurrentUserId(), afterSaleId, reason, desc));
    }

    @PostMapping("/reviews")
    public Result<Map<String, Object>> createReview(@Valid @RequestBody CreateReviewCommand request) {
        return Result.ok(shoppingService.createReview(UserContext.requireCurrentUserId(), request));
    }

    @GetMapping("/reviews")
    public Result<List<Map<String, Object>>> myReviews() {
        return Result.ok(shoppingService.myReviews(UserContext.requireCurrentUserId()));
    }

    @DeleteMapping("/reviews/{commentId}")
    public Result<Void> deleteReview(@PathVariable long commentId) {
        shoppingService.deleteReview(UserContext.requireCurrentUserId(), commentId);
        return Result.ok();
    }

    @PostMapping("/reviews/upload-image")
    public Result<Map<String, Object>> uploadReviewImage(@RequestParam("file") MultipartFile file,
                                                         HttpServletRequest request) {
        return Result.ok(shoppingService.uploadReviewImage(file, request));
    }

    @GetMapping("/products/{goodsId}/reviews")
    public Result<PageResult<Map<String, Object>>> productReviews(@PathVariable long goodsId,
                                                                  @RequestParam(defaultValue = "1") int pageNum,
                                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(shoppingService.productReviews(goodsId, pageNum, pageSize));
    }
}
