package com.shopping.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-center")
public class MerchantDataCenterController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/overview")
    public Map<String, Object> overview(
            @RequestParam Long merchantId,
            @RequestParam(required = false, defaultValue = "7") Integer days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> kpi = new HashMap<>();
        res.put("kpi", kpi);
        res.put("trend", new ArrayList<>());
        res.put("topGoods", new ArrayList<>());
        res.put("timeDistribution", new ArrayList<>());
        res.put("categoryShare", new ArrayList<>());
        res.put("customerSplit", new HashMap<>());
        res.put("stock", new HashMap<>());
        res.put("afterSaleTodos", new ArrayList<>());
        res.put("badReviews", new HashMap<>());
        res.put("compare", new HashMap<>());
        res.put("month", new HashMap<>());
        res.put("userProfile", new HashMap<>());
        res.put("updateTime", LocalDateTime.now());

        int d = days == null ? 7 : days;
        if (d < 1) d = 1;
        if (d > 90) d = 90;

        LocalDate today = LocalDate.now();
        LocalDate startDay = null;
        LocalDate endDay = null;
        boolean hasCustomRange = false;
        if (startDate != null && endDate != null && !startDate.isBlank() && !endDate.isBlank()) {
            try {
                startDay = LocalDate.parse(startDate.trim());
                endDay = LocalDate.parse(endDate.trim());
                hasCustomRange = true;
            } catch (Exception ignored) {
                hasCustomRange = false;
            }
        }
        if (!hasCustomRange) {
            endDay = today;
            startDay = today.minusDays(d - 1L);
        } else {
            if (startDay.isAfter(endDay)) {
                LocalDate tmp = startDay;
                startDay = endDay;
                endDay = tmp;
            }
            long len = ChronoUnit.DAYS.between(startDay, endDay) + 1;
            if (len > 90) {
                endDay = endDay;
                startDay = endDay.minusDays(89);
                len = 90;
            }
            d = (int) Math.max(1, Math.min(90, len));
        }

        if (merchantId == null || jdbcTemplate == null) {
            kpi.put("rangeDays", d);
            kpi.put("turnoverTotal", BigDecimal.ZERO);
            kpi.put("turnoverToday", BigDecimal.ZERO);
            kpi.put("paidOrderTotal", 0);
            kpi.put("paidOrderToday", 0);
            kpi.put("pendingShip", 0);
            kpi.put("pendingReceive", 0);
            kpi.put("completed", 0);
            kpi.put("pendingAfterSale", 0);
            return res;
        }

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime rangeStart = startDay.atStartOfDay();
        LocalDateTime rangeEnd = endDay.plusDays(1).atStartOfDay();
        LocalDateTime prevRangeStart = rangeStart.minusDays(d);
        LocalDateTime prevRangeEnd = rangeStart;
        LocalDate monthStartDay = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime monthStart = monthStartDay.atStartOfDay();
        Timestamp rangeStartTs = Timestamp.valueOf(rangeStart);
        Timestamp rangeEndTs = Timestamp.valueOf(rangeEnd);

        BigDecimal turnoverTotal = queryDecimal(
                "SELECT IFNULL(SUM(pay_amount),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3)",
                merchantId
        );
        BigDecimal turnoverToday = queryDecimal(
                "SELECT IFNULL(SUM(pay_amount),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time >= ?",
                merchantId,
                Timestamp.valueOf(todayStart)
        );
        Integer paidOrderTotal = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3)",
                merchantId
        );
        Integer paidOrderToday = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time >= ?",
                merchantId,
                Timestamp.valueOf(todayStart)
        );

        Integer pendingShip = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status=1",
                merchantId
        );
        Integer pendingReceive = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status=2",
                merchantId
        );
        Integer completed = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status=3",
                merchantId
        );
        Integer pendingAfterSale = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_after_sale WHERE merchant_id=? AND handle_status=0",
                merchantId
        );

        BigDecimal turnoverYesterday = queryDecimal(
                "SELECT IFNULL(SUM(pay_amount),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time >= ? AND pay_time < ?",
                merchantId,
                Timestamp.valueOf(yesterdayStart),
                Timestamp.valueOf(todayStart)
        );
        Integer paidOrderYesterday = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time >= ? AND pay_time < ?",
                merchantId,
                Timestamp.valueOf(yesterdayStart),
                Timestamp.valueOf(todayStart)
        );

        BigDecimal turnoverRange = queryDecimal(
                "SELECT IFNULL(SUM(pay_amount),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ?",
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        Integer paidOrderRange = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ?",
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        BigDecimal turnoverPrevRange = queryDecimal(
                "SELECT IFNULL(SUM(pay_amount),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ?",
                merchantId,
                Timestamp.valueOf(prevRangeStart),
                Timestamp.valueOf(prevRangeEnd)
        );
        Integer paidOrderPrevRange = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ?",
                merchantId,
                Timestamp.valueOf(prevRangeStart),
                Timestamp.valueOf(prevRangeEnd)
        );

        BigDecimal turnoverMonth = queryDecimal(
                "SELECT IFNULL(SUM(pay_amount),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ?",
                merchantId,
                Timestamp.valueOf(monthStart)
        );
        Integer paidOrderMonth = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_order WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ?",
                merchantId,
                Timestamp.valueOf(monthStart)
        );

        kpi.put("rangeDays", d);
        kpi.put("turnoverTotal", turnoverTotal);
        kpi.put("turnoverToday", turnoverToday);
        kpi.put("paidOrderTotal", paidOrderTotal);
        kpi.put("paidOrderToday", paidOrderToday);
        kpi.put("pendingShip", pendingShip);
        kpi.put("pendingReceive", pendingReceive);
        kpi.put("completed", completed);
        kpi.put("pendingAfterSale", pendingAfterSale);
        kpi.put("turnoverYesterday", turnoverYesterday);
        kpi.put("paidOrderYesterday", paidOrderYesterday);

        Map<String, BigDecimal> turnoverByDay = new HashMap<>();
        Map<String, Integer> orderByDay = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(pay_time,'%Y-%m-%d') d, IFNULL(SUM(pay_amount),0) turnover, IFNULL(COUNT(1),0) cnt " +
                        "FROM tb_order " +
                        "WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ? " +
                        "GROUP BY d ORDER BY d",
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        for (Map<String, Object> r : rows) {
            String key = String.valueOf(r.get("d"));
            turnoverByDay.put(key, toDecimal(r.get("turnover")));
            orderByDay.put(key, toInt(r.get("cnt")));
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            LocalDate day = startDay.plusDays(i);
            String key = day.toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", key);
            item.put("turnover", turnoverByDay.getOrDefault(key, BigDecimal.ZERO));
            item.put("orderCount", orderByDay.getOrDefault(key, 0));
            trend.add(item);
        }
        res.put("trend", trend);

        List<Map<String, Object>> topGoods = jdbcTemplate.queryForList(
                "SELECT oi.goods_id goodsId, oi.goods_name goodsName, IFNULL(SUM(oi.num),0) quantity, IFNULL(SUM(oi.total_price),0) amount " +
                        "FROM tb_order_item oi " +
                        "JOIN tb_order o ON o.order_id = oi.order_id " +
                        "WHERE oi.merchant_id=? AND o.merchant_id=? AND o.pay_status=1 AND o.order_status IN (1,2,3) AND o.pay_time IS NOT NULL AND o.pay_time >= ? AND o.pay_time < ? " +
                        "GROUP BY oi.goods_id, oi.goods_name " +
                        "ORDER BY quantity DESC, amount DESC LIMIT 10",
                merchantId,
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        res.put("topGoods", topGoods);

        List<Map<String, Object>> hourRows = jdbcTemplate.queryForList(
                "SELECT HOUR(pay_time) h, IFNULL(COUNT(1),0) cnt " +
                        "FROM tb_order " +
                        "WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ? " +
                        "GROUP BY h ORDER BY h",
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        int[] hours = new int[24];
        for (Map<String, Object> r : hourRows) {
            int h = toInt(r.get("h"));
            if (h < 0 || h > 23) continue;
            hours[h] = toInt(r.get("cnt"));
        }
        int night = 0;
        int morning = 0;
        int afternoon = 0;
        int evening = 0;
        for (int h = 0; h <= 5; h++) night += hours[h];
        for (int h = 6; h <= 11; h++) morning += hours[h];
        for (int h = 12; h <= 17; h++) afternoon += hours[h];
        for (int h = 18; h <= 23; h++) evening += hours[h];
        List<Map<String, Object>> timeDistribution = new ArrayList<>();
        timeDistribution.add(mapOf("label", "凌晨(0-5)", "count", night));
        timeDistribution.add(mapOf("label", "上午(6-11)", "count", morning));
        timeDistribution.add(mapOf("label", "下午(12-17)", "count", afternoon));
        timeDistribution.add(mapOf("label", "晚上(18-23)", "count", evening));
        res.put("timeDistribution", timeDistribution);

        BigDecimal totalByCategoryAll = queryDecimal(
                "SELECT IFNULL(SUM(oi.total_price),0) " +
                        "FROM tb_order_item oi " +
                        "JOIN tb_order o ON o.order_id = oi.order_id " +
                        "WHERE oi.merchant_id=? AND o.merchant_id=? AND o.pay_status=1 AND o.order_status IN (1,2,3) AND o.pay_time IS NOT NULL AND o.pay_time >= ? AND o.pay_time < ?",
                merchantId,
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        Integer totalQtyByCategoryAll = queryInt(
                "SELECT IFNULL(SUM(oi.num),0) " +
                        "FROM tb_order_item oi " +
                        "JOIN tb_order o ON o.order_id = oi.order_id " +
                        "WHERE oi.merchant_id=? AND o.merchant_id=? AND o.pay_status=1 AND o.order_status IN (1,2,3) AND o.pay_time IS NOT NULL AND o.pay_time >= ? AND o.pay_time < ?",
                merchantId,
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        List<Map<String, Object>> categoryRows = jdbcTemplate.queryForList(
                "SELECT IFNULL(c.cate_name,'未分类') name, IFNULL(SUM(oi.total_price),0) amount, IFNULL(SUM(oi.num),0) quantity " +
                        "FROM tb_order_item oi " +
                        "JOIN tb_order o ON o.order_id = oi.order_id " +
                        "LEFT JOIN tb_goods g ON g.goods_id = oi.goods_id " +
                        "LEFT JOIN tb_category c ON c.cate_id = g.cate_id " +
                        "WHERE oi.merchant_id=? AND o.merchant_id=? AND o.pay_status=1 AND o.order_status IN (1,2,3) AND o.pay_time IS NOT NULL AND o.pay_time >= ? AND o.pay_time < ? " +
                        "GROUP BY name ORDER BY amount DESC LIMIT 8",
                merchantId,
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        BigDecimal topSum = BigDecimal.ZERO;
        int topQtySum = 0;
        List<Map<String, Object>> categoryShare = new ArrayList<>();
        for (Map<String, Object> r : categoryRows) {
            String name = String.valueOf(r.get("name"));
            BigDecimal amount = toDecimal(r.get("amount"));
            int qty = toInt(r.get("quantity"));
            topSum = topSum.add(amount);
            topQtySum += Math.max(0, qty);
            Map<String, Object> row = new HashMap<>();
            row.put("name", name);
            row.put("amount", amount);
            row.put("quantity", qty);
            categoryShare.add(row);
        }
        BigDecimal remain = totalByCategoryAll.subtract(topSum);
        int remainQty = Math.max(0, (totalQtyByCategoryAll == null ? 0 : totalQtyByCategoryAll) - topQtySum);
        if (remain.compareTo(BigDecimal.ZERO) > 0 || remainQty > 0) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "其他");
            row.put("amount", remain.compareTo(BigDecimal.ZERO) > 0 ? remain : BigDecimal.ZERO);
            row.put("quantity", remainQty);
            categoryShare.add(row);
        }
        res.put("categoryShare", categoryShare);

        List<Map<String, Object>> splitRows = jdbcTemplate.queryForList(
                "SELECT user_id uid, IFNULL(COUNT(1),0) cnt, IFNULL(SUM(pay_amount),0) amount " +
                        "FROM tb_order " +
                        "WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ? " +
                        "GROUP BY uid",
                merchantId,
                rangeStartTs,
                rangeEndTs
        );
        Map<Long, Map<String, Object>> rangeByUser = new HashMap<>();
        List<Long> rangeUserIds = new ArrayList<>();
        for (Map<String, Object> r : splitRows) {
            Long uid = toLong(r.get("uid"));
            if (uid == null) continue;
            rangeUserIds.add(uid);
            rangeByUser.put(uid, r);
        }
        Map<Long, Timestamp> firstPayMap = new HashMap<>();
        if (!rangeUserIds.isEmpty()) {
            String inSql = buildInSql(rangeUserIds.size());
            List<Object> args = new ArrayList<>();
            args.add(merchantId);
            args.addAll(rangeUserIds);
            List<Map<String, Object>> firstRows = jdbcTemplate.queryForList(
                    "SELECT user_id uid, MIN(pay_time) firstPay " +
                            "FROM tb_order " +
                            "WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) AND pay_time IS NOT NULL AND user_id IN " + inSql +
                            " GROUP BY uid",
                    args.toArray()
            );
            for (Map<String, Object> r : firstRows) {
                Long uid = toLong(r.get("uid"));
                Timestamp fp = toTimestamp(r.get("firstPay"));
                if (uid == null || fp == null) continue;
                firstPayMap.put(uid, fp);
            }
        }
        int newUserCount = 0;
        int oldUserCount = 0;
        int newOrderCount = 0;
        int oldOrderCount = 0;
        BigDecimal newTurnover = BigDecimal.ZERO;
        BigDecimal oldTurnover = BigDecimal.ZERO;
        for (Long uid : rangeUserIds) {
            Map<String, Object> stat = rangeByUser.get(uid);
            if (stat == null) continue;
            int cnt = toInt(stat.get("cnt"));
            BigDecimal amt = toDecimal(stat.get("amount"));
            Timestamp first = firstPayMap.get(uid);
            boolean isNew = first != null && !first.before(rangeStartTs);
            if (isNew) {
                newUserCount += 1;
                newOrderCount += cnt;
                newTurnover = newTurnover.add(amt);
            } else {
                oldUserCount += 1;
                oldOrderCount += cnt;
                oldTurnover = oldTurnover.add(amt);
            }
        }
        Map<String, Object> customerSplit = new HashMap<>();
        customerSplit.put("newUserCount", newUserCount);
        customerSplit.put("oldUserCount", oldUserCount);
        customerSplit.put("newOrderCount", newOrderCount);
        customerSplit.put("oldOrderCount", oldOrderCount);
        customerSplit.put("newTurnover", newTurnover);
        customerSplit.put("oldTurnover", oldTurnover);
        res.put("customerSplit", customerSplit);

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("gender", new ArrayList<>());
        userProfile.put("age", new ArrayList<>());
        userProfile.put("region", new ArrayList<>());

        Map<String, Integer> genderCount = new HashMap<>();
        Map<String, Integer> ageCount = new HashMap<>();
        Map<String, Integer> regionCount = new HashMap<>();
        if (!rangeUserIds.isEmpty()) {
            String inSql = buildInSql(rangeUserIds.size());
            List<Object> args = new ArrayList<>();
            args.addAll(rangeUserIds);
            List<Map<String, Object>> users;
            try {
                users = jdbcTemplate.queryForList(
                        "SELECT user_id uid, gender gender, birthday birthday, id_card idCard FROM tb_user WHERE user_id IN " + inSql,
                        args.toArray()
                );
            } catch (Exception ignored) {
                users = jdbcTemplate.queryForList(
                        "SELECT user_id uid, id_card idCard FROM tb_user WHERE user_id IN " + inSql,
                        args.toArray()
                );
            }
            LocalDate nowDay = LocalDate.now();
            for (Map<String, Object> u : users) {
                String idCard = String.valueOf(u.get("idCard") == null ? "" : u.get("idCard")).trim();
                String gender = mapGender(u.get("gender"), idCard);
                genderCount.put(gender, genderCount.getOrDefault(gender, 0) + 1);

                LocalDate birthday = toLocalDate(u.get("birthday"));
                String ageBucket = birthday != null ? parseAgeBucketFromBirthday(birthday, nowDay) : parseAgeBucketFromIdCard(idCard, nowDay);
                ageCount.put(ageBucket, ageCount.getOrDefault(ageBucket, 0) + 1);
            }

            List<Map<String, Object>> orderAddrs = jdbcTemplate.queryForList(
                    "SELECT user_id uid, receive_addr addr " +
                            "FROM tb_order " +
                            "WHERE merchant_id=? AND pay_status=1 AND order_status IN (1,2,3) " +
                            "  AND pay_time IS NOT NULL AND pay_time >= ? AND pay_time < ? " +
                            "  AND user_id IN " + inSql +
                            "ORDER BY pay_time DESC, order_id DESC",
                    mergeArgs(merchantId, rangeStartTs, rangeEndTs, rangeUserIds).toArray()
            );
            Map<Long, Boolean> seen = new HashMap<>();
            for (Map<String, Object> row : orderAddrs) {
                Long uid = toLong(row.get("uid"));
                if (uid == null) continue;
                if (seen.putIfAbsent(uid, Boolean.TRUE) != null) continue;
                String province = provinceFromAddress(String.valueOf(row.get("addr") == null ? "" : row.get("addr")));
                regionCount.put(province, regionCount.getOrDefault(province, 0) + 1);
            }
        }

        userProfile.put("gender", toDistList(genderCount, List.of("男", "女", "未知")));
        userProfile.put("age", toDistList(ageCount, List.of("0-17", "18-24", "25-34", "35-44", "45+", "未知")));
        userProfile.put("region", topNWithOther(regionCount, 8));
        res.put("userProfile", userProfile);

        int warningStock = queryInt("SELECT IFNULL(warning_stock,0) FROM tb_merchant_stock_setting WHERE merchant_id=? LIMIT 1", merchantId);
        List<Map<String, Object>> stockWarnings = jdbcTemplate.queryForList(
                "SELECT g.goods_id goodsId, g.goods_name goodsName, s.sku_id skuId, IFNULL(s.sku_name,'默认') skuSpec, " +
                        "IFNULL(s.stock,0) stock, IFNULL(s.lock_stock,0) lockStock, (IFNULL(s.stock,0)-IFNULL(s.lock_stock,0)) available " +
                        "FROM tb_goods_sku s " +
                        "JOIN tb_goods g ON g.goods_id = s.goods_id " +
                        "WHERE g.merchant_id=? AND IFNULL(s.status,1)=1 AND (IFNULL(s.stock,0)-IFNULL(s.lock_stock,0)) <= ? " +
                        "ORDER BY available ASC, g.goods_id DESC LIMIT 10",
                merchantId,
                warningStock
        );
        Map<String, Object> stock = new HashMap<>();
        stock.put("warningStock", warningStock);
        stock.put("items", stockWarnings);
        res.put("stock", stock);

        List<Map<String, Object>> afterSaleTodos = jdbcTemplate.queryForList(
                "SELECT after_sale_id id, after_sale_type type, apply_amount refundAmount, apply_reason reason, apply_time createTime " +
                        "FROM tb_after_sale " +
                        "WHERE merchant_id=? AND handle_status=0 " +
                        "ORDER BY apply_time DESC LIMIT 10",
                merchantId
        );
        res.put("afterSaleTodos", afterSaleTodos);

        List<Map<String, Object>> badReviewRows = jdbcTemplate.queryForList(
                "SELECT comment_id id, goods_id goodsId, goods_score goodsScore, comment_content content, comment_time commentTime " +
                        "FROM tb_goods_comment " +
                        "WHERE merchant_id=? AND IFNULL(is_valid,1)=1 AND (reply_content IS NULL OR reply_content='') AND IFNULL(goods_score,5) <= 2 " +
                        "ORDER BY comment_time DESC LIMIT 10",
                merchantId
        );
        int badReviewCount = queryInt(
                "SELECT IFNULL(COUNT(1),0) FROM tb_goods_comment WHERE merchant_id=? AND IFNULL(is_valid,1)=1 AND (reply_content IS NULL OR reply_content='') AND IFNULL(goods_score,5) <= 2",
                merchantId
        );
        Map<String, Object> badReviews = new HashMap<>();
        badReviews.put("count", badReviewCount);
        badReviews.put("items", badReviewRows);
        res.put("badReviews", badReviews);

        Map<String, Object> compare = new HashMap<>();
        compare.put("turnoverToday", turnoverToday);
        compare.put("turnoverYesterday", turnoverYesterday);
        compare.put("paidOrderToday", paidOrderToday);
        compare.put("paidOrderYesterday", paidOrderYesterday);
        compare.put("turnoverRange", turnoverRange);
        compare.put("turnoverPrevRange", turnoverPrevRange);
        compare.put("paidOrderRange", paidOrderRange);
        compare.put("paidOrderPrevRange", paidOrderPrevRange);
        res.put("compare", compare);

        Map<String, Object> month = new HashMap<>();
        month.put("monthStart", monthStart);
        month.put("turnoverMonth", turnoverMonth);
        month.put("paidOrderMonth", paidOrderMonth);
        res.put("month", month);
        return res;
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        try {
            BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return v == null ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer queryInt(String sql, Object... args) {
        try {
            Integer v = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer toInt(Object v) {
        if (v == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try {
            long x = Long.parseLong(String.valueOf(v));
            return x <= 0 ? null : x;
        } catch (Exception e) {
            return null;
        }
    }

    private Timestamp toTimestamp(Object v) {
        if (v == null) return null;
        if (v instanceof Timestamp ts) return ts;
        if (v instanceof java.sql.Date d) return new Timestamp(d.getTime());
        if (v instanceof LocalDateTime dt) return Timestamp.valueOf(dt);
        if (v instanceof LocalDate d) return Timestamp.valueOf(d.atStartOfDay());
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        String raw = s.contains("T") ? s.replace("T", " ") : s;
        if (raw.length() == 10) raw = raw + " 00:00:00";
        try {
            return Timestamp.valueOf(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> mapOf(Object k1, Object v1, Object k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(String.valueOf(k1), v1);
        m.put(String.valueOf(k2), v2);
        return m;
    }

    private String buildInSql(int size) {
        int n = Math.max(1, size);
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }

    private String mapGender(Object genderValue, String idCard) {
        int g = toInt(genderValue);
        if (g == 1) return "男";
        if (g == 2) return "女";
        return parseGenderFromIdCard(idCard);
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Date) {
            return ((java.sql.Date) v).toLocalDate();
        }
        if (v instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) v).toLocalDateTime().toLocalDate();
        }
        String s = String.valueOf(v).trim();
        if (s.isBlank()) return null;
        try {
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String parseAgeBucketFromBirthday(LocalDate birth, LocalDate nowDay) {
        if (birth == null || nowDay == null) return "未知";
        if (birth.isAfter(nowDay)) return "未知";
        int age;
        try {
            age = Period.between(birth, nowDay).getYears();
        } catch (Exception ignored) {
            return "未知";
        }
        if (age < 0) return "未知";
        if (age <= 17) return "0-17";
        if (age <= 24) return "18-24";
        if (age <= 34) return "25-34";
        if (age <= 44) return "35-44";
        return "45+";
    }

    private String parseGenderFromIdCard(String idCard) {
        String v = idCard == null ? "" : idCard.trim();
        if (v.length() == 18 && v.matches("\\d{17}[0-9Xx]")) {
            char c = v.charAt(16);
            if (c >= '0' && c <= '9') {
                int n = c - '0';
                return (n % 2 == 1) ? "男" : "女";
            }
            return "未知";
        }
        return "未知";
    }

    private String parseAgeBucketFromIdCard(String idCard, LocalDate nowDay) {
        String v = idCard == null ? "" : idCard.trim();
        if (v.length() == 18 && v.matches("\\d{17}[0-9Xx]")) {
            try {
                int y = Integer.parseInt(v.substring(6, 10));
                int m = Integer.parseInt(v.substring(10, 12));
                int d = Integer.parseInt(v.substring(12, 14));
                LocalDate birth = LocalDate.of(y, m, d);
                if (birth.isAfter(nowDay)) return "未知";
                int age = Period.between(birth, nowDay).getYears();
                if (age < 0) return "未知";
                if (age <= 17) return "0-17";
                if (age <= 24) return "18-24";
                if (age <= 34) return "25-34";
                if (age <= 44) return "35-44";
                return "45+";
            } catch (Exception ignored) {
                return "未知";
            }
        }
        return "未知";
    }

    private List<Map<String, Object>> toDistList(Map<String, Integer> counts, List<String> order) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (order != null) {
            for (String k : order) {
                int v = counts == null ? 0 : counts.getOrDefault(k, 0);
                list.add(mapOf("name", k, "count", v));
            }
        }
        return list;
    }

    private List<Map<String, Object>> topNWithOther(Map<String, Integer> counts, int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (counts == null || counts.isEmpty()) {
            list.add(mapOf("name", "未知", "count", 0));
            return list;
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue() == null ? 0 : b.getValue(), a.getValue() == null ? 0 : a.getValue()));
        int limit = Math.max(1, n);
        int sumTop = 0;
        int sumAll = 0;
        for (Map.Entry<String, Integer> e : entries) {
            sumAll += e.getValue() == null ? 0 : e.getValue();
        }
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            int v = e.getValue() == null ? 0 : e.getValue();
            if (i < limit) {
                list.add(mapOf("name", e.getKey(), "count", v));
                sumTop += v;
            }
        }
        int other = Math.max(0, sumAll - sumTop);
        if (other > 0) {
            list.add(mapOf("name", "其他", "count", other));
        }
        return list;
    }

    private List<Object> mergeArgs(Object merchantId, Object rangeStart, Object rangeEnd, List<Long> userIds) {
        List<Object> args = new ArrayList<>();
        args.add(merchantId);
        args.add(rangeStart);
        args.add(rangeEnd);
        if (userIds != null && !userIds.isEmpty()) {
            args.addAll(userIds);
        }
        return args;
    }

    private String provinceFromAddress(String addr) {
        String s = addr == null ? "" : addr.trim();
        if (s.isBlank()) return "未知";
        s = s.replaceAll("\\s+", "");
        String[] markers = new String[]{"特别行政区", "自治区", "省", "市"};
        for (String m : markers) {
            int idx = s.indexOf(m);
            if (idx > 0 && idx <= 12) {
                return s.substring(0, idx + m.length());
            }
        }
        return "未知";
    }
}
