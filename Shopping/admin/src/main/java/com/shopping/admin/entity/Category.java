package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long categoryId;
    private String categoryName;
    private Long parentId;
    private Integer level;
    private String icon;
    private Integer sortNo;
    private Integer status;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
