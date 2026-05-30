package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_live_goods")
public class LiveGoods {
    @TableId(value = "lg_id", type = IdType.AUTO)
    private Long id;

    @TableField("live_id")
    private Long liveId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("live_price")
    private BigDecimal livePrice;

    @TableField("goods_sort")
    private Integer sort;

    @TableField("is_on_shelf")
    private Integer isOnShelf;

    @TableField(exist = false)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String goodsName;

    @TableField(exist = false)
    private String goodsPic;

    @TableField(exist = false)
    private String skuSpec;

    @TableField(exist = false)
    private BigDecimal originPrice;
}
