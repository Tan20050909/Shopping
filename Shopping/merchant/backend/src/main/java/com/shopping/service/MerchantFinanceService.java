package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.MerchantFinance;
import com.shopping.entity.MerchantWithdraw;
import com.shopping.entity.Order;
import com.shopping.mapper.MerchantFinanceMapper;
import com.shopping.mapper.MerchantWithdrawMapper;
import com.shopping.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MerchantFinanceService extends ServiceImpl<MerchantFinanceMapper, MerchantFinance> {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MerchantWithdrawMapper withdrawMapper;

    public MerchantFinance getByMerchantId(Long merchantId) {
        MerchantFinance finance = getOne(new LambdaQueryWrapper<MerchantFinance>().eq(MerchantFinance::getMerchantId, merchantId));
        if (finance != null) {
            return refreshFinance(finance);
        }

        MerchantFinance created = new MerchantFinance();
        created.setMerchantId(merchantId);
        created.setBalance(BigDecimal.ZERO);
        created.setUnsettledAmount(BigDecimal.ZERO);
        created.setCommissionRate(new BigDecimal("0.0500"));
        created.setFreezeAmount(BigDecimal.ZERO);
        created.setVersion(0);
        created.setUpdateTime(LocalDateTime.now());
        save(created);
        MerchantFinance saved = getOne(new LambdaQueryWrapper<MerchantFinance>().eq(MerchantFinance::getMerchantId, merchantId));
        return refreshFinance(saved);
    }

    private MerchantFinance refreshFinance(MerchantFinance finance) {
        if (finance == null || finance.getMerchantId() == null) return finance;
        Long merchantId = finance.getMerchantId();

        BigDecimal commissionRate = finance.getCommissionRate() == null ? new BigDecimal("0.0500") : finance.getCommissionRate();
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0) commissionRate = BigDecimal.ZERO;
        if (commissionRate.compareTo(BigDecimal.ONE) > 0) commissionRate = BigDecimal.ONE;

        BigDecimal completedGross = sumOrderPayAmount(merchantId, Arrays.asList(3));
        BigDecimal unsettled = sumOrderPayAmount(merchantId, Arrays.asList(1, 2));

        BigDecimal completedNet = completedGross.multiply(BigDecimal.ONE.subtract(commissionRate));

        BigDecimal withdrawFrozen = sumWithdrawAmount(merchantId, Arrays.asList(0, 1));
        BigDecimal withdrawSuccess = sumWithdrawAmount(merchantId, Arrays.asList(2));

        BigDecimal available = completedNet.subtract(withdrawSuccess).subtract(withdrawFrozen);
        if (available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;

        MerchantFinance patch = new MerchantFinance();
        patch.setId(finance.getId());
        patch.setBalance(available);
        patch.setUnsettledAmount(unsettled);
        patch.setFreezeAmount(withdrawFrozen);
        patch.setCommissionRate(commissionRate);
        patch.setUpdateTime(LocalDateTime.now());
        updateById(patch);

        finance.setBalance(available);
        finance.setUnsettledAmount(unsettled);
        finance.setFreezeAmount(withdrawFrozen);
        finance.setCommissionRate(commissionRate);
        finance.setUpdateTime(patch.getUpdateTime());
        return finance;
    }

    private BigDecimal sumOrderPayAmount(Long merchantId, List<Integer> statuses) {
        if (merchantId == null) return BigDecimal.ZERO;
        return orderMapper.selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order>()
                        .select("IFNULL(SUM(pay_amount),0)")
                        .eq("merchant_id", merchantId)
                        .eq("pay_status", 1)
                        .in(statuses != null && !statuses.isEmpty(), "order_status", statuses))
                .stream()
                .findFirst()
                .map(v -> new BigDecimal(String.valueOf(v)))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal sumWithdrawAmount(Long merchantId, List<Integer> statuses) {
        if (merchantId == null) return BigDecimal.ZERO;
        return withdrawMapper.selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MerchantWithdraw>()
                        .select("IFNULL(SUM(withdraw_amount),0)")
                        .eq("merchant_id", merchantId)
                        .in(statuses != null && !statuses.isEmpty(), "withdraw_status", statuses))
                .stream()
                .findFirst()
                .map(v -> new BigDecimal(String.valueOf(v)))
                .orElse(BigDecimal.ZERO);
    }
}
