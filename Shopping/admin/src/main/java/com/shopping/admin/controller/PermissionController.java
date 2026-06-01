package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Permission;
import com.shopping.admin.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/list")
    public Result<List<Permission>> list(@RequestParam(required = false) String module) {
        return Result.success(permissionService.listByModule(module));
    }

    @GetMapping("/all")
    public Result<List<Permission>> all() {
        return Result.success(permissionService.getAllPermissions());
    }
}
