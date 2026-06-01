package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    public Result<Map<String, Object>> salesReport(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(reportService.getSalesReport(days, startDate, endDate));
    }

    @GetMapping("/user-analysis")
    public Result<Map<String, Object>> userAnalysis(@RequestParam(defaultValue = "30") int days) {
        return Result.success(reportService.getUserAnalysis(days));
    }

    @GetMapping("/goods-ranking")
    public Result<Map<String, Object>> goodsRanking(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int top) {
        return Result.success(reportService.getGoodsRanking(days, top));
    }

    @GetMapping("/merchant-ranking")
    public Result<Map<String, Object>> merchantRanking(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int top) {
        return Result.success(reportService.getMerchantRanking(days, top));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        return Result.success(reportService.getOverview(days));
    }
}
