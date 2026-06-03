package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.AfterSale;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.AfterSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AfterSaleService extends ServiceImpl<AfterSaleMapper, AfterSale> {

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<AfterSale> listAfterSales(long current, long size, Integer handleStatus, Integer afterSaleType, String keyword) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        if (handleStatus != null) wrapper.eq(AfterSale::getHandleStatus, handleStatus);
        if (afterSaleType != null) wrapper.eq(AfterSale::getAfterSaleType, afterSaleType);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AfterSale::getAfterSaleNo, keyword)
                    .or().like(AfterSale::getApplyReason, keyword));
        }
        wrapper.orderByDesc(AfterSale::getApplyTime);
        Page<AfterSale> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    /**
     * 平台处理售后
     * 状态：0=待商家处理 1=商家同意 2=商家拒绝 3=平台介入 4=退款成功 5=已撤销
     * 允许流转：0→1/2/3, 1→4
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAfterSale(Long afterSaleId, Integer handleStatus, String platformRemark, Long adminId) {
        AfterSale afterSale = getById(afterSaleId);
        if (afterSale == null) throw new BusinessException("售后单不存在");

        int current = afterSale.getHandleStatus();
        boolean allowed = (current == 0 && (handleStatus == 1 || handleStatus == 2 || handleStatus == 3))
                       || (current == 1 && handleStatus == 4);
        if (!allowed) {
            throw new BusinessException("当前状态不允许此操作");
        }

        afterSale.setHandleStatus(handleStatus);
        if (platformRemark != null) afterSale.setPlatformRemark(platformRemark);
        afterSale.setHandleTime(LocalDateTime.now());
        afterSale.setHandleAdminId(adminId);
        updateById(afterSale);

        // 退款成功时创建/完成退款单 + 同步订单项
        if (handleStatus == 4) {
            ensureRefundCompleted(afterSale, null);
        }

        String statusText = switch (handleStatus) {
            case 1 -> "商家同意";
            case 2 -> "商家拒绝";
            case 3 -> "平台介入";
            case 4 -> "退款成功";
            case 5 -> "已撤销";
            default -> "待处理";
        };
        operationLogService.saveBizLog(adminId, "售后管理", "HANDLE",
                "处理售后单：" + afterSale.getAfterSaleNo() + "，状态：" + statusText,
                "/api/after-sale/handle", "POST", "afterSaleId=" + afterSaleId + ",handleStatus=" + handleStatus);
        notificationService.createBusinessNotification(1L, 2, "售后处理结果",
                "售后单" + afterSale.getAfterSaleNo() + "已处理，结果：" + statusText, "after_sale", afterSaleId);
    }

    /**
     * 获取完整的售后监管详情（含订单、日志、物流、纠纷）
     */
    public Map<String, Object> getDetail(Long afterSaleId) {
        AfterSale afterSale = getById(afterSaleId);
        if (afterSale == null) return null;

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("afterSale", afterSale);
        result.put("afterSaleId", afterSale.getAfterSaleId());

        // 查询关联订单
        if (afterSale.getOrderId() != null) {
            try {
                Map<String, Object> order = jdbcTemplate.queryForMap(
                        "SELECT order_id, order_no, group_no, order_status, pay_status, pay_amount, create_time " +
                        "FROM tb_order WHERE order_id = ?", afterSale.getOrderId());
                // 查询订单项
                var items = jdbcTemplate.queryForList(
                        "SELECT oi.order_item_id, oi.goods_name, oi.sku_name, oi.goods_pic AS snapshotGoodsPic,\n" +
                        "       g.goods_pic AS currentGoodsPic,\n" +
                        "       (SELECT pic_url FROM tb_goods_pic WHERE goods_id = oi.goods_id AND is_deleted = 0 ORDER BY pic_sort, pic_id LIMIT 1) AS firstGalleryPic,\n" +
                        "       COALESCE(NULLIF(g.goods_pic, ''), oi.goods_pic) AS goods_pic, oi.price, oi.num, oi.total_price\n" +
                        "FROM tb_order_item oi\n" +
                        "LEFT JOIN tb_goods g ON g.goods_id = oi.goods_id\n" +
                        "WHERE oi.order_id = ? ORDER BY oi.order_item_id", afterSale.getOrderId());
                result.put("order", order);
                result.put("orderItems", items);
            } catch (Exception ignored) {
            }
        }

        // 查询售后处理日志
        try {
            var logs = jdbcTemplate.queryForList(
                    "SELECT log_id, before_status, after_status, operator_type, operator_id, operation_desc, create_time " +
                    "FROM tb_after_sale_log WHERE after_sale_id = ? ORDER BY create_time ASC, log_id ASC", afterSaleId);
            result.put("logs", logs);
        } catch (Exception ignored) {
            result.put("logs", java.util.List.of());
        }

        // 查询退货物流
        try {
            Map<String, Object> logistics = getReturnLogistics(afterSaleId);
            if (logistics != null) result.put("returnLogistics", logistics);
        } catch (Exception ignored) {
        }

        // 查询纠纷信息
        try {
            Map<String, Object> dispute = jdbcTemplate.queryForMap(
                    "SELECT dispute_id, dispute_no, dispute_status, judge_result, final_amount, platform_opinion, judge_time " +
                    "FROM tb_dispute WHERE after_sale_id = ? ORDER BY create_time DESC LIMIT 1", afterSaleId);
            result.put("dispute", dispute);
        } catch (Exception ignored) {
        }

        // 查询退款信息
        try {
            Map<String, Object> refund = jdbcTemplate.queryForMap(
                    "SELECT refund_id, refund_no, refund_status, refund_amount, refund_channel, success_time " +
                    "FROM tb_refund WHERE after_sale_id = ? LIMIT 1", afterSaleId);
            result.put("refund", refund);
        } catch (Exception ignored) {
        }

        return result;
    }

    /**
     * 查询售后关联的买家退货物流信息
     */
    public Map<String, Object> getReturnLogistics(Long afterSaleId) {
        AfterSale afterSale = getById(afterSaleId);
        if (afterSale == null || afterSale.getOrderId() == null) {
            return null;
        }
        try {
            Map<String, Object> logistics = jdbcTemplate.queryForMap(
                    "SELECT logistics_id, express_company, express_no, logistics_status, create_time, update_time " +
                    "FROM tb_logistics WHERE order_id = ? AND biz_type = 1 LIMIT 1",
                    afterSale.getOrderId());
            if (logistics != null) {
                // 查询物流轨迹
                Object logisticsId = logistics.get("logistics_id");
                if (logisticsId != null) {
                    var traces = jdbcTemplate.queryForList(
                            "SELECT trace_id, trace_content, trace_time, trace_location " +
                            "FROM tb_logistics_trace WHERE logistics_id = ? ORDER BY trace_time ASC",
                            logisticsId);
                    logistics.put("traces", traces);
                }
            }
            return logistics;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 确保退款单已创建并标记成功。
     * @param afterSale 售后单实体
     * @param overrideAmount 裁决金额（非null时按此金额退款，否则按售后申请金额）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureRefundCompleted(AfterSale afterSale, BigDecimal overrideAmount) {
        Long afterSaleId = afterSale.getAfterSaleId();

        // 查真实 payment_id
        Long paymentId = null;
        if (afterSale.getGroupId() != null && afterSale.getUserId() != null) {
            try {
                Map<String, Object> payment = jdbcTemplate.queryForMap(
                        "SELECT payment_id FROM tb_payment WHERE group_id = ? AND user_id = ? LIMIT 1",
                        afterSale.getGroupId(), afterSale.getUserId());
                paymentId = ((Number) payment.get("payment_id")).longValue();
            } catch (Exception ignored) {
            }
        }
        if (paymentId == null) {
            throw new BusinessException("支付单不存在，不能生成退款单");
        }

        // 查是否已有退款单
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_refund WHERE after_sale_id = ?", Integer.class, afterSaleId);

        BigDecimal refundAmount = (overrideAmount != null && overrideAmount.compareTo(BigDecimal.ZERO) > 0)
                ? overrideAmount
                : (afterSale.getApplyAmount() != null ? afterSale.getApplyAmount() : BigDecimal.ZERO);

        if (existing == null || existing == 0) {
            // 创建退款单 refund_status=1
            String refundNo = "R" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            jdbcTemplate.update(
                    "INSERT INTO tb_refund(refund_no, payment_id, group_id, order_id, after_sale_id, user_id, merchant_id, " +
                    "refund_amount, refund_status, refund_channel, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 9, ?)",
                    refundNo,
                    paymentId,
                    afterSale.getGroupId() != null ? afterSale.getGroupId() : 0,
                    afterSale.getOrderId() != null ? afterSale.getOrderId() : 0,
                    afterSaleId,
                    afterSale.getUserId() != null ? afterSale.getUserId() : 0,
                    afterSale.getMerchantId() != null ? afterSale.getMerchantId() : 0,
                    refundAmount,
                    afterSale.getApplyReason()
            );
        }

        // 更新退款单为成功
        String thirdRefundNo = "ADMIN_REFUND_" + System.currentTimeMillis() + afterSaleId;
        jdbcTemplate.update(
                "UPDATE tb_refund SET refund_status = 2, third_refund_no = ?, success_time = NOW(), refund_amount = ? " +
                "WHERE after_sale_id = ? AND refund_status <> 2",
                thirdRefundNo, refundAmount, afterSaleId);

        // 同步订单项 after_sale_status=2
        if (afterSale.getOrderItemId() != null) {
            jdbcTemplate.update("UPDATE tb_order_item SET after_sale_status = 2 WHERE order_item_id = ?",
                    afterSale.getOrderItemId());
        }
    }
}
