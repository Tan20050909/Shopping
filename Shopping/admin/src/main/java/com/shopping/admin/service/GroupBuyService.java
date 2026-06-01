package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.GroupBuy;
import com.shopping.admin.exception.BusinessException;
import com.shopping.admin.mapper.GroupBuyMapper;
import org.springframework.stereotype.Service;

@Service
public class GroupBuyService extends ServiceImpl<GroupBuyMapper, GroupBuy> {

    public PageResult<GroupBuy> listGroupBuys(long current, long size, Integer groupStatus) {
        LambdaQueryWrapper<GroupBuy> wrapper = new LambdaQueryWrapper<>();
        if (groupStatus != null) wrapper.eq(GroupBuy::getGroupStatus, groupStatus);
        wrapper.orderByDesc(GroupBuy::getCreateTime);
        Page<GroupBuy> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }

    public void updateGroupBuyStatus(Long groupId, Integer groupStatus) {
        GroupBuy gb = getById(groupId);
        if (gb == null) throw new BusinessException("团购活动不存在");
        gb.setGroupStatus(groupStatus);
        updateById(gb);
    }
}
