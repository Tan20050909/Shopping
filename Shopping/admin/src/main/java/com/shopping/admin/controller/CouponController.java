package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Coupon;
import com.shopping.admin.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer couponType,
                          @RequestParam(required = false) Integer status) {
        return Result.success(couponService.listCoupons(current, size, keyword, couponType, status));
    }

    @GetMapping("/{id}")
    public Result<Coupon> detail(@PathVariable Long id) {
        return Result.success(couponService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Coupon coupon) {
        couponService.addCoupon(coupon);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Coupon coupon) {
        couponService.updateCoupon(coupon);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateCouponStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.success();
    }
}
