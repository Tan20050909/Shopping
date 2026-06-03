package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_order_item")
public class OrderItem {
    @TableId(value = "order_item_id", type = IdType.AUTO)
    private Long id;

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
    private String spec;

    @TableField("goods_pic")
    private String goodsPic;

    /** 当前商品主图（来自 tb_goods.goods_pic），非数据库字段 */
    @TableField(exist = false)
    private String currentGoodsPic;

    /** 商品图集第一张（来自 tb_goods_pic），非数据库字段 */
    @TableField(exist = false)
    private String firstGalleryPic;

    private BigDecimal price;

    @TableField("num")
    private Integer quantity;

    @TableField("total_price")
    private BigDecimal totalPrice;

    @TableField("comment_status")
    private Integer commentStatus;

    @TableField("after_sale_status")
    private Integer afterSaleStatus;

    @TableField("create_time")
    private LocalDateTime createTime;
}
