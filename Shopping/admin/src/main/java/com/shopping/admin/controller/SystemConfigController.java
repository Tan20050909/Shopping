package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.SystemConfig;
import com.shopping.admin.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/list")
    public Result<List<SystemConfig>> list(@RequestParam(required = false) String configGroup) {
        try {
            return Result.success(systemConfigService.listByGroup(configGroup));
        } catch (Exception e) {
            return Result.success(List.of());
        }
    }

    @GetMapping("/map")
    public Result<Map<String, String>> getConfigMap(@RequestParam(required = false) String configGroup) {
        try {
            return Result.success(systemConfigService.getConfigMap(configGroup));
        } catch (Exception e) {
            return Result.success(Map.of());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SystemConfig config) {
        systemConfigService.updateConfigValue(id, config.getConfigValue());
        return Result.success();
    }

    @PostMapping
    public Result<Void> add(@RequestBody SystemConfig config) {
        systemConfigService.save(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        systemConfigService.removeById(id);
        return Result.success();
    }
}
