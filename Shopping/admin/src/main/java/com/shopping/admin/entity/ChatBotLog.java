package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_chat_bot_log")
public class ChatBotLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long adminId;
    private String question;
    private String answer;
    private String category;
    private Integer helpful;
    private LocalDateTime createTime;
}
