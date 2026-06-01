package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_group_buy")
public class GroupBuy {
    @TableId(type = IdType.AUTO)
    private Long groupId;
    private Long goodsId;
    private BigDecimal groupPrice;
    private Integer groupRequired;
    private Integer groupStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
