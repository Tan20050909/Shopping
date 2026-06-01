package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_refund")
public class Refund {
    @TableId(value = "refund_id", type = IdType.AUTO)
    private Long refundId;
    @TableField("refund_no")
    private String refundNo;
    @TableField("payment_id")
    private Long paymentId;
    @TableField("group_id")
    private Long groupId;
    @TableField("order_id")
    private Long orderId;
    @TableField("after_sale_id")
    private Long afterSaleId;
    @TableField("user_id")
    private Long userId;
    @TableField("merchant_id")
    private Long merchantId;
    @TableField("refund_amount")
    private BigDecimal refundAmount;
    @TableField("refund_status")
    private Integer refundStatus;
    @TableField("refund_channel")
    private Integer refundChannel;
    @TableField("third_refund_no")
    private String thirdRefundNo;
    private String reason;
    @TableField("apply_time")
    private LocalDateTime applyTime;
    @TableField("success_time")
    private LocalDateTime successTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
