package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_category")
public class Category {
    @TableId(value = "cate_id", type = IdType.AUTO)
    private Long categoryId;

    @TableField("parent_cate_id")
    private Long parentId;

    @TableField("cate_name")
    private String categoryName;

    @TableField("cate_sort")
    private Integer sortNo;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
