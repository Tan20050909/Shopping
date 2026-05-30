package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_coupon_scope")
public class CouponScope {
    @TableId(value = "scope_id", type = IdType.AUTO)
    private Long id;

    @TableField("coupon_id")
    private Long couponId;

    @TableField("scope_type")
    private Integer scopeType;

    @TableField("target_id")
    private Long targetId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
