package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Coupon;
import com.shopping.mapper.CouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CouponService extends ServiceImpl<CouponMapper, Coupon> {

    @Autowired
    private CouponScopeService couponScopeService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public Coupon create(Coupon coupon) {
        ensureGrantTypeColumn();
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getCouponType() == null) {
            coupon.setCouponType(2);
        }
        if (coupon.getGrantType() == null) {
            coupon.setGrantType(1);
        }
        if (coupon.getDiscountType() == null) {
            coupon.setDiscountType(1);
        }
        if (coupon.getLimitEnabled() == null) {
            coupon.setLimitEnabled(1);
        }
        if (coupon.getPerLimit() == null) {
            coupon.setPerLimit(1);
        }
        if (coupon.getLimitEnabled() != null && coupon.getLimitEnabled() == 0) {
            coupon.setPerLimit(0);
        }
        if (coupon.getCanStack() == null) {
            coupon.setCanStack(0);
        }
        if (coupon.getAuditStatus() == null) {
            coupon.setAuditStatus(1);
        }
        coupon.setStatus(0);
        if (coupon.getIsDeleted() == null) {
            coupon.setIsDeleted(0);
        }
        if (coupon.getTotalCount() == null) {
            coupon.setTotalCount(0);
        }
        if (coupon.getSurplusNum() == null) {
            coupon.setSurplusNum(coupon.getTotalCount());
        }
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        save(coupon);
        couponScopeService.replaceScopes(coupon.getId(), coupon.getScopeType(), coupon.getTargetIds());
        return coupon;
    }

    public List<Coupon> listByMerchantId(Long merchantId) {
        ensureGrantTypeColumn();
        List<Coupon> list = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getMerchantId, merchantId)
                .and(w -> w.eq(Coupon::getIsDeleted, 0).or().isNull(Coupon::getIsDeleted))
                .orderByDesc(Coupon::getCreateTime));
        List<Long> ids = list.stream().map(Coupon::getId).filter(v -> v != null).toList();
        Map<Long, List<Long>> targetsByCouponId = new HashMap<>();
        Map<Long, Integer> scopeTypeByCouponId = new HashMap<>();
        for (var s : couponScopeService.listByCouponIds(ids)) {
            if (s == null || s.getCouponId() == null) continue;
            Long cid = s.getCouponId();
            scopeTypeByCouponId.put(cid, s.getScopeType());
            if (s.getScopeType() != null && s.getScopeType() != 1 && s.getTargetId() != null && s.getTargetId() > 0) {
                targetsByCouponId.computeIfAbsent(cid, k -> new ArrayList<>()).add(s.getTargetId());
            }
        }
        for (Coupon c : list) {
            Integer total = c.getTotalCount() == null ? 0 : c.getTotalCount();
            Integer surplus = c.getSurplusNum() == null ? 0 : c.getSurplusNum();
            c.setUsedCount(Math.max(0, total - surplus));
            Integer st = scopeTypeByCouponId.get(c.getId());
            c.setScopeType(st == null ? 1 : st);
            c.setTargetIds(targetsByCouponId.getOrDefault(c.getId(), List.of()));
        }
        return list;
    }

    public List<Coupon> listPlatformCoupons() {
        ensureGrantTypeColumn();
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> list = list(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCouponType, 1)
                .and(w -> w.eq(Coupon::getIsDeleted, 0).or().isNull(Coupon::getIsDeleted))
                .eq(Coupon::getAuditStatus, 1)
                .eq(Coupon::getStatus, 1)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .orderByDesc(Coupon::getCreateTime));
        for (Coupon c : list) {
            Integer total = c.getTotalCount() == null ? 0 : c.getTotalCount();
            Integer surplus = c.getSurplusNum() == null ? 0 : c.getSurplusNum();
            c.setUsedCount(Math.max(0, total - surplus));
        }
        return list;
    }

    public boolean updateStatus(Long id, Integer status) {
        Coupon existed = getById(id);
        if (existed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "优惠券不存在");
        }
        int s = status == null ? 0 : status;
        if (s != 0 && s != 1 && s != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法状态");
        }
        if (s == 1) {
            if (existed.getAuditStatus() != null && existed.getAuditStatus() == 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核驳回的优惠券不能上线");
            }
            LocalDateTime now = LocalDateTime.now();
            if (existed.getStartTime() != null && now.isBefore(existed.getStartTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未到生效时间，不能上线");
            }
            if (existed.getEndTime() != null && now.isAfter(existed.getEndTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已过期，不能上线");
            }
        }
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setStatus(s);
        coupon.setUpdateTime(LocalDateTime.now());
        return updateById(coupon);
    }

    @Transactional
    public Coupon update(Long id, Coupon patch) {
        ensureGrantTypeColumn();
        Coupon existed = getById(id);
        if (existed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "优惠券不存在");
        }
        if (existed.getStatus() != null && existed.getStatus() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已上线优惠券不可编辑");
        }

        Integer limitEnabled = patch.getLimitEnabled() == null ? 1 : patch.getLimitEnabled();
        Coupon toSave = new Coupon();
        toSave.setId(id);
        toSave.setName(patch.getName());
        toSave.setGrantType(patch.getGrantType());
        toSave.setDiscountType(patch.getDiscountType());
        toSave.setDiscountAmount(patch.getDiscountAmount());
        toSave.setDiscountRate(patch.getDiscountRate());
        toSave.setMinAmount(patch.getMinAmount());
        toSave.setLimitEnabled(limitEnabled);
        toSave.setPerLimit(limitEnabled == 0 ? 0 : patch.getPerLimit());
        toSave.setCanStack(patch.getCanStack());
        toSave.setTotalCount(patch.getTotalCount());
        toSave.setSurplusNum(patch.getSurplusNum());
        toSave.setStartTime(patch.getStartTime());
        toSave.setEndTime(patch.getEndTime());
        toSave.setUpdateTime(LocalDateTime.now());
        updateById(toSave);

        couponScopeService.replaceScopes(id, patch.getScopeType(), patch.getTargetIds());

        Coupon updated = getById(id);
        if (updated == null) return null;
        Integer total = updated.getTotalCount() == null ? 0 : updated.getTotalCount();
        Integer surplus = updated.getSurplusNum() == null ? 0 : updated.getSurplusNum();
        updated.setUsedCount(Math.max(0, total - surplus));
        updated.setScopeType(patch.getScopeType());
        updated.setTargetIds(patch.getTargetIds());
        return updated;
    }

    @Transactional
    public boolean grantToUser(Long merchantId, Long couponId, Long userId) {
        ensureGrantTypeColumn();
        if (merchantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId不能为空");
        }
        if (couponId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponId不能为空");
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId不能为空");
        }
        if (jdbcTemplate == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "服务未启用(JdbcTemplate不可用)");
        }

        Coupon c = getById(couponId);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "优惠券不存在");
        }
        if (c.getMerchantId() == null || !c.getMerchantId().equals(merchantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该优惠券不属于当前商家");
        }
        if (c.getStatus() == null || c.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券未上线，不能发放");
        }
        if (c.getAuditStatus() != null && c.getAuditStatus() == 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核驳回的优惠券不能发放");
        }
        LocalDateTime now = LocalDateTime.now();
        if (c.getStartTime() != null && now.isBefore(c.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未到生效时间，不能发放");
        }
        if (c.getEndTime() != null && now.isAfter(c.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已过期，不能发放");
        }

        Integer limitEnabled = c.getLimitEnabled() == null ? 1 : c.getLimitEnabled();
        if (limitEnabled != 0) {
            Integer perLimit = c.getPerLimit() == null ? 1 : c.getPerLimit();
            if (perLimit < 1) perLimit = 1;
            Integer owned = jdbcTemplate.queryForObject(
                    "select count(1) from tb_user_coupon where user_id=? and coupon_id=?",
                    Integer.class,
                    userId, couponId
            );
            int ownedCount = owned == null ? 0 : owned;
            if (ownedCount >= perLimit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户已达到该券的领取上限");
            }
        }

        int dec = jdbcTemplate.update(
                "update tb_coupon set surplus_num = surplus_num - 1 where coupon_id=? and surplus_num > 0",
                couponId
        );
        if (dec < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存不足，无法发放");
        }

        jdbcTemplate.update(
                "insert into tb_user_coupon(user_id, coupon_id, merchant_id, use_status, receive_time, expire_time) values(?, ?, ?, 0, now(), ?)",
                userId, couponId, merchantId, c.getEndTime()
        );
        return true;
    }

    private void ensureGrantTypeColumn() {
        if (jdbcTemplate == null) {
            return;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                    """, Integer.class, "tb_coupon", "grant_type");
            if (count != null && count > 0) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }
        // 字段不存在，禁止运行期自动 DDL
        // 请使用基准 SQL 初始化数据库，确保 tb_coupon 表包含 grant_type 字段
        // 依赖 grant_type 的功能将按现有逻辑明确失败
    }

    private static BigDecimal bd(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public void validateForCreateOrUpdate(Coupon coupon) {
        ensureGrantTypeColumn();
        if (coupon.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId不能为空");
        }
        Integer couponType = coupon.getCouponType() == null ? 2 : coupon.getCouponType();
        if (couponType != 1 && couponType != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "couponType不合法");
        }
        int grantType = coupon.getGrantType() == null ? 1 : coupon.getGrantType();
        if (grantType != 1 && grantType != 2 && grantType != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grantType不合法");
        }
        if (coupon.getName() == null || coupon.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券名称不能为空");
        }
        if (coupon.getDiscountType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠类型不能为空");
        }
        if (coupon.getStartTime() == null || coupon.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "有效期不能为空");
        }
        if (!coupon.getEndTime().isAfter(coupon.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (coupon.getTotalCount() == null || coupon.getTotalCount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "总发行量必须大于0");
        }
        if (coupon.getSurplusNum() != null && coupon.getSurplusNum() > coupon.getTotalCount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "剩余数量不能大于总发行量");
        }
        int limitEnabled = coupon.getLimitEnabled() == null ? 1 : coupon.getLimitEnabled();
        if (limitEnabled != 0) {
            if (coupon.getPerLimit() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写每人限领数量");
            }
            if (coupon.getPerLimit() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每人限领至少为1");
            }
        }
        int dt = coupon.getDiscountType();
        if (dt == 1 || dt == 3) {
            if (bd(coupon.getDiscountAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "面额必须大于0");
            }
            if (dt == 3) {
                if (bd(coupon.getMinAmount()).compareTo(BigDecimal.ZERO) != 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无门槛券门槛金额必须为0");
                }
            }
        }
        if (dt == 2) {
            if (coupon.getDiscountRate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "折扣率不能为空");
            }
            BigDecimal r = coupon.getDiscountRate();
            if (r.compareTo(new BigDecimal("0.0001")) < 0 || r.compareTo(BigDecimal.ONE) >= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "折扣率必须在(0,1)之间");
            }
        }
        int st = coupon.getScopeType() == null ? 1 : coupon.getScopeType();
        if (st != 1 && st != 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持全场或指定商品");
        }
        if (st == 4) {
            if (coupon.getTargetIds() == null || coupon.getTargetIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择适用商品");
            }
        }
    }
}
