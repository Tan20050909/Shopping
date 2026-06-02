package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.AfterSale;
import com.shopping.admin.service.AfterSaleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/after-sale")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer handleStatus,
                          @RequestParam(required = false) Integer afterSaleType,
                          @RequestParam(required = false) String keyword) {
        return Result.success(afterSaleService.listAfterSales(current, size, handleStatus, afterSaleType, keyword));
    }

    @GetMapping("/{id}")
    public Result<AfterSale> detail(@PathVariable Long id) {
        return Result.success(afterSaleService.getById(id));
    }

    @GetMapping("/{id}/logistics")
    public Result<Map<String, Object>> logistics(@PathVariable Long id) {
        Map<String, Object> logistics = afterSaleService.getReturnLogistics(id);
        return Result.success(logistics);
    }

    @PostMapping("/handle")
    public Result<Void> handle(@RequestBody HandleRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        afterSaleService.handleAfterSale(request.afterSaleId, request.handleStatus, request.platformRemark, adminId);
        return Result.success();
    }

    public record HandleRequest(Long afterSaleId, Integer handleStatus, String platformRemark) {}
}
