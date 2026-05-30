package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_comment")
public class GoodsComment {
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long id;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("user_id")
    private Long userId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("goods_score")
    private Integer goodsScore;

    @TableField("service_score")
    private Integer serviceScore;

    @TableField("logistics_score")
    private Integer logisticsScore;

    @TableField("comment_content")
    private String content;

    @TableField("comment_pic")
    private String commentPic;

    @TableField("is_top")
    private Integer isTop;

    @TableField("is_valid")
    private Integer isValid;

    @TableField("comment_time")
    private LocalDateTime commentTime;

    @TableField("reply_content")
    private String merchantReply;

    @TableField("reply_time")
    private LocalDateTime replyTime;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    @TableField(exist = false)
    private String userNickname;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private List<String> commentPics = new ArrayList<>();

    @TableField(exist = false)
    private Long appealId;

    @TableField(exist = false)
    private Integer appealStatus;

    @TableField(exist = false)
    private String appealHandleRemark;
}
