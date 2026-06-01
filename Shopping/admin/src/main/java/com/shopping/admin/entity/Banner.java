package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long bannerId;
    private String bannerTitle;
    private String imageUrl;
    /** 跳转类型：1-商品详情 2-分类页面 3-外部链接 4-活动页面 5-无跳转 */
    private Integer jumpType;
    private String jumpValue;
    /** 展示位置：1-首页顶部 2-首页中部 3-活动页 4-分类页 */
    private Integer displayPosition;
    private Integer sortNo;
    /** 状态：0-禁用 1-启用 2-待上架(定时) */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
