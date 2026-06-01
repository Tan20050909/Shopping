package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_setting")
public class MerchantSetting {
    @TableId(value = "setting_id", type = IdType.AUTO)
    private Long settingId;

    private Long merchantId;

    @TableField("business_hours")
    private String businessHours;

    @TableField("refund_time")
    private Integer refundTime;

    @TableField("exchange_time")
    private Integer exchangeTime;

    private String freightTemplate;

    @TableField("ai_reply_enabled")
    private Integer aiReplyEnabled;

    @TableField("ai_resume_minutes")
    private Integer aiResumeMinutes;

    @TableField(exist = false)
    private String afterSaleRule;

    @TableField(exist = false)
    private String shopDecoration;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private LocalDateTime createTime;
}
