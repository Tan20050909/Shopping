package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.User;
import com.shopping.admin.entity.UserRiskLog;
import com.shopping.admin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String riskTag,
                          @RequestParam(required = false) Integer userLevel) {
        return Result.success(userService.listUsers(current, size, keyword, status, riskTag, userLevel));
    }

    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    /** 更新风控标签 */
    @PutMapping("/{id}/risk-tag")
    public Result<Void> updateRiskTag(@PathVariable Long id, @RequestParam String riskTag,
                                       @RequestParam String reason, HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("adminId");
        userService.updateRiskTag(id, riskTag, reason, adminId);
        return Result.success();
    }

    /** 用户风控记录 */
    @GetMapping("/risk-logs")
    public Result<PageResult<UserRiskLog>> riskLogs(@RequestParam(required = false) Long userId,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.listRiskLogs(userId, current, size));
    }
}
