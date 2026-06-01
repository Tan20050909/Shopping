package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.entity.ChatSession;
import com.shopping.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ChatSessionService extends ServiceImpl<ChatSessionMapper, ChatSession> {

    public ChatSession getOrCreate(Long userId, Long merchantId) {
        if (userId == null || merchantId == null) return null;
        ChatSession existed = getOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getMerchantId, merchantId));
        if (existed != null) return existed;
        ChatSession created = new ChatSession();
        created.setUserId(userId);
        created.setMerchantId(merchantId);
        created.setSessionStatus(1);
        created.setAiPaused(0);
        created.setUserUnreadCount(0);
        created.setMerchantUnreadCount(0);
        created.setCreateTime(LocalDateTime.now());
        created.setUpdateTime(LocalDateTime.now());
        save(created);
        return created;
    }
}
