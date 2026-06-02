package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopping.admin.entity.*;
import com.shopping.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MerchantMapper merchantMapper;
    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final DisputeMapper disputeMapper;
    private final AbnormalOrderMapper abnormalOrderMapper;
    private final CouponMapper couponMapper;
    private final UserMapper userMapper;
    private final InsightMapper insightMapper;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("merchantTotal", merchantMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.put("merchantPendingAudit", merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getAuditStatus, 0)));
        stats.put("goodsTotal", goodsMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.put("goodsPendingAudit", goodsMapper.selectCount(new LambdaQueryWrapper<Goods>().eq(Goods::getAuditStatus, 0)));
        stats.put("orderTotal", orderMapper.selectCount(new LambdaQueryWrapper<>()));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.put("todayOrders", orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart)));

        // GMV
        List<Order> todayPaidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart).eq(Order::getPayStatus, 1));
        BigDecimal gmv = todayPaidOrders.stream().map(Order::getPayAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("todayGMV", gmv);
        stats.put("todaySales", getTodaySales(todayStart));
        stats.put("userTotal", userMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.put("todayUsers", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, todayStart)));

        // 退款率
        long totalToday = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStart));
        long refundToday = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, todayStart).eq(Order::getOrderStatus, 5));
        stats.put("refundRate", totalToday > 0 ? new BigDecimal(refundToday * 100.0 / totalToday).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        stats.put("afterSalePending", afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getHandleStatus, 0)));
        stats.put("disputePending", disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>().in(Dispute::getDisputeStatus, 0, 1, 2)));
        stats.put("abnormalPending", abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getHandleStatus, 0)));
        stats.put("couponTotal", couponMapper.selectCount(new LambdaQueryWrapper<>()));

        // 运营SLA与真实风险预警：避免售后/纠纷积压、库存断货、低信用商户、优惠券临期
        LocalDateTime now = LocalDateTime.now();
        stats.put("afterSaleOverdue", afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getHandleStatus, 0)
                .lt(AfterSale::getApplyTime, now.minusHours(48))));
        stats.put("afterSaleDueSoon", afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getHandleStatus, 0)
                .between(AfterSale::getApplyTime, now.minusHours(48), now.minusHours(36))));
        stats.put("disputeOverdue", disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>()
                .in(Dispute::getDisputeStatus, 0, 1, 2)
                .lt(Dispute::getCreateTime, now.minusHours(72))));
        // tb_goods 表无 stock 列，暂返回 0
        stats.put("lowStockGoods", 0L);
        stats.put("frozenMerchants", merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, 3)));
        stats.put("expiringCoupons", couponMapper.selectCount(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .between(Coupon::getEndTime, now, now.plusDays(7))));

        // 商家状态统计
        Map<String, Long> merchantByStatus = new LinkedHashMap<>();
        merchantByStatus.put("营业中", merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, 1)));
        merchantByStatus.put("停业", merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, 0)));
        merchantByStatus.put("平台冻结", merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getStatus, 3)));
        stats.put("merchantByStatus", merchantByStatus);

        // tb_user 表无 risk_tag 列，暂返回全量用户数作为 NORMAL
        Map<String, Long> userByRisk = new LinkedHashMap<>();
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        userByRisk.put("NORMAL", totalUsers);
        userByRisk.put("BRUSH_SUSPECT", 0L);
        userByRisk.put("FOCUS_WATCH", 0L);
        userByRisk.put("FROZEN", 0L);
        stats.put("userByRisk", userByRisk);

        return stats;
    }

    private BigDecimal getTodaySales(LocalDateTime todayStart) {
        List<Order> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart)
                .ne(Order::getOrderStatus, 4));
        return todayOrders.stream()
                .map(Order::getActualAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> getOrderTrend(int days) {
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .between(Order::getCreateTime, start, end));
            counts.add(count);
        }
        return Map.of("dates", dates, "counts", counts);
    }

    public Map<String, Object> getSalesTrend(int days) {
        List<String> dates = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.toString());
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            List<Order> dayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .between(Order::getCreateTime, start, end)
                    .ne(Order::getOrderStatus, 4));
            BigDecimal total = dayOrders.stream()
                    .map(Order::getActualAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amounts.add(total);
        }
        return Map.of("dates", dates, "amounts", amounts);
    }

    public Map<String, Object> getCategoryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingPayment", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 0)));
        stats.put("pendingShip", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 1)));
        stats.put("pendingReceive", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 2)));
        stats.put("completed", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 3)));
        stats.put("cancelled", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 4)));
        stats.put("refunded", orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 5)));
        return stats;
    }

    /** 获取智能洞察列表 */
    public List<Insight> getInsights(Integer handleStatus) {
        try {
            LambdaQueryWrapper<Insight> wrapper = new LambdaQueryWrapper<>();
            if (handleStatus != null) wrapper.eq(Insight::getHandleStatus, handleStatus);
            wrapper.orderByDesc(Insight::getSeverity).orderByDesc(Insight::getCreateTime);
            return insightMapper.selectList(wrapper);
        } catch (Exception e) {
            // tb_insight 表可能不存在，返回空列表避免 Dashboard 报错
            return new ArrayList<>();
        }
    }

    /** 处理洞察 */
    public void handleInsight(Long insightId, Integer handleStatus, Long adminId) {
        Insight insight = insightMapper.selectById(insightId);
        if (insight == null) return;
        insight.setHandleStatus(handleStatus);
        insight.setHandleAdminId(adminId);
        insight.setHandleTime(LocalDateTime.now());
        insightMapper.updateById(insight);
    }

    /** 用户增长漏斗 */
    public Map<String, Object> getUserFunnel() {
        Map<String, Object> funnel = new LinkedHashMap<>();
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getLastLoginTime, LocalDate.now().minusDays(30).atStartOfDay()));
        // tb_user 表无 order_count 列，buyers/repeatBuyers 暂返回 0
        funnel.put("totalUsers", totalUsers);
        funnel.put("activeUsers", activeUsers);
        funnel.put("buyers", 0L);
        funnel.put("repeatBuyers", 0L);
        return funnel;
    }

    /** 异常订单分布 */
    public Map<String, Object> getAbnormalDistribution() {
        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("timeoutNotShip", abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getAbnormalType, 2)));
        dist.put("highRisk", abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getAbnormalType, 4)));
        dist.put("abnormalRefund", abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getAbnormalType, 5)));
        dist.put("timeoutNotPay", abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getAbnormalType, 1)));
        return dist;
    }
}
