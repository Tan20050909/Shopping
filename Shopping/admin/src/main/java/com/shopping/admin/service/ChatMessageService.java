package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.common.PageResult;
import com.shopping.admin.entity.ChatMessage;
import com.shopping.admin.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    public PageResult<ChatMessage> listMessages(long current, long size, Long fromId, Long toId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        if (fromId != null) wrapper.eq(ChatMessage::getFromId, fromId);
        if (toId != null) wrapper.eq(ChatMessage::getToId, toId);
        wrapper.orderByDesc(ChatMessage::getCreateTime);
        Page<ChatMessage> page = page(new Page<>(current, size), wrapper);
        return new PageResult<>(page);
    }
}
