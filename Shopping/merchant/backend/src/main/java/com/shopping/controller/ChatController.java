package com.shopping.controller;

import com.shopping.entity.ChatMessage;
import com.shopping.entity.ChatSession;
import com.shopping.entity.User;
import com.shopping.mapper.UserMapper;
import com.shopping.service.ChatMessageService;
import com.shopping.service.ChatService;
import com.shopping.service.ChatSessionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/session")
    public ChatSession getOrCreateSession(@RequestParam Long merchantId, @RequestParam Long userId) {
        ChatSession s = chatService.getOrCreateSession(userId, merchantId);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数错误");
        }
        return s;
    }

    @GetMapping("/session/list")
    public List<Map<String, Object>> listSessions(@RequestParam Long merchantId) {
        if (merchantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数错误");
        }
        List<ChatSession> sessions = chatSessionService.list(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getMerchantId, merchantId)
                .orderByDesc(ChatSession::getLastMessageTime)
                .orderByDesc(ChatSession::getUpdateTime)
                .orderByDesc(ChatSession::getSessionId));
        if (sessions == null || sessions.isEmpty()) return new ArrayList<>();

        List<Long> userIds = sessions.stream()
                .map(ChatSession::getUserId)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds));
            for (User u : users) {
                if (u != null && u.getId() != null) userMap.put(u.getId(), u);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            if (s == null) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("sessionId", s.getSessionId());
            row.put("userId", s.getUserId());
            row.put("merchantId", s.getMerchantId());
            row.put("lastMessageContent", s.getLastMessageContent());
            row.put("lastMessageTime", s.getLastMessageTime());
            row.put("merchantUnreadCount", s.getMerchantUnreadCount());
            row.put("userUnreadCount", s.getUserUnreadCount());
            row.put("aiPaused", s.getAiPaused());
            row.put("sessionStatus", s.getSessionStatus());
            User u = s.getUserId() == null ? null : userMap.get(s.getUserId());
            row.put("userNickname", u == null ? null : u.getNickname());
            row.put("userAvatar", u == null ? null : u.getAvatar());
            result.add(row);
        }
        return result;
    }

    @GetMapping("/message/list")
    public List<ChatMessage> listMessages(
            @RequestParam Long sessionId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer limit
    ) {
        return chatMessageService.listBySession(sessionId, beforeId, limit);
    }

    @PostMapping("/message/send")
    public ChatMessage sendMessage(@RequestBody Map<String, Object> body) {
        Long sessionId = toLong(body.get("sessionId"));
        Long userId = toLong(body.get("userId"));
        Long merchantId = toLong(body.get("merchantId"));
        Integer senderType = toInt(body.get("senderType"));
        Long senderId = toLong(body.get("senderId"));
        Integer receiverType = toInt(body.get("receiverType"));
        Long receiverId = toLong(body.get("receiverId"));
        Integer messageType = toInt(body.get("messageType"));
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));
        Integer relatedType = toInt(body.get("relatedType"));
        Long relatedId = toLong(body.get("relatedId"));

        ChatMessage msg = chatService.sendMessage(
                sessionId,
                userId,
                merchantId,
                senderType,
                senderId,
                receiverType,
                receiverId,
                messageType,
                content,
                relatedType,
                relatedId
        );
        if (msg == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "发送失败");
        }
        return msg;
    }

    @PostMapping("/read")
    public Map<String, Object> markRead(@RequestParam Long sessionId, @RequestParam Integer receiverType, @RequestParam Long receiverId) {
        int updated = chatService.markRead(sessionId, receiverType, receiverId);
        Map<String, Object> res = new HashMap<>();
        res.put("updated", updated);
        return res;
    }

    @PostMapping("/message/revoke")
    public Map<String, Object> revokeMessage(@RequestBody Map<String, Object> body) {
        Long messageId = toLong(body.get("messageId"));
        Integer senderType = toInt(body.get("senderType"));
        Long senderId = toLong(body.get("senderId"));
        boolean ok = chatService.revokeMessage(messageId, senderType, senderId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "撤回失败（仅支持2分钟内撤回自己发送的消息）");
        }
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        return res;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}
