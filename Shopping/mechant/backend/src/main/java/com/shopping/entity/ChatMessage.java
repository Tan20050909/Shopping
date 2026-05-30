package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_chat_message")
public class ChatMessage {
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    private Long sessionId;

    private Integer senderType;

    private Long senderId;

    private Integer receiverType;

    private Long receiverId;

    private Integer messageType;

    private String content;

    private Integer relatedType;

    private Long relatedId;

    private Integer isRead;

    private LocalDateTime readTime;

    private Integer isDeleted;

    private LocalDateTime createTime;
}

