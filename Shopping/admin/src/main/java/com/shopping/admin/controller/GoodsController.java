package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Goods;
import com.shopping.admin.service.GoodsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer auditStatus,
                          @RequestParam(required = false) Integer status) {
        return Result.success(goodsService.listGoods(current, size, keyword, auditStatus, status));
    }

    @GetMapping("/{id}")
    public Result<Goods> detail(@PathVariable Long id) {
        return Result.success(goodsService.getById(id));
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody AuditRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        goodsService.auditGoods(request.goodsId, request.auditStatus, request.auditRemark, adminId);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        goodsService.updateGoodsStatus(id, request.status(), request.reason());
        return Result.success();
    }

    public record AuditRequest(Long goodsId, Integer auditStatus, String auditRemark) {}
    public record StatusRequest(Integer status, String reason) {}
}
