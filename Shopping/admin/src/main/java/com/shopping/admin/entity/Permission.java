package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long permissionId;
    private String permissionName;
    private String permissionCode;
    /** 所属模块 */
    @TableField(exist = false)
    private String module;
    /** 类型：1-菜单 2-按钮 3-数据 */
    private Integer permissionType;
    private Long parentId;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
