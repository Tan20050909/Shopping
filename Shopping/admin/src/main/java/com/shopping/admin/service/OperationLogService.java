package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.OperationLog;
import com.shopping.admin.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {

    public PageResult<OperationLog> listLogs(long current, long size, String module, String operationType,
                                             Long adminId, String keyword, Integer status) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getOperationModule, module);
        }
        // operationType 已不存在于表中，改为模糊匹配 operationContent
        if (adminId != null) {
            wrapper.eq(OperationLog::getOperatorId, adminId);
        }
        if (status != null) {
            wrapper.eq(OperationLog::getOperationResult, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getOperationModule, keyword)
                    .or().like(OperationLog::getOperationContent, keyword));
        }
        wrapper.orderByDesc(OperationLog::getOperationTime);
        Page<OperationLog> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    /**
     * 保存操作日志
     * operationType/requestUrl/requestMethod/requestParams/errorMsg 拼接进 operationContent
     */
    public void saveLog(Long adminId, String adminName, String module, String operationType,
                        String operationDesc, String requestUrl, String requestMethod,
                        String requestParams, String ip, Integer status, String errorMsg) {
        OperationLog logEntry = new OperationLog();
        Long finalAdminId = adminId != null ? adminId : 1L;
        logEntry.setOperatorId(finalAdminId);
        logEntry.setOperatorType(3); // 3=平台管理员
        logEntry.setOperationModule(module);
        logEntry.setOperationIp(ip != null ? ip : "127.0.0.1");
        logEntry.setOperationResult(status != null ? status : 1);
        logEntry.setOperationTime(java.time.LocalDateTime.now());

        // 将 operationType/requestUrl/requestMethod/requestParams/errorMsg 拼接进 operationContent
        StringBuilder content = new StringBuilder();
        if (StringUtils.hasText(operationType)) {
            content.append("[").append(operationType).append("] ");
        }
        if (StringUtils.hasText(operationDesc)) {
            content.append(operationDesc);
        }
        if (StringUtils.hasText(requestUrl)) {
            content.append(" | URL: ").append(requestUrl);
        }
        if (StringUtils.hasText(requestMethod)) {
            content.append(" | Method: ").append(requestMethod);
        }
        if (StringUtils.hasText(requestParams)) {
            content.append(" | Params: ").append(requestParams);
        }
        if (StringUtils.hasText(errorMsg)) {
            content.append(" | Error: ").append(errorMsg);
        }
        logEntry.setOperationContent(content.toString());

        // 以下字段仅供 Java 层使用，不会写入数据库
        logEntry.setAdminName(adminName != null ? adminName : resolveAdminName(finalAdminId));

        save(logEntry);
    }

    public void saveBizLog(Long adminId, String module, String operationType, String operationDesc,
                           String requestUrl, String requestMethod, String requestParams) {
        saveLog(adminId, resolveAdminName(adminId), module, operationType, operationDesc,
                requestUrl, requestMethod, requestParams, "127.0.0.1", 1, null);
    }

    public void saveBizLog(Long adminId, String module, String operationType, String operationDesc,
                           String requestUrl, String requestMethod, String requestParams, String ip) {
        saveLog(adminId, resolveAdminName(adminId), module, operationType, operationDesc,
                requestUrl, requestMethod, requestParams, ip != null ? ip : "127.0.0.1", 1, null);
    }

    public void saveBizFailLog(Long adminId, String module, String operationType, String operationDesc,
                               String requestUrl, String requestMethod, String requestParams, String errorMsg) {
        saveLog(adminId, resolveAdminName(adminId), module, operationType, operationDesc,
                requestUrl, requestMethod, requestParams, "127.0.0.1", 0, errorMsg);
    }

    /** 从HttpServletRequest中获取真实IP */
    public static String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }

    private String resolveAdminName(Long adminId) {
        if (adminId == null) return "系统";
        if (adminId == 1L) return "超级管理员";
        return "管理员" + adminId;
    }
}
