package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.Coupon;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.CouponMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CouponService extends ServiceImpl<CouponMapper, Coupon> {

    public PageResult<Coupon> listCoupons(long current, long size, String keyword, Integer couponType, Integer status) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Coupon::getCouponName, keyword);
        }
        if (couponType != null) {
            wrapper.eq(Coupon::getCouponType, couponType);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        Page<Coupon> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public void addCoupon(Coupon coupon) {
        validateCoupon(coupon);
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        if (coupon.getPerLimit() == null) coupon.setPerLimit(0);
        if (coupon.getScopeType() == null) coupon.setScopeType(1);
        save(coupon);
    }

    public void updateCoupon(Coupon coupon) {
        Coupon existing = getById(coupon.getCouponId());
        if (existing == null) throw new BusinessException("优惠券不存在");
        validateCoupon(coupon);
        // 已发放的优惠券不允许减少总量
        if (coupon.getTotalCount() != null && existing.getReceivedCount() != null
                && coupon.getTotalCount() < existing.getReceivedCount()) {
            throw new BusinessException("发放总量不能小于已领取数量(" + existing.getReceivedCount() + ")");
        }
        updateById(coupon);
    }

    public void updateCouponStatus(Long couponId, Integer status) {
        Coupon coupon = getById(couponId);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        coupon.setStatus(status);
        updateById(coupon);
    }

    public void deleteCoupon(Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        if (coupon.getReceivedCount() != null && coupon.getReceivedCount() > 0) {
            throw new BusinessException("已有用户领取该优惠券，无法删除，请禁用");
        }
        removeById(couponId);
    }

    private void validateCoupon(Coupon coupon) {
        if (coupon.getCouponName() == null || coupon.getCouponName().isBlank()) {
            throw new BusinessException("优惠券名称不能为空");
        }
        if (coupon.getCouponType() == null) throw new BusinessException("请选择优惠券类型");
        if (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("优惠值必须大于0");
        }
        // 折扣类型校验：折扣率1-9.9
        if (coupon.getCouponType() == 2 && coupon.getDiscountValue().compareTo(new BigDecimal("9.9")) > 0) {
            throw new BusinessException("折扣率不能大于9.9折");
        }
        if (coupon.getTotalCount() == null || coupon.getTotalCount() <= 0) {
            throw new BusinessException("发放总量必须大于0");
        }
        if (coupon.getStartTime() == null || coupon.getEndTime() == null) {
            throw new BusinessException("请设置有效期");
        }
        if (coupon.getEndTime().isBefore(coupon.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
    }
}
