package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.*;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    private final OperationLogService operationLogService;
    private final NotificationService notificationService;
    private final ChatMessageService chatMessageService;
    private final LogisticsMapper logisticsMapper;
    private final LogisticsTraceMapper logisticsTraceMapper;
    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;


    public PageResult<Order> listOrders(long current, long size, String keyword, Integer orderStatus, Integer payStatus) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().like(Order::getConsignee, keyword));
        }
        if (orderStatus != null) {
            wrapper.eq(Order::getOrderStatus, orderStatus);
        }
        if (payStatus != null) {
            wrapper.eq(Order::getPayStatus, payStatus);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    /**
     * 发货：bizType=0, logisticsStatus=2, 先查后更新/插入, 补写 tb_logistics_trace
     */
    @Transactional
    public void shipOrder(Long orderId, String trackingNo, String shippingCompany) {
        Order order = getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getOrderStatus() != 1) throw new BusinessException("订单状态不允许发货");

        // 1. 按 order_id + biz_type=0 先查，有则更新，无则插入
        Logistics existing = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getOrderId, orderId)
                .eq(Logistics::getBizType, 0));

        if (existing != null) {
            existing.setExpressCompany(shippingCompany);
            existing.setExpressNo(trackingNo);
            existing.setLogisticsStatus(2); // 2=已发货
            existing.setUpdateTime(LocalDateTime.now());
            logisticsMapper.updateById(existing);
        } else {
            Logistics logistics = new Logistics();
            logistics.setOrderId(orderId);
            logistics.setMerchantId(order.getMerchantId());
            logistics.setBizType(0); // 0=订单发货
            logistics.setExpressCompany(shippingCompany);
            logistics.setExpressNo(trackingNo);
            logistics.setLogisticsStatus(2); // 2=已发货
            logistics.setCreateTime(LocalDateTime.now());
            logisticsMapper.insert(logistics);
            existing = logistics;
        }

        // 2. 补写 tb_logistics_trace
        LogisticsTrace trace = new LogisticsTrace();
        trace.setLogisticsId(existing.getLogisticsId());
        trace.setTraceContent("快递已揽收，物流公司：" + shippingCompany + "，运单号：" + trackingNo);
        trace.setTraceTime(LocalDateTime.now());
        trace.setTraceLocation("平台发货");
        logisticsTraceMapper.insert(trace);

        // 3. 同步 tb_order.order_status=2, delivery_time=NOW()
        order.setOrderStatus(2);
        order.setDeliveryTime(LocalDateTime.now());
        updateById(order);

        // 4. 记录操作日志
        operationLogService.saveBizLog(null, "订单管理", "SHIP",
                "订单发货：" + order.getOrderNo() + "，物流：" + shippingCompany + " " + trackingNo,
                "/api/order/" + orderId + "/ship", "PUT", "trackingNo=" + trackingNo);

        // 5. 发送通知
        notificationService.createBusinessNotification(1L, 1, "订单发货通知",
                "订单" + order.getOrderNo() + "已发货，物流单号：" + trackingNo, "order", orderId);
    }

    public void remindMerchantShip(Long orderId, String remark) {
        remindMerchantShip(orderId, remark, null);
    }

    /**
     * 提醒商家发货：保留聊天消息、通知、操作日志；删除对 adminNote 的伪落库
     */
    public void remindMerchantShip(Long orderId, String remark, Long adminId) {
        Order order = getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getOrderStatus() != 1) throw new BusinessException("仅待发货订单可提醒商家发货");
        if (order.getMerchantId() == null) throw new BusinessException("订单未关联商家，无法发送提醒");

        String content = "【平台发货提醒】订单" + order.getOrderNo() + "已付款但尚未发货，请尽快安排发货。";
        if (remark != null && !remark.isBlank()) {
            content += "备注：" + remark;
        }

        // 1. 写聊天消息
        ChatMessage message = new ChatMessage();
        message.setFromId(adminId != null ? adminId : 1L);
        message.setToId(order.getMerchantId());
        message.setFromType(3);
        message.setToType(2);
        message.setContent(content);
        message.setMsgType(1);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        chatMessageService.save(message);

        // 2. 记录操作日志（不写 adminNote）
        operationLogService.saveBizLog(adminId, "订单监管", "REMIND_SHIP",
                "提醒商家发货：" + order.getOrderNo() + "，商家ID：" + order.getMerchantId(),
                "/api/order/" + orderId + "/remind-ship", "POST", remark);

        // 3. 发送通知
        notificationService.createBusinessNotification(adminId, 5, "已提醒商家发货",
                "已向商家ID " + order.getMerchantId() + " 发送订单" + order.getOrderNo() + "的发货提醒", "order", orderId);
    }


    /**
     * 退款：查 tb_payment 获取 paymentId, refund_channel=9, refund_status=2, 防重复退款
     */
    @Transactional
    public void refundOrder(Long orderId, BigDecimal refundAmount, String refundReason) {
        Order order = getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");

        // 校验订单状态：只允许待发货(1)或待收货(2)的订单退款
        if (order.getOrderStatus() != 1 && order.getOrderStatus() != 2) {
            throw new BusinessException(400, "当前订单状态不允许退款，仅待发货/待收货订单可退款");
        }

        // 校验支付状态：必须已支付
        if (order.getPayStatus() != 1) {
            throw new BusinessException(400, "订单未支付，不能退款");
        }

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "退款金额必须大于0");
        }
        // 使用 payAmount 作为实付金额校验
        BigDecimal paidAmount = order.getPayAmount();
        if (paidAmount != null && refundAmount.compareTo(paidAmount) > 0) {
            throw new BusinessException(400, "退款金额不能超过订单实付金额：" + paidAmount);
        }

        // 防重复退款：同一 order_id 已有成功退款时拒绝
        Long existRefund = refundMapper.selectCount(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, orderId)
                .eq(Refund::getRefundStatus, 2));
        if (existRefund > 0) {
            throw new BusinessException(400, "该订单已有成功退款记录，不可重复退款");
        }

        // 按 group_id + user_id 查询真实 tb_payment.payment_id
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getGroupId, order.getGroupId())
                .eq(Payment::getUserId, order.getUserId())
                .eq(Payment::getPayStatus, 1) // 1=已支付
                .last("limit 1"));

        // 查不到支付单时抛业务异常，不允许插入 paymentId=null
        if (payment == null) {
            throw new BusinessException(400, "支付单不存在，不能退款。请检查该订单的支付记录");
        }

        // 1. 写 tb_refund
        Refund refund = new Refund();
        refund.setRefundNo("RF" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        refund.setOrderId(orderId);
        refund.setGroupId(order.getGroupId());
        refund.setUserId(order.getUserId());
        refund.setMerchantId(order.getMerchantId());
        refund.setPaymentId(payment.getPaymentId());
        refund.setRefundAmount(refundAmount);
        refund.setRefundStatus(2); // 2=退款成功
        refund.setRefundChannel(9); // 9=平台模拟退款
        refund.setThirdRefundNo("PLATFORM_" + System.currentTimeMillis());
        refund.setReason(refundReason);
        refund.setApplyTime(LocalDateTime.now());
        refund.setSuccessTime(LocalDateTime.now());
        refundMapper.insert(refund);

        // 2. 同步 tb_order.order_status=5
        order.setOrderStatus(5);
        updateById(order);

        // 3. 记录操作日志
        operationLogService.saveBizLog(null, "订单管理", "REFUND",
                "订单退款：" + order.getOrderNo() + "，金额：" + refundAmount,
                "/api/order/" + orderId + "/refund", "PUT", "refundAmount=" + refundAmount);

        // 4. 发送通知
        notificationService.createBusinessNotification(1L, 2, "订单退款提醒",
                "订单" + order.getOrderNo() + "已执行退款，金额：" + refundAmount, "order", orderId);
    }

    /**
     * 更新订单备注（adminNote 不存在于 tb_order，改为仅记录操作日志）
     */
    public void updateNote(Long orderId, String adminNote) {
        Order order = getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        // adminNote 不存在于 tb_order，不写库，仅记录操作日志
        operationLogService.saveBizLog(null, "订单管理", "NOTE",
                "更新订单备注：" + order.getOrderNo() + "，内容：" + adminNote,
                "/api/order/" + orderId + "/note", "PUT", adminNote);
    }

    /**
     * 取消订单
     */
    public void cancelOrder(Long orderId) {
        Order order = getById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getOrderStatus() >= 3) throw new BusinessException("订单已完成，无法取消");
        order.setOrderStatus(4);
        order.setCancelTime(LocalDateTime.now());
        updateById(order);
        operationLogService.saveBizLog(null, "订单管理", "CANCEL",
                "取消订单：" + order.getOrderNo(), "/api/order/" + orderId + "/cancel", "PUT", "orderId=" + orderId);
        notificationService.createBusinessNotification(1L, 2, "订单取消提醒",
                "订单" + order.getOrderNo() + "已取消", "order", orderId);
    }
}
