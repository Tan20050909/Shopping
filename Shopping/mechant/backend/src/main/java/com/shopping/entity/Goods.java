package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods")
public class Goods {
    @TableId(value = "goods_id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("cate_id")
    private Long categoryId;

    @TableField("goods_name")
    private String name;

    @TableField("goods_intro")
    private String description;

    @TableField("ship_from")
    private String shipFrom;

    private Integer status;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("goods_pic")
    private String goodsPic;

    @TableField("goods_video")
    private String goodsVideo;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Long favoriteCount;

    @TableField(exist = false)
    private Boolean favorited;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private Long followerCount;

    @TableField(exist = false)
    private Boolean followed;

    @TableField(exist = false)
    private Long buyCount;
}
