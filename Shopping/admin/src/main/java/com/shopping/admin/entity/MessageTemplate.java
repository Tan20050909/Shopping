package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_message_template")
public class MessageTemplate {
    @TableId(type = IdType.AUTO)
    private Long templateId;
    private String templateName;
    private String triggerEvent;
    private String templateContent;
    private String channels;
    /** 接收人类型：1-商家 2-用户 3-管理员 */
    private Integer targetType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
