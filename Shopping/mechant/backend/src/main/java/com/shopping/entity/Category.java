package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_category")
public class Category {
    @TableId(value = "cate_id", type = IdType.AUTO)
    private Long id;

    @TableField("cate_name")
    private String name;

    @TableField("parent_cate_id")
    private Long parentId;

    @TableField("cate_sort")
    private Integer sort;

    @TableField("status")
    private Integer status;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
