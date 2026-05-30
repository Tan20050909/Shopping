package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Order;
import com.shopping.entity.OrderItem;
import com.shopping.entity.Goods;
import com.shopping.entity.User;
import com.shopping.entity.AfterSale;
import com.shopping.mapper.GoodsMapper;
import com.shopping.mapper.AfterSaleMapper;
import com.shopping.mapper.OrderItemMapper;
import com.shopping.mapper.OrderMapper;
import com.shopping.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public List<Order> listByMerchantId(Long merchantId, Integer status) {
        return listByMerchantId(merchantId, status, null);
    }

    public List<Order> listByMerchantId(Long merchantId, Integer status, Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getMerchantId, merchantId);
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = list(wrapper);
        fillOrderThumbPics(orders);
        fillUserInfo(orders);
        return orders;
    }

    public boolean updateStatus(Long id, Integer status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        return updateById(order);
    }

    @Transactional
    public boolean updateFreight(Long id, BigDecimal freight) {
        Order existed = getById(id);
        if (existed == null) {
            return false;
        }

        BigDecimal total = existed.getTotalAmount() == null ? BigDecimal.ZERO : existed.getTotalAmount();
        BigDecimal discount = existed.getDiscountAmount() == null ? BigDecimal.ZERO : existed.getDiscountAmount();
        BigDecimal f = freight == null ? BigDecimal.ZERO : freight;
        if (f.compareTo(BigDecimal.ZERO) < 0) {
            f = BigDecimal.ZERO;
        }
        BigDecimal pay = total.subtract(discount).add(f);
        if (pay.compareTo(BigDecimal.ZERO) < 0) {
            pay = BigDecimal.ZERO;
        }

        Order patch = new Order();
        patch.setId(id);
        patch.setFreight(f);
        patch.setPayAmount(pay);
        patch.setUpdateTime(LocalDateTime.now());
        boolean ok = updateById(patch);
        if (ok) {
            syncGroupPayAmount(existed.getGroupId());
        }
        return ok;
    }

    @Transactional
    public boolean updatePayAmount(Long id, BigDecimal payAmount) {
        Order existed = getById(id);
        if (existed == null) {
            return false;
        }
        if (existed.getStatus() == null || existed.getStatus() != 0) {
            return false;
        }
        BigDecimal total = existed.getTotalAmount() == null ? BigDecimal.ZERO : existed.getTotalAmount();
        BigDecimal freight = existed.getFreight() == null ? BigDecimal.ZERO : existed.getFreight();
        BigDecimal target = payAmount == null ? BigDecimal.ZERO : payAmount;
        if (target.compareTo(BigDecimal.ZERO) < 0) target = BigDecimal.ZERO;

        BigDecimal discount = total.add(freight).subtract(target);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
            target = total.add(freight);
        }

        Order patch = new Order();
        patch.setId(id);
        patch.setDiscountAmount(discount);
        patch.setPayAmount(target);
        patch.setUpdateTime(LocalDateTime.now());
        boolean ok = updateById(patch);
        if (ok) {
            syncGroupPayAmount(existed.getGroupId());
        }
        return ok;
    }

    private void syncGroupPayAmount(Long groupId) {
        if (groupId == null || jdbcTemplate == null) {
            return;
        }
        try {
            BigDecimal sumPay = jdbcTemplate.queryForObject(
                    "SELECT IFNULL(SUM(pay_amount), 0) FROM tb_order WHERE group_id = ?",
                    BigDecimal.class,
                    groupId
            );
            BigDecimal sumDiscount = jdbcTemplate.queryForObject(
                    "SELECT IFNULL(SUM(discount_amount), 0) FROM tb_order WHERE group_id = ?",
                    BigDecimal.class,
                    groupId
            );
            jdbcTemplate.update(
                    "UPDATE tb_order_group SET pay_amount = ?, discount_amount = ? WHERE group_id = ?",
                    sumPay == null ? BigDecimal.ZERO : sumPay,
                    sumDiscount == null ? BigDecimal.ZERO : sumDiscount,
                    groupId
            );
            jdbcTemplate.update(
                    "UPDATE tb_payment SET pay_amount = ? WHERE group_id = ? AND pay_status = 0",
                    sumPay == null ? BigDecimal.ZERO : sumPay,
                    groupId
            );
        } catch (Exception ignored) {
        }
    }

    private void fillOrderThumbPics(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> ids = orders.stream().map(Order::getId).filter(v -> v != null).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }

        List<Long> groupIds = orders.stream().map(Order::getGroupId).filter(v -> v != null).distinct().collect(Collectors.toList());

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .and(w -> w.in(OrderItem::getOrderId, ids).or().in(!groupIds.isEmpty(), OrderItem::getGroupId, groupIds))
                .orderByDesc(OrderItem::getCreateTime));

        List<Long> missingGoodsIds = new ArrayList<>();
        for (OrderItem it : items) {
            if (it == null) continue;
            String pic = it.getGoodsPic();
            boolean invalidLocal = false;
            if (pic != null && !pic.isBlank()) {
                String v = pic.trim();
                invalidLocal = v.startsWith("/goods/") || v.startsWith("goods/") || v.startsWith("/images/") || v.startsWith("images/");
            }
            if (pic != null && !pic.isBlank() && !invalidLocal) continue;
            Long goodsId = it.getGoodsId();
            if (goodsId != null) missingGoodsIds.add(goodsId);
        }

        Map<Long, String> goodsPicMap = new HashMap<>();
        if (!missingGoodsIds.isEmpty()) {
            List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                    .select(Goods::getId, Goods::getGoodsPic)
                    .in(Goods::getId, missingGoodsIds));
            for (Goods g : goodsList) {
                if (g == null || g.getId() == null) continue;
                String pic = g.getGoodsPic();
                if (pic == null || pic.isBlank()) continue;
                goodsPicMap.put(g.getId(), pic);
            }
        }

        Map<Long, List<String>> picsByOrderId = new HashMap<>();
        Map<Long, List<String>> picsByGroupId = new HashMap<>();
        Map<Long, List<String>> namesByOrderId = new HashMap<>();
        Map<Long, List<String>> namesByGroupId = new HashMap<>();
        Map<Long, Integer> afterSaleByOrderId = new HashMap<>();
        Map<Long, Integer> afterSaleByGroupId = new HashMap<>();
        for (OrderItem it : items) {
            if (it == null) continue;
            Long orderId = it.getOrderId();
            Long groupId = it.getGroupId();
            String pic = it.getGoodsPic();
            boolean invalidLocal = false;
            if (pic != null && !pic.isBlank()) {
                String v = pic.trim();
                invalidLocal = v.startsWith("/goods/") || v.startsWith("goods/") || v.startsWith("/images/") || v.startsWith("images/");
            }
            if (pic == null || pic.isBlank() || invalidLocal) {
                pic = goodsPicMap.get(it.getGoodsId());
            }

            if (it.getAfterSaleStatus() != null && it.getAfterSaleStatus() > 0) {
                if (orderId != null) {
                    afterSaleByOrderId.put(orderId, afterSaleByOrderId.getOrDefault(orderId, 0) + 1);
                }
                if (groupId != null) {
                    afterSaleByGroupId.put(groupId, afterSaleByGroupId.getOrDefault(groupId, 0) + 1);
                }
            }

            if (pic == null || pic.isBlank()) continue;
            if (orderId != null) {
                List<String> list = picsByOrderId.computeIfAbsent(orderId, k -> new ArrayList<>());
                if (list.size() < 5) list.add(pic);
            }
            if (groupId != null) {
                List<String> list = picsByGroupId.computeIfAbsent(groupId, k -> new ArrayList<>());
                if (list.size() < 5) list.add(pic);
            }

            String name = it.getGoodsName();
            if (name != null && !name.isBlank()) {
                if (orderId != null) {
                    List<String> list = namesByOrderId.computeIfAbsent(orderId, k -> new ArrayList<>());
                    if (list.size() < 10) list.add(name);
                }
                if (groupId != null) {
                    List<String> list = namesByGroupId.computeIfAbsent(groupId, k -> new ArrayList<>());
                    if (list.size() < 10) list.add(name);
                }
            }
        }

        for (Order o : orders) {
            if (o == null) continue;
            List<String> pics = picsByOrderId.get(o.getId());
            if ((pics == null || pics.isEmpty()) && o.getGroupId() != null) {
                pics = picsByGroupId.get(o.getGroupId());
            }
            o.setItemPics(pics == null ? new ArrayList<>() : pics);

            List<String> names = namesByOrderId.get(o.getId());
            if ((names == null || names.isEmpty()) && o.getGroupId() != null) {
                names = namesByGroupId.get(o.getGroupId());
            }
            o.setItemNames(names == null ? new ArrayList<>() : names);

            Integer afterSaleCount = afterSaleByOrderId.get(o.getId());
            if (afterSaleCount == null && o.getGroupId() != null) {
                afterSaleCount = afterSaleByGroupId.get(o.getGroupId());
            }
            o.setAfterSaleCount(afterSaleCount == null ? 0 : afterSaleCount);
        }

        fillAfterSaleDone(orders);
    }

    private void fillAfterSaleDone(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> groupIds = orders.stream()
                .map(Order::getGroupId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty() && groupIds.isEmpty()) return;

        List<AfterSale> list = afterSaleMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AfterSale>()
                .in(!orderIds.isEmpty(), "order_id", orderIds)
                .or()
                .in(!groupIds.isEmpty(), "group_id", groupIds));

        Map<Long, int[]> statOrder = new HashMap<>();
        Map<Long, int[]> statGroup = new HashMap<>();
        for (AfterSale a : list) {
            if (a == null) continue;
            int s0 = a.getStatus() == null ? 0 : a.getStatus();
            int t0 = a.getType() == null ? 0 : a.getType();
            boolean done = s0 == 2 || s0 == 3 || (t0 == 1 && s0 == 1);
            if (a.getOrderId() != null) {
                int[] s = statOrder.computeIfAbsent(a.getOrderId(), k -> new int[]{0, 0});
                s[0] += 1;
                if (done) s[1] += 1;
            }
            if (a.getGroupId() != null) {
                int[] s = statGroup.computeIfAbsent(a.getGroupId(), k -> new int[]{0, 0});
                s[0] += 1;
                if (done) s[1] += 1;
            }
        }

        for (Order o : orders) {
            if (o == null) continue;
            int[] st = o.getId() == null ? null : statOrder.get(o.getId());
            if ((st == null || st[0] == 0) && o.getGroupId() != null) {
                st = statGroup.get(o.getGroupId());
            }
            if (st == null || st[0] == 0) {
                o.setAfterSaleDone(false);
            } else {
                o.setAfterSaleDone(st[1] == st[0]);
            }
        }
    }

    private void fillUserInfo(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return;
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds));
        Map<Long, User> map = new HashMap<>();
        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            map.put(u.getId(), u);
        }
        for (Order o : orders) {
            if (o == null || o.getUserId() == null) continue;
            User u = map.get(o.getUserId());
            if (u == null) continue;
            if (u.getNickname() != null && !u.getNickname().isBlank()) {
                o.setUserNickname(u.getNickname());
            }
            if (u.getAvatar() != null && !u.getAvatar().isBlank()) {
                o.setUserAvatar(u.getAvatar());
            }
        }
    }
}
