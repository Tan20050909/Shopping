package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_cart")
public class Cart {
    @TableId(type = IdType.AUTO)
    private Long cartId;
    private Long userId;
    private Long goodsId;
    private Integer quantity;
    private Integer checked;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
