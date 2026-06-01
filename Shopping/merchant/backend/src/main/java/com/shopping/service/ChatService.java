package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shopping.entity.ChatMessage;
import com.shopping.entity.ChatSession;
import com.shopping.entity.MerchantSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class ChatService {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private MerchantSettingService merchantSettingService;

    @Autowired
    private AiChatReplyService aiChatReplyService;

    public ChatSession getOrCreateSession(Long userId, Long merchantId) {
        return chatSessionService.getOrCreate(userId, merchantId);
    }

    public ChatMessage sendMessage(
            Long sessionId,
            Long userId,
            Long merchantId,
            Integer senderType,
            Long senderId,
            Integer receiverType,
            Long receiverId,
            Integer messageType,
            String content,
            Integer relatedType,
            Long relatedId
    ) {
        if (senderType == null || receiverType == null || receiverId == null) return null;

        LocalDateTime now = LocalDateTime.now();
        ChatSession session;
        if (sessionId != null && sessionId > 0) {
            session = chatSessionService.getById(sessionId);
        } else {
            session = chatSessionService.getOrCreate(userId, merchantId);
        }
        if (session == null) return null;

        MerchantSetting setting = merchantId == null ? null : merchantSettingService.getByMerchantId(merchantId);
        int resumeMinutes = setting != null && setting.getAiResumeMinutes() != null ? setting.getAiResumeMinutes() : 30;
        resumeMinutes = Math.max(1, Math.min(24 * 60, resumeMinutes));

        Integer aiPaused = session.getAiPaused();
        if (aiPaused == null) aiPaused = 0;

        if (senderType == 1 && aiPaused == 1) {
            LocalDateTime last = session.getLastMessageTime();
            if (last != null) {
                long idleMin = Duration.between(last, now).toMinutes();
                if (idleMin >= resumeMinutes) {
                    session.setAiPaused(0);
                    chatSessionService.update(new LambdaUpdateWrapper<ChatSession>()
                            .eq(ChatSession::getSessionId, session.getSessionId())
                            .set(ChatSession::getAiPaused, 0)
                            .set(ChatSession::getUpdateTime, now));
                    aiPaused = 0;
                }
            }
        }

        Integer msgType = messageType == null ? 1 : messageType;
        if (msgType == 4 && relatedType != null && relatedId != null) {
            ChatMessage existed = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, session.getSessionId())
                    .eq(ChatMessage::getMessageType, 4)
                    .eq(ChatMessage::getRelatedType, relatedType)
                    .eq(ChatMessage::getRelatedId, relatedId)
                    .eq(ChatMessage::getIsDeleted, 0)
                    .orderByDesc(ChatMessage::getMessageId)
                    .last("limit 1"));
            if (existed != null) {
                return existed;
            }
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(session.getSessionId());
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setReceiverType(receiverType);
        msg.setReceiverId(receiverId);
        msg.setMessageType(msgType);
        msg.setContent(content == null ? "" : content);
        msg.setRelatedType(relatedType);
        msg.setRelatedId(relatedId);
        msg.setIsRead(0);
        msg.setIsDeleted(0);
        msg.setCreateTime(now);
        chatMessageService.save(msg);

        String summary = summarize(msg);
        LambdaUpdateWrapper<ChatSession> upd = new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, session.getSessionId())
                .set(ChatSession::getLastMessageId, msg.getMessageId())
                .set(ChatSession::getLastMessageContent, summary)
                .set(ChatSession::getLastMessageTime, now)
                .set(ChatSession::getUpdateTime, now);
        if (receiverType == 1) {
            upd.setSql("user_unread_count = user_unread_count + 1");
        } else if (receiverType == 2) {
            upd.setSql("merchant_unread_count = merchant_unread_count + 1");
        }
        if (senderType == 2) {
            upd.set(ChatSession::getAiPaused, 1);
            aiPaused = 1;
        }
        chatSessionService.update(upd);

        boolean aiEnabled = setting != null && setting.getAiReplyEnabled() != null && setting.getAiReplyEnabled() == 1;
        if (aiEnabled && senderType == 1 && aiPaused == 0 && msgType == 1) {
            String reply = aiChatReplyService.generateReply(merchantId, content, setting);
            if (reply != null && !reply.isBlank()) {
                sendMessageInternal(
                        session.getSessionId(),
                        userId,
                        merchantId,
                        3,
                        null,
                        1,
                        userId,
                        1,
                        reply,
                        null,
                        null,
                        false
                );
            }
        }
        return msg;
    }

    public int markRead(Long sessionId, Integer receiverType, Long receiverId) {
        if (sessionId == null || receiverType == null || receiverId == null) return 0;
        int updated = chatMessageService.markRead(sessionId, receiverType, receiverId);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ChatSession> w = new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .set(ChatSession::getUpdateTime, now);
        if (receiverType == 1) {
            w.set(ChatSession::getUserUnreadCount, 0);
        } else if (receiverType == 2) {
            w.set(ChatSession::getMerchantUnreadCount, 0);
        }
        chatSessionService.update(w);
        return updated;
    }

    public boolean revokeMessage(Long messageId, Integer senderType, Long senderId) {
        if (messageId == null || messageId <= 0) return false;
        if (senderType == null) return false;
        ChatMessage msg = chatMessageService.getById(messageId);
        if (msg == null || msg.getIsDeleted() != null && msg.getIsDeleted() == 1) return false;
        if (!Objects.equals(msg.getSenderType(), senderType)) return false;
        if (senderType != 2 || senderId == null || !Objects.equals(msg.getSenderId(), senderId)) return false;
        LocalDateTime ct = msg.getCreateTime();
        if (ct == null) return false;
        if (Duration.between(ct, LocalDateTime.now()).toSeconds() > 120) return false;

        boolean removed = chatMessageService.update(new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getMessageId, messageId)
                .eq(ChatMessage::getSenderType, senderType)
                .eq(ChatMessage::getSenderId, senderId)
                .eq(ChatMessage::getIsDeleted, 0)
                .set(ChatMessage::getIsDeleted, 1));
        if (!removed) return false;

        Long sessionId = msg.getSessionId();
        if (sessionId != null) {
            if (msg.getIsRead() != null && msg.getIsRead() == 0) {
                LambdaUpdateWrapper<ChatSession> dec = new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
                        .set(ChatSession::getUpdateTime, LocalDateTime.now());
                if (msg.getReceiverType() != null && msg.getReceiverType() == 1) {
                    dec.setSql("user_unread_count = GREATEST(user_unread_count - 1, 0)");
                } else if (msg.getReceiverType() != null && msg.getReceiverType() == 2) {
                    dec.setSql("merchant_unread_count = GREATEST(merchant_unread_count - 1, 0)");
                }
                chatSessionService.update(dec);
            }

            ChatMessage last = chatMessageService.getOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sessionId)
                    .eq(ChatMessage::getIsDeleted, 0)
                    .orderByDesc(ChatMessage::getCreateTime)
                    .orderByDesc(ChatMessage::getMessageId)
                    .last("limit 1"));
            if (last == null) {
                chatSessionService.update(new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
                        .set(ChatSession::getLastMessageId, null)
                        .set(ChatSession::getLastMessageContent, null)
                        .set(ChatSession::getLastMessageTime, null)
                        .set(ChatSession::getUpdateTime, LocalDateTime.now()));
            } else {
                chatSessionService.update(new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
                        .set(ChatSession::getLastMessageId, last.getMessageId())
                        .set(ChatSession::getLastMessageContent, summarize(last))
                        .set(ChatSession::getLastMessageTime, last.getCreateTime())
                        .set(ChatSession::getUpdateTime, LocalDateTime.now()));
            }
        }
        return true;
    }

    private ChatMessage sendMessageInternal(
            Long sessionId,
            Long userId,
            Long merchantId,
            Integer senderType,
            Long senderId,
            Integer receiverType,
            Long receiverId,
            Integer messageType,
            String content,
            Integer relatedType,
            Long relatedId,
            boolean triggerAi
    ) {
        if (!triggerAi) {
            ChatSession session = sessionId == null ? null : chatSessionService.getById(sessionId);
            if (session == null && (userId == null || merchantId == null)) return null;
            if (session == null) {
                session = chatSessionService.getOrCreate(userId, merchantId);
            }
            if (session == null) return null;

            LocalDateTime now = LocalDateTime.now();
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(session.getSessionId());
            msg.setSenderType(senderType);
            msg.setSenderId(senderId);
            msg.setReceiverType(receiverType);
            msg.setReceiverId(receiverId);
            msg.setMessageType(messageType == null ? 1 : messageType);
            msg.setContent(content == null ? "" : content);
            msg.setRelatedType(relatedType);
            msg.setRelatedId(relatedId);
            msg.setIsRead(0);
            msg.setIsDeleted(0);
            msg.setCreateTime(now);
            chatMessageService.save(msg);

            String summary = summarize(msg);
            LambdaUpdateWrapper<ChatSession> upd = new LambdaUpdateWrapper<ChatSession>()
                    .eq(ChatSession::getSessionId, session.getSessionId())
                    .set(ChatSession::getLastMessageId, msg.getMessageId())
                    .set(ChatSession::getLastMessageContent, summary)
                    .set(ChatSession::getLastMessageTime, now)
                    .set(ChatSession::getUpdateTime, now);
            if (receiverType == 1) {
                upd.setSql("user_unread_count = user_unread_count + 1");
            } else if (receiverType == 2) {
                upd.setSql("merchant_unread_count = merchant_unread_count + 1");
            }
            chatSessionService.update(upd);
            return msg;
        }
        return sendMessage(sessionId, userId, merchantId, senderType, senderId, receiverType, receiverId, messageType, content, relatedType, relatedId);
    }

    private String summarize(ChatMessage msg) {
        int t = msg.getMessageType() == null ? 1 : msg.getMessageType();
        String c = msg.getContent() == null ? "" : msg.getContent().trim();
        if (t == 2) return "【图片】";
        if (t == 3) return "【卡片】";
        if (t == 4) return "【订单】";
        if (t == 5) return "【消息】";
        if (c.isEmpty()) return "【消息】";
        if (c.length() <= 60) return c;
        return c.substring(0, 60);
    }
}
