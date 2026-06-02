package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_coupon")
public class Coupon {
    @TableId(value = "coupon_id", type = IdType.AUTO)
    private Long couponId;
    /** 优惠券名称，映射到实际列 coupon_name */
    @TableField("coupon_name")
    private String couponName;
    /** 优惠券类型：1-满减 2-折扣 3-无门槛 */
    private Integer couponType;
    /** 优惠金额/折扣值，映射到实际列 denomination（折扣券在 discount_rate 列） */
    @TableField("denomination")
    private BigDecimal discountValue;
    /** 满减门槛 */
    private BigDecimal minAmount;
    /** 发放总量，映射到实际列 total_num */
    @TableField("total_num")
    private Integer totalCount;
    /** 已领取数量，表中不存在（表仅有 surplus_num = 剩余数量） */
    @TableField(exist = false)
    private Integer receivedCount;
    /** 已使用数量，表中不存在 */
    @TableField(exist = false)
    private Integer usedCount;
    /** 适用范围，表中不存在 */
    @TableField(exist = false)
    private Integer scopeType;
    /** 适用范围ID列表，表中不存在 */
    @TableField(exist = false)
    private String scopeIds;
    /** 每人限领数量 */
    private Integer perLimit;
    /** 使用说明，表中不存在 */
    @TableField(exist = false)
    private String usageDesc;
    /** 防刷规则JSON，表中不存在 */
    @TableField(exist = false)
    private String antiFraudRules;
    /** 预算金额，表中不存在 */
    @TableField(exist = false)
    private BigDecimal budgetAmount;
    /** 已消耗预算，表中不存在 */
    @TableField(exist = false)
    private BigDecimal budgetUsed;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
