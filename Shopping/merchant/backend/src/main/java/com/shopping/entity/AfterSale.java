package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_after_sale")
public class AfterSale {
    @TableId(value = "after_sale_id", type = IdType.AUTO)
    private Long id;

    @TableField("after_sale_no")
    private String afterSaleNo;

    @TableField("group_id")
    private Long groupId;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("user_id")
    private Long userId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("after_sale_type")
    private Integer type;

    @TableField("apply_reason")
    private String reason;

    @TableField("apply_evidence")
    private String evidence;

    @TableField("apply_amount")
    private BigDecimal refundAmount;

    @TableField("handle_status")
    private Integer status;

    @TableField("merchant_remark")
    private String merchantRemark;

    @TableField("platform_remark")
    private String platformRemark;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("apply_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 以下为商品图片字段（非数据库字段，由服务层填充） */
    @TableField(exist = false)
    private String goodsPic;
    @TableField(exist = false)
    private String currentGoodsPic;
    @TableField(exist = false)
    private String firstGalleryPic;
    @TableField(exist = false)
    private String displayPic;
}
