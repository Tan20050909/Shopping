package com.shopping.controller;

import com.shopping.entity.Logistics;
import com.shopping.entity.LogisticsTrace;
import com.shopping.service.LogisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @PostMapping
    public Logistics create(@RequestBody Logistics logistics) {
        return logisticsService.create(logistics);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return logisticsService.updateStatus(id, status);
    }

    @GetMapping("/{id}/traces")
    public List<LogisticsTrace> listTraces(@PathVariable Long id) {
        return logisticsService.listTracesByLogisticsId(id);
    }

    @GetMapping("/order/{orderId}/traces")
    public List<LogisticsTrace> listTracesByOrderId(@PathVariable Long orderId, @RequestParam(required = false) Integer bizType) {
        return logisticsService.listTracesByOrderId(orderId, bizType);
    }
}
