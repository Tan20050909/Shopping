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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantService extends ServiceImpl<MerchantMapper, Merchant> {

    private final MerchantAuditLogMapper merchantAuditLogMapper;
    private final MerchantCreditLogMapper merchantCreditLogMapper;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<Merchant> listMerchants(long current, long size, String keyword,
                                               Integer auditStatus, Integer status,
                                               Integer merchantType) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Merchant::getMerchantName, keyword)
                    .or().like(Merchant::getPhone, keyword));
        }
        if (auditStatus != null) wrapper.eq(Merchant::getAuditStatus, auditStatus);
        if (status != null) wrapper.eq(Merchant::getStatus, status);
        if (merchantType != null) wrapper.eq(Merchant::getMerchantType, merchantType);
        wrapper.orderByDesc(Merchant::getRegisterTime);
        Page<Merchant> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public void auditMerchant(Long merchantId, Integer auditStatus, String auditRemark, Long adminId) {
        Merchant merchant = getById(merchantId);
        if (merchant == null) throw new BusinessException("商家不存在");
        if (auditStatus == 2 && (auditRemark == null || auditRemark.isBlank())) {
            throw new BusinessException("拒绝审核必须填写原因");
        }
        merchant.setAuditStatus(auditStatus);
        merchant.setAuditRemark(auditRemark);
        if (auditStatus == 1) {
            merchant.setStatus(1);
            merchant.setAuditTime(LocalDateTime.now());
        }
        if (auditStatus == 2) {
            merchant.setStatus(0);
        }
        updateById(merchant);

        MerchantAuditLog log = new MerchantAuditLog();
        log.setMerchantId(merchantId);
        log.setAuditStatus(auditStatus);
        log.setAuditAdminId(adminId);
        log.setAuditRemark(auditRemark);
        merchantAuditLogMapper.insert(log);

        String resultText = auditStatus == 1 ? "审核通过" : auditStatus == 2 ? "审核拒绝" : "审核退回";
        operationLogService.saveBizLog(adminId, "商户管理", "AUDIT",
                resultText + "商户：" + merchant.getMerchantName(), "/api/merchant/audit", "POST",
                "merchantId=" + merchantId + ",auditStatus=" + auditStatus);
        notificationService.createBusinessNotification(1L, 3, "商户审核结果",
                "商户" + "\"" + merchant.getMerchantName() + "\"" + "已" + resultText, "merchant", merchantId);
    }

    /** 更新营业状态：只允许 status=0（停业）或 status=1（恢复营业） */
    public void updateMerchantStatus(Long merchantId, Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException("此接口只允许设置停业(0)或恢复营业(1)，冻结请使用冻结接口");
        }
        Merchant merchant = getById(merchantId);
        if (merchant == null) throw new BusinessException("商家不存在");
        if (status == 1 && (merchant.getAuditStatus() == null || merchant.getAuditStatus() != 1)) {
            throw new BusinessException("商家未审核通过，不能恢复营业");
        }
        merchant.setStatus(status);
        updateById(merchant);
        operationLogService.saveBizLog(null, "商户管理", "STATUS",
                (status == 1 ? "恢复营业：" : "停业商户：") + merchant.getMerchantName(),
                "/api/merchant/" + merchantId + "/status", "PUT", "status=" + status);
    }

    /** 更新商家信用分（写入 audit_remark 记录信用分变动） */
    public void updateCreditScore(Long merchantId, int scoreChange, String reason, String dimension, Long operatorId) {
        Merchant merchant = getById(merchantId);
        if (merchant == null) throw new BusinessException("商家不存在");

        BigDecimal before = merchant.getShopScore() == null ? new BigDecimal("5.0") : merchant.getShopScore();
        BigDecimal after = before.add(BigDecimal.valueOf(scoreChange));
        if (after.compareTo(BigDecimal.ZERO) < 0) after = BigDecimal.ZERO;
        if (after.compareTo(new BigDecimal("5.0")) > 0) after = new BigDecimal("5.0");
        merchant.setShopScore(after);
        updateById(merchant);

        operationLogService.saveBizLog(operatorId, "商户信用", "CREDIT",
                "商户" + "\"" + merchant.getMerchantName() + "\"" + "信用分变动：" + before + " -> " + after,
                "/api/merchant/credit", "POST", "merchantId=" + merchantId + ",scoreChange=" + scoreChange);

        // 信用分降至危险等级，自动冻结
        if (after.compareTo(new BigDecimal("1.5")) < 0 && (merchant.getStatus() == null || merchant.getStatus() != 3)) {
            merchant.setStatus(3);
            merchant.setAuditRemark("信用分降至" + after + "，自动冻结");
            updateById(merchant);
        }
    }

    /** 冻结商家（status=3） */
    public void freezeMerchant(Long merchantId, String reason) {
        Merchant merchant = getById(merchantId);
        if (merchant == null) throw new BusinessException("商家不存在");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("冻结原因不能为空");
        }
        merchant.setStatus(3);
        merchant.setAuditRemark(reason);
        updateById(merchant);
        operationLogService.saveBizLog(null, "商户管理", "FREEZE",
                "冻结商户：" + merchant.getMerchantName() + "，原因：" + reason,
                "/api/merchant/" + merchantId + "/freeze", "PUT", "status=3");
        notificationService.createBusinessNotification(1L, 4, "商户冻结通知",
                "商户" + "\"" + merchant.getMerchantName() + "\"" + "已被平台冻结，原因：" + reason, "merchant", merchantId);
    }

    /** 解冻商家 */
    public void unfreezeMerchant(Long merchantId) {
        Merchant merchant = getById(merchantId);
        if (merchant == null) throw new BusinessException("商家不存在");
        if (merchant.getAuditStatus() != null && merchant.getAuditStatus() == 1) {
            merchant.setStatus(1);
        } else {
            merchant.setStatus(0);
        }
        merchant.setAuditRemark(null);
        updateById(merchant);
        operationLogService.saveBizLog(null, "商户管理", "UNFREEZE",
                "解冻商户：" + merchant.getMerchantName(),
                "/api/merchant/" + merchantId + "/unfreeze", "PUT", "");
        notificationService.createBusinessNotification(1L, 3, "商户解冻通知",
                "商户" + "\"" + merchant.getMerchantName() + "\"" + "已被解冻", "merchant", merchantId);
    }

    /** 商家清退（status=3 + auditRemark="平台清退"） */
    public void delistMerchant(Long merchantId) {
        Merchant merchant = getById(merchantId);
        if (merchant == null) throw new BusinessException("商家不存在");
        merchant.setStatus(3);
        merchant.setAuditRemark("平台清退");
        updateById(merchant);
        operationLogService.saveBizLog(null, "商户管理", "DELIST",
                "清退商户：" + merchant.getMerchantName(), "/api/merchant/" + merchantId + "/delist", "POST", "merchantId=" + merchantId);
        notificationService.createBusinessNotification(1L, 4, "商户清退提醒",
                "商户" + "\"" + merchant.getMerchantName() + "\"" + "已被平台清退", "merchant", merchantId);
    }

    public List<MerchantAuditLog> getAuditLogs(Long merchantId) {
        return merchantAuditLogMapper.selectList(
            new LambdaQueryWrapper<MerchantAuditLog>()
                .eq(MerchantAuditLog::getMerchantId, merchantId)
                .orderByDesc(MerchantAuditLog::getCreateTime));
    }
}
