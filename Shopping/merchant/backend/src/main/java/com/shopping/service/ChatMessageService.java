package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.ChatMessage;
import com.shopping.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    public List<ChatMessage> listBySession(Long sessionId, Long beforeId, Integer limit) {
        if (sessionId == null) return List.of();
        int n = limit == null ? 50 : Math.max(1, Math.min(200, limit));
        LambdaQueryWrapper<ChatMessage> w = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getIsDeleted, 0);
        if (beforeId != null && beforeId > 0) {
            w.lt(ChatMessage::getMessageId, beforeId);
        }
        w.orderByDesc(ChatMessage::getMessageId);
        w.last("limit " + n);
        List<ChatMessage> list = list(w);
        Collections.reverse(list);
        return list;
    }

    public int markRead(Long sessionId, Integer receiverType, Long receiverId) {
        if (sessionId == null || receiverType == null || receiverId == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ChatMessage> w = new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getReceiverType, receiverType)
                .eq(ChatMessage::getReceiverId, receiverId)
                .eq(ChatMessage::getIsDeleted, 0)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1)
                .set(ChatMessage::getReadTime, now);
        return baseMapper.update(null, w);
    }
}

