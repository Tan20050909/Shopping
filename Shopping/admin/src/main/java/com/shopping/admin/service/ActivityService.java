package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.Activity;
import com.shopping.admin.mapper.ActivityMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActivityService extends ServiceImpl<ActivityMapper, Activity> {

    public PageResult<Activity> listActivities(long current, long size, String keyword,
                                                 Integer activityType, Integer status) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) wrapper.like(Activity::getActivityName, keyword);
        if (activityType != null) wrapper.eq(Activity::getActivityType, activityType);
        if (status != null) wrapper.eq(Activity::getStatus, status);
        wrapper.orderByDesc(Activity::getCreateTime);
        Page<Activity> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }
}
