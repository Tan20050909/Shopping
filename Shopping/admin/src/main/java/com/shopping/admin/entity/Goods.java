package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods")
public class Goods {
    @TableId(value = "goods_id", type = IdType.AUTO)
    private Long goodsId;
    @TableField("merchant_id")
    private Long merchantId;
    @TableField("cate_id")
    private Long categoryId;
    @TableField("goods_name")
    private String goodsName;
    @TableField("goods_intro")
    private String goodsIntro;
    @TableField("goods_pic")
    private String goodsPic;
    @TableField("goods_video")
    private String goodsVideo;
    @TableField("goods_score")
    private BigDecimal goodsScore;
    @TableField("sell_count")
    private Integer sellCount;
    @TableField("comment_count")
    private Integer commentCount;
    private String keywords;
    @TableField("ship_from")
    private String shipFrom;
    @TableField(exist = false)
    private String goodsDesc;
    @TableField(exist = false)
    private String mainImage;
    @TableField(exist = false)
    private String images;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String merchantName;
    @TableField(exist = false)
    private BigDecimal price;
    @TableField(exist = false)
    private BigDecimal originalPrice;
    @TableField(exist = false)
    private Integer stock;
    @TableField(exist = false)
    private Integer sales;
    @TableField("audit_status")
    private Integer auditStatus;
    @TableField("audit_remark")
    private String auditRemark;
    @TableField(exist = false)
    private String violationType;
    @TableField(exist = false)
    private Integer violationLevel;
    @TableField(exist = false)
    private String aiCheckResult;
    @TableField(exist = false)
    private LocalDateTime aiCheckTime;
    @TableField(exist = false)
    private BigDecimal commissionRate;
    private Integer status;
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
