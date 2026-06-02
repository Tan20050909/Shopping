package org.example.user.controller;

import jakarta.validation.Valid;
import org.example.common.BizException;
import org.example.common.Result;
import org.example.context.UserContext;
import org.example.dto.UpdateProfileRequest;
import org.example.service.ShoppingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserAssetController {
    private final ShoppingService shoppingService;
    private final JdbcTemplate jdbc;

    public UserAssetController(ShoppingService shoppingService, JdbcTemplate jdbc) {
        this.shoppingService = shoppingService;
        this.jdbc = jdbc;
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        return Result.ok(shoppingService.me(UserContext.requireCurrentUserId()));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        shoppingService.updateProfile(UserContext.requireCurrentUserId(), request);
        return Result.ok();
    }

    @GetMapping("/coupons")
    public Result<List<Map<String, Object>>> coupons(@RequestParam(required = false) Integer status) {
        return Result.ok(shoppingService.coupons(UserContext.requireCurrentUserId(), status));
    }

    @GetMapping("/coupon-center")
    public Result<List<Map<String, Object>>> couponCenter() {
        ensureCouponGrantTypeColumn();
        long userId = UserContext.requireCurrentUserId();
        boolean hasGrantType = hasColumn("tb_coupon", "grant_type");
        String grantTypeSelect = hasGrantType ? "c.grant_type" : "1";
        String grantTypeGroupBy = hasGrantType ? "c.grant_type," : "";
        String couponCenterSql = """
                SELECT c.coupon_id, c.coupon_name, c.coupon_type, c.merchant_id, m.merchant_name,
                       %s AS grant_type,
                       c.discount_type, c.denomination, c.discount_rate, c.min_amount,
                       c.start_time, c.end_time, c.surplus_num, c.per_limit, c.status,
                       COALESCE(uc.received_count, 0) AS received_count,
                       GROUP_CONCAT(
                           CASE cs.scope_type
                               WHEN 1 THEN '全场通用'
                               WHEN 2 THEN CONCAT('指定店铺 ', COALESCE(sm.merchant_name, cs.target_id))
                               WHEN 3 THEN CONCAT('指定分类 ', cs.target_id)
                               WHEN 4 THEN CONCAT('指定商品 ', cs.target_id)
                               ELSE '指定范围'
                           END
                           ORDER BY cs.scope_type, cs.target_id SEPARATOR '、'
                       ) AS scope_text
                FROM tb_coupon c
                LEFT JOIN tb_merchant m ON m.merchant_id = c.merchant_id
                LEFT JOIN tb_coupon_scope cs ON cs.coupon_id = c.coupon_id
                LEFT JOIN tb_merchant sm ON sm.merchant_id = cs.target_id AND cs.scope_type = 2
                LEFT JOIN (
                    SELECT coupon_id, COUNT(*) AS received_count
                    FROM tb_user_coupon
                    WHERE user_id = ?
                    GROUP BY coupon_id
                ) uc ON uc.coupon_id = c.coupon_id
                WHERE c.audit_status = 1
                  AND c.is_deleted = 0
                  AND c.end_time >= NOW()
                GROUP BY c.coupon_id, c.coupon_name, c.coupon_type, c.merchant_id, m.merchant_name, %s
                         c.discount_type, c.denomination, c.discount_rate, c.min_amount,
                         c.start_time, c.end_time, c.surplus_num, c.per_limit, c.status, uc.received_count
                ORDER BY c.start_time, c.end_time, c.coupon_id
                """.formatted(grantTypeSelect, grantTypeGroupBy);
        List<Map<String, Object>> coupons = jdbc.queryForList(couponCenterSql, userId);

        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> coupon : coupons) {
            int status = number(coupon.get("status")).intValue();
            int grantType = number(coupon.getOrDefault("grant_type", 1)).intValue();
            int surplus = number(coupon.get("surplus_num")).intValue();
            int perLimit = number(coupon.get("per_limit")).intValue();
            int received = number(coupon.get("received_count")).intValue();
            LocalDateTime start = toLocalDateTime(coupon.get("start_time"));
            LocalDateTime end = toLocalDateTime(coupon.get("end_time"));
            String reason = "";
            if (status != 1) {
                reason = "未上线";
            } else if (grantType == 3) {
                reason = "需商家发放";
            } else if (start != null && now.isBefore(start)) {
                reason = "未开始";
            } else if (end != null && now.isAfter(end)) {
                reason = "已过期";
            } else if (surplus <= 0) {
                reason = "已抢光";
            } else if (received >= perLimit) {
                reason = "已领取";
            }
            coupon.put("canReceive", reason.isBlank());
            coupon.put("cannotReceiveReason", reason);
            coupon.putIfAbsent("scopeText", coupon.getOrDefault("scope_text", "全场通用"));
        }
        return Result.ok(coupons);
    }

    @Transactional
    @PostMapping("/coupons/{couponId}/receive")
    public Result<Void> receiveCoupon(@PathVariable long couponId) {
        ensureCouponGrantTypeColumn();
        long userId = UserContext.requireCurrentUserId();
        String grantTypeSelect = hasColumn("tb_coupon", "grant_type") ? "grant_type" : "1 AS grant_type";
        String receiveSql = """
                SELECT coupon_id, coupon_type, merchant_id, %s, surplus_num, status, audit_status, is_deleted,
                       start_time, end_time, per_limit
                FROM tb_coupon
                WHERE coupon_id = ?
                FOR UPDATE
                """.formatted(grantTypeSelect);
        Map<String, Object> coupon = one(receiveSql, couponId);
        if (coupon == null) {
            throw new BizException("优惠券不存在");
        }
        if (number(coupon.get("audit_status")).intValue() != 1 || number(coupon.get("is_deleted")).intValue() != 0) {
            throw new BizException("优惠券不存在或未通过审核");
        }
        if (number(coupon.get("status")).intValue() != 1) {
            throw new BizException("优惠券未上线");
        }
        if (number(coupon.getOrDefault("grant_type", 1)).intValue() == 3) {
            throw new BizException("该券需由商家发放");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = toLocalDateTime(coupon.get("start_time"));
        LocalDateTime end = toLocalDateTime(coupon.get("end_time"));
        if (start != null && now.isBefore(start)) {
            throw new BizException("优惠券活动未开始");
        }
        if (end != null && now.isAfter(end)) {
            throw new BizException("优惠券已过期");
        }
        if (number(coupon.get("surplus_num")).intValue() <= 0) {
            throw new BizException("优惠券已领完");
        }
        Long existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM tb_user_coupon WHERE user_id = ? AND coupon_id = ?
                """, Long.class, userId, couponId);
        if (existing != null && existing >= number(coupon.get("per_limit")).intValue()) {
            throw new BizException("已达到这张券的领取上限");
        }
        int updated = jdbc.update("UPDATE tb_coupon SET surplus_num = surplus_num - 1 WHERE coupon_id = ? AND surplus_num > 0", couponId);
        if (updated == 0) {
            throw new BizException("优惠券已领完");
        }
        int couponType = number(coupon.get("coupon_type")).intValue();
        Object userCouponMerchantId = couponType == 1 ? 0 : coupon.get("merchant_id");
        jdbc.update("""
                INSERT INTO tb_user_coupon(user_id, coupon_id, merchant_id, use_status, receive_time, expire_time)
                VALUES (?, ?, ?, 0, NOW(), ?)
                """, userId, couponId, userCouponMerchantId, coupon.get("end_time"));
        return Result.ok();
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Number number(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(v).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
    }

    private void ensureCouponGrantTypeColumn() {
        if (hasColumn("tb_coupon", "grant_type")) {
            return;
        }
        // 字段不存在时禁止运行期自动 DDL，领券逻辑按普通可领取券降级处理。
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                    """, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> favorites() {
        return Result.ok(shoppingService.collects(UserContext.requireCurrentUserId()));
    }

    @PostMapping("/favorites/{goodsId}")
    public Result<Map<String, Object>> addFavorite(@PathVariable long goodsId) {
        return Result.ok(shoppingService.setCollect(UserContext.requireCurrentUserId(), goodsId, true));
    }

    @DeleteMapping("/favorites/{goodsId}")
    public Result<Map<String, Object>> removeFavorite(@PathVariable long goodsId) {
        return Result.ok(shoppingService.setCollect(UserContext.requireCurrentUserId(), goodsId, false));
    }

    @GetMapping("/browse-history")
    public Result<List<Map<String, Object>>> browseHistory() {
        return Result.ok(shoppingService.browseHistory(UserContext.requireCurrentUserId()));
    }

    @DeleteMapping("/browse-history")
    public Result<Void> clearBrowseHistory() {
        shoppingService.clearBrowseHistory(UserContext.requireCurrentUserId());
        return Result.ok();
    }

    @DeleteMapping("/browse-history/{historyId}")
    public Result<Void> deleteBrowseHistory(@PathVariable long historyId) {
        shoppingService.deleteBrowseHistory(UserContext.requireCurrentUserId(), historyId);
        return Result.ok();
    }
}
