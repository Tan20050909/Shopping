package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_comment")
public class GoodsReview {
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long reviewId;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("user_id")
    private Long userId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("goods_score")
    private Integer rating;

    @TableField("comment_content")
    private String content;

    @TableField("comment_pic")
    private String images;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    @TableField("reply_content")
    private String merchantReply;

    @TableField("reply_time")
    private LocalDateTime replyTime;

    @TableField("is_valid")
    private Integer isValid;

    @TableField("is_top")
    private Integer isTop;

    @TableField("comment_time")
    private LocalDateTime createTime;

    /** 前端展示用：0=显示, 1=隐藏（非数据库字段，由 isValid 推导） */
    @TableField(exist = false)
    private Integer isHidden;
}
