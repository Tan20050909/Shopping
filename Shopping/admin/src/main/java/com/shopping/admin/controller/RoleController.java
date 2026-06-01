package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Role;
import com.shopping.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        return Result.success(roleService.listRoles(current, size, keyword));
    }

    @GetMapping("/all")
    public Result<List<Role>> all() {
        return Result.success(roleService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Role role) {
        roleService.save(role);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Role role) {
        roleService.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }

    /** 获取角色权限ID列表 */
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        return Result.success(roleService.getRolePermissionIds(id));
    }

    /** 设置角色权限 */
    @PutMapping("/{id}/permissions")
    public Result<Void> setPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        roleService.setRolePermissions(id, permissionIds);
        return Result.success();
    }
}
