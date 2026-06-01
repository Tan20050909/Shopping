package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_order_item")
public class OrderItem {
    @TableId(value = "order_item_id", type = IdType.AUTO)
    private Long orderItemId;
    @TableField("order_id")
    private Long orderId;
    @TableField("group_id")
    private Long groupId;
    @TableField("merchant_id")
    private Long merchantId;
    @TableField("goods_id")
    private Long goodsId;
    @TableField("sku_id")
    private Long skuId;
    @TableField("goods_name")
    private String goodsName;
    @TableField("sku_name")
    private String skuName;
    @TableField("goods_pic")
    private String goodsPic;
    private BigDecimal price;
    @TableField("num")
    private Integer num;
    @TableField("total_price")
    private BigDecimal totalPrice;
    @TableField("comment_status")
    private Integer commentStatus;
    @TableField("after_sale_status")
    private Integer afterSaleStatus;
    @TableField("create_time")
    private LocalDateTime createTime;
}
