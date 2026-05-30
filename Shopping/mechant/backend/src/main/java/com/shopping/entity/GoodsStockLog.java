package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_goods_stock_log")
public class GoodsStockLog {
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("old_stock")
    private Integer oldStock;

    @TableField("new_stock")
    private Integer newStock;

    @TableField("change_stock")
    private Integer changeStock;

    @TableField("create_time")
    private LocalDateTime createTime;
}

