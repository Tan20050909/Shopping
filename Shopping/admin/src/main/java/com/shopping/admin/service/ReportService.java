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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsReviewMapper goodsReviewMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Map<String, Object> getSalesReport(int days, String startDate, String endDate) {
        LocalDateTime start, end;
        if (startDate != null && endDate != null) {
            start = LocalDate.parse(startDate).atStartOfDay();
            end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            days = (int) java.time.Duration.between(start, end).toDays() + 1;
        } else {
            start = LocalDate.now().minusDays(days - 1).atStartOfDay();
            end = LocalDate.now().atTime(LocalTime.MAX);
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        List<Long> userCounts = new ArrayList<>();

        BigDecimal totalSales = BigDecimal.ZERO;
        long totalOrders = 0;
        BigDecimal maxDaySales = BigDecimal.ZERO;
        String maxDaySalesDate = "";
        BigDecimal minDaySales = new BigDecimal(Long.MAX_VALUE);
        String minDaySalesDate = "";

        for (int i = 0; i < days; i++) {
            LocalDate date = start.toLocalDate().plusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            List<Order> dayOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .between(Order::getCreateTime, dayStart, dayEnd)
                    .ne(Order::getOrderStatus, 4));

            BigDecimal dayTotal = dayOrders.stream().map(Order::getActualAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayCount = dayOrders.size();

            dates.add(date.format(FMT));
            amounts.add(dayTotal);
            orderCounts.add(dayCount);

            long dayUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().between(User::getCreateTime, dayStart, dayEnd));
            userCounts.add(dayUsers);

            totalSales = totalSales.add(dayTotal);
            totalOrders += dayCount;

            if (dayTotal.compareTo(maxDaySales) > 0) { maxDaySales = dayTotal; maxDaySalesDate = date.format(FMT); }
            if (dayTotal.compareTo(minDaySales) < 0 || minDaySales.equals(new BigDecimal(Long.MAX_VALUE))) { minDaySales = dayTotal; minDaySalesDate = date.format(FMT); }
        }

        BigDecimal avgDaySales = days > 0 ? totalSales.divide(new BigDecimal(days), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal avgOrderAmount = totalOrders > 0 ? totalSales.divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("amounts", amounts);
        result.put("orderCounts", orderCounts);
        result.put("userCounts", userCounts);
        result.put("totalSales", totalSales);
        result.put("totalOrders", totalOrders);
        result.put("avgDaySales", avgDaySales);
        result.put("avgOrderAmount", avgOrderAmount);
        result.put("maxDaySales", maxDaySales);
        result.put("maxDaySalesDate", maxDaySalesDate);
        result.put("minDaySales", minDaySales.equals(new BigDecimal(Long.MAX_VALUE)) ? BigDecimal.ZERO : minDaySales);
        result.put("minDaySalesDate", minDaySalesDate);
        return result;
    }

    public Map<String, Object> getUserAnalysis(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();

        List<String> dates = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> activeUsers = new ArrayList<>();

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long periodNewUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreateTime, start));

        for (int i = 0; i < days; i++) {
            LocalDate date = start.toLocalDate().plusDays(i);
            long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .between(User::getCreateTime, date.atStartOfDay(), date.atTime(LocalTime.MAX)));
            dates.add(date.format(FMT));
            newUsers.add(count);
            // 活跃用户 = 当天有订单的用户（简化处理用注册用户代替）
            activeUsers.add(count);
        }

        // 用户性别分布
        long maleUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getGender, 1));
        long femaleUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getGender, 2));

        return Map.of(
                "dates", dates,
                "newUsers", newUsers,
                "activeUsers", activeUsers,
                "totalUsers", totalUsers,
                "periodNewUsers", periodNewUsers,
                "avgDailyNew", periodNewUsers > 0 ? periodNewUsers / days : 0,
                "maleUsers", maleUsers,
                "femaleUsers", femaleUsers
        );
    }

    public Map<String, Object> getGoodsRanking(int days, int top) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, start)
                .ne(Order::getOrderStatus, 4));
        return Map.of("period", days + "天", "orders", orders.size(), "top", top);
    }

    public Map<String, Object> getMerchantRanking(int days, int top) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Merchant> merchants = merchantMapper.selectList(new LambdaQueryWrapper<Merchant>().eq(Merchant::getAuditStatus, 1));
        return Map.of("totalMerchants", merchants.size(), "period", days + "天", "top", top);
    }

    public Map<String, Object> getOverview(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<>());
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long totalMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<>());
        long totalGoods = goodsMapper.selectCount(new LambdaQueryWrapper<>());
        long totalReviews = goodsReviewMapper.selectCount(new LambdaQueryWrapper<>());

        BigDecimal totalSales = orderMapper.selectList(new LambdaQueryWrapper<Order>().ne(Order::getOrderStatus, 4))
                .stream().map(Order::getActualAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodSales = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, start).ne(Order::getOrderStatus, 4))
                .stream().map(Order::getActualAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        long periodOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>().ge(Order::getCreateTime, start).ne(Order::getOrderStatus, 4));
        long periodNewUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreateTime, start));

        // 今日 vs 昨日对比
        BigDecimal todaySales = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart).ne(Order::getOrderStatus, 4))
                .stream().map(Order::getActualAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdaySales = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .between(Order::getCreateTime, todayStart.minusDays(1), todayStart).ne(Order::getOrderStatus, 4))
                .stream().map(Order::getActualAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 订单状态分布
        Map<String, Long> orderStatusMap = new LinkedHashMap<>();
        String[] statusNames = {"pendingPayment", "pendingShip", "pendingReceive", "completed", "cancelled", "refunded"};
        for (int i = 0; i < statusNames.length; i++) {
            orderStatusMap.put(statusNames[i], orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, i)));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalUsers", totalUsers);
        result.put("totalMerchants", totalMerchants);
        result.put("totalGoods", totalGoods);
        result.put("totalReviews", totalReviews);
        result.put("totalSales", totalSales);
        result.put("periodSales", periodSales);
        result.put("periodOrders", periodOrders);
        result.put("periodNewUsers", periodNewUsers);
        result.put("todaySales", todaySales);
        result.put("yesterdaySales", yesterdaySales);
        result.put("orderStatusMap", orderStatusMap);
        return result;
    }
}
