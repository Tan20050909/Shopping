package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_chat_session")
public class ChatSession {
    @TableId(value = "session_id", type = IdType.AUTO)
    private Long sessionId;

    private Long userId;

    private Long merchantId;

    private Integer sessionStatus;

    @TableField("ai_paused")
    private Integer aiPaused;

    private Long lastMessageId;

    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    private Integer userUnreadCount;

    private Integer merchantUnreadCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
