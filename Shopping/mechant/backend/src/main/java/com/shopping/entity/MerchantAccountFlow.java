package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_account_flow")
public class MerchantAccountFlow {
    @TableId(value = "flow_id", type = IdType.AUTO)
    private Long id;

    @TableField("flow_no")
    private String flowNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("order_id")
    private Long orderId;

    @TableField("refund_id")
    private Long refundId;

    @TableField("withdraw_id")
    private Long withdrawId;

    @TableField("flow_type")
    private Integer flowType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("freeze_after")
    private BigDecimal freezeAfter;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}

