package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.AbnormalOrder;
import com.shopping.admin.service.AbnormalOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/abnormal")
@RequiredArgsConstructor
public class AbnormalOrderController {

    private final AbnormalOrderService abnormalOrderService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer handleStatus,
                          @RequestParam(required = false) Integer abnormalType) {
        return Result.success(abnormalOrderService.listAbnormals(current, size, handleStatus, abnormalType));
    }

    @GetMapping("/{id}")
    public Result<AbnormalOrder> detail(@PathVariable Long id) {
        return Result.success(abnormalOrderService.getById(id));
    }

    @PostMapping("/handle")
    public Result<Void> handle(@RequestBody HandleRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        abnormalOrderService.handleAbnormal(request.abnormalId, request.handleStatus, request.handleRemark, adminId);
        return Result.success();
    }

    public record HandleRequest(Long abnormalId, Integer handleStatus, String handleRemark) {}
}
