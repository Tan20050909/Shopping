package com.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.entity.ChatMessage;
import com.shopping.entity.Order;
import com.shopping.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatAutoCardService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatService chatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 30000)
    public void ensureAddrConfirmCards() {
        LocalDateTime since = LocalDateTime.now().minusDays(3);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, since)
                .orderByDesc(Order::getCreateTime)
                .last("limit 200"));
        if (orders == null || orders.isEmpty()) return;

        for (Order o : orders) {
            if (o == null || o.getId() == null) continue;
            if (o.getMerchantId() == null || o.getUserId() == null) continue;
            boolean paid = (o.getPayStatus() != null && o.getPayStatus() == 1) || o.getPayTime() != null;
            if (!paid) {
                chatMessageService.update(new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getIsDeleted, 0)
                        .eq(ChatMessage::getRelatedType, 2)
                        .eq(ChatMessage::getRelatedId, o.getId())
                        .eq(ChatMessage::getMessageType, 4)
                        .set(ChatMessage::getIsDeleted, 1));
                chatMessageService.update(new LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getIsDeleted, 0)
                        .eq(ChatMessage::getMessageType, 5)
                        .like(ChatMessage::getContent, "\"type\":\"addr_confirm\"")
                        .like(ChatMessage::getContent, "\"orderId\":" + o.getId())
                        .set(ChatMessage::getIsDeleted, 1));
                continue;
            }

            long exists = chatMessageService.count(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getRelatedType, 2)
                    .eq(ChatMessage::getRelatedId, o.getId())
                    .eq(ChatMessage::getMessageType, 4)
                    .eq(ChatMessage::getIsDeleted, 0));
            if (exists > 0) continue;

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "addr_confirm");
            payload.put("orderId", o.getId());
            payload.put("orderNo", o.getOrderNo());
            payload.put("consignee", o.getConsignee());
            payload.put("phone", o.getConsigneePhone());
            payload.put("addr", o.getReceiveAddr());
            payload.put("payAmount", o.getPayAmount());
            payload.put("totalAmount", o.getTotalAmount());
            String content;
            try {
                content = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                content = "{\"type\":\"addr_confirm\"}";
            }

            chatService.sendMessage(
                    null,
                    o.getUserId(),
                    o.getMerchantId(),
                    4,
                    null,
                    1,
                    o.getUserId(),
                    4,
                    content,
                    2,
                    o.getId()
            );
        }
    }
}
