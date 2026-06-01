package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long activityId;
    private String activityName;
    /** 活动类型：1-限时折扣 2-满减活动 3-拼团活动 4-秒杀活动 5-新人专享 */
    private Integer activityType;
    private String activityDesc;
    /** 适用范围：1-全场 2-指定分类 3-指定商品 */
    private Integer scopeType;
    private String scopeIds;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private BigDecimal maxDiscount;
    /** 成团人数(拼团) */
    private Integer groupRequired;
    /** 拼团价 */
    private BigDecimal groupPrice;
    /** 成团时效(小时) */
    private Integer groupTimeoutHours;
    /** 秒杀库存 */
    private Integer seckillStock;
    /** 每人限购数量 */
    private Integer perLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 状态：0-草稿 1-进行中 2-已结束 3-已取消 */
    private Integer status;
    private BigDecimal budgetAmount;
    private BigDecimal budgetUsed;
    private Long createAdminId;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
