package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Admin;
import com.shopping.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.success(adminService.login(request.username(), request.password()));
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("adminId");
        return Result.success(adminService.buildAdminInfo(adminId));
    }


    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        return Result.success(adminService.listAdmins(current, size, keyword));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Admin admin) {
        adminService.addAdmin(admin);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Admin admin) {
        adminService.updateAdmin(admin);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return Result.success();
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getRoles(@PathVariable Long id) {
        return Result.success(adminService.getRoleIds(id));
    }

    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Object roleIdsBody) {
        List<Long> roleIds;
        if (roleIdsBody instanceof List<?> list) {
            roleIds = list.stream().map(v -> Long.parseLong(v.toString())).toList();
        } else if (roleIdsBody == null || roleIdsBody.toString().isBlank()) {
            roleIds = List.of();
        } else {
            roleIds = List.of(Long.parseLong(roleIdsBody.toString()));
        }
        adminService.assignRoles(id, roleIds);
        return Result.success();
    }

    @PutMapping("/change-password")
    public Result<Void> changePassword(HttpServletRequest request, @RequestBody ChangePasswordRequest req) {
        Long adminId = (Long) request.getAttribute("adminId");
        adminService.changePassword(adminId, req.oldPassword(), req.newPassword());
        return Result.success();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}
}
