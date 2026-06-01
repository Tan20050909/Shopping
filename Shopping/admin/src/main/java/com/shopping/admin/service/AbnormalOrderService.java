package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.AbnormalOrder;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.AbnormalOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AbnormalOrderService extends ServiceImpl<AbnormalOrderMapper, AbnormalOrder> {

    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<AbnormalOrder> listAbnormals(long current, long size, Integer handleStatus, Integer abnormalType) {
        LambdaQueryWrapper<AbnormalOrder> wrapper = new LambdaQueryWrapper<>();
        if (handleStatus != null) wrapper.eq(AbnormalOrder::getHandleStatus, handleStatus);
        if (abnormalType != null) wrapper.eq(AbnormalOrder::getAbnormalType, abnormalType);
        wrapper.orderByDesc(AbnormalOrder::getCreateTime);
        Page<AbnormalOrder> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public void handleAbnormal(Long abnormalId, Integer handleStatus, String handleRemark, Long adminId) {
        AbnormalOrder abnormal = getById(abnormalId);
        if (abnormal == null) throw new BusinessException("异常记录不存在");
        if (abnormal.getHandleStatus() == 1) throw new BusinessException("该记录已处理");
        abnormal.setHandleStatus(handleStatus);
        abnormal.setHandleAdminId(adminId);
        abnormal.setHandleRemark(handleRemark);
        abnormal.setHandleTime(java.time.LocalDateTime.now());
        updateById(abnormal);
        operationLogService.saveBizLog(adminId, "异常订单", "HANDLE",
                "处理异常订单记录：" + abnormalId + "，状态：" + handleStatus,
                "/api/abnormal/handle", "POST", "abnormalId=" + abnormalId);
        notificationService.createBusinessNotification(1L, 2, "异常订单处理结果",
                "异常订单记录" + abnormalId + "已处理", "abnormal", abnormalId);
    }
}
