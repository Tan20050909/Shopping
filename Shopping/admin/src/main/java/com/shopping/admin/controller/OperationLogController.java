package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String module,
                          @RequestParam(required = false) String operationType,
                          @RequestParam(required = false) Long adminId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status) {
        return Result.success(operationLogService.listLogs(current, size, module, operationType, adminId, keyword, status));
    }
}
