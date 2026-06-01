package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.AfterSale;
import com.shopping.admin.entity.Dispute;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.AfterSaleMapper;
import com.shopping.admin.mapper.DisputeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DisputeService extends ServiceImpl<DisputeMapper, Dispute> {

    private final AfterSaleMapper afterSaleMapper;
    private final AfterSaleService afterSaleService;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<Dispute> listDisputes(long current, long size, Integer disputeStatus,
                                             Integer applyType, Integer judgeResult) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<>();
        if (disputeStatus != null) wrapper.eq(Dispute::getDisputeStatus, disputeStatus);
        if (applyType != null) wrapper.eq(Dispute::getApplyType, applyType);
        if (judgeResult != null) wrapper.eq(Dispute::getJudgeResult, judgeResult);
        wrapper.orderByDesc(Dispute::getCreateTime);
        Page<Dispute> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    /**
     * 平台处理纠纷（判责）
     * @param judgeResult 判责结果：1-支持用户 2-支持商家 3-部分支持 4-双方协商关闭
     * @param finalAmount 最终退款或补偿金额
     */
    @Transactional(rollbackFor = Exception.class)
    public void judgeDispute(Long disputeId, Integer judgeResult, BigDecimal finalAmount,
                             String platformOpinion, Long adminId) {
        Dispute dispute = getById(disputeId);
        if (dispute == null) throw new BusinessException("纠纷单不存在");

        // 部分支持必须填写裁决金额
        if (judgeResult != null && judgeResult == 3) {
            if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("部分支持必须填写裁决金额且大于0");
            }
            // 校验裁决金额不超过售后申请金额
            if (dispute.getAfterSaleId() != null) {
                AfterSale afterSale = afterSaleMapper.selectById(dispute.getAfterSaleId());
                if (afterSale != null && afterSale.getApplyAmount() != null
                        && finalAmount.compareTo(afterSale.getApplyAmount()) > 0) {
                    throw new BusinessException("裁决金额不能超过售后申请金额：" + afterSale.getApplyAmount());
                }
            }
        }

        dispute.setDisputeStatus(3); // 已裁决
        dispute.setJudgeResult(judgeResult);
        dispute.setFinalAmount(finalAmount);
        dispute.setPlatformAdminId(adminId);
        dispute.setPlatformOpinion(platformOpinion);
        dispute.setJudgeTime(LocalDateTime.now());
        updateById(dispute);

        // 同步售后单 + 退款（传入裁决金额）
        syncAfterSaleAndRefund(dispute, judgeResult, finalAmount, platformOpinion);

        String resultText = switch (judgeResult != null ? judgeResult : 0) {
            case 1 -> "支持用户";
            case 2 -> "支持商家";
            case 3 -> "部分支持";
            case 4 -> "协商关闭";
            default -> "已处理";
        };
        operationLogService.saveBizLog(adminId, "纠纷仲裁", "JUDGE",
                "处理纠纷单：" + dispute.getDisputeNo() + "，结果：" + resultText,
                "/api/dispute/judge", "POST", "disputeId=" + disputeId);
        notificationService.createBusinessNotification(1L, 2, "纠纷处理结果",
                "纠纷单" + dispute.getDisputeNo() + "已裁决，结果：" + resultText,
                "dispute", disputeId);
    }

    /**
     * 将纠纷裁决结果同步到售后单 + 退款闭环
     */
    private void syncAfterSaleAndRefund(Dispute dispute, Integer judgeResult, BigDecimal finalAmount, String platformOpinion) {
        if (dispute.getAfterSaleId() == null) return;
        AfterSale afterSale = afterSaleMapper.selectById(dispute.getAfterSaleId());
        if (afterSale == null) return;

        AfterSale update = new AfterSale();
        update.setAfterSaleId(afterSale.getAfterSaleId());
        update.setUpdateTime(LocalDateTime.now());

        if (judgeResult != null && (judgeResult == 1 || judgeResult == 3)) {
            // 支持用户 或 部分支持 → 退款成功
            update.setHandleStatus(4);
            update.setHandleTime(LocalDateTime.now());
            // 传入裁决金额：部分支持时按 finalAmount 退款，全额支持时按售后申请金额
            BigDecimal refundAmount = (judgeResult == 3 && finalAmount != null && finalAmount.compareTo(BigDecimal.ZERO) > 0)
                    ? finalAmount
                    : null; // null 表示用售后申请金额
            afterSaleService.ensureRefundCompleted(afterSale, refundAmount);
        } else if (judgeResult != null && judgeResult == 2) {
            // 支持商家 → 恢复为商家拒绝
            update.setHandleStatus(2);
            update.setHandleTime(LocalDateTime.now());
        }

        if (platformOpinion != null && !platformOpinion.isBlank()) {
            update.setPlatformRemark(platformOpinion);
        }
        afterSaleMapper.updateById(update);
    }
}
