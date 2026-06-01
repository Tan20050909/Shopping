package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_goods_comment_appeal")
public class GoodsCommentAppeal {
    @TableId(value = "appeal_id", type = IdType.AUTO)
    private Long id;

    @TableField("comment_id")
    private Long commentId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("appeal_reason")
    private String appealReason;

    @TableField("appeal_evidence")
    private String appealEvidence;

    @TableField("appeal_status")
    private Integer appealStatus;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("handle_time")
    private LocalDateTime handleTime;
}

