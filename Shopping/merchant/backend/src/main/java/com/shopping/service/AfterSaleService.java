package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.AfterSale;
import com.shopping.entity.AfterSaleDetail;
import com.shopping.entity.Logistics;
import com.shopping.entity.LogisticsTrace;
import com.shopping.entity.Order;
import com.shopping.entity.OrderItem;
import com.shopping.mapper.AfterSaleMapper;
import com.shopping.mapper.OrderMapper;
import com.shopping.mapper.OrderItemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AfterSaleService extends ServiceImpl<AfterSaleMapper, AfterSale> {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AfterSale> listByMerchantId(Long merchantId, Integer status) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId));
        List<Long> orderIds = orders == null ? new ArrayList<>() : orders.stream()
                .map(Order::getId)
                .filter(v -> v != null)
                .collect(Collectors.toList());

        List<Long> groupIds = orders == null ? new ArrayList<>() : orders.stream()
                .map(Order::getGroupId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getMerchantId, merchantId));
        List<Long> orderItemIds = orderItems == null ? new ArrayList<>() : orderItems.stream()
                .map(OrderItem::getId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(AfterSale::getMerchantId, merchantId)
                .or()
                .in(!orderIds.isEmpty(), AfterSale::getOrderId, orderIds)
                .or()
                .in(!orderItemIds.isEmpty(), AfterSale::getOrderItemId, orderItemIds)
                .or()
                .in(!groupIds.isEmpty(), AfterSale::getGroupId, groupIds));
        if (status != null) {
            wrapper.eq(AfterSale::getStatus, status);
        }
        wrapper.orderByDesc(AfterSale::getCreateTime);
        return list(wrapper);
    }

    public boolean handle(Long id, Integer status, String remark, String evidence) {
        AfterSale existed = getById(id);
        if (existed == null) {
            return false;
        }

        if (status == null || (status != 1 && status != 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法操作");
        }

        Integer type = existed.getType();

        int nextStatus;
        if (status == 2) {
            nextStatus = 2;  // 拒绝
        } else {
            // 同意：商家同意后买家自行填写退货单号，故此处不再拦截
            nextStatus = 1;  // 商家已同意
        }

        String merchantRemark = mergeMerchantRemark(existed.getMerchantRemark(), remark, evidence);
        AfterSale afterSale = new AfterSale();
        afterSale.setId(id);
        afterSale.setStatus(nextStatus);
        afterSale.setMerchantRemark(merchantRemark);
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSale.setHandleTime(LocalDateTime.now());
        return updateById(afterSale);
    }

    public boolean complete(Long id) {
        AfterSale existed = getById(id);
        if (existed == null) {
            return false;
        }
        if (existed.getType() != null && existed.getType() == 2) {
            Logistics merchant = logisticsService.getByOrderIdAndBizType(existed.getOrderId(), LogisticsService.BIZ_TYPE_AFTER_SALE_MERCHANT_SHIP);
            String no = merchant == null ? "" : String.valueOf(merchant.getTrackingNo() == null ? "" : merchant.getTrackingNo()).trim();
            if (no.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先填写售后发货单号");
            }
        }
        // 退货退款：完成前需要买家已填写退货单号
        if (existed.getType() != null && existed.getType() == 4) {
            Logistics buyer = logisticsService.getByOrderIdAndBizType(existed.getOrderId(), LogisticsService.BIZ_TYPE_AFTER_SALE_BUYER_RETURN);
            String no = buyer == null ? "" : String.valueOf(buyer.getTrackingNo() == null ? "" : buyer.getTrackingNo()).trim();
            if (no.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先等待买家填写退货单号");
            }
        }
        AfterSale patch = new AfterSale();
        patch.setId(id);
        patch.setStatus(3);
        patch.setUpdateTime(LocalDateTime.now());
        if (existed.getHandleTime() == null) {
            patch.setHandleTime(LocalDateTime.now());
        }
        return updateById(patch);
    }

    public AfterSaleDetail detail(Long afterSaleId) {
        AfterSale afterSale = getById(afterSaleId);
        if (afterSale == null) {
            return null;
        }
        AfterSaleDetail detail = new AfterSaleDetail();
        detail.setAfterSale(afterSale);

        Order order = afterSale.getOrderId() == null ? null : orderMapper.selectById(afterSale.getOrderId());
        detail.setOrder(order);

        // 查询订单项（含商品图片、名称等信息）
        if (afterSale.getOrderId() != null) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, afterSale.getOrderId())
            );
            detail.setOrderItems(items);
        }

        Long orderId = afterSale.getOrderId();
        Logistics buyerLogistics = logisticsService.getByOrderIdAndBizType(orderId, LogisticsService.BIZ_TYPE_AFTER_SALE_BUYER_RETURN);
        Logistics merchantLogistics = logisticsService.getByOrderIdAndBizType(orderId, LogisticsService.BIZ_TYPE_AFTER_SALE_MERCHANT_SHIP);

        List<LogisticsTrace> buyerTraces = orderId == null ? List.of() : logisticsService.listTracesByOrderId(orderId, LogisticsService.BIZ_TYPE_AFTER_SALE_BUYER_RETURN);
        List<LogisticsTrace> merchantTraces = orderId == null ? List.of() : logisticsService.listTracesByOrderId(orderId, LogisticsService.BIZ_TYPE_AFTER_SALE_MERCHANT_SHIP);

        detail.setBuyerLogistics(buyerLogistics);
        detail.setBuyerLogisticsTraces(buyerTraces);
        detail.setMerchantLogistics(merchantLogistics);
        detail.setMerchantLogisticsTraces(merchantTraces);

        // 查询关联纠纷信息
        try {
            List<Map<String, Object>> disputes = jdbcTemplate.queryForList(
                "SELECT dispute_id, dispute_no, dispute_status, judge_result, final_amount, platform_opinion, judge_time " +
                "FROM tb_dispute WHERE after_sale_id = ? ORDER BY create_time DESC LIMIT 1", afterSaleId
            );
            if (disputes != null && !disputes.isEmpty()) {
                detail.setDispute(disputes.get(0));
            }
        } catch (Exception ignored) {
            // tb_dispute 表可能不存在，忽略
        }

        return detail;
    }

    public Logistics uploadBuyerLogistics(Long afterSaleId, String expressCompany, String expressNo) {
        AfterSale afterSale = getById(afterSaleId);
        if (afterSale == null || afterSale.getOrderId() == null || afterSale.getMerchantId() == null) {
            return null;
        }
        Integer t = afterSale.getType();
        if (t == null || (t != 2 && t != 4)) {
            return null;
        }
        Logistics logistics = logisticsService.upsertAfterSaleBuyerReturnLogistics(
                afterSale.getOrderId(),
                afterSale.getMerchantId(),
                expressCompany,
                expressNo
        );
        AfterSale update = new AfterSale();
        update.setId(afterSaleId);
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);
        return logistics;
    }

    public Logistics merchantAfterSaleShip(Long afterSaleId, String expressCompany, String expressNo) {
        AfterSale afterSale = getById(afterSaleId);
        if (afterSale == null || afterSale.getOrderId() == null || afterSale.getMerchantId() == null) {
            return null;
        }
        if (afterSale.getType() == null || afterSale.getType() != 2) {
            return null;
        }
        Logistics logistics = logisticsService.upsertAfterSaleMerchantShipLogistics(
                afterSale.getOrderId(),
                afterSale.getMerchantId(),
                expressCompany,
                expressNo
        );
        AfterSale update = new AfterSale();
        update.setId(afterSaleId);
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);
        return logistics;
    }

    private String mergeMerchantRemark(String existed, String remark, String evidence) {
        String r = remark == null ? "" : remark.trim();
        String e = evidence == null ? "" : evidence.trim();
        if (e.isBlank() && r.isBlank()) {
            return existed;
        }
        try {
            Map<String, Object> map = new HashMap<>();
            if (existed != null && existed.trim().startsWith("{")) {
                map = objectMapper.readValue(existed, Map.class);
            } else if (existed != null && !existed.isBlank()) {
                map.put("remark", existed);
            }
            if (!r.isBlank()) {
                map.put("remark", r);
            }
            if (!e.isBlank()) {
                map.put("evidence", e);
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            if (e.isBlank()) return r.isBlank() ? existed : r;
            if (r.isBlank()) return e;
            return r + "\n" + e;
        }
    }
}
