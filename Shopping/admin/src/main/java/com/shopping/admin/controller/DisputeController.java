package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Dispute;
import com.shopping.admin.service.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/dispute")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer disputeStatus,
                          @RequestParam(required = false) Integer applyType,
                          @RequestParam(required = false) Integer judgeResult) {
        return Result.success(disputeService.listDisputes(current, size, disputeStatus, applyType, judgeResult));
    }

    @GetMapping("/{id}")
    public Result<Dispute> detail(@PathVariable Long id) {
        return Result.success(disputeService.getById(id));
    }

    @PostMapping("/judge")
    public Result<Void> judge(@RequestBody JudgeRequest request, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        if (request.disputeId() == null) {
            return Result.error(400, "纠纷ID不能为空");
        }
        if (request.judgeResult() == null) {
            return Result.error(400, "判责结果不能为空");
        }
        disputeService.judgeDispute(request.disputeId(),
                request.judgeResult(),
                request.finalAmount(),
                request.platformOpinion(),
                adminId);
        return Result.success();
    }

    public record JudgeRequest(Long disputeId, Integer judgeResult, BigDecimal finalAmount,
                               String platformOpinion) {}
}
