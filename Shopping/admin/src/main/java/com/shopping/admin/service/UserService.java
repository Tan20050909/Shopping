package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.User;
import com.shopping.admin.entity.UserRiskLog;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.UserMapper;
import com.shopping.admin.mapper.UserRiskLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserRiskLogMapper userRiskLogMapper;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;

    public PageResult<User> listUsers(long current, long size, String keyword, Integer status,
                                       String riskTag, Integer userLevel) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (status != null) wrapper.eq(User::getStatus, status);
        if (StringUtils.hasText(riskTag)) wrapper.eq(User::getRiskTag, riskTag);
        if (userLevel != null) wrapper.eq(User::getUserLevel, userLevel);
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    public void updateUserStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        if (status == 0) user.setRiskTag("FROZEN");
        try {
            updateById(user);
        } catch (Exception e) {
            throw new BusinessException("更新用户状态失败: " + e.getMessage());
        }
        try {
            operationLogService.saveBizLog(null, "用户管理", "STATUS",
                    (status == 1 ? "启用用户：" : "禁用用户：") + user.getUsername(),
                    "/api/user/" + userId + "/status", "PUT", "status=" + status);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
        if (status == 0) {
            try {
                notificationService.createBusinessNotification(1L, 4, "用户冻结提醒",
                        "用户" + "\"" + user.getUsername() + "\"" + "已被禁用/冻结，请关注风险", "user", userId);
            } catch (Exception e) {
                log.warn("创建通知失败: {}", e.getMessage());
            }
        }
    }

    /** 更新用户风控标签 */
    public void updateRiskTag(Long userId, String riskTag, String reason, Long adminId) {
        User user = getById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        String oldTag = user.getRiskTag();
        user.setRiskTag(riskTag);
        updateById(user);

        UserRiskLog log = new UserRiskLog();
        log.setUserId(userId);
        log.setRiskType(riskTag);
        log.setRiskDesc(reason);
        log.setRiskLevel("BRUSH_SUSPECT".equals(riskTag) || "FROZEN".equals(riskTag) ? 3 : 1);
        log.setHandleStatus(1);
        log.setHandleAdminId(adminId);
        log.setHandleResult("标记为" + riskTag);
        userRiskLogMapper.insert(log);
        operationLogService.saveBizLog(adminId, "用户风控", "RISK_TAG",
                "用户" + "\"" + user.getUsername() + "\"" + "风控标签由" + oldTag + "调整为" + riskTag,
                "/api/user/" + userId + "/risk-tag", "PUT", "riskTag=" + riskTag + ",reason=" + reason);
        notificationService.createBusinessNotification(1L, 4, "用户风控标签变更",
                "用户" + "\"" + user.getUsername() + "\"" + "已被标记为" + riskTag + "，原因：" + reason, "user", userId);
    }

    /** 获取用户风控记录 */
    public PageResult<UserRiskLog> listRiskLogs(Long userId, long current, long size) {
        LambdaQueryWrapper<UserRiskLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(UserRiskLog::getUserId, userId);
        wrapper.orderByDesc(UserRiskLog::getCreateTime);
        Page<UserRiskLog> page = userRiskLogMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }
}
