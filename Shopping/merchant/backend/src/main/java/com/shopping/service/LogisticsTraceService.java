package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.LogisticsTrace;
import com.shopping.mapper.LogisticsTraceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogisticsTraceService extends ServiceImpl<LogisticsTraceMapper, LogisticsTrace> {

    public List<LogisticsTrace> listByLogisticsId(Long logisticsId) {
        if (logisticsId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<LogisticsTrace>()
                .eq(LogisticsTrace::getLogisticsId, logisticsId)
                .orderByDesc(LogisticsTrace::getTraceTime)
                .orderByDesc(LogisticsTrace::getId));
    }

    public boolean addTrace(Long logisticsId, String content, String location, LocalDateTime time) {
        if (logisticsId == null) {
            return false;
        }
        String c = content == null ? "" : content.trim();
        if (c.isBlank()) {
            return false;
        }
        LogisticsTrace trace = new LogisticsTrace();
        trace.setLogisticsId(logisticsId);
        trace.setTraceContent(c);
        trace.setTraceLocation(location == null || location.trim().isBlank() ? null : location.trim());
        trace.setTraceTime(time == null ? LocalDateTime.now() : time);
        return save(trace);
    }
}
