package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantFinance;
import com.shopping.entity.MerchantWithdraw;
import com.shopping.mapper.MerchantWithdrawMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MerchantWithdrawService extends ServiceImpl<MerchantWithdrawMapper, MerchantWithdraw> {

    @Autowired
    private MerchantFinanceService financeService;

    public MerchantWithdraw apply(MerchantWithdraw withdraw) {
        if (withdraw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        if (withdraw.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantId不能为空");
        }
        BigDecimal amount = withdraw.getAmount() == null ? BigDecimal.ZERO : withdraw.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提现金额必须大于0");
        }
        MerchantFinance finance = financeService.getByMerchantId(withdraw.getMerchantId());
        BigDecimal balance = finance == null || finance.getBalance() == null ? BigDecimal.ZERO : finance.getBalance();
        if (amount.compareTo(balance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "可提现余额不足");
        }
        if (withdraw.getWithdrawNo() == null || withdraw.getWithdrawNo().isBlank()) {
            withdraw.setWithdrawNo("WD" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        }
        if (withdraw.getBankName() == null || withdraw.getBankName().isBlank()) {
            withdraw.setBankName("未填写");
        }
        if (withdraw.getBankCard() == null || withdraw.getBankCard().isBlank()) {
            withdraw.setBankCard("未填写");
        }
        if (withdraw.getAccountName() == null || withdraw.getAccountName().isBlank()) {
            withdraw.setAccountName("未填写");
        }
        withdraw.setStatus(0);
        withdraw.setCreateTime(LocalDateTime.now());
        save(withdraw);
        return withdraw;
    }

    public List<MerchantWithdraw> listByMerchantId(Long merchantId) {
        return list(new LambdaQueryWrapper<MerchantWithdraw>()
                .eq(MerchantWithdraw::getMerchantId, merchantId)
                .orderByDesc(MerchantWithdraw::getCreateTime));
    }
}
