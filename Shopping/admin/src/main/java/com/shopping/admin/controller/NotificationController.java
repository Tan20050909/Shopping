package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.Notification;
import com.shopping.admin.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<PageResult<Notification>> list(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(notificationService.listNotifications(adminId, type, isRead, current, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount(@RequestParam(required = false) Long adminId) {
        long count = notificationService.getUnreadCount(adminId);
        return Result.success(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam(required = false) Long adminId) {
        notificationService.markAllRead(adminId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return Result.success();
    }
}
