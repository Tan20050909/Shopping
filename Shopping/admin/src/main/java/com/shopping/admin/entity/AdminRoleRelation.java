package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_admin_role_relation")
public class AdminRoleRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long roleId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
