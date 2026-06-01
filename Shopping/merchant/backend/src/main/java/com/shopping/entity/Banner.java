package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_banner")
public class Banner {
    @TableId(value = "banner_id", type = IdType.AUTO)
    private Long bannerId;

    @TableField("banner_title")
    private String bannerTitle;

    @TableField("image_url")
    private String imageUrl;

    @TableField("jump_type")
    private Integer jumpType;

    @TableField("jump_value")
    private String jumpValue;

    @TableField("display_position")
    private Integer displayPosition;

    @TableField("sort_no")
    private Integer sortNo;

    private Integer status;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
