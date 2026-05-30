package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_withdraw")
public class MerchantWithdraw {
    @TableId(value = "withdraw_id", type = IdType.AUTO)
    private Long id;

    @TableField("withdraw_no")
    private String withdrawNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("withdraw_amount")
    private BigDecimal amount;

    @TableField("bank_name")
    private String bankName;

    @TableField("bank_card")
    private String bankCard;

    @TableField("account_name")
    private String accountName;

    @TableField("withdraw_status")
    private Integer status;

    @TableField("audit_remark")
    private String remark;

    @TableField("apply_time")
    private LocalDateTime createTime;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("success_time")
    private LocalDateTime successTime;
}
