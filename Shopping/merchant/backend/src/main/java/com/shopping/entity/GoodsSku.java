package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_sku")
public class GoodsSku {
    @TableId(value = "sku_id", type = IdType.AUTO)
    private Long id;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("sku_name")
    private String spec;

    @TableField("spec_params")
    private String specParams;

    private BigDecimal price;

    private Integer stock;

    @TableField("lock_stock")
    private Integer lockStock;

    @TableField("sku_code")
    private String skuCode;

    private Integer status;

    @TableField(exist = false)
    private Integer warningStock;

    @TableField(exist = false)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;
}
