package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Merchant;
import com.shopping.admin.entity.MerchantAuditLog;
import com.shopping.admin.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer auditStatus,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Integer merchantType) {
        return Result.success(merchantService.listMerchants(current, size, keyword, auditStatus, status, merchantType));
    }

    @GetMapping("/{id}")
    public Result<Merchant> detail(@PathVariable Long id) {
        return Result.success(merchantService.getById(id));
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody AuditRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        merchantService.auditMerchant(request.merchantId, request.auditStatus, request.auditRemark, adminId);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        merchantService.updateMerchantStatus(id, status);
        return Result.success();
    }

    /** 更新信用分 */
    @PostMapping("/credit")
    public Result<Void> updateCredit(@RequestBody CreditRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        merchantService.updateCreditScore(request.merchantId, request.scoreChange, request.reason, request.dimension, adminId);
        return Result.success();
    }

    /** 冻结商家 */
    @PutMapping("/{id}/freeze")
    public Result<Void> freeze(@PathVariable Long id, @RequestBody FreezeRequest request) {
        merchantService.freezeMerchant(id, request.reason());
        return Result.success();
    }

    /** 解冻商家 */
    @PutMapping("/{id}/unfreeze")
    public Result<Void> unfreeze(@PathVariable Long id) {
        merchantService.unfreezeMerchant(id);
        return Result.success();
    }

    /** 商家清退 */
    @PostMapping("/{id}/delist")
    public Result<Void> delist(@PathVariable Long id) {
        merchantService.delistMerchant(id);
        return Result.success();
    }

    /** 审核日志 */
    @GetMapping("/{id}/audit-logs")
    public Result<List<MerchantAuditLog>> auditLogs(@PathVariable Long id) {
        return Result.success(merchantService.getAuditLogs(id));
    }

    public record AuditRequest(Long merchantId, Integer auditStatus, String auditRemark) {}
    public record CreditRequest(Long merchantId, Integer scoreChange, String reason, String dimension) {}
    public record FreezeRequest(String reason) {}
}
