package com.shopping.controller;

import com.shopping.entity.Coupon;
import com.shopping.service.CouponService;
import com.shopping.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private MerchantService merchantService;

    @PostMapping
    public Coupon create(@RequestBody Coupon coupon) {
        if (coupon.getMerchantId() != null) {
            merchantService.requireOperating(coupon.getMerchantId());
        }
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
        Coupon existing = couponService.getById(id);
        if (existing == null) {
            throw new RuntimeException("优惠券不存在");
        }
        merchantService.requireOperating(existing.getMerchantId());
        couponService.validateForCreateOrUpdate(coupon);
        if (coupon.getSurplusNum() == null) {
            coupon.setSurplusNum(coupon.getTotalCount());
        }
        return couponService.update(id, coupon);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Coupon existing = couponService.getById(id);
        if (existing != null && existing.getMerchantId() != null) {
            merchantService.requireOperating(existing.getMerchantId());
        }
        return couponService.updateStatus(id, status);
    }

    @PostMapping("/grant")
    public boolean grant(@RequestBody Map<String, Object> body) {
        Long merchantId = body.get("merchantId") == null ? null : Long.valueOf(String.valueOf(body.get("merchantId")));
        Long couponId = body.get("couponId") == null ? null : Long.valueOf(String.valueOf(body.get("couponId")));
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        if (merchantId != null) {
            merchantService.requireOperating(merchantId);
        }
        return couponService.grantToUser(merchantId, couponId, userId);
    }

    @PutMapping("/grant")
    public boolean grantByPut(@RequestBody Map<String, Object> body) {
        return grant(body);
    }
}
