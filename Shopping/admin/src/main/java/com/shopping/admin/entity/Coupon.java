package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long couponId;
    private String couponName;
    private Integer couponType;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    /** 适用范围：1-全场通用 2-指定分类 3-指定商品 */
    private Integer scopeType;
    /** 适用范围ID列表（分类ID或商品ID，逗号分隔） */
    private String scopeIds;
    /** 每人限领数量，0表示不限 */
    private Integer perLimit;
    /** 使用说明 */
    private String usageDesc;
    /** 防刷规则JSON */
    @TableField("anti_fraud_rules")
    private String antiFraudRules;
    /** 预算金额 */
    private BigDecimal budgetAmount;
    /** 已消耗预算 */
    private BigDecimal budgetUsed;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
