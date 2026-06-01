package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_finance")
public class MerchantFinance {
    @TableId(value = "finance_id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("shop_balance")
    private BigDecimal balance;

    @TableField("unsettle_amount")
    private BigDecimal unsettledAmount;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("freeze_amount")
    private BigDecimal freezeAmount;

    @TableField("version")
    private Integer version;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
