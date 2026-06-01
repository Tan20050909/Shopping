package com.shopping.controller;

import com.shopping.entity.MerchantFinance;
import com.shopping.entity.MerchantAccountFlow;
import com.shopping.entity.MerchantWithdraw;
import com.shopping.service.MerchantAccountFlowService;
import com.shopping.service.MerchantFinanceService;
import com.shopping.service.MerchantWithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class MerchantFinanceController {

    @Autowired
    private MerchantFinanceService financeService;

    @Autowired
    private MerchantWithdrawService withdrawService;

    @Autowired
    private MerchantAccountFlowService flowService;

    @GetMapping
    public MerchantFinance getFinance(@RequestParam Long merchantId) {
        return financeService.getByMerchantId(merchantId);
    }

    @PostMapping("/withdraw")
    public MerchantWithdraw applyWithdraw(@RequestBody MerchantWithdraw withdraw) {
        return withdrawService.apply(withdraw);
    }

    @GetMapping("/withdraw/list")
    public List<MerchantWithdraw> listWithdraw(@RequestParam Long merchantId) {
        return withdrawService.listByMerchantId(merchantId);
    }

    @GetMapping("/flow/list")
    public List<MerchantAccountFlow> listFlow(
            @RequestParam Long merchantId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer flowType
    ) {
        return flowService.listByMerchantId(merchantId, limit, flowType);
    }
}
