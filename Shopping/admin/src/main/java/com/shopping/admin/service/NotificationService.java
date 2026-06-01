package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.*;
import com.shopping.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final MerchantMapper merchantMapper;
    private final GoodsMapper goodsMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final DisputeMapper disputeMapper;
    private final AbnormalOrderMapper abnormalOrderMapper;
    private final OrderMapper orderMapper;

    public PageResult<Notification> listNotifications(Long adminId, Integer type, Integer isRead, long current, long size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (adminId != null) wrapper.eq(Notification::getAdminId, adminId);
        if (type != null) wrapper.eq(Notification::getType, type);
        if (isRead != null) wrapper.eq(Notification::getIsRead, isRead);
        wrapper.orderByDesc(Notification::getCreateTime);
        Page<Notification> page = notificationMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public long getUnreadCount(Long adminId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (adminId != null) wrapper.eq(Notification::getAdminId, adminId);
        wrapper.eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    public void markRead(Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n != null) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    public void markAllRead(Long adminId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        if (adminId != null) wrapper.eq(Notification::getAdminId, adminId);
        wrapper.eq(Notification::getIsRead, 0).set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }

    public void deleteNotification(Long notificationId) {
        notificationMapper.deleteById(notificationId);
    }

    public void createBusinessNotification(Long adminId, int type, String title, String content, String bizType, Long bizId) {
        Notification n = new Notification();
        n.setAdminId(adminId != null ? adminId : 1L);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /**
     * 每分钟检查待办事项，自动生成通知
     */
    @Scheduled(fixedRate = 60000)
    public void autoGenerateNotifications() {
        try {
            // 检查待审核商户
            long pendingMerchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>().eq(Merchant::getAuditStatus, 0));
            if (pendingMerchants > 0) {
                checkOrCreateNotification(1L, 2, "待审核商户提醒",
                        String.format("当前有%d个商户等待审核，请及时处理", pendingMerchants), "merchant", null);
            }

            // 检查待审核商品
            long pendingGoods = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>().eq(Goods::getAuditStatus, 0));
            if (pendingGoods > 0) {
                checkOrCreateNotification(1L, 2, "待审核商品提醒",
                        String.format("当前有%d个商品等待审核", pendingGoods), "goods", null);
            }

            // 检查待处理售后
            long pendingAfterSale = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>().eq(AfterSale::getHandleStatus, 0));
            if (pendingAfterSale > 5) {
                checkOrCreateNotification(1L, 4, "售后积压预警",
                        String.format("待处理售后已达%d个，请优先处理", pendingAfterSale), "after_sale", null);
            }

            // 检查待处理争议
            long pendingDispute = disputeMapper.selectCount(new LambdaQueryWrapper<Dispute>().in(Dispute::getDisputeStatus, 0, 1, 2));
            if (pendingDispute > 3) {
                checkOrCreateNotification(1L, 4, "争议积压预警",
                        String.format("待处理争议%d个，请及时处理避免升级", pendingDispute), "dispute", null);
            }

            // 检查异常订单
            long pendingAbnormal = abnormalOrderMapper.selectCount(new LambdaQueryWrapper<AbnormalOrder>().eq(AbnormalOrder::getHandleStatus, 0));
            if (pendingAbnormal > 0) {
                checkOrCreateNotification(1L, 4, "异常订单提醒",
                        String.format("有%d笔异常订单需要处理", pendingAbnormal), "abnormal", null);
            }
        } catch (Exception e) {
            log.warn("自动生成通知异常: {}", e.getMessage());
        }
    }

    private void checkOrCreateNotification(Long adminId, int type, String title, String content, String bizType, Long bizId) {
        // 同一天内相同通知只保留一条，避免定时任务每5分钟刷屏
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (adminId != null) wrapper.eq(Notification::getAdminId, adminId);
        wrapper.eq(Notification::getType, type)
               .eq(Notification::getTitle, title)
               .eq(Notification::getContent, content)
               .ge(Notification::getCreateTime, todayStart);
        long count = notificationMapper.selectCount(wrapper);
        if (count == 0) {
            Notification n = new Notification();
            n.setAdminId(adminId);
            n.setTitle(title);
            n.setContent(content);
            n.setType(type);
            n.setBizType(bizType);
            n.setBizId(bizId);
            n.setIsRead(0);
            n.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(n);
        }
    }
}
