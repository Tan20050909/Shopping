package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.Logistics;
import com.shopping.entity.LogisticsTrace;
import com.shopping.entity.Order;
import com.shopping.mapper.LogisticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogisticsService extends ServiceImpl<LogisticsMapper, Logistics> {
    public static final int BIZ_TYPE_ORDER = 0;
    public static final int BIZ_TYPE_AFTER_SALE_BUYER_RETURN = 1;
    public static final int BIZ_TYPE_AFTER_SALE_MERCHANT_SHIP = 2;

    @Autowired
    private OrderService orderService;

    @Autowired
    private LogisticsTraceService logisticsTraceService;

    @Transactional
    public Logistics create(Logistics logistics) {
        return shipOrder(logistics.getOrderId(), logistics.getMerchantId(), logistics.getCompany(), logistics.getTrackingNo());
    }

    @Transactional
    public Logistics shipOrder(Long orderId, Long merchantId, String expressCompany, String expressNo) {
        UpsertResult upsert = upsertLogisticsOnly(orderId, merchantId, expressCompany, expressNo, BIZ_TYPE_ORDER);
        Logistics logistics = upsert.logistics;

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setDeliveryTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);

        String company = normalize(expressCompany);
        String no = normalize(expressNo);
        if (upsert.created) {
            logisticsTraceService.addTrace(logistics.getId(), "商家已发货：" + company + "（" + no + "）", null, LocalDateTime.now());
        } else if (upsert.changed) {
            logisticsTraceService.addTrace(logistics.getId(), "运单信息已更新：" + company + "（" + no + "）", null, LocalDateTime.now());
        }

        return logistics;
    }

    @Transactional
    public Logistics upsertAfterSaleBuyerReturnLogistics(Long orderId, Long merchantId, String expressCompany, String expressNo) {
        UpsertResult upsert = upsertLogisticsOnly(orderId, merchantId, expressCompany, expressNo, BIZ_TYPE_AFTER_SALE_BUYER_RETURN);
        Logistics logistics = upsert.logistics;
        String company = normalize(expressCompany);
        String no = normalize(expressNo);
        if (upsert.created) {
            logisticsTraceService.addTrace(logistics.getId(), "买家已寄回：" + company + "（" + no + "）", null, LocalDateTime.now());
        } else if (upsert.changed) {
            logisticsTraceService.addTrace(logistics.getId(), "买家寄回运单已更新：" + company + "（" + no + "）", null, LocalDateTime.now());
        }
        return logistics;
    }

    @Transactional
    public Logistics upsertAfterSaleMerchantShipLogistics(Long orderId, Long merchantId, String expressCompany, String expressNo) {
        UpsertResult upsert = upsertLogisticsOnly(orderId, merchantId, expressCompany, expressNo, BIZ_TYPE_AFTER_SALE_MERCHANT_SHIP);
        Logistics logistics = upsert.logistics;
        String company = normalize(expressCompany);
        String no = normalize(expressNo);
        if (upsert.created) {
            logisticsTraceService.addTrace(logistics.getId(), "商家售后发货：" + company + "（" + no + "）", null, LocalDateTime.now());
        } else if (upsert.changed) {
            logisticsTraceService.addTrace(logistics.getId(), "商家售后运单已更新：" + company + "（" + no + "）", null, LocalDateTime.now());
        }
        return logistics;
    }

    public boolean updateStatus(Long id, Integer status) {
        Logistics logistics = new Logistics();
        logistics.setId(id);
        logistics.setStatus(status);
        logistics.setUpdateTime(LocalDateTime.now());
        boolean ok = updateById(logistics);
        if (ok) {
            String content = statusToTraceContent(status);
            if (content != null) {
                logisticsTraceService.addTrace(id, content, null, LocalDateTime.now());
            }
        }
        return ok;
    }

    public List<LogisticsTrace> listTracesByLogisticsId(Long logisticsId) {
        return logisticsTraceService.listByLogisticsId(logisticsId);
    }

    public List<LogisticsTrace> listTracesByOrderId(Long orderId) {
        return listTracesByOrderId(orderId, BIZ_TYPE_ORDER);
    }

    public Logistics getByOrderIdAndBizType(Long orderId, Integer bizType) {
        if (orderId == null) return null;
        int bt = bizType == null ? BIZ_TYPE_ORDER : bizType;
        return getOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, orderId)
                .eq(Logistics::getBizType, bt));
    }

    public List<LogisticsTrace> listTracesByOrderId(Long orderId, Integer bizType) {
        Logistics logistics = getOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, orderId)
                .eq(Logistics::getBizType, bizType == null ? BIZ_TYPE_ORDER : bizType));
        if (logistics == null) {
            return List.of();
        }
        return logisticsTraceService.listByLogisticsId(logistics.getId());
    }

    private UpsertResult upsertLogisticsOnly(Long orderId, Long merchantId, String expressCompany, String expressNo, Integer bizType) {
        int bt = bizType == null ? BIZ_TYPE_ORDER : bizType;
        Logistics existed = getOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, orderId)
                .eq(Logistics::getBizType, bt));
        String existedCompany = existed == null ? null : existed.getCompany();
        String existedNo = existed == null ? null : existed.getTrackingNo();
        boolean created = false;

        Logistics logistics = existed;
        if (logistics == null) {
            created = true;
            logistics = new Logistics();
            logistics.setOrderId(orderId);
            logistics.setMerchantId(merchantId);
            logistics.setBizType(bt);
            logistics.setCompany(expressCompany);
            logistics.setTrackingNo(expressNo);
            logistics.setStatus(1);
            logistics.setCreateTime(LocalDateTime.now());
            logistics.setUpdateTime(LocalDateTime.now());
            save(logistics);
        } else {
            logistics.setMerchantId(merchantId);
            logistics.setCompany(expressCompany);
            logistics.setTrackingNo(expressNo);
            logistics.setUpdateTime(LocalDateTime.now());
            updateById(logistics);
        }

        boolean changed = !safeEquals(existedCompany, expressCompany) || !safeEquals(existedNo, expressNo);
        return new UpsertResult(logistics, created, !created && changed);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean safeEquals(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static class UpsertResult {
        private final Logistics logistics;
        private final boolean created;
        private final boolean changed;

        private UpsertResult(Logistics logistics, boolean created, boolean changed) {
            this.logistics = logistics;
            this.created = created;
            this.changed = changed;
        }
    }

    private String statusToTraceContent(Integer status) {
        if (status == null) return null;
        switch (status) {
            case 0:
                return "待发货";
            case 1:
                return "待揽收";
            case 2:
                return "运输中";
            case 3:
                return "派送中";
            case 4:
                return "已签收";
            case 5:
                return "拒收";
            default:
                return null;
        }
    }
}
