package com.shopping.controller;

import com.shopping.entity.Coupon;
import com.shopping.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping
    public Coupon create(@RequestBody Coupon coupon) {
        couponService.validateForCreateOrUpdate(coupon);
        return couponService.create(coupon);
    }

    @GetMapping("/list")
    public List<Coupon> list(@RequestParam Long merchantId) {
        return couponService.listByMerchantId(merchantId);
    }

    @GetMapping("/platform/list")
    public List<Coupon> platformList() {
        return couponService.listPlatformCoupons();
    }

    @PutMapping("/{id}")
    public Coupon update(@PathVariable Long id, @RequestBody Coupon coupon) {
        couponService.validateForCreateOrUpdate(coupon);
        if (coupon.getSurplusNum() == null) {
            coupon.setSurplusNum(coupon.getTotalCount());
        }
        return couponService.update(id, coupon);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return couponService.updateStatus(id, status);
    }

    @PostMapping("/grant")
    public boolean grant(@RequestBody Map<String, Object> body) {
        Long merchantId = body.get("merchantId") == null ? null : Long.valueOf(String.valueOf(body.get("merchantId")));
        Long couponId = body.get("couponId") == null ? null : Long.valueOf(String.valueOf(body.get("couponId")));
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        return couponService.grantToUser(merchantId, couponId, userId);
    }

    @PutMapping("/grant")
    public boolean grantByPut(@RequestBody Map<String, Object> body) {
        return grant(body);
    }
}
