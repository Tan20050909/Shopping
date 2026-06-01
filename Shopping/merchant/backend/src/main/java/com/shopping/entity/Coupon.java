package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("tb_coupon")
public class Coupon {
    @TableId(value = "coupon_id", type = IdType.AUTO)
    private Long id;

    @TableField("coupon_type")
    private Integer couponType;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("coupon_name")
    private String name;

    @TableField("grant_type")
    private Integer grantType;

    @TableField("discount_type")
    private Integer discountType;

    @TableField("denomination")
    private BigDecimal discountAmount;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("min_amount")
    private BigDecimal minAmount;

    @TableField("per_limit")
    private Integer perLimit;

    @TableField("limit_enabled")
    private Integer limitEnabled;

    @TableField("can_stack")
    private Integer canStack;

    @TableField("total_num")
    private Integer totalCount;

    @TableField("surplus_num")
    private Integer surplusNum;

    @TableField(exist = false)
    private Integer usedCount;

    @TableField(exist = false)
    private Integer scopeType;

    @TableField(exist = false)
    private List<Long> targetIds;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("audit_status")
    private Integer auditStatus;

    private Integer status;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

