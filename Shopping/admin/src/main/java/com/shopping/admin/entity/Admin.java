package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_platform_admin")
public class Admin {
    @TableId(value = "admin_id", type = IdType.AUTO)
    private Long adminId;

    @TableField("admin_name")
    private String username;

    private String password;

    @TableField("real_name")
    private String realName;

    private String phone;

    @TableField(exist = false)
    private String email;

    @TableField(exist = false)
    private String avatar;

    private Integer status;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("login_ip")
    private String loginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
