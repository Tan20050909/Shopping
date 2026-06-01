package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_payment")
public class Payment {
    @TableId(value = "payment_id", type = IdType.AUTO)
    private Long paymentId;
    @TableField("pay_no")
    private String payNo;
    @TableField("group_id")
    private Long groupId;
    @TableField("order_id")
    private Long orderId;
    @TableField("user_id")
    private Long userId;
    @TableField("pay_amount")
    private BigDecimal payAmount;
    @TableField("pay_channel")
    private Integer payChannel;
    @TableField("pay_status")
    private Integer payStatus;
    @TableField("third_trade_no")
    private String thirdTradeNo;
    @TableField("pay_time")
    private LocalDateTime payTime;
    @TableField("expire_time")
    private LocalDateTime expireTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
