package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Insight;
import com.shopping.admin.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(dashboardService.getStats());
    }

    @GetMapping("/order-trend")
    public Result<Map<String, Object>> orderTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.success(dashboardService.getOrderTrend(days));
    }

    @GetMapping("/sales-trend")
    public Result<Map<String, Object>> salesTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.success(dashboardService.getSalesTrend(days));
    }

    @GetMapping("/category-stats")
    public Result<Map<String, Object>> categoryStats() {
        return Result.success(dashboardService.getCategoryStats());
    }

    /** 智能洞察列表 */
    @GetMapping("/insights")
    public Result<List<Insight>> insights(@RequestParam(required = false) Integer handleStatus) {
        return Result.success(dashboardService.getInsights(handleStatus));
    }

    /** 处理洞察 */
    @PostMapping("/insight/{id}/handle")
    public Result<Void> handleInsight(@PathVariable Long id, @RequestParam Integer handleStatus,
                                       HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        dashboardService.handleInsight(id, handleStatus, adminId);
        return Result.success();
    }

    /** 用户增长漏斗 */
    @GetMapping("/user-funnel")
    public Result<Map<String, Object>> userFunnel() {
        return Result.success(dashboardService.getUserFunnel());
    }

    /** 异常订单分布 */
    @GetMapping("/abnormal-distribution")
    public Result<Map<String, Object>> abnormalDistribution() {
        return Result.success(dashboardService.getAbnormalDistribution());
    }
}
