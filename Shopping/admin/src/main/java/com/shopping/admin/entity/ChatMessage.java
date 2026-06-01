package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long messageId;
    private Long fromId;
    private Long toId;
    private Integer fromType;
    private Integer toType;
    private String content;
    private Integer msgType;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
