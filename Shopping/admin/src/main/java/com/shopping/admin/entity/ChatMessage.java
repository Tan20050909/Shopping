package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_chat_message")
public class ChatMessage {
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;
    /** 发送者ID，映射到实际列 sender_id */
    @TableField("sender_id")
    private Long fromId;
    /** 接收者ID，映射到实际列 receiver_id */
    @TableField("receiver_id")
    private Long toId;
    /** 发送者类型，映射到实际列 sender_type */
    @TableField("sender_type")
    private Integer fromType;
    /** 接收者类型，映射到实际列 receiver_type */
    @TableField("receiver_type")
    private Integer toType;
    /** 会话ID，映射到实际列 session_id（该字段 NOT NULL 无默认值，保存时必须提供） */
    @TableField("session_id")
    private Long sessionId;
    private String content;
    /** 消息类型，映射到实际列 message_type */
    @TableField("message_type")
    private Integer msgType;
    @TableField("is_read")
    private Integer isRead;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
