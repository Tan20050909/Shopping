package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_sku")
public class GoodsSku {
    @TableId(value = "sku_id", type = IdType.AUTO)
    private Long skuId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("sku_name")
    private String skuName;

    @TableField("spec_params")
    private String specParams;

    private BigDecimal price;

    private Integer stock;

    @TableField("lock_stock")
    private Integer lockStock;

    @TableField("stock_warn")
    private Integer stockWarn;

    @TableField("sku_code")
    private String skuCode;

    private Integer status;

    private Integer version;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
