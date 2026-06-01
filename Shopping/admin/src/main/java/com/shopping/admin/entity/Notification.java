package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long notificationId;
    private Long adminId;
    private String title;
    private String content;
    /** 通知类型：1-系统通知 2-待办提醒 3-审核通知 4-预警通知 */
    private Integer type;
    /** 关联业务ID */
    @TableField("related_id")
    private Long bizId;
    /** 关联业务类型：merchant/goods/order/after_sale/dispute/abnormal */
    @TableField("related_type")
    private String bizType;
    /** 是否已读：0-未读 1-已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
